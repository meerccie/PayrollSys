package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;
import com.payroll.domain.payroll.PayFrequency;
import java.math.BigDecimal;

public record BirTaxBracket(
        PayFrequency frequency,
        int bracketOrder,
        Money minTaxable,
        Money maxTaxable,
        Money fixedTax,
        BigDecimal excessRate) {

    public boolean matches(Money taxableIncome) {
        boolean geMin = minTaxable == null || taxableIncome.greaterThanOrEqual(minTaxable);
        boolean leMax = maxTaxable == null || taxableIncome.compareTo(maxTaxable) <= 0;
        return geMin && leMax;
    }

    public Money computeTax(Money taxableIncome) {
        if (excessRate.compareTo(BigDecimal.ZERO) == 0) {
            return fixedTax;
        }
        Money base = minTaxable == null ? Money.ZERO : minTaxable;
        Money excess = taxableIncome.subtract(base).max(Money.ZERO);
        return fixedTax.add(excess.multiply(excessRate));
    }
}
