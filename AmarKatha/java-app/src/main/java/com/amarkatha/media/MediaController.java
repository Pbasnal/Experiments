package com.amarkatha.media;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediaController {

    private final MediaStore mediaStore;

    public MediaController(MediaStore mediaStore) {
        this.mediaStore = mediaStore;
    }

    @GetMapping("/media/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri.substring(context.length());
        String key = path.startsWith("/media/") ? path.substring("/media/".length()) : "";
        if (key.isBlank() || !mediaStore.exists(key)) {
            return ResponseEntity.notFound().build();
        }
        Path file = mediaStore.resolvePath(key);
        String contentType = Files.probeContentType(file);
        if (contentType == null || contentType.isBlank()) {
            if (key.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            } else if (key.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (key.toLowerCase().endsWith(".jpg") || key.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else {
                contentType = "application/octet-stream";
            }
        }
        // Covers are re-uploaded in place (original.jpg); don't pin them immutable.
        boolean coverAsset = key.contains("/cover/");
        String cacheControl = coverAsset
                ? "public, max-age=60, must-revalidate"
                : "public, max-age=31536000, immutable";
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(file));
    }
}
