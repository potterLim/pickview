package com.pickview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickview.api.ApiFailure;
import com.pickview.api.OperationsController;
import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.domain.EApprovalDecision;
import com.pickview.domain.EPaymentChannel;
import com.pickview.domain.EPaymentOutcome;
import com.pickview.domain.Password;
import com.pickview.domain.ProductId;
import com.pickview.media.MediaStorage;
import com.pickview.media.ThumbnailService;
import com.pickview.model.Account;
import com.pickview.model.Product;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BoundaryValidationTest {

    @Test
    void missingApprovalNeverDefaultsToRejection() throws Exception {
        OperationsController.ApprovalRequest request = new ObjectMapper().readValue(
            "{}", OperationsController.ApprovalRequest.class
        );
        assertThrows(ApiFailure.class, request::toDecision);
        assertEquals(EApprovalDecision.REJECT, new OperationsController.ApprovalRequest(false).toDecision());
    }

    @Test
    void checkoutRejectsNullElementsAndCopiesItsInput() {
        assertThrows(ApiFailure.class, () -> new CommerceService.CheckoutRequest(
            Arrays.asList((ProductId) null), "key", EPaymentChannel.CARD, EPaymentOutcome.SUCCESS
        ));
        List<ProductId> ids = new ArrayList<>(List.of(new ProductId("video")));
        CommerceService.CheckoutRequest request = new CommerceService.CheckoutRequest(ids, "key", EPaymentChannel.CARD, EPaymentOutcome.SUCCESS);
        ids.clear();
        assertEquals(List.of(new ProductId("video")), request.productIds());
        assertThrows(UnsupportedOperationException.class, () -> request.productIds().clear());
    }

    @Test
    void passwordEnforcesEncodedByteLimitBeforeHashing() {
        assertThrows(ApiFailure.class, () -> new Password("가".repeat(30)));
        Password password = new Password("가".repeat(24));
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(password.matches(encoder, password.encode(encoder)));
    }

    @Test
    void thumbnailClosesItsOriginalInputStream() throws Exception {
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(160, 90, BufferedImage.TYPE_INT_RGB), "png", imageBytes);
        AtomicBoolean isClosed = new AtomicBoolean(false);
        InputStream source = new ByteArrayInputStream(imageBytes.toByteArray()) {
            @Override
            public void close() throws IOException {
                isClosed.set(true);
                super.close();
            }
        };
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", imageBytes.toByteArray()) {
            @Override
            public InputStream getInputStream() {
                return source;
            }
        };
        CatalogService catalog = mock(CatalogService.class);
        MediaStorage storage = mock(MediaStorage.class);
        Account seller = mock(Account.class);
        Product product = mock(Product.class);
        when(seller.getSellerStatus()).thenReturn(com.pickview.domain.ESellerStatus.APPROVED);
        when(catalog.requireOwnedProduct(seller, new ProductId("video"))).thenReturn(product);
        new ThumbnailService(catalog, storage).upload(seller, new ProductId("video"), file);
        assertTrue(isClosed.get());
    }
}
