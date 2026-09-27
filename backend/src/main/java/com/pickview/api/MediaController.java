package com.pickview.api;

import com.pickview.media.MediaService;
import com.pickview.domain.ProductId;
import com.pickview.domain.AccountId;
import com.pickview.domain.VideoDuration;
import com.pickview.media.ThumbnailService;
import com.pickview.security.AccountService;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaController {

    private final MediaService mMedia;
    private final AccountService mAccounts;
    private final ThumbnailService mThumbnails;

    public MediaController(MediaService media, AccountService accounts, ThumbnailService thumbnails) {
        mMedia = media;
        mAccounts = accounts;
        mThumbnails = thumbnails;
    }

    @PostMapping("/api/seller/products/{id}/thumbnail")
    public void uploadThumbnail(Principal principal, @PathVariable String id, @RequestParam MultipartFile file)
        throws Exception {
        mThumbnails.upload(mAccounts.requireAccount(principal.getName()), new ProductId(id), file);
    }

    @GetMapping("/api/public/thumbnails/{id}")
    public ResponseEntity<FileSystemResource> getThumbnail(@PathVariable String id) throws Exception {
        return ResponseEntity.ok()
            .header("Content-Type", "image/png")
            .header("Cache-Control", "no-cache")
            .body(new FileSystemResource(mThumbnails.getPublicThumbnail(new ProductId(id))));
    }

    @PostMapping("/api/seller/products/{id}/upload")
    public void upload(
        Principal principal,
        @PathVariable String id,
        @RequestParam MultipartFile file,
        @RequestParam double previewSeconds
    ) throws Exception {
        mMedia.uploadVideo(mAccounts.requireAccount(principal.getName()), new ProductId(id), file, new VideoDuration(previewSeconds));
    }

    @PostMapping("/api/media/ticket/{id}")
    public Map<String, String> issueTicket(Principal principal, @PathVariable String id) {
        return Map.of("path", "/api/media/stream/" + mMedia.issueTicket(new AccountId(principal.getName()), new ProductId(id)));
    }

    @PostMapping("/api/media/review/{id}")
    public Map<String, String> issueReviewTicket(Principal principal, @PathVariable String id) {
        return Map.of(
            "path",
            "/api/media/stream/" + mMedia.issueReviewTicket(mAccounts.requireAccount(principal.getName()), new ProductId(id))
        );
    }

    @GetMapping("/api/media/stream/{token}")
    public ResponseEntity<FileSystemResource> getStream(@PathVariable String token) throws Exception {
        return serveVideo(mMedia.getStream(token));
    }

    @GetMapping("/api/public/preview/{id}")
    public ResponseEntity<FileSystemResource> getPreview(@PathVariable String id) throws Exception {
        return serveVideo(mMedia.getPreview(new ProductId(id)));
    }

    private ResponseEntity<FileSystemResource> serveVideo(Path file) {
        if (!java.nio.file.Files.exists(file)) {
            throw new ApiFailure(404, "Demo media has not been prepared. Run media setup.");
        }
        return ResponseEntity.ok()
            .header("Cache-Control", "private, no-store")
            .header("Accept-Ranges", "bytes")
            .header("Content-Type", "video/mp4")
            .body(new FileSystemResource(file));
    }
}
