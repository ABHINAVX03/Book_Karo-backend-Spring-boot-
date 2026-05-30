-- ride_request.otp is required by RideRequest entity (V1 schema) but missing on some prod DBs
ALTER TABLE ride_request ADD COLUMN IF NOT EXISTS otp VARCHAR(255);
