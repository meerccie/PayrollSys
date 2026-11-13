package com.payroll.domain.payroll.calc;

import com.payroll.domain.attendance.DayType;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.attendance.TimeEntryStatus;
import com.payroll.domain.common.Money;
import com.payroll.domain.compensation.DeductionType;
import com.payroll.domain.compensation.EarningType;
import com.payroll.domain.compensation.EmployeeDeduction;
import com.payroll.domain.compensation.EmployeeEarning;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.employee.PayBasis;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveRequestStatus;
import com.payroll.domain.leave.LeaveType;
import com.payroll.domain.payroll.CutoffType;
import com.payroll.domain.payroll.PayFrequency;
import com.payroll.domain.payroll.PayPeriod;
import com.payroll.domain.payroll.Payslip;
import com.payroll.domain.payroll.PayslipLine;
import com.payroll.domain.payroll.PayslipLineCategory;
import com.payroll.domain.payroll.StatutorySchedule;
import com.payroll.domain.payroll.statutory.StatutoryContribution;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure Philippine payroll calculation engine.
 */
public final class PayrollCalculator {

    public Payslip calculate(PayrollContext ctx) {
        Employee emp = ctx.employee();
        PayPeriod period = ctx.payPeriod();
        PayrollSettings settings = ctx.settings();
        PremiumRateTable rates = ctx.premiumRates();

        Money dailyRate = emp.dailyRate(settings.annualWorkDays());
        Money hourlyRate = emp.hourlyRate(settings.annualWorkDays());

        List<PayslipLine> lines = new ArrayList<>();
        List<TimeEntry> approvedEntries = ctx.timeEntries().stream()
                .filter(e -> e.getStatus() == TimeEntryStatus.APPROVED)
                .toList();

        // 1. Basic pay
        Money basicPay = computeBasicPay(emp, period, approvedEntries, ctx.approvedLeaves(), ctx.leaveTypesById(), dailyRate, hourlyRate, lines);

        // 2. Absences & late/undertime
        Money totalAbsencesAndLate = computeAbsencesAndLate(
                emp, approvedEntries, ctx.approvedLeaves(), ctx.leaveTypesById(), dailyRate, hourlyRate, lines);

        // 3. Overtime, holiday premiums, night differential
        Money totalOvertimeAndPremiums = computeOvertimeAndPremiums(emp, approvedEntries, hourlyRate, rates, lines);

        // 4. Earnings (recurring + one-off)
        Money taxableEarnings = Money.ZERO;
        Money nonTaxableEarnings = Money.ZERO;
        for (EmployeeEarning earning : ctx.earnings()) {
            if (!earning.appliesTo(period.getId(), period.getStartDate(), period.getEndDate())) {
                continue;
            }
            EarningType type = ctx.earningTypesById().get(earning.getEarningTypeId());
            if (type == null) continue;
            Money amt = earning.getAmount();
            if (type.isTaxable()) {
                taxableEarnings = taxableEarnings.add(amt);
            } else {
                nonTaxableEarnings = nonTaxableEarnings.add(amt);
            }
            lines.add(new PayslipLine(
                    PayslipLineCategory.EARNING, type.getCode(), type.getName(),
                    BigDecimal.ONE, amt, amt, type.isTaxable()));
        }
        Money totalEarnings = taxableEarnings.add(nonTaxableEarnings);

        Money grossPay = basicPay
                .subtract(totalAbsencesAndLate)
                .add(totalOvertimeAndPremiums)
                .add(totalEarnings)
                .max(Money.ZERO);

        Money taxableGross = basicPay
                .subtract(totalAbsencesAndLate)
                .add(totalOvertimeAndPremiums)
                .add(taxableEarnings)
                .max(Money.ZERO);

        // 5. Statutory contributions (SSS, PhilHealth, Pag-IBIG)
        Money monthlyBasis = emp.monthlyEquivalent(settings.annualWorkDays());
        StatutoryContribution sss = applySchedule(ctx.sssTable().compute(monthlyBasis), period, settings.statutorySchedule());
        StatutoryContribution philHealth = applySchedule(ctx.philHealthTable().compute(monthlyBasis), period, settings.statutorySchedule());
        StatutoryContribution pagIbig = applySchedule(ctx.pagIbigTable().compute(monthlyBasis), period, settings.statutorySchedule());

        addStatutoryLines(lines, sss, philHealth, pagIbig);

        Money totalStatutoryEe = sss.employeeShare()
                .add(philHealth.employeeShare())
                .add(pagIbig.employeeShare());

        // 6. Taxable income & BIR withholding tax
        Money taxableIncome = taxableGross.subtract(totalStatutoryEe).max(Money.ZERO);
        Money withholdingTax = ctx.birTaxTable().compute(period.getFrequency(), taxableIncome);
        if (withholdingTax.isPositive()) {
            lines.add(new PayslipLine(
                    PayslipLineCategory.TAX, "BIR_WHT", "Withholding Tax (BIR)",
                    BigDecimal.ONE, withholdingTax, withholdingTax, false));
        }

        // 7. Other deductions (loans, advances) - protect net pay >= 0
        Money mandatory = totalStatutoryEe.add(withholdingTax);
        Money availableForOther = grossPay.subtract(mandatory).max(Money.ZERO);
        Money otherDeductions = Money.ZERO;
        boolean hasNetPayWarning = grossPay.lessThan(mandatory);

        for (EmployeeDeduction deduction : ctx.deductions()) {
            if (!deduction.appliesTo(period.getId(), period.getStartDate(), period.getEndDate())) {
                continue;
            }
            DeductionType type = ctx.deductionTypesById().get(deduction.getDeductionTypeId());
            if (type == null) continue;
            Money requested = deduction.effectiveAmountForPeriod();
            Money remainingRoom = availableForOther.subtract(otherDeductions).max(Money.ZERO);
            Money applied = requested.min(remainingRoom);
            if (applied.lessThan(requested)) {
                hasNetPayWarning = true;
            }
            if (applied.isPositive()) {
                otherDeductions = otherDeductions.add(applied);
                lines.add(new PayslipLine(
                        PayslipLineCategory.DEDUCTION, type.getCode(), type.getName(),
                        BigDecimal.ONE, applied, applied, false));
            }
        }

        Money totalDeductions = mandatory.add(otherDeductions);
        Money netPay = grossPay.subtract(totalDeductions).max(Money.ZERO);

        return new Payslip(
                UUID.randomUUID(),
                ctx.payrollRunId(),
                emp.getId(),
                period.getId(),
                emp.getEmployeeNo(),
                emp.fullName(),
                ctx.departmentName(),
                ctx.positionName(),
                basicPay,
                totalEarnings,
                totalOvertimeAndPremiums,
                totalAbsencesAndLate,
                grossPay,
                taxableIncome,
                sss.employeeShare(),
                philHealth.employeeShare(),
                pagIbig.employeeShare(),
                withholdingTax,
                otherDeductions,
                totalDeductions,
                netPay,
                sss.employerShare(),
                sss.ecShare(),
                philHealth.employerShare(),
                pagIbig.employerShare(),
                hasNetPayWarning,
                lines,
                ctx.generatedAt());
    }

    private Money computeBasicPay(
            Employee emp,
            PayPeriod period,
            List<TimeEntry> entries,
            List<LeaveRequest> leaves,
            java.util.Map<UUID, LeaveType> leaveTypesById,
            Money dailyRate,
            Money hourlyRate,
            List<PayslipLine> lines) {
        if (emp.getPayBasis() == PayBasis.MONTHLY) {
            Money basic = period.getFrequency() == PayFrequency.MONTHLY
                    ? emp.getBaseRate()
                    : emp.getBaseRate().divide(2);
            lines.add(new PayslipLine(
                    PayslipLineCategory.BASIC, "BASIC", "Basic Pay",
                    BigDecimal.ONE, basic, basic, true));
            return basic;
        }
        // DAILY basis: paid for regular hours worked on REGULAR days + paid approved leave days
        BigDecimal regularHours = entries.stream()
                .filter(e -> e.getDayType() == DayType.REGULAR)
                .map(TimeEntry::getRegularHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Money workedPay = hourlyRate.multiply(regularHours);

        BigDecimal paidLeaveDays = leaves.stream()
                .filter(l -> l.getStatus() == LeaveRequestStatus.APPROVED)
                .filter(l -> {
                    LeaveType lt = leaveTypesById.get(l.getLeaveTypeId());
                    return lt != null && lt.isPaid();
                })
                .map(LeaveRequest::getDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Money leavePay = dailyRate.multiply(paidLeaveDays);

        Money basic = workedPay.add(leavePay);
        lines.add(new PayslipLine(
                PayslipLineCategory.BASIC, "BASIC", "Basic Pay (Daily)",
                regularHours.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP).add(paidLeaveDays),
                dailyRate, basic, true));
        return basic;
    }

    private Money computeAbsencesAndLate(
            Employee emp,
            List<TimeEntry> entries,
            List<LeaveRequest> leaves,
            java.util.Map<UUID, LeaveType> leaveTypesById,
            Money dailyRate,
            Money hourlyRate,
            List<PayslipLine> lines) {
        Money total = Money.ZERO;

        if (emp.getPayBasis() == PayBasis.MONTHLY) {
            BigDecimal unpaidLeaveDays = leaves.stream()
                    .filter(l -> l.getStatus() == LeaveRequestStatus.APPROVED)
                    .filter(l -> {
                        LeaveType lt = leaveTypesById.get(l.getLeaveTypeId());
                        return lt != null && !lt.isPaid();
                    })
                    .map(LeaveRequest::getDays)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (unpaidLeaveDays.compareTo(BigDecimal.ZERO) > 0) {
                Money absenceAmt = dailyRate.multiply(unpaidLeaveDays);
                total = total.add(absenceAmt);
                lines.add(new PayslipLine(
                        PayslipLineCategory.ABSENCE, "LWOP", "Leave Without Pay",
                        unpaidLeaveDays, dailyRate, absenceAmt, true));
            }
        }

        int totalLateAndUtMinutes = entries.stream()
                .mapToInt(e -> e.getLateMinutes() + e.getUndertimeMinutes())
                .sum();
        if (totalLateAndUtMinutes > 0) {
            BigDecimal hours = BigDecimal.valueOf(totalLateAndUtMinutes)
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            Money lateAmt = hourlyRate.multiply(hours);
            total = total.add(lateAmt);
            lines.add(new PayslipLine(
                    PayslipLineCategory.LATE_UNDERTIME, "LATE_UT", "Late / Undertime",
                    BigDecimal.valueOf(totalLateAndUtMinutes), hourlyRate, lateAmt, true));
        }

        return total;
    }

    private Money computeOvertimeAndPremiums(
            Employee emp,
            List<TimeEntry> entries,
            Money hourlyRate,
            PremiumRateTable rates,
            List<PayslipLine> lines) {
        Money total = Money.ZERO;

        for (TimeEntry entry : entries) {
            DayType dt = entry.getDayType();
            if (dt != DayType.REGULAR && entry.getRegularHours().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal mult = rates.workMultiplier(dt);
                BigDecimal effectiveMult = emp.getPayBasis() == PayBasis.MONTHLY
                        ? mult.subtract(BigDecimal.ONE)
                        : mult;
                if (effectiveMult.compareTo(BigDecimal.ZERO) > 0) {
                    Money premium = hourlyRate.multiply(effectiveMult).multiply(entry.getRegularHours());
                    total = total.add(premium);
                    lines.add(new PayslipLine(
                            PayslipLineCategory.HOLIDAY, dt.name(), dt.name() + " Work Premium",
                            entry.getRegularHours(), hourlyRate.multiply(effectiveMult), premium, true));
                }
            }

            if (entry.getOvertimeHours().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal otMult = rates.overtimeMultiplier(dt);
                Money otAmt = hourlyRate.multiply(otMult).multiply(entry.getOvertimeHours());
                total = total.add(otAmt);
                lines.add(new PayslipLine(
                        PayslipLineCategory.OVERTIME, "OT_" + dt.name(), "Overtime (" + dt.name() + ")",
                        entry.getOvertimeHours(), hourlyRate.multiply(otMult), otAmt, true));
            }

            if (entry.getNightDiffHours().compareTo(BigDecimal.ZERO) > 0) {
                Money ndAmt = hourlyRate.multiply(rates.nightDifferential()).multiply(entry.getNightDiffHours());
                total = total.add(ndAmt);
                lines.add(new PayslipLine(
                        PayslipLineCategory.NIGHT_DIFF, "NIGHT_DIFF", "Night Differential (10%)",
                        entry.getNightDiffHours(), hourlyRate.multiply(rates.nightDifferential()), ndAmt, true));
            }
        }

        return total;
    }

    private StatutoryContribution applySchedule(StatutoryContribution fullMonthly, PayPeriod period, StatutorySchedule schedule) {
        if (period.getFrequency() == PayFrequency.MONTHLY || period.getCutoff() == CutoffType.FULL_MONTH) {
            return fullMonthly;
        }
        return switch (schedule) {
            case SPLIT -> fullMonthly.half();
            case FIRST_CUTOFF -> period.getCutoff() == CutoffType.FIRST ? fullMonthly : StatutoryContribution.ZERO;
            case SECOND_CUTOFF -> period.getCutoff() == CutoffType.SECOND ? fullMonthly : StatutoryContribution.ZERO;
        };
    }

    private void addStatutoryLines(
            List<PayslipLine> lines,
            StatutoryContribution sss,
            StatutoryContribution philHealth,
            StatutoryContribution pagIbig) {
        if (sss.employeeShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.STATUTORY, "SSS_EE", "SSS Contribution (EE)", BigDecimal.ONE, sss.employeeShare(), sss.employeeShare(), false));
        }
        if (philHealth.employeeShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.STATUTORY, "PHIC_EE", "PhilHealth Contribution (EE)", BigDecimal.ONE, philHealth.employeeShare(), philHealth.employeeShare(), false));
        }
        if (pagIbig.employeeShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.STATUTORY, "HDMF_EE", "Pag-IBIG Contribution (EE)", BigDecimal.ONE, pagIbig.employeeShare(), pagIbig.employeeShare(), false));
        }
        if (sss.employerShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.EMPLOYER_CONTRIB, "SSS_ER", "SSS Contribution (ER)", BigDecimal.ONE, sss.employerShare(), sss.employerShare(), false));
        }
        if (sss.ecShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.EMPLOYER_CONTRIB, "SSS_EC", "SSS EC (ER)", BigDecimal.ONE, sss.ecShare(), sss.ecShare(), false));
        }
        if (philHealth.employerShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.EMPLOYER_CONTRIB, "PHIC_ER", "PhilHealth Contribution (ER)", BigDecimal.ONE, philHealth.employerShare(), philHealth.employerShare(), false));
        }
        if (pagIbig.employerShare().isPositive()) {
            lines.add(new PayslipLine(PayslipLineCategory.EMPLOYER_CONTRIB, "HDMF_ER", "Pag-IBIG Contribution (ER)", BigDecimal.ONE, pagIbig.employerShare(), pagIbig.employerShare(), false));
        }
    }
}
