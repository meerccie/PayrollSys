package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PhilHealthTable(
        LocalDate effectiveDate,
        BigDecimal totalRate,
        Money salaryFloor,
        Money salaryCeiling) {

    public static PhilHealthTable default2026() {
        return new PhilHealthTable(
                LocalDate.of(2026, 1, 1),
                new BigDecimal("0.05"),
                Money.of("10000.00"),
                Money.of("100000.00"));
    }

    public StatutoryContribution compute(Money monthlyBasic) {
        if (monthlyBasic == null || !monthlyBasic.isPositive()) {
            return StatutoryContribution.ZERO;
        }
        Money basis = monthlyBasic.max(salaryFloor).min(salaryCeiling);
        Money total = basis.multiply(totalRate);
        Money ee = total.divide(2);
        Money er = total.subtract(ee);
        return new StatutoryContribution(ee, er, Money.ZERO);
    }
}
