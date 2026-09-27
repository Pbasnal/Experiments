package com.amarkatha.engagement;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(NotificationMailSender.class)
public class DisabledNotificationMailSender implements NotificationMailSender {

    @Override
    public void send(String to, String subject, String bodyText) {
        throw new IllegalStateException("Mail delivery is disabled (amarkatha.mail.enabled=false)");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
