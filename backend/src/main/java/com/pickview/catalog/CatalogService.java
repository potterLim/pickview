package com.pickview.catalog;

import jakarta.validation.constraints.NotNull;

import com.pickview.api.ApiFailure;
import com.pickview.model.Account;
import com.pickview.model.Product;
import com.pickview.model.Engagement;
import com.pickview.model.Audit;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.IProductRepository;
import com.pickview.repository.IEngagementRepository;
import com.pickview.repository.IAuditRepository;
import com.pickview.repository.IOrderLineRepository;
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

    public CatalogService(IProductRepository products, IAccountRepository accounts,
                          IEngagementRepository engagements, IAuditRepository audits, IOrderLineRepository lines) {
        mProducts = products;
        mAccounts = accounts;
        mEngagements = engagements;
        mAudits = audits;
        mLines = lines;
    }

    public Product requireProduct(String id) {
        return mProducts.findById(id).orElseThrow(() -> new ApiFailure(404, "영상을 찾을 수 없습니다. / Video not found."));
    }

    public List<ProductView> listPublished() {
        return mProducts.findAll().stream().filter(product -> product.getStatus().equals("APPROVED") && !product.isBlocked())
                .map(this::describeProduct).toList();
    }

    public List<ProductView> listOwned(String sellerId) {
        return mProducts.findAll().stream().filter(product -> product.getSellerId().equals(sellerId)).map(this::describeProduct).toList();
    }

    public ProductView describeProduct(Product product) {
        Account seller = mAccounts.findById(product.getSellerId()).orElseThrow();
        List<Engagement> reviews = mEngagements.findAll().stream()
                .filter(item -> item.getKind().equals("REVIEW") && item.getTargetId().equals(product.getId())).toList();
        double rating = reviews.stream().mapToDouble(Engagement::getNumberValue).average().orElse(0);
        long sales = mLines.findAll().stream().filter(line -> line.getProductId().equals(product.getId()) && !line.isRefunded()).count();
        return new ProductView(product.getId(), product.getSellerId(), seller.getDisplayName(), product.getTitle(),
                product.getDescription(), product.getCategory(), product.getPriceWon(), product.getTermDays(),
                product.getStatus(), product.getThumbnail(), product.getDurationSeconds(), product.getKind(),
                expandVideoIds(product), rating, reviews.size(), sales, product.getCreatedAt(), product.isBlocked(), product.getTags(),
                product.getMediaKey().equals("sample-" + product.getId() + ".mp4"));
    }

    public List<String> expandVideoIds(Product product) {
        if (product.getKind().equals("VIDEO")) { return List.of(product.getId()); }
        return Arrays.stream(product.getBundleIds().split(",")).filter(id -> !id.isBlank()).toList();
    }

    @Transactional
    public Product createProduct(Account seller, ProductRequest request) {
        if (!seller.getSellerStatus().equals("APPROVED")) { throw new ApiFailure(403, "판매자 승인이 필요합니다. / Seller approval required."); }
        validateProduct(request);
        String bundleIds = "";
        if (request.kind().equals("BUNDLE")) {
            if (request.videoIds().size() < 2 || request.videoIds().stream().distinct().count() != request.videoIds().size()) {
                throw new ApiFailure(400, "서로 다른 영상 2개 이상을 선택하세요. / Choose distinct videos.");
            }
            int individualPrice = 0;
            for (String id : request.videoIds()) {
                Product video = requireProduct(id);
                if (!video.getSellerId().equals(seller.getId()) || !video.getKind().equals("VIDEO") || !video.getStatus().equals("APPROVED")) {
                    throw new ApiFailure(400, "본인의 승인된 영상만 묶을 수 있습니다. / Approved own videos only.");
                }
                individualPrice += video.getPriceWon();
            }
            if (request.priceWon() > individualPrice) { throw new ApiFailure(400, "패키지는 개별 합계 이하로 설정하세요. / Bundle exceeds individual total."); }
            bundleIds = String.join(",", request.videoIds());
        }
        Product product = new Product(UUID.randomUUID().toString(), seller.getId(), request.title(), request.description(),
                request.category(), request.priceWon(), request.termDays(), "DRAFT", request.thumbnail(), "", "", 0,
                request.kind(), bundleIds, false, System.currentTimeMillis());
        product.changeTags(request.tags());
        return mProducts.save(product);
    }

    @Transactional
    public void updateProduct(Account seller, String id, ProductRequest request) {
        Product product = requireOwnedProduct(seller, id);
        validateProduct(request);
        product.revise(request.title(), request.description(), request.priceWon(), request.termDays());
        if (!product.getKind().equals(request.kind()) || (product.getKind().equals("BUNDLE")
                && !expandVideoIds(product).equals(request.videoIds()))) {
            throw new ApiFailure(400, "상품 유형과 패키지 구성은 변경할 수 없습니다. / Product composition is immutable.");
        }
        if (product.getKind().equals("BUNDLE") && request.priceWon() > expandVideoIds(product).stream()
                .map(this::requireProduct).mapToInt(Product::getPriceWon).sum()) {
            throw new ApiFailure(400, "Bundle exceeds individual total");
        }
        product.changePresentation(request.category(), request.thumbnail());
        product.changeTags(request.tags());
        mAudits.save(new Audit(UUID.randomUUID().toString(), seller.getId(), "EDIT_PRODUCT", id, request.title(), System.currentTimeMillis()));
    }

    @Transactional
    public void changePublication(Account seller, String id, String action) {
        Product product = requireOwnedProduct(seller, id);
        if (action.equals("WITHDRAW")) { product.withdraw(); return; }
        if (!action.equals("SUBMIT")) { throw new ApiFailure(400, "Invalid action"); }
        if (product.getKind().equals("VIDEO") && product.getMediaKey().isBlank()) { throw new ApiFailure(400, "영상을 먼저 업로드하세요. / Upload a video first."); }
        product.submit();
    }

    public Product requireOwnedProduct(Account seller, String id) {
        Product product = requireProduct(id);
        if (!product.getSellerId().equals(seller.getId())) { throw new ApiFailure(403, "본인의 상품만 변경할 수 있습니다. / Owner only."); }
        return product;
    }

    private void validateProduct(ProductRequest request) {
        if (request.title().isBlank() || request.title().length() > 150 || request.description().length() > 10000 || request.tags().length() > 300
                || !List.of("EDUCATION", "FINANCE", "COMEDY").contains(request.category())
                || !List.of(0, 7, 30, 90).contains(request.termDays())
                || request.priceWon() < 0 || request.priceWon() > 1000000
                || (request.priceWon() != 0 && (request.priceWon() < 1000 || request.priceWon() % 100 != 0))
                || !List.of("VIDEO", "BUNDLE").contains(request.kind()) || !request.hasRights()) {
            throw new ApiFailure(400, "상품 정보와 권리 확인을 점검하세요. / Invalid product details.");
        }
        if (!request.thumbnail().isEmpty() && !request.thumbnail().matches("[a-zA-Z0-9_-]+")) {
            throw new ApiFailure(400, "Invalid thumbnail key");
        }
    }

    public record ProductRequest(@NotNull String title, @NotNull String description, @NotNull String category, int priceWon, int termDays,
                                 @NotNull String thumbnail, @NotNull String kind, @NotNull List<String> videoIds, boolean hasRights, String tags) {
        public ProductRequest { tags = tags == null ? "" : tags.strip(); }
    }
    public record ProductView(String id, String sellerId, String sellerName, String title, String description,
                              String category, int priceWon, int termDays, String status, String thumbnail,
                              double durationSeconds, String kind, List<String> videoIds, double rating,
                              int reviewCount, long sales, long createdAt, boolean blocked, String tags, boolean isDemo) {}
}
