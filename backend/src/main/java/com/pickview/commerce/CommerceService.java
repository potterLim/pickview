package com.pickview.commerce;

import com.pickview.api.ApiFailure;
import com.pickview.catalog.CatalogService;
import com.pickview.domain.AccountId;
import com.pickview.domain.EAccessTerm;
import com.pickview.domain.EFeeRate;
import com.pickview.domain.EOrderStatus;
import com.pickview.domain.EPaymentChannel;
import com.pickview.domain.EPaymentOutcome;
import com.pickview.domain.EProductStatus;
import com.pickview.domain.ProductId;
import com.pickview.domain.WonAmount;
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
import org.springframework.http.HttpStatus;
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

    public boolean canWatch(AccountId buyerId, ProductId videoId) {
        return mGrants.hasValidGrant(buyerId.getValue(), videoId.getValue(), System.currentTimeMillis());
    }

    @Transactional
    public OrderView checkout(Account buyer, CheckoutRequest request) {
        // Serialize a buyer's checkouts across server instances to prevent double grants.
        mEntityManager.find(Account.class, buyer.getId(), LockModeType.PESSIMISTIC_WRITE);
        Purchase previousOrNull = mPurchases.findAll().stream()
            .filter(purchase -> purchase.getBuyerId().equals(buyer.getId()) && purchase.getRequestKey().equals(request.requestKey()))
            .findFirst()
            .orElse(null);
        if (previousOrNull != null) {
            return describeOrder(previousOrNull);
        }
        if (request.requestKey().isBlank()
            || request.requestKey().length() > 100
            || request.productIds().isEmpty()
            || request.productIds().size() > 30
            || request.channel() == null
            || request.outcome() == null) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "결제 요청을 확인하세요. / Invalid checkout.");
        }
        List<Product> products = request.productIds().stream().map(mCatalog::requireProduct).toList();
        validateCart(buyer, products);
        Purchase purchase = mPurchases.save(
            new Purchase(
                UUID.randomUUID().toString(),
                buyer.getId(),
                request.requestKey(),
                EOrderStatus.valueOf(request.outcome().name()),
                EPaymentChannel.valueOf(request.channel().name()),
                System.currentTimeMillis()
            )
        );
        if (request.outcome() != EPaymentOutcome.SUCCESS) {
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

    public List<OrderView> listOrders(AccountId buyerId) {
        return mPurchases.findAll().stream()
            .filter(purchase -> purchase.getBuyerId().equals(buyerId.getValue()))
            .map(this::describeOrder)
            .toList();
    }

    public List<LibraryView> listLibrary(AccountId buyerId) {
        return mGrants.findAll().stream()
            .filter(grant -> grant.getBuyerId().equals(buyerId.getValue()) && !grant.isRevoked())
            .map(grant ->
                new LibraryView(
                    grant.getId(),
                    mCatalog.describeProduct(mCatalog.requireProduct(new ProductId(grant.getProductId()))),
                    grant.getExpiresAt(),
                    grant.isValid(System.currentTimeMillis())
                )
            )
            .toList();
    }

    public OrderView describeOrder(Purchase purchase) {
        List<LineView> lines = mLines.findAll().stream()
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
        Set<ProductId> videoIds = new HashSet<>();
        for (Product product : products) {
            if (!product.getStatus().equals(EProductStatus.APPROVED)
                || product.isBlocked()
                || product.getSellerId().equals(buyer.getId())) {
                throw new ApiFailure(HttpStatus.CONFLICT, "구매할 수 없는 상품입니다. / Product unavailable or self purchase.");
            }
            for (ProductId videoId : mCatalog.expandVideoIds(product)) {
                if (mCatalog.requireProduct(videoId).isBlocked()
                    || !videoIds.add(videoId)
                    || canWatch(new AccountId(buyer.getId()), videoId)) {
                    throw new ApiFailure(HttpStatus.CONFLICT, "중복되거나 이용할 수 없는 영상입니다. / Duplicate or unavailable video.");
                }
            }
        }
    }

    private void grantProduct(Account buyer, Purchase purchase, Product product) {
        WonAmount price = new WonAmount(product.getPriceWon());
        WonAmount channelFee = price.calculateFee(EFeeRate.MOCK_CHANNEL);
        WonAmount platformFee = price.subtract(channelFee).calculateFee(EFeeRate.PLATFORM);
        OrderLine line = mLines.save(
            new OrderLine(
                UUID.randomUUID().toString(),
                purchase.getId(),
                buyer.getId(),
                product.getSellerId(),
                product.getId(),
                product.getTitle(),
                product.getPriceWon(),
                channelFee.getWon(),
                platformFee.getWon(),
                price.subtract(channelFee).subtract(platformFee).getWon(),
                product.getTermDays(),
                false,
                ""
            )
        );
        long expiresAt = EAccessTerm.parseDays(product.getTermDays()).calculateExpiry(java.time.Instant.ofEpochMilli(purchase.getCreatedAt()));
        for (ProductId videoId : mCatalog.expandVideoIds(product)) {
            mGrants.save(
                new Grant(
                    UUID.randomUUID().toString(),
                    buyer.getId(),
                    videoId.getValue(),
                    line.getId(),
                    expiresAt,
                    false
                )
            );
        }
    }

    public record CheckoutRequest(
        @NotNull List<ProductId> productIds,
        @NotNull String requestKey,
        @NotNull EPaymentChannel channel,
        @NotNull EPaymentOutcome outcome
    ) {
        public CheckoutRequest {
            if (requestKey == null
                || requestKey.isBlank()
                || requestKey.length() > 100
                || channel == null
                || outcome == null) {
                throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid checkout request");
            }
            if (productIds == null
                || productIds.isEmpty()
                || productIds.size() > 30
                || productIds.stream().anyMatch(java.util.Objects::isNull)) {
                throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid product selection");
            }
            productIds = List.copyOf(productIds);
        }
    }

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
        EOrderStatus status,
        EPaymentChannel channel,
        long createdAt,
        List<LineView> lines,
        int totalWon
    ) {
        public OrderView {
            lines = List.copyOf(lines);
        }
    }

    public record LibraryView(String id, CatalogService.ProductView product, long expiresAt, boolean active) {}
}
