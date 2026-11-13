package com.payroll.domain.payroll;

import static org.assertj.core.api.Assertions.assertThat;

import com.payroll.domain.attendance.DayType;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.common.Money;
import com.payroll.domain.compensation.DeductionType;
import com.payroll.domain.compensation.EarningType;
import com.payroll.domain.compensation.EmployeeDeduction;
import com.payroll.domain.compensation.EmployeeEarning;
import com.payroll.domain.employee.BankAccount;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.employee.EmploymentStatus;
import com.payroll.domain.employee.PayBasis;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveType;
import com.payroll.domain.payroll.calc.PayrollCalculator;
import com.payroll.domain.payroll.calc.PayrollContext;
import com.payroll.domain.payroll.calc.PayrollSettings;
import com.payroll.domain.payroll.calc.PremiumRateTable;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayrollCalculatorTest {

    private final PayrollCalculator calculator = new PayrollCalculator();

    private Employee monthlyEmployee(String monthlyRate) {
        return Employee.create(
                "EMP-001", "Juan", null, "Dela Cruz",
                LocalDate.of(1995, 5, 15), "juan@example.com", "09171234567", "Manila",
                "123-456-789-000", "34-1234567-8", "12-345678901-2", "1234-5678-9012",
                LocalDate.of(2024, 1, 1), EmploymentStatus.REGULAR, PayBasis.MONTHLY,
                Money.of(monthlyRate), UUID.randomUUID(), UUID.randomUUID(),
                new BankAccount("BPI", "1234567890", "Juan Dela Cruz"),
                Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }

    private PayrollContext context(
            Employee emp,
            PayPeriod period,
            List<TimeEntry> entries,
            List<LeaveRequest> leaves,
            Map<UUID, LeaveType> leaveTypes,
            List<EmployeeEarning> earnings,
            Map<UUID, EarningType> earningTypes,
            List<EmployeeDeduction> deductions,
            Map<UUID, DeductionType> deductionTypes) {
        return new PayrollContext(
                UUID.randomUUID(),
                period,
                emp,
                "Engineering",
                "Software Engineer",
                entries,
                leaves,
                leaveTypes,
                earnings,
                earningTypes,
                deductions,
                deductionTypes,
                SssTable.default2026(),
                PhilHealthTable.default2026(),
                PagIbigTable.default2026(),
                BirTaxTable.default2026(),
                PremiumRateTable.standardPh(),
                PayrollSettings.defaultPh(),
                Instant.now());
    }

    @Test
    void goldenMonthly30kEmployee() {
        Employee emp = monthlyEmployee("30000.00");
        PayPeriod period = PayPeriod.create(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 31), PayFrequency.MONTHLY, CutoffType.FULL_MONTH, 2026, 3);

        Payslip slip = calculator.calculate(context(emp, period, List.of(), List.of(), Map.of(), List.of(), Map.of(), List.of(), Map.of()));

        assertThat(slip.getBasicPay()).isEqualTo(Money.of("30000.00"));
        assertThat(slip.getGrossPay()).isEqualTo(Money.of("30000.00"));
        assertThat(slip.getSssEmployee()).isEqualTo(Money.of("1500.00"));
        assertThat(slip.getSssEmployer()).isEqualTo(Money.of("3000.00"));
        assertThat(slip.getSssEcEmployer()).isEqualTo(Money.of("30.00"));
        assertThat(slip.getPhilHealthEmployee()).isEqualTo(Money.of("750.00"));
        assertThat(slip.getPagIbigEmployee()).isEqualTo(Money.of("200.00"));
        assertThat(slip.getTaxableIncome()).isEqualTo(Money.of("27550.00"));
        assertThat(slip.getWithholdingTax()).isEqualTo(Money.of("1007.55"));
        assertThat(slip.getNetPay()).isEqualTo(Money.of("26542.45"));
    }

    @Test
    void goldenSemiMonthlySplit30kEmployee() {
        Employee emp = monthlyEmployee("30000.00");
        PayPeriod period = PayPeriod.create(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15), PayFrequency.SEMI_MONTHLY, CutoffType.FIRST, 2026, 5);

        Payslip slip = calculator.calculate(context(emp, period, List.of(), List.of(), Map.of(), List.of(), Map.of(), List.of(), Map.of()));

        assertThat(slip.getBasicPay()).isEqualTo(Money.of("15000.00"));
        assertThat(slip.getSssEmployee()).isEqualTo(Money.of("750.00"));
        assertThat(slip.getPhilHealthEmployee()).isEqualTo(Money.of("375.00"));
        assertThat(slip.getPagIbigEmployee()).isEqualTo(Money.of("100.00"));
        assertThat(slip.getTaxableIncome()).isEqualTo(Money.of("13775.00"));
        assertThat(slip.getWithholdingTax()).isEqualTo(Money.of("503.70"));
        assertThat(slip.getNetPay()).isEqualTo(Money.of("13271.30"));
    }

    @Test
    void overtimeAndNonTaxableAllowanceAndLoanCap() {
        Employee emp = monthlyEmployee("20000.00");
        PayPeriod period = PayPeriod.create(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15), PayFrequency.SEMI_MONTHLY, CutoffType.FIRST, 2026, 5);

        TimeEntry otEntry = TimeEntry.create(
                emp.getId(), LocalDate.of(2026, 3, 5),
                LocalTime.of(9, 0), LocalTime.of(20, 0),
                new BigDecimal("8.00"), new BigDecimal("2.00"), BigDecimal.ZERO,
                0, 0, DayType.REGULAR);
        otEntry.approve();

        EarningType rice = EarningType.create("RICE", "Rice Subsidy", false, true);
        EmployeeEarning riceEarning = EmployeeEarning.create(
                emp.getId(), rice.getId(), Money.of("1000.00"), true, LocalDate.of(2026, 1, 1), null, null);

        DeductionType loanType = DeductionType.create("LOAN", "Company Loan", true, true);
        EmployeeDeduction hugeLoan = EmployeeDeduction.create(
                emp.getId(), loanType.getId(), Money.of("50000.00"), true, LocalDate.of(2026, 1, 1), null, null, Money.of("50000.00"));

        Payslip slip = calculator.calculate(context(
                emp, period,
                List.of(otEntry),
                List.of(), Map.of(),
                List.of(riceEarning), Map.of(rice.getId(), rice),
                List.of(hugeLoan), Map.of(loanType.getId(), loanType)));

        assertThat(slip.getTotalOvertimeAndPremiums().isPositive()).isTrue();
        assertThat(slip.getTotalEarnings()).isEqualTo(Money.of("1000.00"));
        assertThat(slip.isHasNetPayWarning()).isTrue();
        assertThat(slip.getNetPay()).isEqualTo(Money.ZERO);
    }

    @Test
    void unpaidLeaveDeductsFromMonthlyEmployee() {
        Employee emp = monthlyEmployee("26100.00");
        // dailyRate = 26100 * 12 / 261 = 1200.00
        PayPeriod period = PayPeriod.create(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15), PayFrequency.SEMI_MONTHLY, CutoffType.FIRST, 2026, 5);

        LeaveType lwop = LeaveType.create("LWOP", "Leave Without Pay", false, BigDecimal.ZERO);
        LeaveRequest req = LeaveRequest.file(emp.getId(), lwop.getId(),
                LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 4),
                BigDecimal.ONE, false, "Personal");
        req.approve(UUID.randomUUID(), Instant.now());

        Payslip slip = calculator.calculate(context(
                emp, period, List.of(),
                List.of(req), Map.of(lwop.getId(), lwop),
                List.of(), Map.of(), List.of(), Map.of()));

        assertThat(slip.getTotalAbsencesAndLate()).isEqualTo(Money.of("1200.00"));
        assertThat(slip.getGrossPay()).isEqualTo(Money.of("11850.00"));
    }
}
