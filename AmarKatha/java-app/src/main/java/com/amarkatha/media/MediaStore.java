package com.amarkatha.media;

import java.io.InputStream;
import java.nio.file.Path;

public interface MediaStore {

    record UploadResult(String key, long bytes) {
    }

    UploadResult putOriginal(String key, InputStream data, String contentType);

    UploadResult putDerivative(String key, InputStream data, String contentType);

    Path resolvePath(String key);

    boolean exists(String key);

    void delete(String key);

    /** Deletes all files under a storage prefix (directory). */
    void deletePrefix(String prefix);
}
