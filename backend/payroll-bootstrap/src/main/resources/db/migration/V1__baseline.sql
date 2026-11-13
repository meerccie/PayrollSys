-- V1__baseline.sql: Core schema for Philippine Payroll System

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE positions (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    employee_no VARCHAR(32) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80) NOT NULL,
    birth_date DATE,
    email VARCHAR(160) NOT NULL UNIQUE,
    phone VARCHAR(40),
    address TEXT,
    tin VARCHAR(32),
    sss_no VARCHAR(32),
    philhealth_no VARCHAR(32),
    pagibig_no VARCHAR(32),
    hire_date DATE NOT NULL,
    separation_date DATE,
    status VARCHAR(24) NOT NULL,
    pay_basis VARCHAR(16) NOT NULL,
    base_rate NUMERIC(14, 2) NOT NULL,
    department_id UUID REFERENCES departments(id),
    position_id UUID REFERENCES positions(id),
    bank_name VARCHAR(100),
    bank_account_no VARCHAR(60),
    bank_account_name VARCHAR(120),
    rest_days VARCHAR(64) NOT NULL DEFAULT 'SATURDAY,SUNDAY',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_employees_dept ON employees(department_id);
CREATE INDEX idx_employees_status ON employees(status);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(24) NOT NULL,
    employee_id UUID UNIQUE REFERENCES employees(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE holidays (
    id UUID PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE time_entries (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    work_date DATE NOT NULL,
    time_in TIME,
    time_out TIME,
    regular_hours NUMERIC(6, 2) NOT NULL DEFAULT 0,
    overtime_hours NUMERIC(6, 2) NOT NULL DEFAULT 0,
    night_diff_hours NUMERIC(6, 2) NOT NULL DEFAULT 0,
    late_minutes INT NOT NULL DEFAULT 0,
    undertime_minutes INT NOT NULL DEFAULT 0,
    day_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_time_entry_emp_date UNIQUE (employee_id, work_date)
);
CREATE INDEX idx_time_entries_date ON time_entries(work_date);

CREATE TABLE leave_types (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT TRUE,
    annual_entitlement NUMERIC(6, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE leave_balances (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    year INT NOT NULL,
    entitled_days NUMERIC(6, 2) NOT NULL DEFAULT 0,
    used_days NUMERIC(6, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_balance UNIQUE (employee_id, leave_type_id, year)
);

CREATE TABLE leave_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days NUMERIC(6, 2) NOT NULL,
    half_day BOOLEAN NOT NULL DEFAULT FALSE,
    reason TEXT,
    status VARCHAR(16) NOT NULL,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_leave_requests_emp ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);

CREATE TABLE earning_types (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    taxable BOOLEAN NOT NULL DEFAULT TRUE,
    recurring_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE deduction_types (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    recurring_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    loan_type BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pay_periods (
    id UUID PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    pay_date DATE NOT NULL,
    frequency VARCHAR(16) NOT NULL,
    cutoff VARCHAR(16) NOT NULL,
    year INT NOT NULL,
    period_number INT NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_pay_period UNIQUE (year, frequency, period_number)
);

CREATE TABLE employee_earnings (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    earning_type_id UUID NOT NULL REFERENCES earning_types(id),
    amount NUMERIC(14, 2) NOT NULL,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    effective_start DATE,
    effective_end DATE,
    pay_period_id UUID REFERENCES pay_periods(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_emp_earnings_emp ON employee_earnings(employee_id);

CREATE TABLE employee_deductions (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    deduction_type_id UUID NOT NULL REFERENCES deduction_types(id),
    amount NUMERIC(14, 2) NOT NULL,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    effective_start DATE,
    effective_end DATE,
    pay_period_id UUID REFERENCES pay_periods(id),
    total_loan_amount NUMERIC(14, 2),
    remaining_balance NUMERIC(14, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_emp_deductions_emp ON employee_deductions(employee_id);

CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY,
    pay_period_id UUID NOT NULL UNIQUE REFERENCES pay_periods(id),
    status VARCHAR(16) NOT NULL,
    employee_count INT NOT NULL DEFAULT 0,
    total_gross_pay NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_net_pay NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_employer_contributions NUMERIC(14, 2) NOT NULL DEFAULT 0,
    computed_at TIMESTAMPTZ,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payslips (
    id UUID PRIMARY KEY,
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id),
    pay_period_id UUID NOT NULL REFERENCES pay_periods(id),
    employee_no VARCHAR(32) NOT NULL,
    employee_name VARCHAR(200) NOT NULL,
    department_name VARCHAR(120),
    position_name VARCHAR(120),
    basic_pay NUMERIC(14, 2) NOT NULL,
    total_earnings NUMERIC(14, 2) NOT NULL,
    total_overtime_and_premiums NUMERIC(14, 2) NOT NULL,
    total_absences_and_late NUMERIC(14, 2) NOT NULL,
    gross_pay NUMERIC(14, 2) NOT NULL,
    taxable_income NUMERIC(14, 2) NOT NULL,
    sss_employee NUMERIC(14, 2) NOT NULL,
    philhealth_employee NUMERIC(14, 2) NOT NULL,
    pagibig_employee NUMERIC(14, 2) NOT NULL,
    withholding_tax NUMERIC(14, 2) NOT NULL,
    other_deductions NUMERIC(14, 2) NOT NULL,
    total_deductions NUMERIC(14, 2) NOT NULL,
    net_pay NUMERIC(14, 2) NOT NULL,
    sss_employer NUMERIC(14, 2) NOT NULL,
    sss_ec_employer NUMERIC(14, 2) NOT NULL,
    philhealth_employer NUMERIC(14, 2) NOT NULL,
    pagibig_employer NUMERIC(14, 2) NOT NULL,
    has_net_pay_warning BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_payslip_run_emp UNIQUE (payroll_run_id, employee_id)
);
CREATE INDEX idx_payslips_emp ON payslips(employee_id);

CREATE TABLE payslip_lines (
    id UUID PRIMARY KEY,
    payslip_id UUID NOT NULL REFERENCES payslips(id) ON DELETE CASCADE,
    line_order INT NOT NULL,
    category VARCHAR(24) NOT NULL,
    code VARCHAR(40) NOT NULL,
    description VARCHAR(200) NOT NULL,
    quantity NUMERIC(10, 4) NOT NULL,
    rate NUMERIC(14, 2) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    taxable BOOLEAN NOT NULL
);
CREATE INDEX idx_payslip_lines_slip ON payslip_lines(payslip_id);

-- Effective-dated statutory tables
CREATE TABLE statutory_sss_tables (
    id UUID PRIMARY KEY,
    effective_date DATE NOT NULL UNIQUE,
    brackets_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE statutory_philhealth_tables (
    id UUID PRIMARY KEY,
    effective_date DATE NOT NULL UNIQUE,
    total_rate NUMERIC(6, 4) NOT NULL,
    salary_floor NUMERIC(14, 2) NOT NULL,
    salary_ceiling NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE statutory_pagibig_tables (
    id UUID PRIMARY KEY,
    effective_date DATE NOT NULL UNIQUE,
    low_threshold NUMERIC(14, 2) NOT NULL,
    low_employee_rate NUMERIC(6, 4) NOT NULL,
    standard_employee_rate NUMERIC(6, 4) NOT NULL,
    employer_rate NUMERIC(6, 4) NOT NULL,
    max_fund_salary NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE statutory_bir_tax_tables (
    id UUID PRIMARY KEY,
    effective_date DATE NOT NULL UNIQUE,
    brackets_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE company_settings (
    id INT PRIMARY KEY DEFAULT 1,
    company_name VARCHAR(200) NOT NULL DEFAULT 'Acme Philippines Inc.',
    company_tin VARCHAR(32),
    company_address TEXT,
    annual_work_days INT NOT NULL DEFAULT 261,
    statutory_schedule VARCHAR(20) NOT NULL DEFAULT 'SPLIT',
    default_pay_frequency VARCHAR(16) NOT NULL DEFAULT 'SEMI_MONTHLY',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
