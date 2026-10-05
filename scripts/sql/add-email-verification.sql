-- Migration script for email verification
-- Safe migration for existing PostgreSQL databases with existing app_user rows

-- 1. Add column if it does not exist with DEFAULT FALSE so existing rows are not null
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;

-- 2. Backfill any existing NULL values to FALSE
UPDATE app_user SET email_verified = FALSE WHERE email_verified IS NULL;

-- 3. Enforce DEFAULT and NOT NULL constraint
ALTER TABLE app_user ALTER COLUMN email_verified SET DEFAULT FALSE;
ALTER TABLE app_user ALTER COLUMN email_verified SET NOT NULL;

-- 4. Create email_verification_tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token ON email_verification_tokens (token);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user ON email_verification_tokens (user_id);
