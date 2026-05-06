# Flyway Database Migrations

This directory contains versioned SQL migration scripts managed by Flyway.

## Overview

Flyway is a database migration tool that allows you to version and manage database schema changes. Instead of using Hibernate's `ddl-auto=update` (which can be unpredictable in production), we use explicit SQL migration scripts.

## Naming Convention

Migration files must follow this naming pattern:

```
V<version>__<description>.sql
```

Examples:
- `V1__Initial_Schema.sql` - Version 1, creates initial tables
- `V2__Seed_Data.sql` - Version 2, inserts seed data
- `V3__Add_User_Phone_Column.sql` - Version 3, adds phone column

**Rules:**
- Prefix: `V` (uppercase)
- Version: Number (1, 2, 3, etc.) or semantic (1.0, 1.1, 2.0)
- Separator: `__` (double underscore)
- Description: Snake_case or camelCase
- Extension: `.sql`

## Migration Workflow

1. **Create Migration**: Add new file with next version number
2. **Test Locally**: Run application, Flyway auto-applies migrations
3. **Commit**: Add migration file to git
4. **Deploy**: Flyway runs migrations on production database

## Flyway Commands

Flyway automatically runs on application startup, but you can also use CLI:

```bash
# Show migration status
mvn flyway:info

# Run migrations
mvn flyway:migrate

# Validate applied migrations match source files
mvn flyway:validate

# Clean database (DANGER: Drops all objects)
mvn flyway:clean
```

## Migration Best Practices

### 1. Never Modify Applied Migrations
Once a migration is applied to any environment (especially production), **never modify it**. Create a new migration instead.

❌ **Bad:**
```sql
-- V1__Initial_Schema.sql (already applied)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255)
    -- LATER: Add email column here  ❌ DON'T DO THIS
);
```

✅ **Good:**
```sql
-- V3__Add_User_Email.sql (new migration)
ALTER TABLE users ADD COLUMN email VARCHAR(255);
```

### 2. Make Migrations Idempotent (When Possible)
Use `IF NOT EXISTS` for creates, `IF EXISTS` for drops:

```sql
CREATE TABLE IF NOT EXISTS users (...);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS status VARCHAR(50);
```

### 3. Test Migrations Thoroughly
- Test on empty database (new install)
- Test on database with existing data (upgrade path)
- Test rollback strategy if needed

### 4. Keep Migrations Small
Better to have many small migrations than one huge migration.

### 5. Add Comments
Explain non-obvious changes:

```sql
-- Add index to improve dispute query performance (filtering by status)
CREATE INDEX idx_dispute_status ON disputes(status);
```

### 6. Handle Data Migrations Carefully
When changing data types or moving data:

```sql
-- 1. Add new column
ALTER TABLE users ADD COLUMN email_new VARCHAR(255);

-- 2. Copy data
UPDATE users SET email_new = email;

-- 3. Drop old column
ALTER TABLE users DROP COLUMN email;

-- 4. Rename new column
ALTER TABLE users RENAME COLUMN email_new TO email;
```

## Production Deployment

For zero-downtime deployments:

1. **Backwards Compatible Migrations**: New code works with old schema
2. **Deploy Migration**: Apply migration first
3. **Deploy Code**: Deploy new application version
4. **Cleanup**: Remove deprecated columns/tables in later migration

## Rollback Strategy

Flyway doesn't auto-rollback. Options:

### Option 1: Manual Rollback (Simple)
Create undo script manually:

```sql
-- V4__Add_Phone_Column.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- U4__Undo_Add_Phone_Column.sql (manual)
ALTER TABLE users DROP COLUMN phone;
```

### Option 2: Flyway Pro Undo Migrations
Requires Flyway Pro license.

### Option 3: Backup & Restore
- Take database backup before migration
- Restore if migration fails

## Current Migrations

- **V1__Initial_Schema.sql**: Creates core tables (users, transactions, disputes, audit_logs)
- **V2__Seed_Data.sql**: Inserts test data (admin, customers, sample transactions)

## Configuration

See `application.properties`:

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-version=0
```

## Troubleshooting

### Error: "Found non-empty schema without schema history table"
**Solution:** Set `spring.flyway.baseline-on-migrate=true` (already configured)

### Error: "Validate failed: Checksum mismatch for migration V1"
**Cause:** Migration file was modified after being applied
**Solution:** 
- Option 1: Revert changes to migration file
- Option 2: Repair Flyway metadata: `mvn flyway:repair`

### Error: Migration fails mid-execution
**Solution:**
1. Fix the issue in the migration file
2. Manually clean up partial changes in database
3. Run `mvn flyway:repair` to mark failed migration as fixed
4. Restart application

## Resources

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Migration Patterns](https://flywaydb.org/documentation/migrations)
- [Best Practices](https://flywaydb.org/documentation/bestpractices)
