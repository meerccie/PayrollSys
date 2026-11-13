package com.payroll.domain.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payroll.domain.common.InvalidStateTransitionException;
import com.payroll.domain.common.Money;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatutoryAndStateMachineTest {

    @Test
    void sssFloorAndCeiling() {
        SssTable sss = SssTable.default2026();
        assertThat(sss.compute(Money.of("3000.00")).employeeShare()).isEqualTo(Money.of("250.00"));
        assertThat(sss.compute(Money.of("3000.00")).ecShare()).isEqualTo(Money.of("10.00"));
        assertThat(sss.compute(Money.of("50000.00")).employeeShare()).isEqualTo(Money.of("1750.00"));
        assertThat(sss.compute(Money.of("50000.00")).employerShare()).isEqualTo(Money.of("3500.00"));
        assertThat(sss.compute(Money.of("50000.00")).ecShare()).isEqualTo(Money.of("30.00"));
    }

    @Test
    void philHealthFloorAndCeiling() {
        PhilHealthTable ph = PhilHealthTable.default2026();
        assertThat(ph.compute(Money.of("8000.00")).employeeShare()).isEqualTo(Money.of("250.00"));
        assertThat(ph.compute(Money.of("150000.00")).employeeShare()).isEqualTo(Money.of("2500.00"));
    }

    @Test
    void pagIbigLowThresholdAndCap() {
        PagIbigTable hd = PagIbigTable.default2026();
        assertThat(hd.compute(Money.of("1500.00")).employeeShare()).isEqualTo(Money.of("15.00"));
        assertThat(hd.compute(Money.of("50000.00")).employeeShare()).isEqualTo(Money.of("200.00"));
        assertThat(hd.compute(Money.of("50000.00")).employerShare()).isEqualTo(Money.of("200.00"));
    }

    @Test
    void birTaxExemptThreshold() {
        BirTaxTable bir = BirTaxTable.default2026();
        assertThat(bir.compute(PayFrequency.MONTHLY, Money.of("20833.00"))).isEqualTo(Money.ZERO);
        assertThat(bir.compute(PayFrequency.SEMI_MONTHLY, Money.of("10417.00"))).isEqualTo(Money.ZERO);
    }

    @Test
    void payrollRunStateTransitions() {
        PayrollRun run = PayrollRun.createDraft(UUID.randomUUID(), "Test");
        assertThatThrownBy(() -> run.approve(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(InvalidStateTransitionException.class);

        run.markComputed(1, Money.of("30000"), Money.of("3457.55"), Money.of("26542.45"), Money.of("3980"), Instant.now());
        run.approve(UUID.randomUUID(), Instant.now());
        assertThat(run.isLocked()).isTrue();
        run.markPaid(Instant.now());
        assertThat(run.getStatus()).isEqualTo(PayrollRunStatus.PAID);
        assertThatThrownBy(run::reopen).isInstanceOf(InvalidStateTransitionException.class);
    }
}
