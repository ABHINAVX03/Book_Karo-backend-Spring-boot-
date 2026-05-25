-- V1.1__rollback_schema.sql
-- Rollback script for V1 schema migration
-- This script should only be used in development/testing environments
-- In production, use Flyway's undo migrations or create forward migrations instead

-- WARNING: This script drops all tables and data
-- Only use in development/testing environments

DO $$ 
BEGIN
    RAISE NOTICE 'Starting rollback of V1 schema...';
    
    -- Drop triggers first
    DROP TRIGGER IF EXISTS update_app_user_updated_at ON app_user;
    DROP TRIGGER IF EXISTS update_rider_updated_at ON rider;
    DROP TRIGGER IF EXISTS update_driver_updated_at ON driver;
    DROP TRIGGER IF EXISTS update_wallet_updated_at ON wallet;
    DROP TRIGGER IF EXISTS update_ride_request_updated_at ON ride_request;
    DROP TRIGGER IF EXISTS update_ride_updated_at ON ride;
    DROP TRIGGER IF EXISTS update_payment_updated_at ON payment;
    DROP TRIGGER IF EXISTS update_rating_updated_at ON rating;
    DROP TRIGGER IF EXISTS update_otp_challenge_updated_at ON otp_challenge;
    
    RAISE NOTICE 'Dropped triggers';
    
    -- Drop function
    DROP FUNCTION IF EXISTS update_updated_at_column();
    
    RAISE NOTICE 'Dropped update_updated_at_column function';
    
    -- Drop tables in reverse order of dependencies
    DROP TABLE IF EXISTS audit_log CASCADE;
    DROP TABLE IF EXISTS otp_challenge CASCADE;
    DROP TABLE IF EXISTS auth_session CASCADE;
    DROP TABLE IF EXISTS rating CASCADE;
    DROP TABLE IF EXISTS payment CASCADE;
    DROP TABLE IF EXISTS wallet_transaction CASCADE;
    DROP TABLE IF EXISTS ride_request_notified_drivers CASCADE;
    DROP TABLE IF EXISTS ride CASCADE;
    DROP TABLE IF EXISTS ride_request CASCADE;
    DROP TABLE IF EXISTS wallet CASCADE;
    DROP TABLE IF EXISTS driver CASCADE;
    DROP TABLE IF EXISTS rider CASCADE;
    DROP TABLE IF EXISTS user_roles CASCADE;
    DROP TABLE IF EXISTS app_user CASCADE;
    
    RAISE NOTICE 'Dropped all tables';
    
    -- Note: We don't drop the PostGIS extension as it might be used by other applications
    -- DROP EXTENSION IF EXISTS postgis;
    
    RAISE NOTICE 'Rollback completed successfully';
    
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Rollback failed: %', SQLERRM;
    RAISE NOTICE 'Some tables may still exist. Manual cleanup may be required.';
END $$;