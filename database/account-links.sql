-- Apply after database/schema.sql.
CREATE TABLE account_links (
    supabase_user_id UUID PRIMARY KEY,
    employee_id BIGINT UNIQUE REFERENCES employees (id),
    technician_id BIGINT UNIQUE REFERENCES technicians (id),
    CONSTRAINT account_links_single_role CHECK (
        (employee_id IS NOT NULL AND technician_id IS NULL)
        OR (employee_id IS NULL AND technician_id IS NOT NULL)
    )
);

-- Account links are provisioned by the database team and read by Spring.
ALTER TABLE account_links ENABLE ROW LEVEL SECURITY;
