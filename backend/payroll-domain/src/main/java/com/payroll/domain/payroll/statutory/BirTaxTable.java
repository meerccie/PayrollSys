package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;
import com.payroll.domain.payroll.PayFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BirTaxTable(LocalDate effectiveDate, List<BirTaxBracket> brackets) {

    public static BirTaxTable default2026() {
        return new BirTaxTable(LocalDate.of(2023, 1, 1), List.of(
                // Monthly TRAIN brackets
                new BirTaxBracket(PayFrequency.MONTHLY, 1, Money.ZERO, Money.of("20833.00"), Money.ZERO, BigDecimal.ZERO),
                new BirTaxBracket(PayFrequency.MONTHLY, 2, Money.of("20833.00"), Money.of("33332.99"), Money.ZERO, new BigDecimal("0.15")),
                new BirTaxBracket(PayFrequency.MONTHLY, 3, Money.of("33333.00"), Money.of("66666.99"), Money.of("1875.00"), new BigDecimal("0.20")),
                new BirTaxBracket(PayFrequency.MONTHLY, 4, Money.of("66667.00"), Money.of("166666.99"), Money.of("8541.80"), new BigDecimal("0.25")),
                new BirTaxBracket(PayFrequency.MONTHLY, 5, Money.of("166667.00"), Money.of("666666.99"), Money.of("33541.80"), new BigDecimal("0.30")),
                new BirTaxBracket(PayFrequency.MONTHLY, 6, Money.of("666667.00"), null, Money.of("183541.80"), new BigDecimal("0.35")),

                // Semi-monthly TRAIN brackets
                new BirTaxBracket(PayFrequency.SEMI_MONTHLY, 1, Money.ZERO, Money.of("10417.00"), Money.ZERO, BigDecimal.ZERO),
                new BirTaxBracket(PayFrequency.SEMI_MONTHLY, 2, Money.of("10417.00"), Money.of("16666.99"), Money.ZERO, new BigDecimal("0.15")),
                new BirTaxBracket(PayFrequency.SEMI_MONTHLY, 3, Money.of("16667.00"), Money.of("33332.99"), Money.of("937.50"), new BigDecimal("0.20")),
                new BirTaxBracket(PayFrequency.SEMI_MONTHLY, 4, Money.of("33333.00"), Money.of("83332.99"), Money.of("4270.70"), new BigDecimal("0.25")),
                new BirTaxBracket(PayFrequency.SEMI_MONTHLY, 5, Money.of("83333.00"), Money.of("333332.99"), Money.of("16770.70"), new BigDecimal("0.30")),
                new BirTaxBracket(PayFrequency.SEMI_MONTHLY, 6, Money.of("333333.00"), null, Money.of("91770.70"), new BigDecimal("0.35"))
        ));
    }

    public Money compute(PayFrequency frequency, Money taxableIncome) {
        if (taxableIncome == null || !taxableIncome.isPositive()) {
            return Money.ZERO;
        }
        List<BirTaxBracket> scoped = brackets.stream()
                .filter(b -> b.frequency() == frequency)
                .toList();
        for (BirTaxBracket bracket : scoped) {
            if (bracket.matches(taxableIncome)) {
                return bracket.computeTax(taxableIncome);
            }
        }
        return scoped.get(scoped.size() - 1).computeTax(taxableIncome);
    }
}
