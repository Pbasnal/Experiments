-- Users and roles (V0: READER, CREATOR, ADMIN)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub      VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    role            VARCHAR(32)  NOT NULL DEFAULT 'READER',
    locale          VARCHAR(10)  NOT NULL DEFAULT 'en',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_app_user_role CHECK (role IN ('READER', 'CREATOR', 'ADMIN'))
);

CREATE INDEX idx_app_user_role ON app_user (role);
