package com.pickview.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickview.api.ApiFailure;
import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.domain.AccountId;
import com.pickview.domain.ProductId;
import com.pickview.domain.VideoDuration;
import java.time.Duration;
import java.time.Instant;
import com.pickview.model.Account;
import com.pickview.model.Product;
import com.pickview.repository.IAccountRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private static final Duration TRANSCODE_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PLAYBACK_TTL = Duration.ofHours(2);
    private static final Duration REVIEW_TTL = Duration.ofMinutes(15);

    private final MediaStorage mStorage;
    private final CatalogService mCatalog;
    private final CommerceService mCommerce;
    private final ObjectMapper mMapper;
    private final String mFfmpeg;
    private final String mFfprobe;
    private final IAccountRepository mAccounts;
    private final ConcurrentHashMap<String, PlaybackTicket> mTickets = new ConcurrentHashMap<>();

    public MediaService(
        MediaStorage storage,
        CatalogService catalog,
        CommerceService commerce,
        ObjectMapper mapper,
        @Value("${pickview.ffmpeg}") String ffmpeg,
        @Value("${pickview.ffprobe}") String ffprobe,
        IAccountRepository accounts
    ) {
        mStorage = storage;
        mCatalog = catalog;
        mCommerce = commerce;
        mMapper = mapper;
        mFfmpeg = ffmpeg;
        mFfprobe = ffprobe;
        mAccounts = accounts;
    }

    @Transactional(rollbackFor = Exception.class)
    public void uploadVideo(Account seller, ProductId productId, MultipartFile file, VideoDuration previewDuration)
        throws Exception {
        Product product = mCatalog.requireOwnedProduct(seller, productId);
        if (!seller.getSellerStatus().equals(com.pickview.domain.ESellerStatus.APPROVED) || !product.getKind().equals(com.pickview.domain.EProductKind.VIDEO)) {
            throw new ApiFailure(403, "Seller video required");
        }
        if (file.isEmpty() || file.getSize() > MAX_VIDEO_BYTES) {
            throw new ApiFailure(400, "100MB 이하 MP4를 선택하세요. / MP4 up to 100MB.");
        }
        try (
            TemporaryFile sourceFile = new TemporaryFile("pickview-upload-", ".mp4");
            TemporaryFile previewFile = new TemporaryFile("pickview-preview-", ".mp4")
        ) {
            Path source = sourceFile.getPath();
            Path preview = previewFile.getPath();
            file.transferTo(source);
            VideoDuration duration = validateVideo(source);
            duration.requireValidPreview(previewDuration);
            runProcess(
                List.of(
                    mFfmpeg,
                    "-y",
                    "-i",
                    source.toString(),
                    "-t",
                    Double.toString(previewDuration.getSeconds()),
                    "-c:v",
                    "libx264",
                    "-preset",
                    "ultrafast",
                    "-c:a",
                    "aac",
                    "-movflags",
                    "+faststart",
                    preview.toString()
                ),
                TRANSCODE_TIMEOUT
            );
            String key = UUID.randomUUID().toString();
            mStorage.storeFile(key + ".mp4", source, "video/mp4");
            mStorage.storeFile(key + "-preview.mp4", preview, "video/mp4");
            product.replaceMedia(key + ".mp4", key + "-preview.mp4", duration.getSeconds());
        }
    }

    public String issueTicket(AccountId buyerId, ProductId productId) {
        Product product = mCatalog.requireProduct(productId);
        if (product.isBlocked() || !mCommerce.canWatch(buyerId, productId)) {
            throw new ApiFailure(403, "시청 권한이 없습니다. / Playback access denied.");
        }
        mTickets.entrySet().removeIf(entry -> entry.getValue().getExpiresAt() < System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        mTickets.put(
            token,
            new PlaybackTicket(buyerId, productId, Instant.now().plus(PLAYBACK_TTL).toEpochMilli(), false)
        );
        return token;
    }

    public String issueReviewTicket(Account account, ProductId productId) {
        Product product = mCatalog.requireProduct(productId);
        if (!canInspect(account, product) || product.getMediaKey().isBlank()) {
            throw new ApiFailure(403, "Review access denied");
        }
        mTickets.entrySet().removeIf(entry -> entry.getValue().getExpiresAt() < System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        mTickets.put(
            token,
            new PlaybackTicket(new AccountId(account.getId()), productId, Instant.now().plus(REVIEW_TTL).toEpochMilli(), true)
        );
        return token;
    }

    public Path getStream(String token) throws Exception {
        PlaybackTicket ticketOrNull = mTickets.get(token);
        if (ticketOrNull == null || ticketOrNull.getExpiresAt() < System.currentTimeMillis()) {
            throw new ApiFailure(403, "Playback link expired");
        }
        Product product = mCatalog.requireProduct(ticketOrNull.getProductId());
        if (ticketOrNull.isReview()) {
            Account account = mAccounts
                .findById(ticketOrNull.getBuyerId().getValue())
                .orElseThrow(() -> new ApiFailure(403, "Account unavailable"));
            if (!canInspect(account, product)) {
                throw new ApiFailure(403, "Review access revoked");
            }
        } else if (product.isBlocked() || !mCommerce.canWatch(ticketOrNull.getBuyerId(), new ProductId(product.getId()))) {
            throw new ApiFailure(403, "Playback access revoked");
        }
        return mStorage.getFile(product.getMediaKey());
    }

    public Path getPreview(ProductId productId) throws Exception {
        Product product = mCatalog.requireProduct(productId);
        if (!product.getStatus().equals(com.pickview.domain.EProductStatus.APPROVED) || product.isBlocked() || product.getPreviewKey().isBlank()) {
            throw new ApiFailure(404, "Preview unavailable");
        }
        return mStorage.getFile(product.getPreviewKey());
    }

    private VideoDuration validateVideo(Path file) throws Exception {
        String output = runProcess(
            List.of(mFfprobe, "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", file.toString()),
            PROBE_TIMEOUT
        );
        JsonNode metadata = mMapper.readTree(output);
        double duration = metadata.path("format").path("duration").asDouble(0);
        boolean hasVideo = false;
        for (JsonNode stream : metadata.path("streams")) {
            String type = stream.path("codec_type").asText();
            if (type.equals("video")) {
                hasVideo = true;
                int width = stream.path("width").asInt();
                int height = stream.path("height").asInt();
                if (
                    !stream.path("codec_name").asText().equals("h264") ||
                    Math.min(width, height) > 1080 ||
                    Math.max(width, height) > 1920
                ) {
                    throw new ApiFailure(400, "H.264, 1080p 이하 영상이 필요합니다. / H.264 up to 1080p required.");
                }
            }
            if (type.equals("audio") && !stream.path("codec_name").asText().equals("aac")) {
                throw new ApiFailure(400, "AAC audio required");
            }
        }
        if (!hasVideo || !Double.isFinite(duration) || duration <= 0 || duration > 600) {
            throw new ApiFailure(400, "10분 이하 영상이 필요합니다. / Maximum 10 minutes.");
        }
        return new VideoDuration(duration);
    }

    private String runProcess(List<String> command, Duration timeout) throws Exception {
        try (TemporaryFile logFile = new TemporaryFile("pickview-media-", ".log")) {
            Path log = logFile.getPath();
            Process process = new ProcessBuilder(new ArrayList<>(command))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new ApiFailure(422, "Video processing timed out");
                }
                if (process.exitValue() != 0) {
                    throw new ApiFailure(422, "영상 처리에 실패했습니다. / Video processing failed.");
                }
                return Files.readString(log);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw failure;
            } finally {
                stopProcess(process);
            }
        }
    }

    private void stopProcess(Process process) throws InterruptedException {
        if (!process.isAlive()) {
            return;
        }
        boolean wasInterrupted = Thread.interrupted();
        process.destroyForcibly();
        try {
            if (!process.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Media process did not terminate");
            }
        } catch (InterruptedException failure) {
            wasInterrupted = true;
            throw failure;
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean canInspect(Account account, Product product) {
        return List.of(com.pickview.domain.ERole.ADMIN, com.pickview.domain.ERole.CONTENT).contains(account.getRole()) || product.getSellerId().equals(account.getId());
    }

    private static final class PlaybackTicket {
        private final AccountId mBuyerId;
        private final ProductId mProductId;
        private final long mExpiresAt;
        private final boolean mIsReview;

        private PlaybackTicket(AccountId buyerId, ProductId productId, long expiresAt, boolean isReview) {
            mBuyerId = buyerId;
            mProductId = productId;
            mExpiresAt = expiresAt;
            mIsReview = isReview;
        }

        private AccountId getBuyerId() {
            return mBuyerId;
        }

        private ProductId getProductId() {
            return mProductId;
        }

        private long getExpiresAt() {
            return mExpiresAt;
        }

        private boolean isReview() {
            return mIsReview;
        }
    }
}
