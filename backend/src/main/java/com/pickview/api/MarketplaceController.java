package com.pickview.api;

import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.community.CommunityService;
import com.pickview.model.Account;
import com.pickview.model.Product;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.security.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketplaceController {

    private final CatalogService mCatalog;
    private final CommerceService mCommerce;
    private final CommunityService mCommunity;
    private final AccountService mAccounts;
    private final IOrderLineRepository mLines;

    public MarketplaceController(
        CatalogService catalog,
        CommerceService commerce,
        CommunityService community,
        AccountService accounts,
        IOrderLineRepository lines
    ) {
        mCatalog = catalog;
        mCommerce = commerce;
        mCommunity = community;
        mAccounts = accounts;
        mLines = lines;
    }

    @GetMapping("/api/public/products")
    public List<CatalogService.ProductView> listProducts() {
        return mCatalog.listPublished();
    }

    @GetMapping("/api/public/products/{id}")
    public CatalogService.ProductView getProduct(@PathVariable String id) {
        Product product = mCatalog.requireProduct(id);
        if (!product.getStatus().equals("APPROVED") || product.isBlocked()) {
            throw new ApiFailure(404, "Product unavailable");
        }
        return mCatalog.describeProduct(product);
    }

    @GetMapping("/api/public/sellers/{id}")
    public Map<String, String> getSeller(@PathVariable String id) {
        Account seller = mAccounts.requireAccount(id);
        return Map.of("id", id, "displayName", seller.getDisplayName(), "bio", seller.getBio());
    }

    @GetMapping("/api/public/products/{id}/reviews")
    public List<CommunityService.EngagementView> getReviews(@PathVariable String id) {
        return mCommunity.listReviews(id);
    }

    @PostMapping("/api/checkout")
    public CommerceService.OrderView checkout(
        Principal principal,
        @Valid @RequestBody CommerceService.CheckoutRequest request
    ) {
        return mCommerce.checkout(mAccounts.requireAccount(principal.getName()), request);
    }

    @GetMapping("/api/orders")
    public List<CommerceService.OrderView> listOrders(Principal principal) {
        return mCommerce.listOrders(principal.getName());
    }

    @GetMapping("/api/library")
    public List<CommerceService.LibraryView> listLibrary(Principal principal) {
        return mCommerce.listLibrary(principal.getName());
    }

    @GetMapping("/api/activity")
    public List<CommunityService.EngagementView> listActivity(Principal principal) {
        return mCommunity.listActivity(principal.getName());
    }

    @PutMapping("/api/activity")
    public void saveActivity(Principal principal, @Valid @RequestBody CommunityService.ActivityRequest request) {
        mCommunity.saveActivity(mAccounts.requireAccount(principal.getName()), request);
    }

    @DeleteMapping("/api/activity/{kind}/{targetId}")
    public void deleteActivity(Principal principal, @PathVariable String kind, @PathVariable String targetId) {
        mCommunity.removeActivity(principal.getName(), kind, targetId);
    }

    @GetMapping("/api/tickets")
    public List<CommunityService.TicketView> listTickets(Principal principal) {
        return mCommunity.listTickets(principal.getName());
    }

    @PostMapping("/api/tickets")
    public void createTicket(Principal principal, @Valid @RequestBody CommunityService.TicketRequest request) {
        mCommunity.createTicket(mAccounts.requireAccount(principal.getName()), request);
    }

    @PostMapping("/api/tickets/{id}/reply")
    public void reply(Principal principal, @PathVariable String id, @Valid @RequestBody ReplyRequest request) {
        mCommunity.replyToInquiry(principal.getName(), id, request.reply());
    }

    @GetMapping("/api/notices")
    public List<CommunityService.NoticeView> listNotices(Principal principal) {
        return mCommunity.listNotices(principal.getName());
    }

    @PostMapping("/api/notices/{id}/read")
    public void readNotice(Principal principal, @PathVariable String id) {
        mCommunity.readNotice(principal.getName(), id);
    }

    @PutMapping("/api/settings")
    @Transactional
    public void saveSettings(Principal principal, @Valid @RequestBody SettingsRequest request) {
        if (!List.of("ko", "en").contains(request.language()) || request.interests().length() > 100) {
            throw new ApiFailure(400, "Invalid settings");
        }
        mAccounts.requireAccount(principal.getName()).changeSettings(request.language(), request.interests());
    }

    @PostMapping("/api/seller/apply")
    @Transactional
    public void applySeller(Principal principal, @Valid @RequestBody SellerRequest request) {
        if (
            request.displayName().isBlank() ||
            request.displayName().length() > 80 ||
            request.bio().length() > 1000 ||
            !List.of("PERSONAL", "BUSINESS").contains(request.type())
        ) {
            throw new ApiFailure(400, "Invalid seller profile");
        }
        Account account = mAccounts.requireAccount(principal.getName());
        if (account.getSellerStatus().equals("APPROVED")) {
            throw new ApiFailure(409, "Already approved");
        }
        account.applySeller(request.displayName(), request.type() + ": " + request.bio());
    }

    @GetMapping("/api/seller/products")
    public List<CatalogService.ProductView> listSellerProducts(Principal principal) {
        return mCatalog.listOwned(principal.getName());
    }

    @PutMapping("/api/seller/profile")
    @Transactional
    public void updateSellerProfile(Principal principal, @Valid @RequestBody SellerRequest request) {
        Account account = mAccounts.requireAccount(principal.getName());
        if (!account.getSellerStatus().equals("APPROVED")) {
            throw new ApiFailure(403, "Approved seller required");
        }
        if (request.displayName().isBlank() || request.displayName().length() > 80 || request.bio().length() > 1000) {
            throw new ApiFailure(400, "Invalid profile");
        }
        account.changeProfile(request.displayName(), request.bio());
    }

    @PostMapping("/api/seller/products")
    public CatalogService.ProductView createProduct(
        Principal principal,
        @Valid @RequestBody CatalogService.ProductRequest request
    ) {
        return mCatalog.describeProduct(mCatalog.createProduct(mAccounts.requireAccount(principal.getName()), request));
    }

    @PutMapping("/api/seller/products/{id}")
    public void updateProduct(
        Principal principal,
        @PathVariable String id,
        @Valid @RequestBody CatalogService.ProductRequest request
    ) {
        mCatalog.updateProduct(mAccounts.requireAccount(principal.getName()), id, request);
    }

    @PostMapping("/api/seller/products/{id}/{action}")
    public void publish(Principal principal, @PathVariable String id, @PathVariable String action) {
        mCatalog.changePublication(mAccounts.requireAccount(principal.getName()), id, action);
    }

    @GetMapping("/api/seller/sales")
    public List<CommerceService.LineView> listSales(Principal principal) {
        return mLines
            .findAll()
            .stream()
            .filter(line -> line.getSellerId().equals(principal.getName()))
            .map(mCommerce::describeLine)
            .toList();
    }

    public record ReplyRequest(@NotNull String reply) {}

    public record SellerRequest(@NotNull String displayName, @NotNull String bio, @NotNull String type) {}

    public record SettingsRequest(@NotNull String language, @NotNull String interests) {}
}
