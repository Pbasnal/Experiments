package com.amarkatha.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
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
        return put(key, data);
    }

    @Override
    public UploadResult putDerivative(String key, InputStream data, String contentType) {
        return put(key, data);
    }

    private UploadResult put(String key, InputStream data) {
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

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolvePath(key));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete media key=" + key, ex);
        }
    }

    @Override
    public void deletePrefix(String prefix) {
        String normalized = sanitize(prefix);
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        Path dir = root.resolve(normalized).normalize();
        if (!dir.startsWith(root) || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new UncheckedIOException("Failed to delete " + path, ex);
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete prefix=" + prefix, ex);
        }
    }

    private static String sanitize(String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new IllegalArgumentException("Invalid media key");
        }
        return key.replace('\\', '/');
    }
}
