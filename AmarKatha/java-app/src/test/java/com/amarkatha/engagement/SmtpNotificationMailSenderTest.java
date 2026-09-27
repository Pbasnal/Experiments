package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.amarkatha.shared.web.AbsoluteUrlBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpNotificationMailSenderTest {

    @Test
    void disabledWithoutPublicBaseUrlEvenWhenSmtpBeanPresent() {
        AbsoluteUrlBuilder urls = new AbsoluteUrlBuilder("");
        SmtpNotificationMailSender sender = new SmtpNotificationMailSender(
                mock(JavaMailSender.class),
                new AmarKathaMailProperties(true, "noreply@test"),
                urls
        );
        assertFalse(sender.isEnabled());
    }

    @Test
    void enabledWhenPublicBaseUrlConfigured() {
        AbsoluteUrlBuilder urls = new AbsoluteUrlBuilder("https://amarkatha.example.com");
        SmtpNotificationMailSender sender = new SmtpNotificationMailSender(
                mock(JavaMailSender.class),
                new AmarKathaMailProperties(true, "noreply@test"),
                urls
        );
        assertTrue(sender.isEnabled());
    }
}
