DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname = 'idx_audit_logs_created'
    ) THEN
        CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
    END IF;
END
$$;
