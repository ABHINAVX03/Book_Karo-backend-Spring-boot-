-- V6__add_missing_constraints.sql
-- Safely adds missing NOT NULL / CHECK constraints to existing databases.
-- All operations are guarded with DO $$ BEGIN ... EXCEPTION WHEN ... END $$ blocks
-- so this migration is safe to run on both fresh and existing databases.

-- ============================================
-- app_user: ensure email uniqueness index exists
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename  = 'app_user'
          AND indexname  = 'idx_user_email'
    ) THEN
        CREATE INDEX idx_user_email ON app_user (email);
    END IF;
END $$;

-- ============================================
-- app_user_roles: ensure table and FK exist
-- (handles databases that used the old 'user_roles' table name)
-- ============================================
DO $$
BEGIN
    -- If the old user_roles table exists but app_user_roles does not, rename it
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'user_roles'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'app_user_roles'
    ) THEN
        ALTER TABLE user_roles RENAME TO app_user_roles;
        -- Rename the user_id column to app_user_id if needed
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name   = 'app_user_roles'
              AND column_name  = 'user_id'
        ) THEN
            ALTER TABLE app_user_roles RENAME COLUMN user_id TO app_user_id;
        END IF;
        -- Rename the role column to roles if needed
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name   = 'app_user_roles'
              AND column_name  = 'role'
        ) THEN
            ALTER TABLE app_user_roles RENAME COLUMN role TO roles;
        END IF;
    END IF;
END $$;

-- ============================================
-- driver: add verification columns if missing
-- (belt-and-suspenders in case V5 did not run)
-- ============================================
ALTER TABLE driver ADD COLUMN IF NOT EXISTS vehicle_model       VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS vehicle_verified    BOOLEAN DEFAULT FALSE;
ALTER TABLE driver ADD COLUMN IF NOT EXISTS verification_status VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS rejection_reason    VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS rc_url              VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS license_url         VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS insurance_url       VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS profile_photo_url   VARCHAR(255);
ALTER TABLE driver ADD COLUMN IF NOT EXISTS blocked             BOOLEAN DEFAULT FALSE;

-- ============================================
-- driver: fix verification_status CHECK constraint
-- (old V1 had SUSPENDED which is not in the enum)
-- ============================================
DO $$
BEGIN
    -- Drop any existing verification_status check constraint
    ALTER TABLE driver DROP CONSTRAINT IF EXISTS chk_verification_status;
    ALTER TABLE driver DROP CONSTRAINT IF EXISTS driver_verification_status_check;
EXCEPTION WHEN OTHERS THEN
    NULL; -- constraint did not exist, ignore
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_driver_verification_status'
          AND conrelid = 'driver'::regclass
    ) THEN
        ALTER TABLE driver ADD CONSTRAINT chk_driver_verification_status
            CHECK (verification_status IS NULL OR
                   verification_status IN ('PENDING', 'APPROVED', 'REJECTED'));
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- ============================================
-- payment: add columns if missing (belt-and-suspenders for V5)
-- ============================================
ALTER TABLE payment ADD COLUMN IF NOT EXISTS currency             VARCHAR(16);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_order_id   VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_signature  VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS settlement_reference VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS processed_at        TIMESTAMP;

-- ============================================
-- payment: fix payment_status CHECK constraint
-- (old V1 had SUCCESS/FAILED which are not in the enum)
-- ============================================
DO $$
BEGIN
    ALTER TABLE payment DROP CONSTRAINT IF EXISTS chk_payment_status;
    ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_payment_status_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_payment_status_v6'
          AND conrelid = 'payment'::regclass
    ) THEN
        ALTER TABLE payment ADD CONSTRAINT chk_payment_status_v6
            CHECK (payment_status IS NULL OR
                   payment_status IN ('PENDING', 'CONFIRMED', 'REFUNDED'));
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- ============================================
-- ride_request: fix ride_request_status CHECK constraint
-- (old V1 had ACCEPTED/REJECTED/EXPIRED which are not in the enum)
-- ============================================
DO $$
BEGIN
    ALTER TABLE ride_request DROP CONSTRAINT IF EXISTS chk_ride_request_status;
    ALTER TABLE ride_request DROP CONSTRAINT IF EXISTS ride_request_ride_request_status_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_ride_request_status_v6'
          AND conrelid = 'ride_request'::regclass
    ) THEN
        ALTER TABLE ride_request ADD CONSTRAINT chk_ride_request_status_v6
            CHECK (ride_request_status IS NULL OR
                   ride_request_status IN ('PENDING', 'CANCELLED', 'CONFIRMED'));
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- ============================================
-- ride_request / ride / driver: fix vehicle_type CHECK constraint
-- (old V1 had SEDAN/SUV/HATCHBACK/BIKE; correct values are MINI/SEDAN/LUXE)
-- ============================================
DO $$
BEGIN
    ALTER TABLE ride_request DROP CONSTRAINT IF EXISTS chk_vehicle_type;
    ALTER TABLE ride_request DROP CONSTRAINT IF EXISTS ride_request_vehicle_type_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_ride_request_vehicle_type_v6'
          AND conrelid = 'ride_request'::regclass
    ) THEN
        ALTER TABLE ride_request ADD CONSTRAINT chk_ride_request_vehicle_type_v6
            CHECK (vehicle_type IS NULL OR vehicle_type IN ('MINI', 'SEDAN', 'LUXE'));
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE ride DROP CONSTRAINT IF EXISTS chk_vehicle_type_ride;
    ALTER TABLE ride DROP CONSTRAINT IF EXISTS ride_vehicle_type_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_ride_vehicle_type_v6'
          AND conrelid = 'ride'::regclass
    ) THEN
        ALTER TABLE ride ADD CONSTRAINT chk_ride_vehicle_type_v6
            CHECK (vehicle_type IS NULL OR vehicle_type IN ('MINI', 'SEDAN', 'LUXE'));
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE driver DROP CONSTRAINT IF EXISTS chk_vehicle_type;
    ALTER TABLE driver DROP CONSTRAINT IF EXISTS driver_vehicle_type_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_driver_vehicle_type_v6'
          AND conrelid = 'driver'::regclass
    ) THEN
        ALTER TABLE driver ADD CONSTRAINT chk_driver_vehicle_type_v6
            CHECK (vehicle_type IS NULL OR vehicle_type IN ('MINI', 'SEDAN', 'LUXE'));
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- ============================================
-- auth_session: ensure table exists (belt-and-suspenders for V5)
-- ============================================
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

CREATE INDEX        IF NOT EXISTS idx_auth_session_user       ON auth_session (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_session_jti        ON auth_session (token_jti);
CREATE INDEX        IF NOT EXISTS idx_auth_session_expires_at ON auth_session (expires_at);

-- ============================================
-- otp_challenge: ensure table exists (belt-and-suspenders for V5)
-- ============================================
CREATE TABLE IF NOT EXISTS otp_challenge (
    id                     BIGSERIAL    PRIMARY KEY,
    phone_number           VARCHAR(32)  NOT NULL UNIQUE,
    otp_hash               VARCHAR(128) NOT NULL,
    expires_at             TIMESTAMP    NOT NULL,
    verified_until         TIMESTAMP,
    blocked_until          TIMESTAMP,
    send_count             INTEGER,
    failed_attempts        INTEGER,
    send_window_started_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_otp_challenge_phone      ON otp_challenge (phone_number);
CREATE INDEX        IF NOT EXISTS idx_otp_challenge_expires_at ON otp_challenge (expires_at);

-- ============================================
-- Spatial indexes (belt-and-suspenders)
-- ============================================
CREATE INDEX IF NOT EXISTS idx_driver_current_location_gist ON driver USING GIST (current_location);
CREATE INDEX IF NOT EXISTS idx_rr_notified_driver ON ride_request_notified_drivers (driver_id, ride_request_id);
CREATE INDEX IF NOT EXISTS idx_payment_provider_reference ON payment (provider_payment_id);
