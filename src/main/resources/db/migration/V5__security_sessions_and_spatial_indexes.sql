CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE driver ADD COLUMN IF NOT EXISTS vehicle_model VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS vehicle_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE driver ADD COLUMN IF NOT EXISTS verification_status VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS rc_url VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS license_url VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS insurance_url VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS blocked BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_driver_current_location_gist ON driver USING GIST (current_location);
CREATE INDEX IF NOT EXISTS idx_rr_notified_driver ON ride_request_notified_drivers (driver_id, ride_request_id);

ALTER TABLE payment ADD COLUMN IF NOT EXISTS currency VARCHAR(16);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_signature VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS settlement_reference VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_payment_provider_reference ON payment (provider_payment_id);

CREATE TABLE IF NOT EXISTS auth_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_jti VARCHAR(255) UNIQUE NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    last_used_at TIMESTAMP,
    replaced_by_token_jti VARCHAR(255),
    created_by_ip VARCHAR(128),
    user_agent VARCHAR(512),
    created_at TIMESTAMP,
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_session_user ON auth_session (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_session_expires_at ON auth_session (expires_at);

CREATE TABLE IF NOT EXISTS otp_challenge (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(32) UNIQUE NOT NULL,
    otp_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_until TIMESTAMP,
    blocked_until TIMESTAMP,
    send_count INTEGER,
    failed_attempts INTEGER,
    send_window_started_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_otp_challenge_expires_at ON otp_challenge (expires_at);
