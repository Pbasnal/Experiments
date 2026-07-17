package com.amarkatha.media;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageDerivativeEncoderTest {

    @Test
    void producesReaderDerivative() throws Exception {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 400, 300);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        ImageDerivativeEncoder.Derivative derivative =
                new ImageDerivativeEncoder().toReaderDerivative(baos.toByteArray(), 800, 0.82f);

        assertTrue(derivative.bytes().length > 100);
        assertTrue(
                "webp".equals(derivative.extension()) || "jpg".equals(derivative.extension()),
                "expected webp or jpeg fallback"
        );
    }
}
