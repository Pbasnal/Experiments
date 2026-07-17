CREATE TABLE feature_flag (
    name        VARCHAR(64) PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL,
    description TEXT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO feature_flag (name, enabled, description) VALUES
    ('invite_required', true, 'Gate creator signup on invite token'),
    ('public_homepage', true, 'Show public homepage'),
    ('webp_required', false, 'Block publish until WebP ready');
