DO $$ DECLARE
    r RECORD;
BEGIN
    -- Disable referential integrity temporarily
    PERFORM 'ALTER TABLE ' || quote_ident(schemaname) || '.' || quote_ident(tablename) || ' DISABLE TRIGGER ALL'
    FROM pg_tables
    WHERE schemaname = 'public';

    -- Loop through all tables in the public schema and truncate them
    FOR r IN (SELECT schemaname, tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.schemaname) || '.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
    END LOOP;

    -- Re-enable referential integrity
    PERFORM 'ALTER TABLE ' || quote_ident(schemaname) || '.' || quote_ident(tablename) || ' ENABLE TRIGGER ALL'
    FROM pg_tables
    WHERE schemaname = 'public';
END $$;
