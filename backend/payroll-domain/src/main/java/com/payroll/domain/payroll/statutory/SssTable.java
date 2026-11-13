package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;
import java.time.LocalDate;
import java.util.List;

public record SssTable(LocalDate effectiveDate, List<SssBracket> brackets) {

    public static SssTable default2026() {
        return new SssTable(LocalDate.of(2026, 1, 1), SssBracket.default2026Brackets());
    }

    public StatutoryContribution compute(Money monthlyCompensation) {
        if (monthlyCompensation == null || !monthlyCompensation.isPositive()) {
            return StatutoryContribution.ZERO;
        }
        for (SssBracket bracket : brackets) {
            if (bracket.matches(monthlyCompensation)) {
                return new StatutoryContribution(
                        bracket.employeeShare(),
                        bracket.employerShare(),
                        bracket.ecShare());
            }
        }
        SssBracket last = brackets.get(brackets.size() - 1);
        return new StatutoryContribution(last.employeeShare(), last.employerShare(), last.ecShare());
    }
}
