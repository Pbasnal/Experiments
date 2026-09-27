package com.amarkatha.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Tagline copy lives in message bundles; the home API resolves it with the request locale.
 */
class HomeTaglineMessageTest {

    private ReloadableResourceBundleMessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
    }

    @Test
    void englishTaglineMatchesProductCopy() {
        assertEquals(
                "Publish on your rhythm. Share a link. Readers know when you're back.",
                messageSource.getMessage("app.tagline", null, Locale.ENGLISH)
        );
    }

    @Test
    void hindiTaglineUsesLocaleBundle() {
        assertEquals(
                "अपनी रफ़्तार से प्रकाशित करें। पाठक जानते हैं कि आप कब लौटेंगे।",
                messageSource.getMessage("app.tagline", null, Locale.forLanguageTag("hi"))
        );
    }
}
