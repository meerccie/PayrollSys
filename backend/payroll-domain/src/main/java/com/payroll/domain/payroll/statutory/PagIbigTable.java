package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PagIbigTable(
        LocalDate effectiveDate,
        Money lowThreshold,
        BigDecimal lowEmployeeRate,
        BigDecimal standardEmployeeRate,
        BigDecimal employerRate,
        Money maxFundSalary) {

    public static PagIbigTable default2026() {
        return new PagIbigTable(
                LocalDate.of(2026, 1, 1),
                Money.of("1500.00"),
                new BigDecimal("0.01"),
                new BigDecimal("0.02"),
                new BigDecimal("0.02"),
                Money.of("10000.00"));
    }

    public StatutoryContribution compute(Money monthlyCompensation) {
        if (monthlyCompensation == null || !monthlyCompensation.isPositive()) {
            return StatutoryContribution.ZERO;
        }
        Money basis = monthlyCompensation.min(maxFundSalary);
        BigDecimal eeRate = monthlyCompensation.compareTo(lowThreshold) <= 0
                ? lowEmployeeRate
                : standardEmployeeRate;
        Money ee = basis.multiply(eeRate);
        Money er = basis.multiply(employerRate);
        return new StatutoryContribution(ee, er, Money.ZERO);
    }
}
