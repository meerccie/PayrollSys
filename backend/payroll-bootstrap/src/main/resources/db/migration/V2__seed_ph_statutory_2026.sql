-- V2__seed_ph_statutory_2026.sql: Default PH statutory tables, leave types, earning/deduction types, and 2026 holidays

INSERT INTO company_settings (id, company_name, annual_work_days, statutory_schedule, default_pay_frequency)
VALUES (1, 'Acme Philippines Inc.', 261, 'SPLIT', 'SEMI_MONTHLY')
ON CONFLICT (id) DO NOTHING;

INSERT INTO statutory_philhealth_tables (id, effective_date, total_rate, salary_floor, salary_ceiling)
VALUES ('11111111-1111-1111-1111-111111111101', '2026-01-01', 0.0500, 10000.00, 100000.00);

INSERT INTO statutory_pagibig_tables (id, effective_date, low_threshold, low_employee_rate, standard_employee_rate, employer_rate, max_fund_salary)
VALUES ('11111111-1111-1111-1111-111111111102', '2026-01-01', 1500.00, 0.0100, 0.0200, 0.0200, 10000.00);

-- Leave types (SIL 5 days statutory minimum)
INSERT INTO leave_types (id, code, name, paid, annual_entitlement, active) VALUES
('22222222-2222-2222-2222-222222222201', 'SIL', 'Service Incentive Leave', TRUE, 5.00, TRUE),
('22222222-2222-2222-2222-222222222202', 'VL', 'Vacation Leave', TRUE, 10.00, TRUE),
('22222222-2222-2222-2222-222222222203', 'SL', 'Sick Leave', TRUE, 10.00, TRUE),
('22222222-2222-2222-2222-222222222204', 'LWOP', 'Leave Without Pay', FALSE, 0.00, TRUE);

-- Common earning & deduction types
INSERT INTO earning_types (id, code, name, taxable, recurring_allowed, active) VALUES
('33333333-3333-3333-3333-333333333301', 'RICE', 'Rice Subsidy', FALSE, TRUE, TRUE),
('33333333-3333-3333-3333-333333333302', 'TRANSPO', 'Transportation Allowance', TRUE, TRUE, TRUE),
('33333333-3333-3333-3333-333333333303', 'BONUS', 'Performance Bonus', TRUE, FALSE, TRUE);

INSERT INTO deduction_types (id, code, name, recurring_allowed, loan_type, active) VALUES
('44444444-4444-4444-4444-444444444401', 'SSS_LOAN', 'SSS Salary Loan', TRUE, TRUE, TRUE),
('44444444-4444-4444-4444-444444444402', 'HDMF_LOAN', 'Pag-IBIG Multi-Purpose Loan', TRUE, TRUE, TRUE),
('44444444-4444-4444-4444-444444444403', 'CASH_ADV', 'Cash Advance', FALSE, FALSE, TRUE);

-- 2026 Philippine National Holidays
INSERT INTO holidays (id, holiday_date, name, type) VALUES
('55555555-5555-5555-5555-555555555501', '2026-01-01', 'New Year''s Day', 'REGULAR'),
('55555555-5555-5555-5555-555555555502', '2026-04-02', 'Maundy Thursday', 'REGULAR'),
('55555555-5555-5555-5555-555555555503', '2026-04-03', 'Good Friday', 'REGULAR'),
('55555555-5555-5555-5555-555555555504', '2026-04-09', 'Araw ng Kagitingan', 'REGULAR'),
('55555555-5555-5555-5555-555555555505', '2026-05-01', 'Labor Day', 'REGULAR'),
('55555555-5555-5555-5555-555555555506', '2026-06-12', 'Independence Day', 'REGULAR'),
('55555555-5555-5555-5555-555555555507', '2026-08-21', 'Ninoy Aquino Day', 'SPECIAL_NON_WORKING'),
('55555555-5555-5555-5555-555555555508', '2026-08-31', 'National Heroes Day', 'REGULAR'),
('55555555-5555-5555-5555-555555555509', '2026-11-01', 'All Saints'' Day', 'SPECIAL_NON_WORKING'),
('55555555-5555-5555-5555-555555555510', '2026-11-30', 'Bonifacio Day', 'REGULAR'),
('55555555-5555-5555-5555-555555555511', '2026-12-08', 'Feast of the Immaculate Conception', 'SPECIAL_NON_WORKING'),
('55555555-5555-5555-5555-555555555512', '2026-12-25', 'Christmas Day', 'REGULAR'),
('55555555-5555-5555-5555-555555555513', '2026-12-30', 'Rizal Day', 'REGULAR');
