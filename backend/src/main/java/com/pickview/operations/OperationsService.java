package com.pickview.operations;

import com.pickview.api.ApiFailure;
import com.pickview.api.UserView;
import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.community.CommunityService;
import com.pickview.domain.AccountId;
import com.pickview.domain.EApprovalDecision;
import com.pickview.domain.EProductDecision;
import com.pickview.domain.EProductKind;
import com.pickview.domain.EProductStatus;
import com.pickview.domain.ERole;
import com.pickview.domain.ETicketKind;
import com.pickview.domain.ETicketStatus;
import com.pickview.domain.OrderLineId;
import com.pickview.domain.ProductId;
import com.pickview.domain.TicketId;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsService {

    private static final int SETTLEMENT_OPEN_DAY = 15;
    private static final int MIN_PAYOUT_WON = 10_000;

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
        requireRole(operator, ERole.CONTENT, ERole.SUPPORT, ERole.FINANCE);
        boolean canReviewContent = hasRole(operator, ERole.CONTENT);
        boolean canManageFinance = hasRole(operator, ERole.FINANCE);
        boolean isAdministrator = operator.getRole().equals(ERole.ADMIN);
        return new DashboardView(
            canReviewContent ? mAccounts.findAll().stream().map(UserView::createFromAccount).toList() : List.of(),
            canReviewContent ? mCatalog.describeProducts(mProducts.findAll()) : List.of(),
            mTickets
                .findAll()
                .stream()
                .filter(ticket -> canReviewTicket(operator, ticket))
                .map(mCommunity::describeTicket)
                .toList(),
            canManageFinance ? mLines.findAll().stream().map(mCommerce::describeLine).toList() : List.of(),
            canManageFinance ? mAdjustments.findAll().stream().map(this::describeAdjustment).toList() : List.of(),
            isAdministrator ? mAudits.findAll().stream().map(this::describeAudit).toList() : List.of()
        );
    }

    public SellerSettlementView getSellerSettlementSummary(AccountId sellerId) {
        int adjustmentWon = mAdjustments
            .findPending(sellerId.getValue(), "")
            .stream()
            .mapToInt(RefundAdjustment::getAmountWon)
            .sum();
        int unsettledWon = mLines
            .findAll()
            .stream()
            .filter(
                line ->
                    line.getSellerId().equals(sellerId.getValue()) &&
                    !line.isRefunded() &&
                    line.getSettlementId().isEmpty()
            )
            .mapToInt(OrderLine::getSellerAmountWon)
            .sum();
        List<Settlement> settlements = mSettlements
            .findAll()
            .stream()
            .filter(item -> item.getSellerId().equals(sellerId.getValue()))
            .toList();
        return new SellerSettlementView(
            unsettledWon - adjustmentWon,
            adjustmentWon,
            settlements.stream().mapToInt(Settlement::getAmountWon).sum(),
            settlements.stream().map(this::describeSettlement).toList()
        );
    }

    @Transactional
    public void reviewSeller(Account operator, AccountId id, EApprovalDecision decision) {
        java.util.Objects.requireNonNull(decision, "decision");
        requireRole(operator, ERole.CONTENT);
        Account seller = mAccounts
            .findById(id.getValue())
            .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Seller not found"));
        if (decision == EApprovalDecision.APPROVE) {
            seller.approveSeller();
        } else {
            seller.rejectSeller();
        }
        audit(
            operator,
            "SELLER_REVIEW",
            id.getValue(),
            decision == EApprovalDecision.APPROVE ? "APPROVED" : "REJECTED"
        );
        mCommunity.notifyUser(id, "판매자 심사 완료 / Seller application reviewed");
    }

    @Transactional
    public void reviewProduct(Account operator, ProductId id, EProductDecision decision) {
        java.util.Objects.requireNonNull(decision, "decision");
        requireRole(operator, ERole.CONTENT);
        Product product = mCatalog.requireProduct(id);
        boolean wasPublished = product.getStatus().equals(EProductStatus.APPROVED);
        switch (decision) {
            case APPROVE -> {
                if (product.getKind().equals(EProductKind.VIDEO) && product.getMediaKey().isBlank()) {
                    throw new ApiFailure(HttpStatus.CONFLICT, "Missing video");
                }
                product.publish();
            }
            case REJECT -> product.reject();
            case WITHDRAW -> product.withdraw();
            case BLOCK -> product.block();
            default -> throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid decision");
        }
        audit(operator, "PRODUCT_REVIEW", id.getValue(), decision.name());
        mCommunity.notifyUser(new AccountId(product.getSellerId()), "영상 심사 결과 / Video review: " + decision);
        if (decision == EProductDecision.APPROVE && !wasPublished) {
            mCommunity.notifyPublication(product);
        }
    }

    @Transactional
    public void resolveTicket(Account operator, TicketId id, String reply, EApprovalDecision decision) {
        java.util.Objects.requireNonNull(decision, "decision");
        Ticket ticket = mEntityManager.find(Ticket.class, id.getValue(), LockModeType.PESSIMISTIC_WRITE);
        if (ticket == null) {
            throw new ApiFailure(HttpStatus.NOT_FOUND, "Ticket not found");
        }
        requireRole(operator, ticket.getKind().equals(ETicketKind.REPORT) ? ERole.CONTENT : ERole.SUPPORT);
        if (
            ticket.getKind().equals(ETicketKind.INQUIRY) ||
            !ticket.getStatus().equals(ETicketStatus.OPEN) ||
            reply.isBlank() ||
            reply.length() > 4000
        ) {
            throw new ApiFailure(HttpStatus.CONFLICT, "처리할 수 없는 문의입니다. / Ticket cannot be resolved.");
        }
        if (ticket.getKind().equals(ETicketKind.REFUND) && decision == EApprovalDecision.APPROVE) {
            refundLine(new OrderLineId(ticket.getTargetId()));
        }
        ticket.resolve(reply, decision == EApprovalDecision.APPROVE ? ETicketStatus.APPROVED : ETicketStatus.REJECTED);
        audit(operator, "TICKET_RESOLVED", id.getValue(), reply);
        mCommunity.notifyUser(new AccountId(ticket.getUserId()), "문의 처리 완료 / Your request was resolved");
    }

    @Transactional
    public void changeRole(Account operator, AccountId id, ERole role) {
        requireRole(operator);
        if (operator.getId().equals(id.getValue()) || role == null) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid role change");
        }
        mAccounts
            .findById(id.getValue())
            .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Account not found"))
            .changeRole(role);
        audit(operator, "ROLE_CHANGED", id.getValue(), role.name());
    }

    @Transactional
    public int settle(Account operator, AccountId sellerId) {
        requireRole(operator, ERole.FINANCE);
        if (mEntityManager.find(Account.class, sellerId.getValue(), LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ApiFailure(HttpStatus.NOT_FOUND, "Seller not found");
        }
        LocalDate today = LocalDate.now(mClock.withZone(ZoneId.of("Asia/Seoul")));
        if (today.getDayOfMonth() < SETTLEMENT_OPEN_DAY) {
            throw new ApiFailure(HttpStatus.CONFLICT, "매월 15일부터 처리 가능합니다. / Settlement opens on the 15th.");
        }
        long cutoff = today.withDayOfMonth(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        List<OrderLine> eligible = mLines
            .findAll()
            .stream()
            .filter(
                line ->
                    line.getSellerId().equals(sellerId.getValue()) &&
                    !line.isRefunded() &&
                    line.getSettlementId().isEmpty() &&
                    mPurchases.findById(line.getPurchaseId()).orElseThrow().getCreatedAt() < cutoff
            )
            .toList();
        List<RefundAdjustment> adjustments = mAdjustments.findPending(sellerId.getValue(), "");
        int amount =
            eligible.stream().mapToInt(OrderLine::getSellerAmountWon).sum() -
            adjustments.stream().mapToInt(RefundAdjustment::getAmountWon).sum();
        if (amount < MIN_PAYOUT_WON) {
            throw new ApiFailure(
                HttpStatus.CONFLICT,
                "정산 가능 금액 1만원 미만은 이월됩니다. / Below payout threshold."
            );
        }
        String id = UUID.randomUUID().toString();
        mSettlements.save(new Settlement(id, sellerId.getValue(), amount, System.currentTimeMillis()));
        for (OrderLine line : eligible) {
            line.settle(id);
        }
        for (RefundAdjustment adjustment : adjustments) {
            adjustment.settle(id);
        }
        audit(operator, "MOCK_SETTLEMENT", sellerId.getValue(), Integer.toString(amount));
        mCommunity.notifyUser(sellerId, "모의 정산 완료 / Mock settlement complete");
        return amount;
    }

    private void refundLine(OrderLineId id) {
        OrderLine existing = mLines
            .findById(id.getValue())
            .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Order not found"));
        // Use the same seller lock as settlement so refund and payout cannot race.
        mEntityManager.find(Account.class, existing.getSellerId(), LockModeType.PESSIMISTIC_WRITE);
        OrderLine line = mEntityManager.find(OrderLine.class, id.getValue(), LockModeType.PESSIMISTIC_WRITE);
        if (line == null) {
            throw new ApiFailure(HttpStatus.NOT_FOUND, "Order not found");
        }
        mEntityManager.refresh(line);
        if (line.isRefunded()) {
            throw new ApiFailure(HttpStatus.CONFLICT, "Already refunded");
        }
        if (!line.getSettlementId().isEmpty()) {
            mAdjustments.save(new RefundAdjustment(line.getId(), line.getSellerId(), line.getSellerAmountWon()));
        }
        line.refund();
        mGrants
            .findAll()
            .stream()
            .filter(grant -> grant.getLineId().equals(id.getValue()))
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

    private boolean hasRole(Account operator, ERole role) {
        return operator.getRole().equals(ERole.ADMIN) || operator.getRole().equals(role);
    }

    private void requireRole(Account operator, ERole... roles) {
        if (!operator.getRole().equals(ERole.ADMIN) && !List.of(roles).contains(operator.getRole())) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "권한이 없습니다. / Permission denied.");
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

    private boolean canReviewTicket(Account operator, Ticket ticket) {
        return (
            (hasRole(operator, ERole.CONTENT) && ticket.getKind().equals(ETicketKind.REPORT)) ||
            (hasRole(operator, ERole.SUPPORT) &&
                List.of(ETicketKind.SUPPORT, ETicketKind.REFUND).contains(ticket.getKind()))
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
    ) {
        public DashboardView {
            accounts = List.copyOf(accounts);
            products = List.copyOf(products);
            tickets = List.copyOf(tickets);
            lines = List.copyOf(lines);
            adjustments = List.copyOf(adjustments);
            audits = List.copyOf(audits);
        }
    }

    public record RefundAdjustmentView(String lineId, String sellerId, int amountWon, String settlementId) {}

    public record AuditView(String id, String actorId, String action, String targetId, String detail, long createdAt) {}

    public record SettlementView(String id, int amountWon, long createdAt) {}

    public record SellerSettlementView(
        int pendingWon,
        int adjustmentWon,
        int paidWon,
        List<SettlementView> settlements
    ) {
        public SellerSettlementView {
            settlements = List.copyOf(settlements);
        }
    }
}
