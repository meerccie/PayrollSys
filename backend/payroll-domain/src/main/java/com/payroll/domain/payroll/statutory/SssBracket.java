package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;
import java.math.BigDecimal;

public record SssBracket(
        Money minCompensation,
        Money maxCompensation,
        Money monthlySalaryCredit,
        Money employeeShare,
        Money employerShare,
        Money ecShare) {

    public boolean matches(Money compensation) {
        boolean geMin = minCompensation == null || compensation.greaterThanOrEqual(minCompensation);
        boolean leMax = maxCompensation == null || compensation.lessThan(maxCompensation)
                || compensation.equals(maxCompensation);
        return geMin && leMax;
    }

    /**
     * Generates the standard 2025/2026 SSS bracket table (5% EE, 10% ER, MSC 5,000..35,000 in 500 steps).
     */
    public static java.util.List<SssBracket> default2026Brackets() {
        java.util.List<SssBracket> brackets = new java.util.ArrayList<>();
        // Below 5,250 -> MSC 5,000
        brackets.add(bracket(null, Money.of("5249.99"), 5000));
        // 5,250 to 34,749.99 -> MSC 5,500 to 34,500 in 500 steps
        for (int msc = 5500; msc <= 34500; msc += 500) {
            Money min = Money.of(msc - 250);
            Money max = Money.of(msc + 249.99);
            brackets.add(bracket(min, max, msc));
        }
        // 34,750 and above -> MSC 35,000
        brackets.add(bracket(Money.of("34750.00"), null, 35000));
        return java.util.List.copyOf(brackets);
    }

    private static SssBracket bracket(Money min, Money max, int mscValue) {
        Money msc = Money.of(mscValue);
        Money ee = msc.multiply(new BigDecimal("0.05"));
        Money er = msc.multiply(new BigDecimal("0.10"));
        Money ec = mscValue < 15000 ? Money.of("10.00") : Money.of("30.00");
        return new SssBracket(min, max, msc, ee, er, ec);
    }
}
