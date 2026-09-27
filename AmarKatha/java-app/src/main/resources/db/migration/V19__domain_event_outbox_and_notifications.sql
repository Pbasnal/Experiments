-- Generic durable domain-event outbox + reader notifications / email delivery

CREATE TABLE domain_event_outbox (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type       VARCHAR(64)  NOT NULL,
    aggregate_type   VARCHAR(64)  NOT NULL,
    aggregate_id     UUID         NOT NULL,
    dedupe_key       VARCHAR(255) NOT NULL,
    payload          JSONB        NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempt_count    INT          NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_error       TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at     TIMESTAMPTZ,
    CONSTRAINT uq_domain_event_outbox_dedupe UNIQUE (dedupe_key),
    CONSTRAINT chk_domain_event_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_domain_event_outbox_pending
    ON domain_event_outbox (status, next_attempt_at, created_at);

CREATE TABLE notification_preference (
    user_id                UUID PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    email_new_chapter      BOOLEAN      NOT NULL DEFAULT FALSE,
    in_app_new_chapter     BOOLEAN      NOT NULL DEFAULT TRUE,
    email_product_updates  BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE reader_notification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type            VARCHAR(64)  NOT NULL,
    series_id       UUID         NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    chapter_id      UUID         NOT NULL REFERENCES chapter (id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    href            VARCHAR(500) NOT NULL,
    -- false = email-delivery anchor only; excluded from in-app list/count/read APIs
    in_app_visible  BOOLEAN      NOT NULL DEFAULT TRUE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reader_notification_user_type_chapter UNIQUE (user_id, type, chapter_id)
);

CREATE INDEX idx_reader_notification_user_created
    ON reader_notification (user_id, created_at DESC)
    WHERE in_app_visible = TRUE;
CREATE INDEX idx_reader_notification_user_unread
    ON reader_notification (user_id)
    WHERE read_at IS NULL AND in_app_visible = TRUE;

CREATE TABLE notification_email_delivery (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    notification_id  UUID         NOT NULL REFERENCES reader_notification (id) ON DELETE CASCADE,
    dedupe_key       VARCHAR(255) NOT NULL,
    to_email         VARCHAR(255) NOT NULL,
    subject          VARCHAR(500) NOT NULL,
    body_text        TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempt_count    INT          NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_error       TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at          TIMESTAMPTZ,
    CONSTRAINT uq_notification_email_delivery_dedupe UNIQUE (dedupe_key),
    CONSTRAINT chk_notification_email_delivery_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_notification_email_delivery_pending
    ON notification_email_delivery (status, next_attempt_at, created_at);
