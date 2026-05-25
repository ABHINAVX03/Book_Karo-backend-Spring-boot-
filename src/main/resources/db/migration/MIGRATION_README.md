# V1 Schema Migration - Complete Production Ready Schema

## Overview
This migration provides a complete, production-ready database schema for the Uber ride-hailing application. The migration is fully idempotent and includes all necessary constraints, indexes, and defaults for data integrity and performance.

## Key Improvements

### 1. **Data Integrity**
- **NOT NULL constraints** on all required columns
- **CHECK constraints** for enum validation, rating ranges, and business rules
- **Foreign key constraints** with appropriate cascade rules
- **Unique constraints** where appropriate (email, phone numbers, etc.)

### 2. **Performance Optimization**
- **Composite indexes** for common query patterns (status + type, available + type)
- **Spatial indexes** (GIST) for location-based queries
- **B-tree indexes** for foreign keys and frequently queried columns
- **Covering indexes** for common filter combinations

### 3. **Idempotency**
- All `CREATE TABLE` statements use `IF NOT EXISTS`
- All `CREATE INDEX` statements use `IF NOT EXISTS`
- `CREATE OR REPLACE` for functions
- Migration can be run multiple times without errors

### 4. **Production Features**
- **Audit logging** table for tracking changes
- **Automatic timestamps** (created_at, updated_at) with triggers
- **Proper cascade rules** for data integrity
- **Default values** for sensible defaults
- **Comments** for documentation

## Schema Changes Summary

### New Tables Added
1. **audit_log** - For tracking all data changes (optional but recommended)
2. **Proper user_roles table** - Fixed junction table for many-to-many relationship

### Enhanced Tables
1. **app_user**
   - Added `created_at` and `updated_at` timestamps
   - Added phone number validation regex
   - Added email validation regex
   - Added constraint for failed login attempts

2. **driver**
   - Added `total_rides` counter
   - Added `created_at` and `updated_at` timestamps
   - Added CHECK constraints for verification status
   - Added CHECK constraints for vehicle type

3. **ride_request**
   - Added `estimated_distance` and `estimated_duration`
   - Added `expires_at` timestamp with default
   - Added `created_at` and `updated_at` timestamps
   - Added spatial indexes for pickup/dropoff locations

4. **ride**
   - Added `actual_distance` and `actual_duration`
   - Added `created_at` and `updated_at` timestamps
   - Added CHECK constraints for timestamp consistency

5. **payment**
   - Added `created_at` and `updated_at` timestamps
   - Added CHECK constraints for payment status

6. **rating**
   - Added `driver_comment` and `rider_comment` fields
   - Added `created_at` and `updated_at` timestamps

7. **wallet_transaction**
   - Added `description` field
   - Added CHECK constraints for transaction types

### New Features
1. **Automatic updated_at triggers** - All tables with `updated_at` columns automatically update on modification
2. **Spatial indexing** - All location columns have GIST indexes for efficient proximity searches
3. **Data validation** - Regex patterns for email and phone numbers
4. **Business rule enforcement** - CHECK constraints enforce rating ranges, fare minimums, etc.

## Index Strategy

### Critical Indexes
1. **Location-based queries**: GIST indexes on all geometry columns
2. **Status filtering**: Indexes on status columns (ride_status, payment_status, etc.)
3. **Foreign key lookups**: Indexes on all foreign key columns
4. **Composite queries**: Indexes on common filter combinations (available + vehicle_type)
5. **Time-based queries**: Indexes on created_at, updated_at, and timestamp columns

### Spatial Indexes
- `idx_driver_current_location_gist` - Driver proximity searches
- `idx_ride_request_pickup_location_gist` - Nearby ride requests
- `idx_ride_request_dropoff_location_gist` - Destination-based queries
- `idx_ride_pickup_location_gist` - Ride history location queries
- `idx_ride_dropoff_location_gist` - Destination analysis

## Cascade Rules

### ON DELETE CASCADE
- `user_roles` → `app_user`
- `rider` → `app_user`
- `driver` → `app_user`
- `wallet` → `app_user`
- `auth_session` → `app_user`
- `rating` → `ride`, `rider`, `driver`
- `wallet_transaction` → `wallet`

### ON DELETE SET NULL
- `ride` → `ride_request` (ride can exist without original request)
- `wallet_transaction` → `ride` (transactions can exist without ride reference)

## Data Validation

### Regex Patterns
- **Email**: `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`
- **Phone**: `^\+?[1-9]\d{1,14}$` (E.164 format)

### Range Checks
- **Ratings**: 0.0 to 5.0 for drivers/riders, 1 to 5 for ride ratings
- **Fares**: >= 0.00
- **Distances**: >= 0.00
- **Durations**: >= 0 seconds

### Enum Validation
All VARCHAR columns that should contain specific values have CHECK constraints:
- `vehicle_type`: SEDAN, SUV, HATCHBACK, BIKE
- `payment_method`: WALLET, CASH, RAZORPAY
- `ride_status`: CANCELLED, CONFIRMED, ENDED, ONGOING
- `verification_status`: PENDING, APPROVED, REJECTED, SUSPENDED

## Testing

### Test Script
Run `test_migration.sql` to verify:
1. Idempotency (can run migration multiple times)
2. Table creation (all tables exist)
3. Constraint enforcement (NOT NULL, CHECK constraints)
4. Index creation (all indexes exist)
5. Foreign key relationships
6. Data insertion (basic CRUD operations)
7. Cascade delete functionality

### Rollback Script
`V1.1__rollback_schema.sql` provides a complete rollback for development/testing. **Do not use in production**.

## Production Deployment Notes

### 1. **Database Requirements**
- PostgreSQL 12+
- PostGIS 3.0+ extension
- Adequate storage for spatial indexes

### 2. **Performance Considerations**
- Spatial indexes increase storage requirements
- Consider partitioning for large tables (ride_history, audit_log)
- Monitor index usage and adjust as needed

### 3. **Migration Strategy**
1. **Development/Testing**: Run V1 migration directly
2. **Staging**: Test with production-like data volume
3. **Production**: 
   - Backup database first
   - Run during maintenance window
   - Monitor application performance
   - Have rollback plan ready

### 4. **Monitoring**
- Monitor spatial index performance
- Track constraint violations
- Audit log growth rate
- Foreign key cascade performance

## Breaking Changes from Previous Version

1. **Added NOT NULL constraints** - Existing NULL values will cause migration failure
2. **Added CHECK constraints** - Invalid data will cause migration failure
3. **Changed column types** - Some columns may have stricter requirements
4. **Added new tables** - Requires empty slots in Flyway migration history

## Data Migration Requirements

If migrating from an existing database with data:

1. **Backup data** before migration
2. **Clean invalid data** (NULLs in required columns, invalid enum values)
3. **Add default values** for new NOT NULL columns
4. **Test migration** on a copy of production data
5. **Plan for downtime** during migration

## Support

For issues with this migration:
1. Check Flyway logs for specific errors
2. Verify database user has necessary permissions
3. Ensure PostGIS extension is available
4. Test with empty database first
5. Consult the test script for validation

## Version History
- **V1.0** (Current): Complete production-ready schema
- **V1.1**: Rollback script for development/testing

---

**Note**: This migration is designed for `spring.jpa.hibernate.ddl-auto=validate` mode. The schema should match the JPA entity definitions exactly.