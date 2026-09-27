package com.pickview.community;

import com.pickview.api.ApiFailure;
import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.domain.AccountId;
import com.pickview.domain.EActivityKind;
import com.pickview.domain.ETicketKind;
import com.pickview.domain.ETicketStatus;
import com.pickview.domain.NoticeId;
import com.pickview.domain.ProductId;
import com.pickview.domain.TicketId;
import com.pickview.model.Account;
import com.pickview.model.Engagement;
import com.pickview.model.Notice;
import com.pickview.model.OrderLine;
import com.pickview.model.Product;
import com.pickview.model.Ticket;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.IEngagementRepository;
import com.pickview.repository.INoticeRepository;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.repository.ITicketRepository;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

    public CommunityService(
        IEngagementRepository engagements,
        ITicketRepository tickets,
        INoticeRepository notices,
        IOrderLineRepository lines,
        IAccountRepository accounts,
        CommerceService commerce,
        CatalogService catalog
    ) {
        mEngagements = engagements;
        mTickets = tickets;
        mNotices = notices;
        mLines = lines;
        mAccounts = accounts;
        mCommerce = commerce;
        mCatalog = catalog;
    }

    public List<EngagementView> listActivity(AccountId userId) {
        return mEngagements
            .findAll()
            .stream()
            .filter(item -> item.getUserId().equals(userId.getValue()))
            .map(this::describeActivity)
            .toList();
    }

    public List<EngagementView> listReviews(ProductId productId) {
        return mEngagements
            .findAll()
            .stream()
            .filter(
                item -> item.getTargetId().equals(productId.getValue()) && item.getKind().equals(EActivityKind.REVIEW)
            )
            .map(this::describeActivity)
            .toList();
    }

    @Transactional
    public void saveActivity(Account account, ActivityRequest request) {
        if (request.kind() == null || request.content().length() > 2000 || !Double.isFinite(request.numberValue())) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid activity");
        }
        if (List.of(EActivityKind.FOLLOW, EActivityKind.BLOCK, EActivityKind.NOTIFY).contains(request.kind())) {
            if (mAccounts.findById(request.targetId()).isEmpty() || account.getId().equals(request.targetId())) {
                throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid account target");
            }
        } else {
            mCatalog.requireProduct(new ProductId(request.targetId()));
        }
        if (request.kind().equals(EActivityKind.PROGRESS)) {
            if (!mCommerce.canWatch(new AccountId(account.getId()), new ProductId(request.targetId()))) {
                throw new ApiFailure(HttpStatus.FORBIDDEN, "구매 후 이용 가능합니다. / Purchase required.");
            }
            if (
                request.numberValue() < 0 ||
                request.numberValue() > mCatalog.requireProduct(new ProductId(request.targetId())).getDurationSeconds()
            ) {
                throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid playback position");
            }
        }
        if (request.kind().equals(EActivityKind.REVIEW)) {
            boolean hasPurchase = mLines
                .findAll()
                .stream()
                .anyMatch(
                    line ->
                        line.getBuyerId().equals(account.getId()) &&
                        line.getProductId().equals(request.targetId()) &&
                        !line.isRefunded()
                );
            if (!hasPurchase || request.numberValue() < 1 || request.numberValue() > 5 || request.content().isBlank()) {
                throw new ApiFailure(
                    HttpStatus.FORBIDDEN,
                    "구매자만 1~5점 후기를 작성할 수 있습니다. / Verified purchase required."
                );
            }
        }
        Engagement item = mEngagements
            .findAll()
            .stream()
            .filter(
                entry ->
                    entry.getUserId().equals(account.getId()) &&
                    entry.getKind().equals(request.kind()) &&
                    entry.getTargetId().equals(request.targetId())
            )
            .findFirst()
            .orElseGet(() ->
                new Engagement(
                    UUID.randomUUID().toString(),
                    account.getId(),
                    request.targetId(),
                    EActivityKind.valueOf(request.kind().name()),
                    "",
                    0,
                    0
                )
            );
        item.revise(request.content(), request.numberValue(), System.currentTimeMillis());
        mEngagements.save(item);
    }

    @Transactional
    public void removeActivity(AccountId userId, EActivityKind kind, String targetId) {
        mEngagements.deleteAll(
            mEngagements
                .findAll()
                .stream()
                .filter(
                    item ->
                        item.getUserId().equals(userId.getValue()) &&
                        item.getKind().equals(kind) &&
                        item.getTargetId().equals(targetId)
                )
                .toList()
        );
    }

    @Transactional
    public void createTicket(Account account, TicketRequest request) {
        if (request.kind() == null || request.message().isBlank() || request.message().length() > 4000) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "문의 내용을 확인하세요. / Check your message.");
        }
        String recipient = "";
        if (request.kind().equals(ETicketKind.INQUIRY) || request.kind().equals(ETicketKind.REPORT)) {
            recipient = mCatalog.requireProduct(new ProductId(request.targetId())).getSellerId();
        }
        if (
            request.kind().equals(ETicketKind.INQUIRY) &&
            isBlocked(new AccountId(account.getId()), new AccountId(recipient))
        ) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "차단한 계정과는 문의할 수 없습니다. / Inquiry blocked.");
        }
        if (request.kind().equals(ETicketKind.REFUND)) {
            OrderLine line = mLines
                .findById(request.targetId())
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Order not found"));
            if (!line.getBuyerId().equals(account.getId()) || line.isRefunded()) {
                throw new ApiFailure(HttpStatus.FORBIDDEN, "Invalid refund target");
            }
            if (
                mTickets
                    .findAll()
                    .stream()
                    .anyMatch(
                        ticket ->
                            ticket.getKind().equals(ETicketKind.REFUND) &&
                            ticket.getTargetId().equals(line.getId()) &&
                            ticket.getStatus().equals(ETicketStatus.OPEN)
                    )
            ) {
                throw new ApiFailure(HttpStatus.CONFLICT, "이미 요청했습니다. / Already requested.");
            }
        }
        mTickets.save(
            new Ticket(
                UUID.randomUUID().toString(),
                account.getId(),
                request.targetId(),
                recipient,
                ETicketKind.valueOf(request.kind().name()),
                request.message(),
                ETicketStatus.OPEN,
                "",
                System.currentTimeMillis()
            )
        );
    }

    public List<TicketView> listTickets(AccountId userId) {
        return mTickets
            .findAll()
            .stream()
            .filter(
                ticket ->
                    ticket.getUserId().equals(userId.getValue()) ||
                    (ticket.getKind().equals(ETicketKind.INQUIRY) && ticket.getRecipientId().equals(userId.getValue()))
            )
            .map(this::describeTicket)
            .toList();
    }

    @Transactional
    public void replyToInquiry(AccountId userId, TicketId ticketId, String reply) {
        Ticket ticket = mTickets
            .findById(ticketId.getValue())
            .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (
            !ticket.getKind().equals(ETicketKind.INQUIRY) ||
            !ticket.getRecipientId().equals(userId.getValue()) ||
            reply.isBlank() ||
            reply.length() > 4000
        ) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "판매자만 답변할 수 있습니다. / Seller only.");
        }
        if (isBlocked(userId, new AccountId(ticket.getUserId()))) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "Inquiry blocked");
        }
        ticket.resolve(reply, ETicketStatus.RESOLVED);
        notifyUser(new AccountId(ticket.getUserId()), "문의 답변이 도착했습니다. / Your inquiry has a reply.");
    }

    public List<NoticeView> listNotices(AccountId userId) {
        return mNotices
            .findAll()
            .stream()
            .filter(notice -> notice.getUserId().equals(userId.getValue()))
            .map(notice -> new NoticeView(notice.getId(), notice.getMessage(), notice.isRead(), notice.getCreatedAt()))
            .toList();
    }

    @Transactional
    public void readNotice(AccountId userId, NoticeId id) {
        Notice notice = mNotices
            .findById(id.getValue())
            .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Notice not found"));
        if (!notice.getUserId().equals(userId.getValue())) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "Owner only");
        }
        notice.markRead();
    }

    public void notifyUser(AccountId userId, String message) {
        mNotices.save(
            new Notice(UUID.randomUUID().toString(), userId.getValue(), message, false, System.currentTimeMillis())
        );
    }

    public void notifyPublication(Product product) {
        mEngagements
            .findAll()
            .stream()
            .filter(
                item -> item.getKind().equals(EActivityKind.NOTIFY) && item.getTargetId().equals(product.getSellerId())
            )
            .filter(item -> !isBlocked(new AccountId(item.getUserId()), new AccountId(product.getSellerId())))
            .forEach(item -> notifyUser(new AccountId(item.getUserId()), "새 영상 / New video: " + product.getTitle()));
    }

    private boolean isBlocked(AccountId first, AccountId second) {
        return mEngagements
            .findAll()
            .stream()
            .anyMatch(
                item ->
                    item.getKind().equals(EActivityKind.BLOCK) &&
                    ((item.getUserId().equals(first.getValue()) && item.getTargetId().equals(second.getValue())) ||
                        (item.getUserId().equals(second.getValue()) && item.getTargetId().equals(first.getValue())))
            );
    }

    public TicketView describeTicket(Ticket ticket) {
        return new TicketView(
            ticket.getId(),
            new AccountId(ticket.getUserId()),
            ticket.getTargetId(),
            ticket.getRecipientId(),
            ticket.getKind(),
            ticket.getMessage(),
            ticket.getStatus(),
            ticket.getReply(),
            ticket.getCreatedAt()
        );
    }

    private EngagementView describeActivity(Engagement item) {
        String name = mAccounts.findById(item.getUserId()).map(Account::getDisplayName).orElse("User");
        return new EngagementView(
            item.getId(),
            item.getTargetId(),
            item.getKind(),
            item.getContent(),
            item.getNumberValue(),
            name
        );
    }

    public record ActivityRequest(
        @NotNull String targetId,
        @NotNull EActivityKind kind,
        @NotNull String content,
        double numberValue
    ) {}

    public record EngagementView(
        String id,
        String targetId,
        EActivityKind kind,
        String content,
        double numberValue,
        String author
    ) {}

    public record TicketRequest(@NotNull String targetId, @NotNull ETicketKind kind, @NotNull String message) {}

    public record TicketView(
        String id,
        AccountId userId,
        String targetId,
        String recipientId,
        ETicketKind kind,
        String message,
        ETicketStatus status,
        String reply,
        long createdAt
    ) {}

    public record NoticeView(String id, String message, boolean read, long createdAt) {}
}
