CREATE TABLE invite_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(64) NOT NULL UNIQUE,
    created_by  UUID REFERENCES app_user (id),
    used_by     UUID REFERENCES app_user (id),
    used_at     TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invite_token_used ON invite_token (used_at) WHERE used_at IS NULL;
