package com.pickview.catalog;

import com.pickview.api.ApiFailure;
import com.pickview.domain.AccountId;
import com.pickview.domain.EAccessTerm;
import com.pickview.domain.ECategory;
import com.pickview.domain.EProductKind;
import com.pickview.domain.EPublicationAction;
import com.pickview.domain.ProductId;
import com.pickview.domain.ProductPrice;
import com.pickview.domain.WonAmount;
import com.pickview.model.Account;
import com.pickview.model.Audit;
import com.pickview.model.Engagement;
import com.pickview.model.Product;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.IAuditRepository;
import com.pickview.repository.IEngagementRepository;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.repository.IProductRepository;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final IProductRepository mProducts;
    private final IAccountRepository mAccounts;
    private final IEngagementRepository mEngagements;
    private final IAuditRepository mAudits;
    private final IOrderLineRepository mLines;

    public CatalogService(
        IProductRepository products,
        IAccountRepository accounts,
        IEngagementRepository engagements,
        IAuditRepository audits,
        IOrderLineRepository lines
    ) {
        mProducts = products;
        mAccounts = accounts;
        mEngagements = engagements;
        mAudits = audits;
        mLines = lines;
    }

    public Product requireProduct(ProductId id) {
        return mProducts
            .findById(id.getValue())
            .orElseThrow(() -> new ApiFailure(404, "영상을 찾을 수 없습니다. / Video not found."));
    }

    public List<ProductView> listPublished() {
        return mProducts
            .findAll()
            .stream()
            .filter(product -> product.getStatus().equals("APPROVED") && !product.isBlocked())
            .map(this::describeProduct)
            .toList();
    }

    public List<ProductView> listOwned(AccountId sellerId) {
        return mProducts
            .findAll()
            .stream()
            .filter(product -> product.getSellerId().equals(sellerId.getValue()))
            .map(this::describeProduct)
            .toList();
    }

    public ProductView describeProduct(Product product) {
        Account seller = mAccounts.findById(product.getSellerId()).orElseThrow();
        List<Engagement> reviews = mEngagements
            .findAll()
            .stream()
            .filter(item -> item.getKind().equals("REVIEW") && item.getTargetId().equals(product.getId()))
            .toList();
        double rating = reviews.stream().mapToDouble(Engagement::getNumberValue).average().orElse(0);
        long sales = mLines
            .findAll()
            .stream()
            .filter(line -> line.getProductId().equals(product.getId()) && !line.isRefunded())
            .count();
        return new ProductView(
            product.getId(),
            product.getSellerId(),
            seller.getDisplayName(),
            product.getTitle(),
            product.getDescription(),
            product.getCategory(),
            product.getPriceWon(),
            product.getTermDays(),
            product.getStatus(),
            product.getThumbnail(),
            product.getDurationSeconds(),
            product.getKind(),
            expandVideoIds(product).stream().map(ProductId::getValue).toList(),
            rating,
            reviews.size(),
            sales,
            product.getCreatedAt(),
            product.isBlocked(),
            product.getTags(),
            product.getMediaKey().equals("sample-" + product.getId() + ".mp4")
        );
    }

    public List<ProductId> expandVideoIds(Product product) {
        if (product.getKind().equals("VIDEO")) {
            return List.of(new ProductId(product.getId()));
        }
        return Arrays.stream(product.getBundleIds().split(","))
            .filter(id -> !id.isBlank())
            .map(ProductId::new)
            .toList();
    }

    @Transactional
    public Product createProduct(Account seller, ProductRequest request) {
        if (!seller.getSellerStatus().equals("APPROVED")) {
            throw new ApiFailure(403, "판매자 승인이 필요합니다. / Seller approval required.");
        }
        validateProduct(request);
        String bundleIds = "";
        if (request.kind() == EProductKind.BUNDLE) {
            if (
                request.videoIds().size() < 2 ||
                request.videoIds().stream().distinct().count() != request.videoIds().size()
            ) {
                throw new ApiFailure(400, "서로 다른 영상 2개 이상을 선택하세요. / Choose distinct videos.");
            }
            int individualPrice = 0;
            for (ProductId id : request.videoIds()) {
                Product video = requireProduct(id);
                if (
                    !video.getSellerId().equals(seller.getId()) ||
                    !video.getKind().equals("VIDEO") ||
                    !video.getStatus().equals("APPROVED")
                ) {
                    throw new ApiFailure(400, "본인의 승인된 영상만 묶을 수 있습니다. / Approved own videos only.");
                }
                individualPrice += video.getPriceWon();
            }
            if (request.priceWon().getAmount().exceeds(new WonAmount(individualPrice))) {
                throw new ApiFailure(400, "패키지는 개별 합계 이하로 설정하세요. / Bundle exceeds individual total.");
            }
            bundleIds = request.videoIds().stream().map(ProductId::getValue).collect(java.util.stream.Collectors.joining(","));
        }
        Product product = new Product(
            UUID.randomUUID().toString(),
            seller.getId(),
            request.title(),
            request.description(),
            request.category().name(),
            request.priceWon().getWon(),
            request.termDays().getDays(),
            "DRAFT",
            request.thumbnail(),
            "",
            "",
            0,
            request.kind().name(),
            bundleIds,
            false,
            System.currentTimeMillis()
        );
        product.changeTags(request.tags());
        return mProducts.save(product);
    }

    @Transactional
    public void updateProduct(Account seller, ProductId id, ProductRequest request) {
        Product product = requireOwnedProduct(seller, id);
        validateProduct(request);
        if (
            !product.getKind().equals(request.kind().name()) ||
            (product.getKind().equals("BUNDLE") && !expandVideoIds(product).equals(request.videoIds()))
        ) {
            throw new ApiFailure(
                400,
                "상품 유형과 패키지 구성은 변경할 수 없습니다. / Product composition is immutable."
            );
        }
        if (
            product.getKind().equals("BUNDLE") &&
            request.priceWon().getWon() >
                expandVideoIds(product).stream().map(this::requireProduct).mapToInt(Product::getPriceWon).sum()
        ) {
            throw new ApiFailure(400, "Bundle exceeds individual total");
        }
        product.revise(request.title(), request.description(), request.priceWon().getAmount(), request.termDays());
        product.changePresentation(request.category().name(), request.thumbnail());
        product.changeTags(request.tags());
        mAudits.save(
            new Audit(
                UUID.randomUUID().toString(),
                seller.getId(),
                "EDIT_PRODUCT",
                id.getValue(),
                request.title(),
                System.currentTimeMillis()
            )
        );
    }

    @Transactional
    public void changePublication(Account seller, ProductId id, EPublicationAction action) {
        Product product = requireOwnedProduct(seller, id);
        if (action == EPublicationAction.WITHDRAW) {
            product.withdraw();
            return;
        }
        if (action != EPublicationAction.SUBMIT) {
            throw new ApiFailure(400, "Invalid action");
        }
        if (product.getKind().equals("VIDEO") && product.getMediaKey().isBlank()) {
            throw new ApiFailure(400, "영상을 먼저 업로드하세요. / Upload a video first.");
        }
        product.submit();
    }

    public Product requireOwnedProduct(Account seller, ProductId id) {
        Product product = requireProduct(id);
        if (!product.getSellerId().equals(seller.getId())) {
            throw new ApiFailure(403, "본인의 상품만 변경할 수 있습니다. / Owner only.");
        }
        return product;
    }

    private void validateProduct(ProductRequest request) {
        if (
            request.title().isBlank() ||
            request.title().length() > 150 ||
            request.description().length() > 10000 ||
            request.tags().length() > 300 ||
            request.category() == null || request.kind() == null || request.termDays() == null || request.priceWon() == null ||
            !request.hasRights()
        ) {
            throw new ApiFailure(400, "상품 정보와 권리 확인을 점검하세요. / Invalid product details.");
        }
        if (!request.thumbnail().isEmpty() && !request.thumbnail().matches("[a-zA-Z0-9_-]+")) {
            throw new ApiFailure(400, "Invalid thumbnail key");
        }
    }

    public record ProductRequest(
        @NotNull String title,
        @NotNull String description,
        @NotNull ECategory category,
        @NotNull ProductPrice priceWon,
        @NotNull EAccessTerm termDays,
        @NotNull String thumbnail,
        @NotNull EProductKind kind,
        @NotNull List<ProductId> videoIds,
        boolean hasRights,
        String tags
    ) {
        public ProductRequest {
            if (title == null || description == null || category == null || priceWon == null
                || termDays == null || thumbnail == null || kind == null) {
                throw new ApiFailure(400, "Missing product details");
            }
            if (videoIds == null || videoIds.stream().anyMatch(java.util.Objects::isNull)) {
                throw new ApiFailure(400, "Invalid bundle selection");
            }
            videoIds = List.copyOf(videoIds);
            tags = tags == null ? "" : tags.strip();
        }
    }

    public record ProductView(
        String id,
        String sellerId,
        String sellerName,
        String title,
        String description,
        String category,
        int priceWon,
        int termDays,
        String status,
        String thumbnail,
        double durationSeconds,
        String kind,
        List<String> videoIds,
        double rating,
        int reviewCount,
        long sales,
        long createdAt,
        boolean blocked,
        String tags,
        boolean isDemo
    ) {
        public ProductView {
            videoIds = List.copyOf(videoIds);
        }
    }
}
