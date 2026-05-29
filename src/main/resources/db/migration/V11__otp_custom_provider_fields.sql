ALTER TABLE otp_challenge
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

ALTER TABLE otp_challenge
    ADD COLUMN IF NOT EXISTS consumed_at TIMESTAMP;

UPDATE otp_challenge
SET created_at = COALESCE(send_window_started_at, expires_at, NOW())
WHERE created_at IS NULL;

ALTER TABLE otp_challenge
    ALTER COLUMN created_at SET NOT NULL;
