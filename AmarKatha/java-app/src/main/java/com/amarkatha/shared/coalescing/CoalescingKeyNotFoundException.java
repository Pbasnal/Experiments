package com.amarkatha.shared.coalescing;

public class CoalescingKeyNotFoundException extends RuntimeException {

    public CoalescingKeyNotFoundException(Object key) {
        super("No result for coalesced key: " + key);
    }
}
