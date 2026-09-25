package com.pickview.api;

import com.pickview.media.MediaService;
import com.pickview.security.AccountService;
import java.security.Principal;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaController {
    private final MediaService mMedia;
    private final AccountService mAccounts;

    public MediaController(MediaService media, AccountService accounts) { mMedia = media; mAccounts = accounts; }

    @PostMapping("/api/seller/products/{id}/upload")
    public void upload(Principal principal, @PathVariable String id, @RequestParam MultipartFile file,
                       @RequestParam double previewSeconds) throws Exception {
        mMedia.uploadVideo(mAccounts.requireAccount(principal.getName()), id, file, previewSeconds);
    }

    @PostMapping("/api/media/ticket/{id}")
    public Map<String, String> issueTicket(Principal principal, @PathVariable String id) {
        return Map.of("path", "/api/media/stream/" + mMedia.issueTicket(principal.getName(), id));
    }

    @GetMapping("/api/media/stream/{token}")
    public ResponseEntity<FileSystemResource> stream(@PathVariable String token) throws Exception { return serveVideo(mMedia.getStream(token)); }

    @GetMapping("/api/public/preview/{id}")
    public ResponseEntity<FileSystemResource> preview(@PathVariable String id) throws Exception { return serveVideo(mMedia.getPreview(id)); }

    private ResponseEntity<FileSystemResource> serveVideo(Path file) {
        if (!java.nio.file.Files.exists(file)) { throw new ApiFailure(404, "Demo media has not been prepared. Run media setup."); }
        return ResponseEntity.ok().header("Cache-Control", "private, no-store").header("Accept-Ranges", "bytes")
                .header("Content-Type", "video/mp4").body(new FileSystemResource(file));
    }
}
