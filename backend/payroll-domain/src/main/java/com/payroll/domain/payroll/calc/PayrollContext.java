package com.payroll.domain.payroll.calc;

import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.compensation.DeductionType;
import com.payroll.domain.compensation.EarningType;
import com.payroll.domain.compensation.EmployeeDeduction;
import com.payroll.domain.compensation.EmployeeEarning;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveType;
import com.payroll.domain.payroll.PayPeriod;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PayrollContext(
        UUID payrollRunId,
        PayPeriod payPeriod,
        Employee employee,
        String departmentName,
        String positionName,
        List<TimeEntry> timeEntries,
        List<LeaveRequest> approvedLeaves,
        Map<UUID, LeaveType> leaveTypesById,
        List<EmployeeEarning> earnings,
        Map<UUID, EarningType> earningTypesById,
        List<EmployeeDeduction> deductions,
        Map<UUID, DeductionType> deductionTypesById,
        SssTable sssTable,
        PhilHealthTable philHealthTable,
        PagIbigTable pagIbigTable,
        BirTaxTable birTaxTable,
        PremiumRateTable premiumRates,
        PayrollSettings settings,
        Instant generatedAt) {
}
