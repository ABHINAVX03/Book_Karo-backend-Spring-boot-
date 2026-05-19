CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(255),
    password VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    roles VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_email ON app_user (email);

CREATE TABLE IF NOT EXISTS rider (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    rating DOUBLE PRECISION,
    CONSTRAINT fk_rider_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS driver (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    rating DOUBLE PRECISION,
    available BOOLEAN,
    vehicle_id VARCHAR(255),
    vehicle_type VARCHAR(255),
    vehicle_model VARCHAR(255),
    vehicle_verified BOOLEAN DEFAULT FALSE,
    verification_status VARCHAR(255),
    rejection_reason VARCHAR(255),
    rc_url VARCHAR(255),
    license_url VARCHAR(255),
    insurance_url VARCHAR(255),
    profile_photo_url VARCHAR(255),
    blocked BOOLEAN DEFAULT FALSE,
    current_location geometry(Point, 4326),
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_driver_vehicle_id ON driver (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_driver_available_type ON driver (available, vehicle_type);
CREATE INDEX IF NOT EXISTS idx_driver_current_location_gist ON driver USING GIST (current_location);

CREATE TABLE IF NOT EXISTS wallet (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    balance NUMERIC(19, 2) DEFAULT 0,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS ride_request (
    id BIGSERIAL PRIMARY KEY,
    pickup_location geometry(Point, 4326),
    drop_off_location geometry(Point, 4326),
    requested_time TIMESTAMP,
    rider_id BIGINT,
    payment_method VARCHAR(50),
    ride_request_status VARCHAR(255),
    vehicle_type VARCHAR(255),
    fare NUMERIC(19, 2),
    otp VARCHAR(255),
    version BIGINT,
    CONSTRAINT fk_ride_request_rider FOREIGN KEY (rider_id) REFERENCES rider (id)
);

CREATE INDEX IF NOT EXISTS idx_ride_request_rider ON ride_request (rider_id);
CREATE INDEX IF NOT EXISTS idx_ride_request_status ON ride_request (ride_request_status);

CREATE TABLE IF NOT EXISTS ride_request_notified_drivers (
    ride_request_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    CONSTRAINT fk_rr_notified_ride_request FOREIGN KEY (ride_request_id) REFERENCES ride_request (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_notified_driver FOREIGN KEY (driver_id) REFERENCES driver (id) ON DELETE CASCADE,
    PRIMARY KEY (ride_request_id, driver_id)
);

CREATE INDEX IF NOT EXISTS idx_rr_notified_driver ON ride_request_notified_drivers (driver_id, ride_request_id);

CREATE TABLE IF NOT EXISTS ride (
    id BIGSERIAL PRIMARY KEY,
    pickup_location geometry(Point, 4326),
    drop_off_location geometry(Point, 4326),
    created_time TIMESTAMP,
    rider_id BIGINT,
    driver_id BIGINT,
    ride_request_id BIGINT UNIQUE,
    payment_method VARCHAR(50),
    ride_status VARCHAR(255),
    vehicle_type VARCHAR(255),
    otp VARCHAR(255),
    fare NUMERIC(19, 2),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    CONSTRAINT fk_ride_rider FOREIGN KEY (rider_id) REFERENCES rider (id),
    CONSTRAINT fk_ride_driver FOREIGN KEY (driver_id) REFERENCES driver (id),
    CONSTRAINT fk_ride_request FOREIGN KEY (ride_request_id) REFERENCES ride_request (id)
);

CREATE INDEX IF NOT EXISTS idx_ride_rider ON ride (rider_id);
CREATE INDEX IF NOT EXISTS idx_ride_driver ON ride (driver_id);

CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    payment_method VARCHAR(50),
    ride_id BIGINT UNIQUE,
    amount NUMERIC(19, 2),
    payment_status VARCHAR(255),
    currency VARCHAR(16),
    provider_order_id VARCHAR(255),
    provider_payment_id VARCHAR(255),
    provider_signature VARCHAR(255),
    settlement_reference VARCHAR(128),
    payment_time TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT fk_payment_ride FOREIGN KEY (ride_id) REFERENCES ride (id)
);

CREATE INDEX IF NOT EXISTS idx_payment_provider_reference ON payment (provider_payment_id);

CREATE TABLE IF NOT EXISTS rating (
    id BIGSERIAL PRIMARY KEY,
    ride_id BIGINT UNIQUE,
    rider_id BIGINT,
    driver_id BIGINT,
    driver_rating INTEGER,
    rider_rating INTEGER,
    CONSTRAINT fk_rating_ride FOREIGN KEY (ride_id) REFERENCES ride (id),
    CONSTRAINT fk_rating_rider FOREIGN KEY (rider_id) REFERENCES rider (id),
    CONSTRAINT fk_rating_driver FOREIGN KEY (driver_id) REFERENCES driver (id)
);

CREATE INDEX IF NOT EXISTS idx_rating_rider ON rating (rider_id);
CREATE INDEX IF NOT EXISTS idx_rating_driver ON rating (driver_id);

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(19, 2),
    transaction_type VARCHAR(10),
    transaction_method VARCHAR(50),
    ride_id BIGINT,
    transaction_id VARCHAR(255),
    wallet_id BIGINT,
    time_stamp TIMESTAMP,
    CONSTRAINT fk_wallet_transaction_ride FOREIGN KEY (ride_id) REFERENCES ride (id),
    CONSTRAINT fk_wallet_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_transaction_wallet ON wallet_transaction (wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transaction_ride ON wallet_transaction (ride_id);

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
