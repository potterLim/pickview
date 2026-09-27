package com.pickview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickview.api.ApiFailure;
import com.pickview.commerce.CommerceService;
import com.pickview.community.CommunityService;
import com.pickview.config.DemoContentUpgrade;
import com.pickview.domain.AccountId;
import com.pickview.domain.EActivityKind;
import com.pickview.domain.EApprovalDecision;
import com.pickview.domain.EPaymentChannel;
import com.pickview.domain.EPaymentOutcome;
import com.pickview.domain.ETicketKind;
import com.pickview.domain.ProductId;
import com.pickview.domain.TicketId;
import com.pickview.model.Account;
import com.pickview.model.Grant;
import com.pickview.model.OrderLine;
import com.pickview.model.Product;
import com.pickview.model.Purchase;
import com.pickview.model.Ticket;
import com.pickview.operations.OperationsService;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.IGrantRepository;
import com.pickview.repository.IOrderLineRepository;
import com.pickview.repository.IProductRepository;
import com.pickview.repository.IPurchaseRepository;
import com.pickview.repository.IRefundAdjustmentRepository;
import com.pickview.repository.ITicketRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:marketplace;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "pickview.seed=false",
        "pickview.storage.path=target/test-media",
    }
)
@Import(MarketplaceIntegrationTest.FixedTime.class)
@Transactional
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class MarketplaceIntegrationTest {

    private final IAccountRepository mAccounts;

    private final IProductRepository mProducts;

    private final IPurchaseRepository mPurchases;

    private final IOrderLineRepository mLines;

    private final IGrantRepository mGrants;

    private final ITicketRepository mTickets;

    private final IRefundAdjustmentRepository mAdjustments;

    private final CommerceService mCommerce;

    private final CommunityService mCommunity;

    private final OperationsService mOperations;

    private final com.pickview.catalog.CatalogService mCatalog;

    private final com.pickview.security.AccountService mAccountService;

    private final org.springframework.test.web.servlet.MockMvc mMvc;

    private Account mBuyer;
    private Account mAdmin;

    @Autowired
    MarketplaceIntegrationTest(
        IAccountRepository accounts,
        IProductRepository products,
        IPurchaseRepository purchases,
        IOrderLineRepository lines,
        IGrantRepository grants,
        ITicketRepository tickets,
        IRefundAdjustmentRepository adjustments,
        CommerceService commerce,
        CommunityService community,
        OperationsService operations,
        com.pickview.catalog.CatalogService catalog,
        com.pickview.security.AccountService accountService,
        org.springframework.test.web.servlet.MockMvc mvc
    ) {
        mAccounts = accounts;
        mProducts = products;
        mPurchases = purchases;
        mLines = lines;
        mGrants = grants;
        mTickets = tickets;
        mAdjustments = adjustments;
        mCommerce = commerce;
        mCommunity = community;
        mOperations = operations;
        mCatalog = catalog;
        mAccountService = accountService;
        mMvc = mvc;
    }

    @BeforeEach
    void prepareMarketplace() {
        mBuyer = saveAccount("buyer", "BUYER");
        mAdmin = saveAccount("admin", "ADMIN");
        saveAccount("seller", "BUYER");
        mProducts.save(
            new Product(
                "video",
                "seller",
                "Title",
                "Description",
                com.pickview.domain.ECategory.EDUCATION,
                20000,
                30,
                com.pickview.domain.EProductStatus.APPROVED,
                "studio",
                "test.mp4",
                "preview.mp4",
                30,
                com.pickview.domain.EProductKind.VIDEO,
                "",
                false,
                1
            )
        );
    }

    @Test
    void sampleUpgradePreservesPurchasesAndDoesNotOverwriteLaterUploads() throws Exception {
        Product sample = mProducts.save(
            new Product(
                "video-1",
                "seller",
                "Old sample",
                "Description",
                com.pickview.domain.ECategory.EDUCATION,
                1234,
                30,
                com.pickview.domain.EProductStatus.APPROVED,
                "studio",
                "demo.mp4",
                "demo-preview.mp4",
                30,
                com.pickview.domain.EProductKind.VIDEO,
                "",
                false,
                1
            )
        );
        mGrants.save(new Grant("sample-grant", "buyer", "video-1", "existing-line", 0, false));
        DemoContentUpgrade upgrade = new DemoContentUpgrade(mProducts, new ObjectMapper(), true);
        upgrade.run();
        assertEquals("sample-video-1.mp4", sample.getMediaKey());
        assertEquals(com.pickview.domain.EProductStatus.APPROVED, sample.getStatus());
        assertEquals(1234, sample.getPriceWon());
        assertTrue(mCommerce.canWatch(new AccountId("buyer"), new ProductId("video-1")));
        sample.replaceMedia("creator-upload.mp4", "creator-preview.mp4", 60);
        upgrade.run();
        assertEquals("creator-upload.mp4", sample.getMediaKey());
        assertEquals(com.pickview.domain.EProductStatus.PENDING, sample.getStatus());
    }

    @Test
    void expiredGrantsDenyPlaybackButAllowRepurchase() {
        mGrants.save(new Grant("expired", "buyer", "video", "old-line", 1, false));
        assertFalse(mCommerce.canWatch(new AccountId("buyer"), new ProductId("video")));
        mCommerce.checkout(mBuyer, new CommerceService.CheckoutRequest(List.of(new ProductId("video")), "new", EPaymentChannel.CARD, EPaymentOutcome.SUCCESS));
        assertTrue(mCommerce.canWatch(new AccountId("buyer"), new ProductId("video")));
    }

    @Test
    void settledRefundIsDeductedExactlyOnceFromNextPayout() {
        saveHistoricalLine("refunded", 15000, "previous-settlement");
        mGrants.save(new Grant("grant", "buyer", "video", "refunded", 0, false));
        mTickets.save(new Ticket("refund-ticket", "buyer", "refunded", "", com.pickview.domain.ETicketKind.REFUND, "Request", com.pickview.domain.ETicketStatus.OPEN, "", 1));
        mOperations.resolveTicket(mAdmin, new TicketId("refund-ticket"), "Approved", EApprovalDecision.APPROVE);
        assertFalse(mCommerce.canWatch(new AccountId("buyer"), new ProductId("video")));
        assertEquals(15000, mAdjustments.findById("refunded").orElseThrow().getAmountWon());
        assertEquals(-15000, mOperations.getSellerSettlementSummary(new AccountId("seller")).pendingWon());
        saveHistoricalLine("eligible", 30000, "");
        assertEquals(15000, mOperations.settle(mAdmin, new AccountId("seller")));
        assertTrue(mAdjustments.findPending("seller", "").isEmpty());
        assertEquals(0, mOperations.getSellerSettlementSummary(new AccountId("seller")).pendingWon());
        assertThrows(ApiFailure.class, () -> mOperations.settle(mAdmin, new AccountId("seller")));
        assertThrows(ApiFailure.class, () -> mOperations.resolveTicket(mAdmin, new TicketId("refund-ticket"), "Again", EApprovalDecision.APPROVE));
    }

    @Test
    void refundDebtCarriesForwardWhenNetPayoutIsBelowThreshold() {
        saveHistoricalLine("refunded", 15000, "previous-settlement");
        mTickets.save(new Ticket("refund-ticket", "buyer", "refunded", "", com.pickview.domain.ETicketKind.REFUND, "Request", com.pickview.domain.ETicketStatus.OPEN, "", 1));
        mOperations.resolveTicket(mAdmin, new TicketId("refund-ticket"), "Approved", EApprovalDecision.APPROVE);
        saveHistoricalLine("small", 20000, "");
        assertThrows(ApiFailure.class, () -> mOperations.settle(mAdmin, new AccountId("seller")));
        assertEquals(1, mAdjustments.findPending("seller", "").size());
        assertEquals("", mLines.findById("small").orElseThrow().getSettlementId());
    }

    @Test
    void blockingPreventsInquiriesButNotSafetyReports() {
        mCommunity.saveActivity(mBuyer, new CommunityService.ActivityRequest("seller", EActivityKind.BLOCK, "", 0));
        assertThrows(ApiFailure.class, () ->
            mCommunity.createTicket(mBuyer, new CommunityService.TicketRequest("video", ETicketKind.INQUIRY, "Hello"))
        );
        mCommunity.createTicket(mBuyer, new CommunityService.TicketRequest("video", ETicketKind.REPORT, "Safety report"));
        assertEquals(1, mCommunity.listTickets(new AccountId("buyer")).size());
    }

    @Test
    void catalogAggregatesReviewsAndSalesForAllProducts() {
        saveHistoricalLine("sale", 20000, "");
        mCommunity.saveActivity(mBuyer, new CommunityService.ActivityRequest("video", EActivityKind.REVIEW, "Useful", 4));
        com.pickview.catalog.CatalogService.ProductView product = mCatalog.listPublished().getFirst();
        assertEquals(4.0, product.rating());
        assertEquals(1, product.reviewCount());
        assertEquals(1, product.sales());
    }

    @Test
    void authenticationFindsOnlyValidTokensAndLogoutRevokesThem() {
        com.pickview.domain.EmailAddress email = new com.pickview.domain.EmailAddress("  USER@Test.local  ");
        com.pickview.domain.Password password = new com.pickview.domain.Password("Valid-password-2026!");
        Account account = mAccountService.register(email, password, "User", true);
        String token = mAccountService.login(email, password);
        assertEquals(account.getId(), mAccountService.authenticateOrNull(token).getId());
        mAccountService.logout(token);
        org.junit.jupiter.api.Assertions.assertNull(mAccountService.authenticateOrNull(token));
    }

    @Test
    void httpBoundaryRejectsOmittedDecisionsAndInvalidCartElements() throws Exception {
        mMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/sellers/seller")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
            .contentType("application/json").content("{}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/checkout")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("buyer"))
            .contentType("application/json")
            .content("{\"productIds\":[null],\"requestKey\":\"key\",\"channel\":\"CARD\",\"outcome\":\"SUCCESS\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/public/products"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].kind").value("VIDEO"));
    }

    private Account saveAccount(String id, String role) {
        return mAccounts.save(new Account(id, id + "@test.local", "unused", id, com.pickview.domain.ERole.valueOf(role), com.pickview.domain.ESellerStatus.APPROVED, "", "ko", ""));
    }

    private void saveHistoricalLine(String id, int amount, String settlement) {
        mPurchases.save(
            new Purchase(
                id + "-purchase",
                "buyer",
                id + "-key",
                com.pickview.domain.EOrderStatus.SUCCESS,
                com.pickview.domain.EPaymentChannel.CARD,
                Instant.parse("2026-08-10T00:00:00Z").toEpochMilli()
            )
        );
        mLines.saveAndFlush(
            new OrderLine(
                id,
                id + "-purchase",
                "buyer",
                "seller",
                "video",
                "Title",
                amount,
                0,
                0,
                amount,
                30,
                false,
                settlement
            )
        );
    }

    @TestConfiguration
    static class FixedTime {

        @Bean
        @Primary
        Clock createFixedClock() {
            return Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
