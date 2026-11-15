ALTER TABLE employees
    ADD COLUMN supervisor_id UUID REFERENCES employees(id);

CREATE INDEX idx_employees_supervisor ON employees(supervisor_id);

ALTER TABLE leave_requests
    ALTER COLUMN status TYPE VARCHAR(32);

ALTER TABLE leave_requests
    ADD COLUMN supervisor_by UUID REFERENCES users(id),
    ADD COLUMN supervisor_at TIMESTAMPTZ,
    ADD COLUMN supervisor_remarks TEXT,
    ADD COLUMN hr_by UUID REFERENCES users(id),
    ADD COLUMN hr_at TIMESTAMPTZ,
    ADD COLUMN hr_remarks TEXT,
    ADD COLUMN rejection_reason TEXT;

ALTER TABLE time_entries
    ADD COLUMN leave_request_id UUID REFERENCES leave_requests(id) ON DELETE SET NULL,
    ADD COLUMN remarks VARCHAR(160);

CREATE INDEX idx_time_entries_leave_req ON time_entries(leave_request_id);
