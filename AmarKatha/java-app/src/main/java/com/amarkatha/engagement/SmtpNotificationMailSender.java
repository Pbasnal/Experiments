package com.amarkatha.engagement;

import com.amarkatha.shared.web.AbsoluteUrlBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP sender. Treats delivery as disabled when {@code amarkatha.public-base-url}
 * is unset so we never send emails with relative / broken links.
 */
@Component
@ConditionalOnProperty(name = "amarkatha.mail.enabled", havingValue = "true")
public class SmtpNotificationMailSender implements NotificationMailSender {

    private final JavaMailSender mailSender;
    private final AmarKathaMailProperties mailProperties;
    private final AbsoluteUrlBuilder absoluteUrlBuilder;

    public SmtpNotificationMailSender(
            JavaMailSender mailSender,
            AmarKathaMailProperties mailProperties,
            AbsoluteUrlBuilder absoluteUrlBuilder
    ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.absoluteUrlBuilder = absoluteUrlBuilder;
    }

    @Override
    public void send(String to, String subject, String bodyText) {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "Mail delivery requires amarkatha.mail.enabled=true and amarkatha.public-base-url"
            );
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(bodyText);
        mailSender.send(message);
    }

    @Override
    public boolean isEnabled() {
        return absoluteUrlBuilder.hasConfiguredBaseUrl();
    }
}
