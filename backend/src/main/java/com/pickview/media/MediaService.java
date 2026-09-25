package com.pickview.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickview.api.ApiFailure;
import com.pickview.catalog.CatalogService;
import com.pickview.commerce.CommerceService;
import com.pickview.model.Account;
import com.pickview.model.Product;
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
    private final MediaStorage mStorage;
    private final CatalogService mCatalog;
    private final CommerceService mCommerce;
    private final ObjectMapper mMapper;
    private final String mFfmpeg;
    private final String mFfprobe;
    private final ConcurrentHashMap<String, PlaybackTicket> mTickets = new ConcurrentHashMap<>();

    public MediaService(MediaStorage storage, CatalogService catalog, CommerceService commerce, ObjectMapper mapper,
                        @Value("${pickview.ffmpeg}") String ffmpeg, @Value("${pickview.ffprobe}") String ffprobe) {
        mStorage = storage; mCatalog = catalog; mCommerce = commerce; mMapper = mapper; mFfmpeg = ffmpeg; mFfprobe = ffprobe;
    }

    @Transactional(rollbackFor = Exception.class)
    public void uploadVideo(Account seller, String productId, MultipartFile file, double previewSeconds) throws Exception {
        Product product = mCatalog.requireOwnedProduct(seller, productId);
        if (!seller.getSellerStatus().equals("APPROVED") || !product.getKind().equals("VIDEO")) { throw new ApiFailure(403, "Seller video required"); }
        if (file.isEmpty() || file.getSize() > 100L * 1024 * 1024) { throw new ApiFailure(400, "100MB 이하 MP4를 선택하세요. / MP4 up to 100MB."); }
        Path source = Files.createTempFile("pickview-upload-", ".mp4");
        Path preview = Files.createTempFile("pickview-preview-", ".mp4");
        try {
            file.transferTo(source);
            double duration = validateVideo(source);
            if (!Double.isFinite(previewSeconds) || previewSeconds <= 0 || previewSeconds > Math.min(60, duration * .2)) {
                throw new ApiFailure(400, "미리보기는 전체의 20% 이내, 최대 60초입니다. / Preview limit exceeded.");
            }
            runProcess(List.of(mFfmpeg, "-y", "-i", source.toString(), "-t", Double.toString(previewSeconds),
                    "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", "-movflags", "+faststart", preview.toString()), 120);
            String key = UUID.randomUUID().toString();
            mStorage.storeFile(key + ".mp4", source, "video/mp4");
            mStorage.storeFile(key + "-preview.mp4", preview, "video/mp4");
            product.replaceMedia(key + ".mp4", key + "-preview.mp4", duration);
        } finally {
            Files.deleteIfExists(source); Files.deleteIfExists(preview);
        }
    }

    public String issueTicket(String buyerId, String productId) {
        Product product = mCatalog.requireProduct(productId);
        if (product.isBlocked() || !mCommerce.canWatch(buyerId, productId)) { throw new ApiFailure(403, "시청 권한이 없습니다. / Playback access denied."); }
        mTickets.entrySet().removeIf(entry -> entry.getValue().expiresAt() < System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        mTickets.put(token, new PlaybackTicket(buyerId, productId, System.currentTimeMillis() + 2 * 60 * 60 * 1000L));
        return token;
    }

    public Path getStream(String token) throws Exception {
        PlaybackTicket ticketOrNull = mTickets.get(token);
        if (ticketOrNull == null || ticketOrNull.expiresAt() < System.currentTimeMillis()) { throw new ApiFailure(403, "Playback link expired"); }
        Product product = mCatalog.requireProduct(ticketOrNull.productId());
        if (product.isBlocked() || !mCommerce.canWatch(ticketOrNull.buyerId(), product.getId())) { throw new ApiFailure(403, "Playback access revoked"); }
        return mStorage.getFile(product.getMediaKey());
    }

    public Path getPreview(String productId) throws Exception {
        Product product = mCatalog.requireProduct(productId);
        if (!product.getStatus().equals("APPROVED") || product.isBlocked() || product.getPreviewKey().isBlank()) { throw new ApiFailure(404, "Preview unavailable"); }
        return mStorage.getFile(product.getPreviewKey());
    }

    private double validateVideo(Path file) throws Exception {
        String output = runProcess(List.of(mFfprobe, "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", file.toString()), 30);
        JsonNode metadata = mMapper.readTree(output);
        double duration = metadata.path("format").path("duration").asDouble(0);
        boolean hasVideo = false;
        for (JsonNode stream : metadata.path("streams")) {
            String type = stream.path("codec_type").asText();
            if (type.equals("video")) {
                hasVideo = true;
                int width = stream.path("width").asInt(); int height = stream.path("height").asInt();
                if (!stream.path("codec_name").asText().equals("h264") || Math.min(width, height) > 1080 || Math.max(width, height) > 1920) {
                    throw new ApiFailure(400, "H.264, 1080p 이하 영상이 필요합니다. / H.264 up to 1080p required.");
                }
            }
            if (type.equals("audio") && !stream.path("codec_name").asText().equals("aac")) { throw new ApiFailure(400, "AAC audio required"); }
        }
        if (!hasVideo || !Double.isFinite(duration) || duration <= 0 || duration > 600) { throw new ApiFailure(400, "10분 이하 영상이 필요합니다. / Maximum 10 minutes."); }
        return duration;
    }

    private String runProcess(List<String> command, int timeoutSeconds) throws Exception {
        Path log = Files.createTempFile("pickview-media-", ".log");
        try {
            Process process = new ProcessBuilder(new ArrayList<>(command)).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new ApiFailure(422, "Video processing timed out"); }
            if (process.exitValue() != 0) { throw new ApiFailure(422, "영상 처리에 실패했습니다. / Video processing failed."); }
            return Files.readString(log);
        } finally { Files.deleteIfExists(log); }
    }

    private record PlaybackTicket(String buyerId, String productId, long expiresAt) {}
}
