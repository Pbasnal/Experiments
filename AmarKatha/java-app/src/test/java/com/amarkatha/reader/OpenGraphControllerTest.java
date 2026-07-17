package com.amarkatha.reader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OpenGraphControllerTest {

    @Test
    void detectsWhatsAppAndFacebookCrawlers() {
        MockHttpServletRequest wa = new MockHttpServletRequest();
        wa.addHeader("User-Agent", "WhatsApp/2.0");
        assertTrue(OpenGraphController.isCrawler(wa));

        MockHttpServletRequest fb = new MockHttpServletRequest();
        fb.addHeader("User-Agent", "facebookexternalhit/1.1");
        assertTrue(OpenGraphController.isCrawler(fb));

        MockHttpServletRequest chrome = new MockHttpServletRequest();
        chrome.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/537.36 Chrome/120.0.0.0"
        );
        assertFalse(OpenGraphController.isCrawler(chrome));
    }
}
