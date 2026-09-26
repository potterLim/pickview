package com.pickview.commerce;

import com.pickview.api.ApiFailure;
import com.pickview.catalog.CatalogService;
import com.pickview.model.Account;
import com.pickview.model.Grant;
import com.pickview.model.Notice;
import com.pickview.model.OrderLine;
import com.pickview.model.Product;
import com.pickview.model.Purchase;
import com.pickview.repository.IGrantRepository;
import com.pickview.repository.INoticeRepository;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.repository.IPurchaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommerceService {

    private final IPurchaseRepository mPurchases;
    private final IOrderLineRepository mLines;
    private final IGrantRepository mGrants;
    private final INoticeRepository mNotices;
    private final CatalogService mCatalog;
    private final EntityManager mEntityManager;

    public CommerceService(
        IPurchaseRepository purchases,
        IOrderLineRepository lines,
        IGrantRepository grants,
        INoticeRepository notices,
        CatalogService catalog,
        EntityManager entityManager
    ) {
        mPurchases = purchases;
        mLines = lines;
        mGrants = grants;
        mNotices = notices;
        mCatalog = catalog;
        mEntityManager = entityManager;
    }

    public boolean canWatch(String buyerId, String videoId) {
        return mGrants
            .findAll()
            .stream()
            .anyMatch(
                grant ->
                    grant.getBuyerId().equals(buyerId) &&
                    grant.getProductId().equals(videoId) &&
                    grant.isValid(System.currentTimeMillis())
            );
    }

    @Transactional
    public OrderView checkout(Account buyer, CheckoutRequest request) {
        // Serialize a buyer's checkouts across server instances to prevent double grants.
        mEntityManager.find(Account.class, buyer.getId(), LockModeType.PESSIMISTIC_WRITE);
        Purchase previousOrNull = mPurchases
            .findAll()
            .stream()
            .filter(
                purchase ->
                    purchase.getBuyerId().equals(buyer.getId()) && purchase.getRequestKey().equals(request.requestKey())
            )
            .findFirst()
            .orElse(null);
        if (previousOrNull != null) {
            return describeOrder(previousOrNull);
        }
        if (
            request.requestKey().isBlank() ||
            request.requestKey().length() > 100 ||
            request.productIds().isEmpty() ||
            request.productIds().size() > 30 ||
            !List.of("CARD", "EASY").contains(request.channel()) ||
            !List.of("SUCCESS", "FAILED", "CANCELED").contains(request.outcome())
        ) {
            throw new ApiFailure(400, "결제 요청을 확인하세요. / Invalid checkout.");
        }
        List<Product> products = request.productIds().stream().map(mCatalog::requireProduct).toList();
        validateCart(buyer, products);
        Purchase purchase = mPurchases.save(
            new Purchase(
                UUID.randomUUID().toString(),
                buyer.getId(),
                request.requestKey(),
                request.outcome(),
                request.channel(),
                System.currentTimeMillis()
            )
        );
        if (!request.outcome().equals("SUCCESS")) {
            return describeOrder(purchase);
        }
        for (Product product : products) {
            grantProduct(buyer, purchase, product);
        }
        mNotices.save(
            new Notice(
                UUID.randomUUID().toString(),
                buyer.getId(),
                "구매 완료 / Purchase complete",
                false,
                System.currentTimeMillis()
            )
        );
        return describeOrder(purchase);
    }

    public List<OrderView> listOrders(String buyerId) {
        return mPurchases
            .findAll()
            .stream()
            .filter(purchase -> purchase.getBuyerId().equals(buyerId))
            .map(this::describeOrder)
            .toList();
    }

    public List<LibraryView> listLibrary(String buyerId) {
        return mGrants
            .findAll()
            .stream()
            .filter(grant -> grant.getBuyerId().equals(buyerId) && !grant.isRevoked())
            .map(grant ->
                new LibraryView(
                    grant.getId(),
                    mCatalog.describeProduct(mCatalog.requireProduct(grant.getProductId())),
                    grant.getExpiresAt(),
                    grant.isValid(System.currentTimeMillis())
                )
            )
            .toList();
    }

    public OrderView describeOrder(Purchase purchase) {
        List<LineView> lines = mLines
            .findAll()
            .stream()
            .filter(line -> line.getPurchaseId().equals(purchase.getId()))
            .map(this::describeLine)
            .toList();
        return new OrderView(
            purchase.getId(),
            purchase.getStatus(),
            purchase.getChannel(),
            purchase.getCreatedAt(),
            lines,
            lines.stream().mapToInt(LineView::priceWon).sum()
        );
    }

    public LineView describeLine(OrderLine line) {
        return new LineView(
            line.getId(),
            line.getProductId(),
            line.getTitle(),
            line.getPriceWon(),
            line.getChannelFeeWon(),
            line.getPlatformFeeWon(),
            line.getSellerAmountWon(),
            line.getTermDays(),
            line.isRefunded(),
            line.getSettlementId()
        );
    }

    private void validateCart(Account buyer, List<Product> products) {
        Set<String> videoIds = new HashSet<>();
        for (Product product : products) {
            if (
                !product.getStatus().equals("APPROVED") ||
                product.isBlocked() ||
                product.getSellerId().equals(buyer.getId())
            ) {
                throw new ApiFailure(409, "구매할 수 없는 상품입니다. / Product unavailable or self purchase.");
            }
            for (String videoId : mCatalog.expandVideoIds(product)) {
                if (
                    mCatalog.requireProduct(videoId).isBlocked() ||
                    !videoIds.add(videoId) ||
                    canWatch(buyer.getId(), videoId)
                ) {
                    throw new ApiFailure(
                        409,
                        "중복되거나 이용할 수 없는 영상입니다. / Duplicate or unavailable video."
                    );
                }
            }
        }
    }

    private void grantProduct(Account buyer, Purchase purchase, Product product) {
        int channelFee = (product.getPriceWon() * 3) / 100;
        int platformFee = ((product.getPriceWon() - channelFee) * 15) / 100;
        OrderLine line = mLines.save(
            new OrderLine(
                UUID.randomUUID().toString(),
                purchase.getId(),
                buyer.getId(),
                product.getSellerId(),
                product.getId(),
                product.getTitle(),
                product.getPriceWon(),
                channelFee,
                platformFee,
                product.getPriceWon() - channelFee - platformFee,
                product.getTermDays(),
                false,
                ""
            )
        );
        long expiresAt = product.getTermDays() == 0 ? 0 : purchase.getCreatedAt() + product.getTermDays() * 86400000L;
        for (String videoId : mCatalog.expandVideoIds(product)) {
            mGrants.save(
                new Grant(UUID.randomUUID().toString(), buyer.getId(), videoId, line.getId(), expiresAt, false)
            );
        }
    }

    public record CheckoutRequest(
        @NotNull List<String> productIds,
        @NotNull String requestKey,
        @NotNull String channel,
        @NotNull String outcome
    ) {}

    public record LineView(
        String id,
        String productId,
        String title,
        int priceWon,
        int channelFeeWon,
        int platformFeeWon,
        int sellerAmountWon,
        int termDays,
        boolean refunded,
        String settlementId
    ) {}

    public record OrderView(
        String id,
        String status,
        String channel,
        long createdAt,
        List<LineView> lines,
        int totalWon
    ) {}

    public record LibraryView(String id, CatalogService.ProductView product, long expiresAt, boolean active) {}
}
