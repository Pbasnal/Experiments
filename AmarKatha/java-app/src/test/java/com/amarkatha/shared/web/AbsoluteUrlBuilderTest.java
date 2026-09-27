package com.amarkatha.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AbsoluteUrlBuilderTest {

    @Test
    void prefersConfiguredPublicBaseUrl() {
        AbsoluteUrlBuilder builder = new AbsoluteUrlBuilder("https://amarkatha.example.com/");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/read/s/demo");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);

        assertEquals(
                "https://amarkatha.example.com/read/s/demo",
                builder.absolute(request, "/read/s/demo")
        );
        assertTrue(builder.hasConfiguredBaseUrl());
        assertEquals(
                "https://amarkatha.example.com/read/s/demo/c/1",
                builder.absolute("/read/s/demo/c/1")
        );
    }

    @Test
    void absoluteWithoutRequestRequiresConfiguredBase() {
        AbsoluteUrlBuilder unset = new AbsoluteUrlBuilder("");
        assertFalse(unset.hasConfiguredBaseUrl());
        assertThrows(IllegalStateException.class, () -> unset.absolute("/read/profile"));
    }

    @Test
    void usesForwardedHeadersWhenPublicBaseUnset() {
        AbsoluteUrlBuilder builder = new AbsoluteUrlBuilder("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/read/s/demo");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "amarkatha.example.com");

        assertEquals(
                "https://amarkatha.example.com/read/s/demo",
                builder.absolute(request, "read/s/demo")
        );
    }

    @Test
    void takesFirstValueFromCommaSeparatedForwardedHeaders() {
        AbsoluteUrlBuilder builder = new AbsoluteUrlBuilder("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https, http");
        request.addHeader("X-Forwarded-Host", "amarkatha.example.com, localhost:8080");

        assertEquals(
                "https://amarkatha.example.com/media/cover.webp",
                builder.absolute(request, "/media/cover.webp")
        );
    }
}
