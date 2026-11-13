package com.payroll.domain.payroll;

import com.payroll.domain.common.InvalidStateTransitionException;
import com.payroll.domain.common.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PayrollRun {

    private final UUID id;
    private final UUID payPeriodId;
    private PayrollRunStatus status;
    private int employeeCount;
    private Money totalGrossPay;
    private Money totalDeductions;
    private Money totalNetPay;
    private Money totalEmployerContributions;
    private Instant computedAt;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant paidAt;
    private String notes;
    private long version;

    public PayrollRun(
            UUID id,
            UUID payPeriodId,
            PayrollRunStatus status,
            int employeeCount,
            Money totalGrossPay,
            Money totalDeductions,
            Money totalNetPay,
            Money totalEmployerContributions,
            Instant computedAt,
            UUID approvedBy,
            Instant approvedAt,
            Instant paidAt,
            String notes,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.payPeriodId = Objects.requireNonNull(payPeriodId, "payPeriodId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.employeeCount = employeeCount;
        this.totalGrossPay = totalGrossPay == null ? Money.ZERO : totalGrossPay;
        this.totalDeductions = totalDeductions == null ? Money.ZERO : totalDeductions;
        this.totalNetPay = totalNetPay == null ? Money.ZERO : totalNetPay;
        this.totalEmployerContributions = totalEmployerContributions == null ? Money.ZERO : totalEmployerContributions;
        this.computedAt = computedAt;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.paidAt = paidAt;
        this.notes = notes;
        this.version = version;
    }

    public static PayrollRun createDraft(UUID payPeriodId, String notes) {
        return new PayrollRun(
                UUID.randomUUID(), payPeriodId, PayrollRunStatus.DRAFT,
                0, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO,
                null, null, null, null, notes, 0L);
    }

    public void markComputed(
            int employeeCount,
            Money totalGrossPay,
            Money totalDeductions,
            Money totalNetPay,
            Money totalEmployerContributions,
            Instant now) {
        if (status != PayrollRunStatus.DRAFT && status != PayrollRunStatus.COMPUTED) {
            throw new InvalidStateTransitionException("Cannot compute payroll run in status " + status);
        }
        this.status = PayrollRunStatus.COMPUTED;
        this.employeeCount = employeeCount;
        this.totalGrossPay = totalGrossPay;
        this.totalDeductions = totalDeductions;
        this.totalNetPay = totalNetPay;
        this.totalEmployerContributions = totalEmployerContributions;
        this.computedAt = Objects.requireNonNull(now);
    }

    public void resetToDraft() {
        if (status != PayrollRunStatus.COMPUTED) {
            throw new InvalidStateTransitionException("Only COMPUTED payroll runs can be reset to DRAFT");
        }
        this.status = PayrollRunStatus.DRAFT;
        this.employeeCount = 0;
        this.totalGrossPay = Money.ZERO;
        this.totalDeductions = Money.ZERO;
        this.totalNetPay = Money.ZERO;
        this.totalEmployerContributions = Money.ZERO;
        this.computedAt = null;
    }

    public void approve(UUID approverId, Instant now) {
        if (status != PayrollRunStatus.COMPUTED) {
            throw new InvalidStateTransitionException("Only COMPUTED payroll runs can be approved");
        }
        this.status = PayrollRunStatus.APPROVED;
        this.approvedBy = Objects.requireNonNull(approverId);
        this.approvedAt = Objects.requireNonNull(now);
    }

    public void reopen() {
        if (status != PayrollRunStatus.APPROVED) {
            throw new InvalidStateTransitionException("Only APPROVED payroll runs can be reopened");
        }
        this.status = PayrollRunStatus.COMPUTED;
        this.approvedBy = null;
        this.approvedAt = null;
    }

    public void markPaid(Instant now) {
        if (status != PayrollRunStatus.APPROVED) {
            throw new InvalidStateTransitionException("Only APPROVED payroll runs can be marked as PAID");
        }
        this.status = PayrollRunStatus.PAID;
        this.paidAt = Objects.requireNonNull(now);
    }

    public boolean isLocked() {
        return status == PayrollRunStatus.APPROVED || status == PayrollRunStatus.PAID;
    }

    public UUID getId() { return id; }
    public UUID getPayPeriodId() { return payPeriodId; }
    public PayrollRunStatus getStatus() { return status; }
    public int getEmployeeCount() { return employeeCount; }
    public Money getTotalGrossPay() { return totalGrossPay; }
    public Money getTotalDeductions() { return totalDeductions; }
    public Money getTotalNetPay() { return totalNetPay; }
    public Money getTotalEmployerContributions() { return totalEmployerContributions; }
    public Instant getComputedAt() { return computedAt; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getPaidAt() { return paidAt; }
    public String getNotes() { return notes; }
    public long getVersion() { return version; }
}
