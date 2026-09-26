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
class MarketplaceIntegrationTest {

    @Autowired
    private IAccountRepository mAccounts;

    @Autowired
    private IProductRepository mProducts;

    @Autowired
    private IPurchaseRepository mPurchases;

    @Autowired
    private IOrderLineRepository mLines;

    @Autowired
    private IGrantRepository mGrants;

    @Autowired
    private ITicketRepository mTickets;

    @Autowired
    private IRefundAdjustmentRepository mAdjustments;

    @Autowired
    private CommerceService mCommerce;

    @Autowired
    private CommunityService mCommunity;

    @Autowired
    private OperationsService mOperations;

    private Account mBuyer;
    private Account mAdmin;

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
                "EDUCATION",
                20000,
                30,
                "APPROVED",
                "studio",
                "test.mp4",
                "preview.mp4",
                30,
                "VIDEO",
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
                "EDUCATION",
                1234,
                30,
                "APPROVED",
                "studio",
                "demo.mp4",
                "demo-preview.mp4",
                30,
                "VIDEO",
                "",
                false,
                1
            )
        );
        mGrants.save(new Grant("sample-grant", "buyer", "video-1", "existing-line", 0, false));
        DemoContentUpgrade upgrade = new DemoContentUpgrade(mProducts, new ObjectMapper(), true);
        upgrade.run();
        assertEquals("sample-video-1.mp4", sample.getMediaKey());
        assertEquals("APPROVED", sample.getStatus());
        assertEquals(1234, sample.getPriceWon());
        assertTrue(mCommerce.canWatch("buyer", "video-1"));
        sample.replaceMedia("creator-upload.mp4", "creator-preview.mp4", 60);
        upgrade.run();
        assertEquals("creator-upload.mp4", sample.getMediaKey());
        assertEquals("PENDING", sample.getStatus());
    }

    @Test
    void expiredGrantsDenyPlaybackButAllowRepurchase() {
        mGrants.save(new Grant("expired", "buyer", "video", "old-line", 1, false));
        assertFalse(mCommerce.canWatch("buyer", "video"));
        mCommerce.checkout(mBuyer, new CommerceService.CheckoutRequest(List.of("video"), "new", "CARD", "SUCCESS"));
        assertTrue(mCommerce.canWatch("buyer", "video"));
    }

    @Test
    void settledRefundIsDeductedExactlyOnceFromNextPayout() {
        saveHistoricalLine("refunded", 15000, "previous-settlement");
        mGrants.save(new Grant("grant", "buyer", "video", "refunded", 0, false));
        mTickets.save(new Ticket("refund-ticket", "buyer", "refunded", "", "REFUND", "Request", "OPEN", "", 1));
        mOperations.resolveTicket(mAdmin, "refund-ticket", "Approved", true);
        assertFalse(mCommerce.canWatch("buyer", "video"));
        assertEquals(15000, mAdjustments.findById("refunded").orElseThrow().getAmountWon());
        assertEquals(-15000, mOperations.getSellerSettlementSummary("seller").get("pendingWon"));
        saveHistoricalLine("eligible", 30000, "");
        assertEquals(15000, mOperations.settle(mAdmin, "seller"));
        assertTrue(mAdjustments.findPending("seller", "").isEmpty());
        assertEquals(0, mOperations.getSellerSettlementSummary("seller").get("pendingWon"));
        assertThrows(ApiFailure.class, () -> mOperations.settle(mAdmin, "seller"));
        assertThrows(ApiFailure.class, () -> mOperations.resolveTicket(mAdmin, "refund-ticket", "Again", true));
    }

    @Test
    void refundDebtCarriesForwardWhenNetPayoutIsBelowThreshold() {
        saveHistoricalLine("refunded", 15000, "previous-settlement");
        mTickets.save(new Ticket("refund-ticket", "buyer", "refunded", "", "REFUND", "Request", "OPEN", "", 1));
        mOperations.resolveTicket(mAdmin, "refund-ticket", "Approved", true);
        saveHistoricalLine("small", 20000, "");
        assertThrows(ApiFailure.class, () -> mOperations.settle(mAdmin, "seller"));
        assertEquals(1, mAdjustments.findPending("seller", "").size());
        assertEquals("", mLines.findById("small").orElseThrow().getSettlementId());
    }

    @Test
    void blockingPreventsInquiriesButNotSafetyReports() {
        mCommunity.saveActivity(mBuyer, new CommunityService.ActivityRequest("seller", "BLOCK", "", 0));
        assertThrows(ApiFailure.class, () ->
            mCommunity.createTicket(mBuyer, new CommunityService.TicketRequest("video", "INQUIRY", "Hello"))
        );
        mCommunity.createTicket(mBuyer, new CommunityService.TicketRequest("video", "REPORT", "Safety report"));
        assertEquals(1, mCommunity.listTickets("buyer").size());
    }

    private Account saveAccount(String id, String role) {
        return mAccounts.save(new Account(id, id + "@test.local", "unused", id, role, "APPROVED", "", "ko", ""));
    }

    private void saveHistoricalLine(String id, int amount, String settlement) {
        mPurchases.save(
            new Purchase(
                id + "-purchase",
                "buyer",
                id + "-key",
                "SUCCESS",
                "CARD",
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
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
