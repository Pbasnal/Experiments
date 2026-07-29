-- Multi-use invite tokens: admin-configurable max uses + counter

ALTER TABLE invite_token
    ADD COLUMN max_uses INT NOT NULL DEFAULT 1,
    ADD COLUMN use_count INT NOT NULL DEFAULT 0;

ALTER TABLE invite_token
    ADD CONSTRAINT chk_invite_max_uses CHECK (max_uses >= 1),
    ADD CONSTRAINT chk_invite_use_count CHECK (use_count >= 0);

UPDATE invite_token
SET use_count = 1
WHERE used_at IS NOT NULL;

DROP INDEX IF EXISTS idx_invite_token_used;

CREATE INDEX idx_invite_token_available
    ON invite_token (created_at DESC)
    WHERE use_count < max_uses;
