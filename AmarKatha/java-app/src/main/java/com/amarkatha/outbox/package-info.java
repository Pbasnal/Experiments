/**
 * Durable generic domain-event outbox infrastructure.
 * Producers (e.g. publishing) append events transactionally; consumers poll asynchronously.
 */
package com.amarkatha.outbox;
