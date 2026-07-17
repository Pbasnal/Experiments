package com.amarkatha.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resizes and encodes reader/cover derivatives.
 * Prefers WebP (ImageIO or {@code cwebp}); falls back to resized JPEG when WebP is unavailable.
 */
@Component
public class ImageDerivativeEncoder {

    private static final Logger log = LoggerFactory.getLogger(ImageDerivativeEncoder.class);

    public record Derivative(byte[] bytes, String extension, String contentType) {
    }

    public Derivative toReaderDerivative(byte[] originalBytes, int maxWidth, float quality) throws IOException {
        BufferedImage resized = resize(originalBytes, maxWidth);
        try {
            return new Derivative(encodeWebp(resized, quality), "webp", "image/webp");
        } catch (Throwable ex) {
            log.warn("WebP encode unavailable, using JPEG derivative: {}", ex.toString());
            return new Derivative(encodeJpeg(resized, quality), "jpg", "image/jpeg");
        }
    }

    private static BufferedImage resize(byte[] originalBytes, int maxWidth) throws IOException {
        // Apply EXIF orientation before resize. ImageIO.read() alone drops orientation
        // and leaves phone photos rotated incorrectly.
        BufferedImage oriented = Thumbnails.of(new ByteArrayInputStream(originalBytes))
                .useExifOrientation(true)
                .scale(1.0)
                .asBufferedImage();
        if (oriented.getWidth() <= maxWidth) {
            return oriented;
        }
        return Thumbnails.of(oriented)
                .width(maxWidth)
                .keepAspectRatio(true)
                .asBufferedImage();
    }

    private static byte[] encodeWebp(BufferedImage image, float quality) throws IOException {
        try {
            return encodeWebpImageIo(image, quality);
        } catch (Throwable imageIoFailure) {
            log.debug("ImageIO WebP unavailable, trying cwebp: {}", imageIoFailure.toString());
            return encodeViaCwebp(image, quality);
        }
    }

    private static byte[] encodeWebpImageIo(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            writers = ImageIO.getImageWritersByFormatName("webp");
        }
        if (!writers.hasNext()) {
            throw new IOException("No WebP ImageWriter registered");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static byte[] encodeViaCwebp(BufferedImage image, float quality) throws IOException {
        Path dir = Files.createTempDirectory("amarkatha-webp");
        Path png = dir.resolve("in.png");
        Path webp = dir.resolve("out.webp");
        try {
            ImageIO.write(image, "png", png.toFile());
            int q = Math.max(0, Math.min(100, Math.round(quality * 100)));
            ProcessBuilder pb = new ProcessBuilder(
                    "cwebp",
                    "-quiet",
                    "-q",
                    String.valueOf(q),
                    png.toAbsolutePath().toString(),
                    "-o",
                    webp.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("cwebp timed out");
            }
            if (process.exitValue() != 0) {
                String out;
                try (InputStream in = process.getInputStream()) {
                    out = new String(in.readAllBytes());
                }
                throw new IOException("cwebp failed (exit " + process.exitValue() + "): " + out);
            }
            if (!Files.isRegularFile(webp)) {
                throw new IOException("cwebp produced no output");
            }
            return Files.readAllBytes(webp);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("cwebp interrupted", ex);
        } finally {
            Files.deleteIfExists(png);
            Files.deleteIfExists(webp);
            Files.deleteIfExists(dir);
        }
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(image)
                    .scale(1.0)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }
}
