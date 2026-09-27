package com.amarkatha.engagement;

/**
 * Sends transactional notification email. Implementations may no-op when mail is disabled.
 */
public interface NotificationMailSender {

    void send(String to, String subject, String bodyText) throws Exception;

    boolean isEnabled();
}
