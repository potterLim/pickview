package com.pickview.media;

import com.pickview.api.ApiFailure;
import com.pickview.catalog.CatalogService;
import com.pickview.domain.EProductStatus;
import com.pickview.domain.ESellerStatus;
import com.pickview.domain.ProductId;
import com.pickview.model.Account;
import com.pickview.model.Product;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ThumbnailService {

    private static final int MIN_WIDTH_PIXELS = 160;
    private static final int MIN_HEIGHT_PIXELS = 90;
    private static final int MAX_EDGE_PIXELS = 4096;
    private static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;

    private final CatalogService mCatalog;
    private final MediaStorage mStorage;

    public ThumbnailService(CatalogService catalog, MediaStorage storage) {
        mCatalog = catalog;
        mStorage = storage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void upload(Account account, ProductId productId, MultipartFile file) throws Exception {
        Product product = mCatalog.requireOwnedProduct(account, productId);
        if (!account.getSellerStatus().equals(ESellerStatus.APPROVED)) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "Approved seller required");
        }
        if (file.isEmpty() || file.getSize() > MAX_THUMBNAIL_BYTES) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "JPG/PNG up to 5MB required");
        }
        BufferedImage image = decodeImage(file);
        Path output = Files.createTempFile("pickview-thumbnail-", ".png");
        try {
            // Re-encode pixels so uploaded metadata and arbitrary trailing data are not served.
            ImageIO.write(image, "png", output.toFile());
            String key = UUID.randomUUID().toString();
            mStorage.storeFile(key + ".png", output, "image/png");
            product.changePresentation(product.getCategory(), key);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    public Path getPublicThumbnail(ProductId id) throws Exception {
        Product product = mCatalog.requireProduct(id);
        if (!product.getStatus().equals(EProductStatus.APPROVED)
            || product.isBlocked()
            || !product.getThumbnail().matches("[a-f0-9-]{36}")) {
            throw new ApiFailure(HttpStatus.NOT_FOUND, "Thumbnail unavailable");
        }
        return mStorage.getFile(product.getThumbnail() + ".png");
    }

    private BufferedImage decodeImage(MultipartFile file) throws Exception {
        try (
            InputStream source = file.getInputStream();
            ImageInputStream input = ImageIO.createImageInputStream(source)
        ) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid image");
            }
            ImageReader reader = readers.next();
            try {
                if (!List.of("png", "jpeg").contains(reader.getFormatName().toLowerCase(java.util.Locale.ROOT))) {
                    throw new ApiFailure(HttpStatus.BAD_REQUEST, "JPG/PNG required");
                }
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < MIN_WIDTH_PIXELS
                    || height < MIN_HEIGHT_PIXELS
                    || width > MAX_EDGE_PIXELS
                    || height > MAX_EDGE_PIXELS) {
                    throw new ApiFailure(HttpStatus.BAD_REQUEST, "Image dimensions must be between 160×90 and 4096×4096");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}
