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
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(new FileSystemResource(file));
    }
}
