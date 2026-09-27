package com.pickview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickview.api.ApiFailure;
import com.pickview.commerce.CommerceService;
import com.pickview.domain.AccountId;
import com.pickview.domain.EAccessTerm;
import com.pickview.domain.EFeeRate;
import com.pickview.domain.EPaymentChannel;
import com.pickview.domain.ProductId;
import com.pickview.domain.ProductPrice;
import com.pickview.domain.WonAmount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DomainValueTest {

    @Test
    void wireCheckoutRetainsTypedIdentifiersAndChoices() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
        {"productIds":["video"],"requestKey":"checkout","channel":"CARD","outcome":"SUCCESS"}
        """;
        CommerceService.CheckoutRequest request = mapper.readValue(json, CommerceService.CheckoutRequest.class);
        assertEquals(new ProductId("video"), request.productIds().getFirst());
        assertEquals(EPaymentChannel.CARD, request.channel());
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(request)));
        assertNotEquals(new AccountId("video"), new ProductId("video"));
    }

    @Test
    void feesUseWonRoundingWithoutIntermediateOverflow() {
        WonAmount price = new WonAmount(20_000);
        WonAmount channelFee = price.calculateFee(EFeeRate.MOCK_CHANNEL);
        WonAmount platformFee = price.subtract(channelFee).calculateFee(EFeeRate.PLATFORM);
        assertEquals(600, channelFee.getWon());
        assertEquals(2_910, platformFee.getWon());
        assertEquals(16_490, price.subtract(channelFee).subtract(platformFee).getWon());
        assertEquals(322_122_547, new WonAmount(Integer.MAX_VALUE).calculateFee(EFeeRate.PLATFORM).getWon());
        assertThrows(ArithmeticException.class, () -> new WonAmount(Integer.MAX_VALUE).add(new WonAmount(1)));
    }

    @Test
    void priceAndTermRejectInvalidChoices() {
        assertThrows(ApiFailure.class, () -> new ProductPrice(1_050));
        assertThrows(ApiFailure.class, () -> EAccessTerm.parseDays(31));
        Instant purchase = Instant.parse("2026-09-27T00:00:00Z");
        assertEquals(0, EAccessTerm.PERPETUAL.calculateExpiry(purchase));
        assertEquals(
            Instant.parse("2026-10-04T00:00:00Z").toEpochMilli(),
            EAccessTerm.ONE_WEEK.calculateExpiry(purchase)
        );
    }
}
