package com.pickview.operations;

import com.pickview.api.ApiFailure;
import com.pickview.api.AuthController.UserView;
import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.community.CommunityService;
import com.pickview.model.Account;
import com.pickview.model.Audit;
import com.pickview.model.Grant;
import com.pickview.model.OrderLine;
import com.pickview.model.Product;
import com.pickview.model.RefundAdjustment;
import com.pickview.model.Settlement;
import com.pickview.model.Ticket;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.IAuditRepository;
import com.pickview.repository.IGrantRepository;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.repository.IProductRepository;
import com.pickview.repository.IPurchaseRepository;
import com.pickview.repository.IRefundAdjustmentRepository;
import com.pickview.repository.ISettlementRepository;
import com.pickview.repository.ITicketRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsService {

    private final IAccountRepository mAccounts;
    private final IProductRepository mProducts;
    private final ITicketRepository mTickets;
    private final IOrderLineRepository mLines;
    private final IGrantRepository mGrants;
    private final IAuditRepository mAudits;
    private final ISettlementRepository mSettlements;
    private final IPurchaseRepository mPurchases;
    private final CatalogService mCatalog;
    private final CommerceService mCommerce;
    private final CommunityService mCommunity;
    private final EntityManager mEntityManager;
    private final IRefundAdjustmentRepository mAdjustments;
    private final Clock mClock;

    public OperationsService(
        IAccountRepository accounts,
        IProductRepository products,
        ITicketRepository tickets,
        IOrderLineRepository lines,
        IGrantRepository grants,
        IAuditRepository audits,
        ISettlementRepository settlements,
        IPurchaseRepository purchases,
        CatalogService catalog,
        CommerceService commerce,
        CommunityService community,
        EntityManager entityManager,
        IRefundAdjustmentRepository adjustments,
        Clock clock
    ) {
        mAccounts = accounts;
        mProducts = products;
        mTickets = tickets;
        mLines = lines;
        mGrants = grants;
        mAudits = audits;
        mSettlements = settlements;
        mPurchases = purchases;
        mCatalog = catalog;
        mCommerce = commerce;
        mCommunity = community;
        mEntityManager = entityManager;
        mAdjustments = adjustments;
        mClock = clock;
    }

    public DashboardView getDashboard(Account operator) {
        requireRole(operator, "CONTENT", "SUPPORT", "FINANCE");
        boolean canReviewContent = hasRole(operator, "CONTENT");
        boolean canHandleSupport = hasRole(operator, "SUPPORT");
        boolean canManageFinance = hasRole(operator, "FINANCE");
        boolean isAdministrator = operator.getRole().equals("ADMIN");
        return new DashboardView(
            canReviewContent ? mAccounts.findAll().stream().map(UserView::fromAccount).toList() : List.of(),
            canReviewContent ? mProducts.findAll().stream().map(mCatalog::describeProduct).toList() : List.of(),
            mTickets
                .findAll()
                .stream()
                .filter(ticket -> canReviewTicket(ticket, canReviewContent, canHandleSupport))
                .map(mCommunity::describeTicket)
                .toList(),
            canManageFinance ? mLines.findAll().stream().map(mCommerce::describeLine).toList() : List.of(),
            canManageFinance ? mAdjustments.findAll().stream().map(this::describeAdjustment).toList() : List.of(),
            isAdministrator ? mAudits.findAll().stream().map(this::describeAudit).toList() : List.of()
        );
    }

    public SellerSettlementView getSellerSettlementSummary(String sellerId) {
        int adjustmentWon = mAdjustments
            .findPending(sellerId, "")
            .stream()
            .mapToInt(RefundAdjustment::getAmountWon)
            .sum();
        int unsettledWon = mLines
            .findAll()
            .stream()
            .filter(
                line -> line.getSellerId().equals(sellerId) && !line.isRefunded() && line.getSettlementId().isEmpty()
            )
            .mapToInt(OrderLine::getSellerAmountWon)
            .sum();
        List<Settlement> settlements = mSettlements
            .findAll()
            .stream()
            .filter(item -> item.getSellerId().equals(sellerId))
            .toList();
        return new SellerSettlementView(
            unsettledWon - adjustmentWon,
            adjustmentWon,
            settlements.stream().mapToInt(Settlement::getAmountWon).sum(),
            settlements.stream().map(this::describeSettlement).toList()
        );
    }

    @Transactional
    public void reviewSeller(Account operator, String id, boolean approve) {
        requireRole(operator, "CONTENT");
        Account seller = mAccounts.findById(id).orElseThrow(() -> new ApiFailure(404, "Seller not found"));
        if (approve) {
            seller.approveSeller();
        } else {
            seller.rejectSeller();
        }
        audit(operator, "SELLER_REVIEW", id, approve ? "APPROVED" : "REJECTED");
        mCommunity.notifyUser(id, "판매자 심사 완료 / Seller application reviewed");
    }

    @Transactional
    public void reviewProduct(Account operator, String id, String decision) {
        requireRole(operator, "CONTENT");
        Product product = mCatalog.requireProduct(id);
        boolean wasPublished = product.getStatus().equals("APPROVED");
        switch (decision) {
            case "APPROVE" -> {
                if (product.getKind().equals("VIDEO") && product.getMediaKey().isBlank()) {
                    throw new ApiFailure(409, "Missing video");
                }
                product.publish();
            }
            case "REJECT" -> product.reject();
            case "WITHDRAW" -> product.withdraw();
            case "BLOCK" -> product.block();
            default -> throw new ApiFailure(400, "Invalid decision");
        }
        audit(operator, "PRODUCT_REVIEW", id, decision);
        mCommunity.notifyUser(product.getSellerId(), "영상 심사 결과 / Video review: " + decision);
        if (decision.equals("APPROVE") && !wasPublished) {
            mCommunity.notifyPublication(product);
        }
    }

    @Transactional
    public void resolveTicket(Account operator, String id, String reply, boolean approve) {
        Ticket ticket = mEntityManager.find(Ticket.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (ticket == null) {
            throw new ApiFailure(404, "Ticket not found");
        }
        requireRole(operator, ticket.getKind().equals("REPORT") ? "CONTENT" : "SUPPORT");
        if (
            ticket.getKind().equals("INQUIRY") ||
            !ticket.getStatus().equals("OPEN") ||
            reply.isBlank() ||
            reply.length() > 4000
        ) {
            throw new ApiFailure(409, "처리할 수 없는 문의입니다. / Ticket cannot be resolved.");
        }
        if (ticket.getKind().equals("REFUND") && approve) {
            refundLine(ticket.getTargetId());
        }
        ticket.resolve(reply, approve ? "APPROVED" : "REJECTED");
        audit(operator, "TICKET_RESOLVED", id, reply);
        mCommunity.notifyUser(ticket.getUserId(), "문의 처리 완료 / Your request was resolved");
    }

    @Transactional
    public void changeRole(Account operator, String id, String role) {
        requireRole(operator);
        if (operator.getId().equals(id) || !List.of("BUYER", "ADMIN", "CONTENT", "SUPPORT", "FINANCE").contains(role)) {
            throw new ApiFailure(400, "Invalid role change");
        }
        mAccounts
            .findById(id)
            .orElseThrow(() -> new ApiFailure(404, "Account not found"))
            .changeRole(role);
        audit(operator, "ROLE_CHANGED", id, role);
    }

    @Transactional
    public int settle(Account operator, String sellerId) {
        requireRole(operator, "FINANCE");
        if (mEntityManager.find(Account.class, sellerId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ApiFailure(404, "Seller not found");
        }
        LocalDate today = LocalDate.now(mClock.withZone(ZoneId.of("Asia/Seoul")));
        if (today.getDayOfMonth() < 15) {
            throw new ApiFailure(409, "매월 15일부터 처리 가능합니다. / Settlement opens on the 15th.");
        }
        long cutoff = today.withDayOfMonth(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        List<OrderLine> eligible = mLines
            .findAll()
            .stream()
            .filter(
                line ->
                    line.getSellerId().equals(sellerId) &&
                    !line.isRefunded() &&
                    line.getSettlementId().isEmpty() &&
                    mPurchases.findById(line.getPurchaseId()).orElseThrow().getCreatedAt() < cutoff
            )
            .toList();
        List<RefundAdjustment> adjustments = mAdjustments.findPending(sellerId, "");
        int amount =
            eligible.stream().mapToInt(OrderLine::getSellerAmountWon).sum() -
            adjustments.stream().mapToInt(RefundAdjustment::getAmountWon).sum();
        if (amount < 10000) {
            throw new ApiFailure(409, "정산 가능 금액 1만원 미만은 이월됩니다. / Below payout threshold.");
        }
        String id = UUID.randomUUID().toString();
        mSettlements.save(new Settlement(id, sellerId, amount, System.currentTimeMillis()));
        for (OrderLine line : eligible) {
            line.settle(id);
        }
        for (RefundAdjustment adjustment : adjustments) {
            adjustment.settle(id);
        }
        audit(operator, "MOCK_SETTLEMENT", sellerId, Integer.toString(amount));
        mCommunity.notifyUser(sellerId, "모의 정산 완료 / Mock settlement complete");
        return amount;
    }

    private void refundLine(String id) {
        OrderLine existing = mLines.findById(id).orElseThrow(() -> new ApiFailure(404, "Order not found"));
        // Use the same seller lock as settlement so refund and payout cannot race.
        mEntityManager.find(Account.class, existing.getSellerId(), LockModeType.PESSIMISTIC_WRITE);
        OrderLine line = mEntityManager.find(OrderLine.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (line == null) {
            throw new ApiFailure(404, "Order not found");
        }
        mEntityManager.refresh(line);
        if (line.isRefunded()) {
            throw new ApiFailure(409, "Already refunded");
        }
        if (!line.getSettlementId().isEmpty()) {
            mAdjustments.save(new RefundAdjustment(line.getId(), line.getSellerId(), line.getSellerAmountWon()));
        }
        line.refund();
        mGrants
            .findAll()
            .stream()
            .filter(grant -> grant.getLineId().equals(id))
            .forEach(Grant::revoke);
        boolean allRefunded = mLines
            .findAll()
            .stream()
            .filter(item -> item.getPurchaseId().equals(line.getPurchaseId()))
            .allMatch(OrderLine::isRefunded);
        if (allRefunded) {
            mPurchases.findById(line.getPurchaseId()).orElseThrow().refund();
        }
    }

    private boolean hasRole(Account operator, String role) {
        return operator.getRole().equals("ADMIN") || operator.getRole().equals(role);
    }

    private void requireRole(Account operator, String... roles) {
        if (!operator.getRole().equals("ADMIN") && !List.of(roles).contains(operator.getRole())) {
            throw new ApiFailure(403, "권한이 없습니다. / Permission denied.");
        }
    }

    private void audit(Account operator, String action, String target, String detail) {
        mAudits.save(
            new Audit(
                UUID.randomUUID().toString(),
                operator.getId(),
                action,
                target,
                detail,
                System.currentTimeMillis()
            )
        );
    }

    private boolean canReviewTicket(Ticket ticket, boolean canReviewContent, boolean canHandleSupport) {
        return (
            (canReviewContent && ticket.getKind().equals("REPORT")) ||
            (canHandleSupport && List.of("SUPPORT", "REFUND").contains(ticket.getKind()))
        );
    }

    private RefundAdjustmentView describeAdjustment(RefundAdjustment adjustment) {
        return new RefundAdjustmentView(
            adjustment.getLineId(),
            adjustment.getSellerId(),
            adjustment.getAmountWon(),
            adjustment.getSettlementId()
        );
    }

    private AuditView describeAudit(Audit audit) {
        return new AuditView(
            audit.getId(),
            audit.getActorId(),
            audit.getAction(),
            audit.getTargetId(),
            audit.getDetail(),
            audit.getCreatedAt()
        );
    }

    private SettlementView describeSettlement(Settlement settlement) {
        return new SettlementView(settlement.getId(), settlement.getAmountWon(), settlement.getCreatedAt());
    }

    public record DashboardView(
        List<UserView> accounts,
        List<CatalogService.ProductView> products,
        List<CommunityService.TicketView> tickets,
        List<CommerceService.LineView> lines,
        List<RefundAdjustmentView> adjustments,
        List<AuditView> audits
    ) {}

    public record RefundAdjustmentView(String lineId, String sellerId, int amountWon, String settlementId) {}

    public record AuditView(String id, String actorId, String action, String targetId, String detail, long createdAt) {}

    public record SettlementView(String id, int amountWon, long createdAt) {}

    public record SellerSettlementView(
        int pendingWon,
        int adjustmentWon,
        int paidWon,
        List<SettlementView> settlements
    ) {}
}
