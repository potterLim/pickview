package com.pickview.community;

import jakarta.validation.constraints.NotNull;

import com.pickview.api.ApiFailure;
import com.pickview.commerce.CommerceService;
import com.pickview.catalog.CatalogService;
import com.pickview.model.Account;
import com.pickview.model.Engagement;
import com.pickview.model.Ticket;
import com.pickview.model.Notice;
import com.pickview.model.OrderLine;
import com.pickview.model.Product;
import com.pickview.repository.IEngagementRepository;
import com.pickview.repository.ITicketRepository;
import com.pickview.repository.INoticeRepository;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.repository.IAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {
    private final IEngagementRepository mEngagements;
    private final ITicketRepository mTickets;
    private final INoticeRepository mNotices;
    private final IOrderLineRepository mLines;
    private final IAccountRepository mAccounts;
    private final CommerceService mCommerce;
    private final CatalogService mCatalog;

    public CommunityService(IEngagementRepository engagements, ITicketRepository tickets, INoticeRepository notices,
                            IOrderLineRepository lines, IAccountRepository accounts, CommerceService commerce, CatalogService catalog) {
        mEngagements = engagements; mTickets = tickets; mNotices = notices; mLines = lines; mAccounts = accounts;
        mCommerce = commerce; mCatalog = catalog;
    }

    public List<EngagementView> listActivity(String userId) {
        return mEngagements.findAll().stream().filter(item -> item.getUserId().equals(userId)).map(this::describeActivity).toList();
    }

    public List<EngagementView> listReviews(String productId) {
        return mEngagements.findAll().stream().filter(item -> item.getTargetId().equals(productId) && item.getKind().equals("REVIEW"))
                .map(this::describeActivity).toList();
    }

    @Transactional
    public void saveActivity(Account account, ActivityRequest request) {
        if (!List.of("WISHLIST", "FOLLOW", "BLOCK", "PROGRESS", "REVIEW", "CART", "NOTIFY").contains(request.kind())
                || request.content().length() > 2000 || !Double.isFinite(request.numberValue())) { throw new ApiFailure(400, "Invalid activity"); }
        if (List.of("FOLLOW", "BLOCK", "NOTIFY").contains(request.kind())) {
            if (mAccounts.findById(request.targetId()).isEmpty() || account.getId().equals(request.targetId())) { throw new ApiFailure(400, "Invalid account target"); }
        } else { mCatalog.requireProduct(request.targetId()); }
        if (request.kind().equals("PROGRESS")) {
            if (!mCommerce.canWatch(account.getId(), request.targetId())) { throw new ApiFailure(403, "구매 후 이용 가능합니다. / Purchase required."); }
            if (request.numberValue() < 0 || request.numberValue() > mCatalog.requireProduct(request.targetId()).getDurationSeconds()) {
                throw new ApiFailure(400, "Invalid playback position");
            }
        }
        if (request.kind().equals("REVIEW")) {
            boolean hasPurchase = mLines.findAll().stream().anyMatch(line -> line.getBuyerId().equals(account.getId())
                    && line.getProductId().equals(request.targetId()) && !line.isRefunded());
            if (!hasPurchase || request.numberValue() < 1 || request.numberValue() > 5 || request.content().isBlank()) {
                throw new ApiFailure(403, "구매자만 1~5점 후기를 작성할 수 있습니다. / Verified purchase required.");
            }
        }
        Engagement item = mEngagements.findAll().stream().filter(entry -> entry.getUserId().equals(account.getId())
                && entry.getKind().equals(request.kind()) && entry.getTargetId().equals(request.targetId())).findFirst()
                .orElseGet(() -> new Engagement(UUID.randomUUID().toString(), account.getId(), request.targetId(), request.kind(), "", 0, 0));
        item.revise(request.content(), request.numberValue(), System.currentTimeMillis());
        mEngagements.save(item);
    }

    @Transactional
    public void removeActivity(String userId, String kind, String targetId) {
        mEngagements.deleteAll(mEngagements.findAll().stream().filter(item -> item.getUserId().equals(userId)
                && item.getKind().equals(kind) && item.getTargetId().equals(targetId)).toList());
    }

    @Transactional
    public void createTicket(Account account, TicketRequest request) {
        if (!List.of("INQUIRY", "SUPPORT", "REPORT", "REFUND").contains(request.kind()) || request.message().isBlank()
                || request.message().length() > 4000) { throw new ApiFailure(400, "문의 내용을 확인하세요. / Check your message."); }
        String recipient = "";
        if (request.kind().equals("INQUIRY") || request.kind().equals("REPORT")) {
            recipient = mCatalog.requireProduct(request.targetId()).getSellerId();
        }
        if (request.kind().equals("INQUIRY") && isBlocked(account.getId(), recipient)) {
            throw new ApiFailure(403, "차단한 계정과는 문의할 수 없습니다. / Inquiry blocked.");
        }
        if (request.kind().equals("REFUND")) {
            OrderLine line = mLines.findById(request.targetId()).orElseThrow(() -> new ApiFailure(404, "Order not found"));
            if (!line.getBuyerId().equals(account.getId()) || line.isRefunded()) { throw new ApiFailure(403, "Invalid refund target"); }
            if (mTickets.findAll().stream().anyMatch(ticket -> ticket.getKind().equals("REFUND")
                    && ticket.getTargetId().equals(line.getId()) && ticket.getStatus().equals("OPEN"))) { throw new ApiFailure(409, "이미 요청했습니다. / Already requested."); }
        }
        mTickets.save(new Ticket(UUID.randomUUID().toString(), account.getId(), request.targetId(), recipient,
                request.kind(), request.message(), "OPEN", "", System.currentTimeMillis()));
    }

    public List<TicketView> listTickets(String userId) {
        return mTickets.findAll().stream().filter(ticket -> ticket.getUserId().equals(userId)
                || (ticket.getKind().equals("INQUIRY") && ticket.getRecipientId().equals(userId))).map(this::describeTicket).toList();
    }

    @Transactional
    public void replyToInquiry(String userId, String ticketId, String reply) {
        Ticket ticket = mTickets.findById(ticketId).orElseThrow(() -> new ApiFailure(404, "Ticket not found"));
        if (!ticket.getKind().equals("INQUIRY") || !ticket.getRecipientId().equals(userId) || reply.isBlank() || reply.length() > 4000) {
            throw new ApiFailure(403, "판매자만 답변할 수 있습니다. / Seller only.");
        }
        if (isBlocked(userId, ticket.getUserId())) { throw new ApiFailure(403, "Inquiry blocked"); }
        ticket.resolve(reply, "RESOLVED");
        notifyUser(ticket.getUserId(), "문의 답변이 도착했습니다. / Your inquiry has a reply.");
    }

    public List<NoticeView> listNotices(String userId) {
        return mNotices.findAll().stream().filter(notice -> notice.getUserId().equals(userId))
                .map(notice -> new NoticeView(notice.getId(), notice.getMessage(), notice.isRead(), notice.getCreatedAt())).toList();
    }

    @Transactional
    public void readNotice(String userId, String id) {
        Notice notice = mNotices.findById(id).orElseThrow(() -> new ApiFailure(404, "Notice not found"));
        if (!notice.getUserId().equals(userId)) { throw new ApiFailure(403, "Owner only"); }
        notice.markRead();
    }

    public void notifyUser(String userId, String message) {
        mNotices.save(new Notice(UUID.randomUUID().toString(), userId, message, false, System.currentTimeMillis()));
    }

    public void notifyPublication(Product product) {
        mEngagements.findAll().stream()
                .filter(item -> item.getKind().equals("NOTIFY") && item.getTargetId().equals(product.getSellerId()))
                .filter(item -> !isBlocked(item.getUserId(), product.getSellerId()))
                .forEach(item -> notifyUser(item.getUserId(), "새 영상 / New video: " + product.getTitle()));
    }

    private boolean isBlocked(String first, String second) {
        return mEngagements.findAll().stream().anyMatch(item -> item.getKind().equals("BLOCK")
                && ((item.getUserId().equals(first) && item.getTargetId().equals(second))
                || (item.getUserId().equals(second) && item.getTargetId().equals(first))));
    }

    public TicketView describeTicket(Ticket ticket) {
        return new TicketView(ticket.getId(), ticket.getUserId(), ticket.getTargetId(), ticket.getRecipientId(), ticket.getKind(),
                ticket.getMessage(), ticket.getStatus(), ticket.getReply(), ticket.getCreatedAt());
    }

    private EngagementView describeActivity(Engagement item) {
        String name = mAccounts.findById(item.getUserId()).map(Account::getDisplayName).orElse("User");
        return new EngagementView(item.getId(), item.getTargetId(), item.getKind(), item.getContent(), item.getNumberValue(), name);
    }

    public record ActivityRequest(@NotNull String targetId, @NotNull String kind, @NotNull String content, double numberValue) {}
    public record EngagementView(String id, String targetId, String kind, String content, double numberValue, String author) {}
    public record TicketRequest(@NotNull String targetId, @NotNull String kind, @NotNull String message) {}
    public record TicketView(String id, String userId, String targetId, String recipientId, String kind, String message, String status, String reply, long createdAt) {}
    public record NoticeView(String id, String message, boolean read, long createdAt) {}
}
