package com.payroll.domain.payroll.statutory;

import com.payroll.domain.common.Money;

public record StatutoryContribution(Money employeeShare, Money employerShare, Money ecShare) {
    public static final StatutoryContribution ZERO = new StatutoryContribution(Money.ZERO, Money.ZERO, Money.ZERO);

    public StatutoryContribution half() {
        return new StatutoryContribution(
                employeeShare.divide(2),
                employerShare.divide(2),
                ecShare.divide(2));
    }
}
