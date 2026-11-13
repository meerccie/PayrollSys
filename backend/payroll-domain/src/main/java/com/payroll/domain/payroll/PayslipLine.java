package com.payroll.domain.payroll;

import com.payroll.domain.common.Money;
import java.math.BigDecimal;

public record PayslipLine(
        PayslipLineCategory category,
        String code,
        String description,
        BigDecimal quantity,
        Money rate,
        Money amount,
        boolean taxable) {
}
