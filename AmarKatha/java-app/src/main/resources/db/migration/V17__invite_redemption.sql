-- History of who redeemed each invite (supports multi-use tokens)

CREATE TABLE invite_redemption (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_token_id UUID NOT NULL REFERENCES invite_token (id),
    user_id         UUID NOT NULL REFERENCES app_user (id),
    redeemed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invite_redemption_token_user UNIQUE (invite_token_id, user_id)
);

CREATE INDEX idx_invite_redemption_token
    ON invite_redemption (invite_token_id, redeemed_at);

-- Backfill last known redeemer from invite_token.used_by (earlier multi-use users are unrecoverable)
INSERT INTO invite_redemption (id, invite_token_id, user_id, redeemed_at)
SELECT gen_random_uuid(), id, used_by, COALESCE(used_at, created_at)
FROM invite_token
WHERE used_by IS NOT NULL;
