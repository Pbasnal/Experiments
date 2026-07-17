package com.amarkatha.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilesystemMediaStore implements MediaStore {

    private final Path root;

    public FilesystemMediaStore(@Value("${amarkatha.media.root:./data/media}") String rootPath) {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to create media root: " + this.root, ex);
        }
    }

    @Override
    public UploadResult putOriginal(String key, InputStream data, String contentType) {
        Path target = resolvePath(key);
        try {
            Files.createDirectories(target.getParent());
            long bytes = Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            return new UploadResult(key, bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store media key=" + key, ex);
        }
    }

    @Override
    public Path resolvePath(String key) {
        Path resolved = root.resolve(sanitize(key)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid media key");
        }
        return resolved;
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolvePath(key));
    }

    private static String sanitize(String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new IllegalArgumentException("Invalid media key");
        }
        return key.replace('\\', '/');
    }
}
