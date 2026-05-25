-- V1__init_schema.sql
-- Complete, idempotent V1 schema migration for Uber ride-hailing application
-- Matches JPA entity definitions exactly (ddl-auto=validate compatible)
-- All tables use IF NOT EXISTS for idempotency
-- NOTE: V5 adds driver verification columns and auth/otp tables — V1 must not duplicate them

-- Enable PostGIS extension for spatial data
CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================
-- USER MANAGEMENT TABLES
-- ============================================

-- Main user table (matches User.java entity)
-- JPA-mapped columns: id, name, email, phone_number, password,
--   is_verified, failed_login_attempts, locked_until
-- roles stored in user_roles (ElementCollection)
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255),
    email         VARCHAR(255) UNIQUE,
    phone_number  VARCHAR(255),
    password      VARCHAR(255),
    is_verified   BOOLEAN      DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until  TIMESTAMP
);

-- Index declared in User @Table annotation
CREATE INDEX IF NOT EXISTS idx_user_email ON app_user (email);

-- User roles table (ElementCollection for Set<Role>)
-- JPA default table name: app_user_roles, columns: app_user_id, roles
CREATE TABLE IF NOT EXISTS app_user_roles (
    app_user_id BIGINT      NOT NULL,
    roles       VARCHAR(50) NOT NULL,

    CONSTRAINT pk_app_user_roles PRIMARY KEY (app_user_id, roles),
    CONSTRAINT fk_app_user_roles_user FOREIGN KEY (app_user_id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_app_user_roles CHECK (roles IN ('ADMIN', 'DRIVER', 'RIDER'))
);

CREATE INDEX IF NOT EXISTS idx_app_user_roles_user ON app_user_roles (app_user_id);

-- ============================================
-- RIDER TABLE
-- ============================================

-- Rider profile (matches Rider.java entity)
-- JPA-mapped columns: id, user_id, rating
CREATE TABLE IF NOT EXISTS rider (
    id      BIGSERIAL          PRIMARY KEY,
    user_id BIGINT             UNIQUE,
    rating  DOUBLE PRECISION,

    CONSTRAINT fk_rider_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rider_user ON rider (user_id);

-- ============================================
-- DRIVER TABLE
-- ============================================

-- Driver profile (matches Driver.java entity)
-- JPA-mapped columns: id, user_id, rating, available, vehicle_id, vehicle_type,
--   current_location
-- NOTE: vehicle_model, vehicle_verified, verification_status, rejection_reason,
--   rc_url, license_url, insurance_url, profile_photo_url, blocked
--   are added by V5 via ADD COLUMN IF NOT EXISTS — do NOT include here
--   to avoid duplicate column errors on existing databases.
CREATE TABLE IF NOT EXISTS driver (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT    UNIQUE,
    rating           DOUBLE PRECISION,
    available        BOOLEAN,
    vehicle_id       VARCHAR(255),
    vehicle_type     VARCHAR(50),
    current_location geometry(Point, 4326),

    CONSTRAINT fk_driver_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_driver_vehicle_type CHECK (
        vehicle_type IS NULL OR vehicle_type IN ('MINI', 'SEDAN', 'LUXE')
    )
);

-- Indexes declared in Driver @Table annotation
CREATE INDEX IF NOT EXISTS idx_driver_vehicle_id       ON driver (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_driver_available_type   ON driver (available, vehicle_type);
-- Spatial index (also created by V5 with IF NOT EXISTS — safe duplicate)
CREATE INDEX IF NOT EXISTS idx_driver_current_location_gist ON driver USING GIST (current_location);

-- ============================================
-- WALLET TABLE
-- ============================================

-- Wallet (matches Wallet.java entity)
-- JPA-mapped columns: id, user_id, balance
CREATE TABLE IF NOT EXISTS wallet (
    id      BIGSERIAL      PRIMARY KEY,
    user_id BIGINT         UNIQUE,
    balance NUMERIC(19, 2) DEFAULT 0.00,

    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wallet_user ON wallet (user_id);

-- ============================================
-- RIDE REQUEST TABLE
-- ============================================

-- Ride request (matches RideRequest.java entity)
-- JPA-mapped columns: id, pickup_location, drop_off_location, requested_time,
--   rider_id, payment_method, ride_request_status, vehicle_type, fare, otp, version
-- ride_request_notified_drivers handled separately as @ManyToMany join table
CREATE TABLE IF NOT EXISTS ride_request (
    id                  BIGSERIAL      PRIMARY KEY,
    pickup_location     geometry(Point, 4326),
    drop_off_location   geometry(Point, 4326),
    requested_time      TIMESTAMP,
    rider_id            BIGINT,
    payment_method      VARCHAR(50),
    ride_request_status VARCHAR(50),
    vehicle_type        VARCHAR(50),
    fare                NUMERIC(19, 2),
    otp                 VARCHAR(255),
    version             BIGINT         DEFAULT 0,

    CONSTRAINT fk_ride_request_rider FOREIGN KEY (rider_id)
        REFERENCES rider (id) ON DELETE CASCADE,
    CONSTRAINT chk_ride_request_payment_method CHECK (
        payment_method IS NULL OR payment_method IN ('WALLET', 'CASH', 'RAZORPAY')
    ),
    CONSTRAINT chk_ride_request_status CHECK (
        ride_request_status IS NULL OR ride_request_status IN ('PENDING', 'CANCELLED', 'CONFIRMED')
    ),
    CONSTRAINT chk_ride_request_vehicle_type CHECK (
        vehicle_type IS NULL OR vehicle_type IN ('MINI', 'SEDAN', 'LUXE')
    )
);

-- Indexes declared in RideRequest @Table annotation
CREATE INDEX IF NOT EXISTS idx_ride_request_rider  ON ride_request (rider_id);
CREATE INDEX IF NOT EXISTS idx_ride_request_status ON ride_request (ride_request_status);

-- Spatial indexes for proximity queries
CREATE INDEX IF NOT EXISTS idx_ride_request_pickup_gist  ON ride_request USING GIST (pickup_location);
CREATE INDEX IF NOT EXISTS idx_ride_request_dropoff_gist ON ride_request USING GIST (drop_off_location);

-- ============================================
-- RIDE REQUEST NOTIFIED DRIVERS (ManyToMany join table)
-- ============================================

-- JPA @JoinTable: name="ride_request_notified_drivers",
--   joinColumns=ride_request_id, inverseJoinColumns=driver_id
CREATE TABLE IF NOT EXISTS ride_request_notified_drivers (
    ride_request_id BIGINT NOT NULL,
    driver_id       BIGINT NOT NULL,

    CONSTRAINT pk_rr_notified_drivers PRIMARY KEY (ride_request_id, driver_id),
    CONSTRAINT fk_rr_notified_ride_request FOREIGN KEY (ride_request_id)
        REFERENCES ride_request (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_notified_driver FOREIGN KEY (driver_id)
        REFERENCES driver (id) ON DELETE CASCADE
);

-- Index declared in WalletTransaction @Table (also referenced in V5)
CREATE INDEX IF NOT EXISTS idx_rr_notified_driver ON ride_request_notified_drivers (driver_id, ride_request_id);

-- ============================================
-- RIDE TABLE
-- ============================================

-- Ride (matches Ride.java entity)
-- JPA-mapped columns: id, pickup_location, drop_off_location, created_time,
--   rider_id, driver_id, ride_request_id, payment_method, ride_status,
--   vehicle_type, otp, fare, started_at, ended_at
CREATE TABLE IF NOT EXISTS ride (
    id                BIGSERIAL      PRIMARY KEY,
    pickup_location   geometry(Point, 4326),
    drop_off_location geometry(Point, 4326),
    created_time      TIMESTAMP,
    rider_id          BIGINT,
    driver_id         BIGINT,
    ride_request_id   BIGINT         UNIQUE,
    payment_method    VARCHAR(50),
    ride_status       VARCHAR(50),
    vehicle_type      VARCHAR(50),
    otp               VARCHAR(255),
    fare              NUMERIC(19, 2),
    started_at        TIMESTAMP,
    ended_at          TIMESTAMP,

    CONSTRAINT fk_ride_rider FOREIGN KEY (rider_id)
        REFERENCES rider (id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_driver FOREIGN KEY (driver_id)
        REFERENCES driver (id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_request FOREIGN KEY (ride_request_id)
        REFERENCES ride_request (id) ON DELETE SET NULL,
    CONSTRAINT chk_ride_payment_method CHECK (
        payment_method IS NULL OR payment_method IN ('WALLET', 'CASH', 'RAZORPAY')
    ),
    CONSTRAINT chk_ride_status CHECK (
        ride_status IS NULL OR ride_status IN ('CANCELLED', 'CONFIRMED', 'ENDED', 'ONGOING')
    ),
    CONSTRAINT chk_ride_vehicle_type CHECK (
        vehicle_type IS NULL OR vehicle_type IN ('MINI', 'SEDAN', 'LUXE')
    )
);

-- Indexes declared in Ride @Table annotation
CREATE INDEX IF NOT EXISTS idx_ride_rider  ON ride (rider_id);
CREATE INDEX IF NOT EXISTS idx_ride_driver ON ride (driver_id);

-- Additional useful indexes
CREATE INDEX IF NOT EXISTS idx_ride_status         ON ride (ride_status);
CREATE INDEX IF NOT EXISTS idx_ride_created_time   ON ride (created_time);
CREATE INDEX IF NOT EXISTS idx_ride_pickup_gist    ON ride USING GIST (pickup_location);
CREATE INDEX IF NOT EXISTS idx_ride_dropoff_gist   ON ride USING GIST (drop_off_location);

-- ============================================
-- WALLET TRANSACTION TABLE
-- ============================================

-- WalletTransaction (matches WalletTransaction.java entity)
-- JPA-mapped columns: id, amount, transaction_type, transaction_method,
--   ride_id, transaction_id, wallet_id, time_stamp
-- Note: @Column(length=10) on transactionType, @Column(length=50) on transactionMethod
CREATE TABLE IF NOT EXISTS wallet_transaction (
    id                 BIGSERIAL      PRIMARY KEY,
    amount             NUMERIC(19, 2),
    transaction_type   VARCHAR(10),
    transaction_method VARCHAR(50),
    ride_id            BIGINT,
    transaction_id     VARCHAR(255),
    wallet_id          BIGINT,
    time_stamp         TIMESTAMP,

    CONSTRAINT fk_wallet_transaction_ride FOREIGN KEY (ride_id)
        REFERENCES ride (id) ON DELETE SET NULL,
    CONSTRAINT fk_wallet_transaction_wallet FOREIGN KEY (wallet_id)
        REFERENCES wallet (id) ON DELETE CASCADE,
    CONSTRAINT chk_transaction_type CHECK (
        transaction_type IS NULL OR transaction_type IN ('CREDIT', 'DEBIT')
    ),
    CONSTRAINT chk_transaction_method CHECK (
        transaction_method IS NULL OR transaction_method IN ('BANKING', 'UPI', 'CARD', 'NETBANKING', 'WALLET', 'RIDE')
    )
);

-- Indexes declared in WalletTransaction @Table annotation
CREATE INDEX IF NOT EXISTS idx_wallet_transaction_wallet ON wallet_transaction (wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transaction_ride   ON wallet_transaction (ride_id);

-- ============================================
-- PAYMENT TABLE
-- ============================================

-- Payment (matches Payment.java entity)
-- JPA-mapped columns: id, payment_method, ride_id, amount, payment_status,
--   currency, provider_order_id, provider_payment_id, provider_signature,
--   settlement_reference, payment_time, processed_at
-- Note: currency is VARCHAR(16), settlement_reference is VARCHAR(128)
-- V5 adds currency, provider_order_id, provider_payment_id, provider_signature,
--   settlement_reference, processed_at via ADD COLUMN IF NOT EXISTS
-- Including them here is safe because V5 uses IF NOT EXISTS guards
CREATE TABLE IF NOT EXISTS payment (
    id                   BIGSERIAL      PRIMARY KEY,
    payment_method       VARCHAR(50),
    ride_id              BIGINT         UNIQUE,
    amount               NUMERIC(19, 2),
    payment_status       VARCHAR(50),
    currency             VARCHAR(16),
    provider_order_id    VARCHAR(255),
    provider_payment_id  VARCHAR(255),
    provider_signature   VARCHAR(255),
    settlement_reference VARCHAR(128),
    payment_time         TIMESTAMP,
    processed_at         TIMESTAMP,

    CONSTRAINT fk_payment_ride FOREIGN KEY (ride_id)
        REFERENCES ride (id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_method CHECK (
        payment_method IS NULL OR payment_method IN ('WALLET', 'CASH', 'RAZORPAY')
    ),
    CONSTRAINT chk_payment_status CHECK (
        payment_status IS NULL OR payment_status IN ('PENDING', 'CONFIRMED', 'REFUNDED')
    )
);

-- Indexes declared in Payment @Table annotation
CREATE INDEX IF NOT EXISTS idx_payment_ride               ON payment (ride_id);
CREATE INDEX IF NOT EXISTS idx_payment_provider_reference ON payment (provider_payment_id);

-- ============================================
-- RATING TABLE
-- ============================================

-- Rating (matches Rating.java entity)
-- JPA-mapped columns: id, ride_id, rider_id, driver_id, driver_rating, rider_rating
-- ride is @OneToOne → ride_id UNIQUE
CREATE TABLE IF NOT EXISTS rating (
    id            BIGSERIAL PRIMARY KEY,
    ride_id       BIGINT    UNIQUE,
    rider_id      BIGINT,
    driver_id     BIGINT,
    driver_rating INTEGER,
    rider_rating  INTEGER,

    CONSTRAINT fk_rating_ride FOREIGN KEY (ride_id)
        REFERENCES ride (id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_rider FOREIGN KEY (rider_id)
        REFERENCES rider (id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_driver FOREIGN KEY (driver_id)
        REFERENCES driver (id) ON DELETE CASCADE
);

-- Indexes declared in Rating @Table annotation
CREATE INDEX IF NOT EXISTS idx_rating_rider  ON rating (rider_id);
CREATE INDEX IF NOT EXISTS idx_rating_driver ON rating (driver_id);

-- ============================================
-- AUTH SESSION TABLE
-- ============================================

-- AuthSession (matches AuthSession.java entity)
-- JPA-mapped columns: id, user_id, token_jti, token_hash, expires_at,
--   revoked_at, last_used_at, replaced_by_token_jti, created_by_ip,
--   user_agent, created_at
-- NOTE: V5 also creates this table with IF NOT EXISTS — safe on fresh DB
-- On existing DB (V5 already ran), this CREATE IF NOT EXISTS is a no-op
CREATE TABLE IF NOT EXISTS auth_session (
    id                    BIGSERIAL    PRIMARY KEY,
    user_id               BIGINT       NOT NULL,
    token_jti             VARCHAR(255) NOT NULL UNIQUE,
    token_hash            VARCHAR(128) NOT NULL,
    expires_at            TIMESTAMP    NOT NULL,
    revoked_at            TIMESTAMP,
    last_used_at          TIMESTAMP,
    replaced_by_token_jti VARCHAR(255),
    created_by_ip         VARCHAR(128),
    user_agent            VARCHAR(512),
    created_at            TIMESTAMP,

    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
);

-- Indexes declared in AuthSession @Table annotation
CREATE INDEX IF NOT EXISTS idx_auth_session_user       ON auth_session (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_session_jti ON auth_session (token_jti);
CREATE INDEX IF NOT EXISTS idx_auth_session_expires_at ON auth_session (expires_at);

-- ============================================
-- OTP CHALLENGE TABLE
-- ============================================

-- OtpChallenge (matches OtpChallenge.java entity)
-- JPA-mapped columns: id, phone_number, otp_hash, expires_at, verified_until,
--   blocked_until, send_count, failed_attempts, send_window_started_at
-- NOTE: V5 also creates this table with IF NOT EXISTS — safe on fresh DB
CREATE TABLE IF NOT EXISTS otp_challenge (
    id                    BIGSERIAL    PRIMARY KEY,
    phone_number          VARCHAR(32)  NOT NULL UNIQUE,
    otp_hash              VARCHAR(128) NOT NULL,
    expires_at            TIMESTAMP    NOT NULL,
    verified_until        TIMESTAMP,
    blocked_until         TIMESTAMP,
    send_count            INTEGER,
    failed_attempts       INTEGER,
    send_window_started_at TIMESTAMP
);

-- Indexes declared in OtpChallenge @Table annotation
CREATE UNIQUE INDEX IF NOT EXISTS idx_otp_challenge_phone      ON otp_challenge (phone_number);
CREATE INDEX        IF NOT EXISTS idx_otp_challenge_expires_at ON otp_challenge (expires_at);

-- ============================================
-- MIGRATION NOTES
-- ============================================
-- This V1 migration is idempotent (all IF NOT EXISTS).
-- Column names match JPA camelCase → snake_case convention exactly.
-- Enum CHECK constraints use only values present in Java enum classes.
-- V2: fixes wallet_transaction enum columns (integer → string) — no-op on fresh DB
-- V3: drops/re-adds payment_method CHECK constraints — no-op on fresh DB
-- V4: adds refresh_token, failed_login_attempts, locked_until to app_user — no-op on fresh DB
-- V5: adds driver verification columns, creates auth_session, otp_challenge — no-op on fresh DB
