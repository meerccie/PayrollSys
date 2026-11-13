package com.payroll.domain.compensation;

import com.payroll.domain.common.Money;
import com.payroll.domain.common.ValidationException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class EmployeeEarning {

    private final UUID id;
    private final UUID employeeId;
    private final UUID earningTypeId;
    private Money amount;
    private boolean recurring;
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;
    private UUID payPeriodId;
    private boolean active;
    private long version;

    public EmployeeEarning(
            UUID id,
            UUID employeeId,
            UUID earningTypeId,
            Money amount,
            boolean recurring,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            UUID payPeriodId,
            boolean active,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.earningTypeId = Objects.requireNonNull(earningTypeId, "earningTypeId must not be null");
        if (amount == null || !amount.isPositive()) {
            throw new ValidationException("Earning amount must be positive");
        }
        if (recurring && effectiveStart == null) {
            throw new ValidationException("Recurring earning requires effectiveStart");
        }
        if (!recurring && payPeriodId == null) {
            throw new ValidationException("One-off earning requires payPeriodId");
        }
        if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            throw new ValidationException("effectiveEnd cannot be before effectiveStart");
        }
        this.amount = amount;
        this.recurring = recurring;
        this.effectiveStart = effectiveStart;
        this.effectiveEnd = effectiveEnd;
        this.payPeriodId = payPeriodId;
        this.active = active;
        this.version = version;
    }

    public static EmployeeEarning create(
            UUID employeeId,
            UUID earningTypeId,
            Money amount,
            boolean recurring,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            UUID payPeriodId) {
        return new EmployeeEarning(
                UUID.randomUUID(), employeeId, earningTypeId, amount, recurring,
                effectiveStart, effectiveEnd, payPeriodId, true, 0L);
    }

    public boolean appliesTo(UUID periodId, LocalDate periodStart, LocalDate periodEnd) {
        if (!active) return false;
        if (!recurring) {
            return periodId.equals(payPeriodId);
        }
        boolean started = !effectiveStart.isAfter(periodEnd);
        boolean notEnded = effectiveEnd == null || !effectiveEnd.isBefore(periodStart);
        return started && notEnded;
    }

    public void update(Money amount, LocalDate effectiveStart, LocalDate effectiveEnd, boolean active) {
        if (amount == null || !amount.isPositive()) {
            throw new ValidationException("Earning amount must be positive");
        }
        if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            throw new ValidationException("effectiveEnd cannot be before effectiveStart");
        }
        this.amount = amount;
        this.effectiveStart = effectiveStart;
        this.effectiveEnd = effectiveEnd;
        this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getEarningTypeId() { return earningTypeId; }
    public Money getAmount() { return amount; }
    public boolean isRecurring() { return recurring; }
    public LocalDate getEffectiveStart() { return effectiveStart; }
    public LocalDate getEffectiveEnd() { return effectiveEnd; }
    public UUID getPayPeriodId() { return payPeriodId; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
