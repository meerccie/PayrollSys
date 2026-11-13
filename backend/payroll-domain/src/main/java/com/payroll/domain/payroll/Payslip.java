package com.payroll.domain.payroll;

import com.payroll.domain.common.Money;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Payslip {

    private final UUID id;
    private final UUID payrollRunId;
    private final UUID employeeId;
    private final UUID payPeriodId;
    private final String employeeNo;
    private final String employeeName;
    private final String departmentName;
    private final String positionName;
    private final Money basicPay;
    private final Money totalEarnings;
    private final Money totalOvertimeAndPremiums;
    private final Money totalAbsencesAndLate;
    private final Money grossPay;
    private final Money taxableIncome;
    private final Money sssEmployee;
    private final Money philHealthEmployee;
    private final Money pagIbigEmployee;
    private final Money withholdingTax;
    private final Money otherDeductions;
    private final Money totalDeductions;
    private final Money netPay;
    private final Money sssEmployer;
    private final Money sssEcEmployer;
    private final Money philHealthEmployer;
    private final Money pagIbigEmployer;
    private final boolean hasNetPayWarning;
    private final List<PayslipLine> lines;
    private final Instant generatedAt;

    public Payslip(
            UUID id,
            UUID payrollRunId,
            UUID employeeId,
            UUID payPeriodId,
            String employeeNo,
            String employeeName,
            String departmentName,
            String positionName,
            Money basicPay,
            Money totalEarnings,
            Money totalOvertimeAndPremiums,
            Money totalAbsencesAndLate,
            Money grossPay,
            Money taxableIncome,
            Money sssEmployee,
            Money philHealthEmployee,
            Money pagIbigEmployee,
            Money withholdingTax,
            Money otherDeductions,
            Money totalDeductions,
            Money netPay,
            Money sssEmployer,
            Money sssEcEmployer,
            Money philHealthEmployer,
            Money pagIbigEmployer,
            boolean hasNetPayWarning,
            List<PayslipLine> lines,
            Instant generatedAt) {
        this.id = Objects.requireNonNull(id);
        this.payrollRunId = Objects.requireNonNull(payrollRunId);
        this.employeeId = Objects.requireNonNull(employeeId);
        this.payPeriodId = Objects.requireNonNull(payPeriodId);
        this.employeeNo = employeeNo;
        this.employeeName = employeeName;
        this.departmentName = departmentName;
        this.positionName = positionName;
        this.basicPay = basicPay;
        this.totalEarnings = totalEarnings;
        this.totalOvertimeAndPremiums = totalOvertimeAndPremiums;
        this.totalAbsencesAndLate = totalAbsencesAndLate;
        this.grossPay = grossPay;
        this.taxableIncome = taxableIncome;
        this.sssEmployee = sssEmployee;
        this.philHealthEmployee = philHealthEmployee;
        this.pagIbigEmployee = pagIbigEmployee;
        this.withholdingTax = withholdingTax;
        this.otherDeductions = otherDeductions;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
        this.sssEmployer = sssEmployer;
        this.sssEcEmployer = sssEcEmployer;
        this.philHealthEmployer = philHealthEmployer;
        this.pagIbigEmployer = pagIbigEmployer;
        this.hasNetPayWarning = hasNetPayWarning;
        this.lines = lines == null ? List.of() : List.copyOf(lines);
        this.generatedAt = generatedAt;
    }

    public Money totalEmployerContributions() {
        return sssEmployer.add(sssEcEmployer).add(philHealthEmployer).add(pagIbigEmployer);
    }

    public UUID getId() { return id; }
    public UUID getPayrollRunId() { return payrollRunId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getPayPeriodId() { return payPeriodId; }
    public String getEmployeeNo() { return employeeNo; }
    public String getEmployeeName() { return employeeName; }
    public String getDepartmentName() { return departmentName; }
    public String getPositionName() { return positionName; }
    public Money getBasicPay() { return basicPay; }
    public Money getTotalEarnings() { return totalEarnings; }
    public Money getTotalOvertimeAndPremiums() { return totalOvertimeAndPremiums; }
    public Money getTotalAbsencesAndLate() { return totalAbsencesAndLate; }
    public Money getGrossPay() { return grossPay; }
    public Money getTaxableIncome() { return taxableIncome; }
    public Money getSssEmployee() { return sssEmployee; }
    public Money getPhilHealthEmployee() { return philHealthEmployee; }
    public Money getPagIbigEmployee() { return pagIbigEmployee; }
    public Money getWithholdingTax() { return withholdingTax; }
    public Money getOtherDeductions() { return otherDeductions; }
    public Money getTotalDeductions() { return totalDeductions; }
    public Money getNetPay() { return netPay; }
    public Money getSssEmployer() { return sssEmployer; }
    public Money getSssEcEmployer() { return sssEcEmployer; }
    public Money getPhilHealthEmployer() { return philHealthEmployer; }
    public Money getPagIbigEmployer() { return pagIbigEmployer; }
    public boolean isHasNetPayWarning() { return hasNetPayWarning; }
    public List<PayslipLine> getLines() { return lines; }
    public Instant getGeneratedAt() { return generatedAt; }
}
