-- Add refresh token for security rotation
ALTER TABLE app_user ADD COLUMN refresh_token TEXT;

-- Convert financial columns to NUMERIC for exact precision (BigDecimal)
ALTER TABLE ride ALTER COLUMN fare TYPE NUMERIC(19, 2);
ALTER TABLE ride_request ALTER COLUMN fare TYPE NUMERIC(19, 2);
ALTER TABLE wallet ALTER COLUMN balance TYPE NUMERIC(19, 2);
ALTER TABLE wallet_transaction ALTER COLUMN amount TYPE NUMERIC(19, 2);
ALTER TABLE payment ALTER COLUMN amount TYPE NUMERIC(19, 2);
