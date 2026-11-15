package com.payroll.domain.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payroll.domain.common.InvalidStateTransitionException;
import com.payroll.domain.common.Money;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveRequestStatus;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Test
    void leaveRequestMultiStepWorkflowTransitions() {
        LeaveRequest req = LeaveRequest.file(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2),
                new BigDecimal("2"), false, "Family trip");
        assertThat(req.getStatus()).isEqualTo(LeaveRequestStatus.PENDING);

        // Cannot skip supervisor & HR stages
        assertThatThrownBy(() -> req.verifyByHr(UUID.randomUUID(), Instant.now(), "skip"))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> req.approve(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(InvalidStateTransitionException.class);

        UUID supervisor = UUID.randomUUID();
        UUID hr = UUID.randomUUID();
        UUID officer = UUID.randomUUID();

        req.recommendBySupervisor(supervisor, Instant.now(), "Team covered");
        assertThat(req.getStatus()).isEqualTo(LeaveRequestStatus.SUPERVISOR_RECOMMENDED);
        assertThat(req.getSupervisorBy()).isEqualTo(supervisor);
        assertThat(req.getSupervisorRemarks()).isEqualTo("Team covered");

        req.verifyByHr(hr, Instant.now(), "2.0 credits verified");
        assertThat(req.getStatus()).isEqualTo(LeaveRequestStatus.HR_VERIFIED);
        assertThat(req.getHrBy()).isEqualTo(hr);
        assertThat(req.getHrRemarks()).isEqualTo("2.0 credits verified");

        req.approve(officer, Instant.now());
        assertThat(req.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(req.getApprovedBy()).isEqualTo(officer);

        req.cancel();
        assertThat(req.getStatus()).isEqualTo(LeaveRequestStatus.CANCELLED);

        LeaveRequest rejected = LeaveRequest.file(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1),
                BigDecimal.ONE, false, "Errand");
        rejected.recommendBySupervisor(supervisor, Instant.now(), "Ok");
        rejected.reject(hr, Instant.now(), "Insufficient credits");
        assertThat(rejected.getStatus()).isEqualTo(LeaveRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Insufficient credits");
    }
}
