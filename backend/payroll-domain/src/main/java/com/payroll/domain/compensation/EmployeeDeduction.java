package com.payroll.domain.compensation;

import com.payroll.domain.common.Money;
import com.payroll.domain.common.ValidationException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class EmployeeDeduction {

    private final UUID id;
    private final UUID employeeId;
    private final UUID deductionTypeId;
    private Money amount;
    private boolean recurring;
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;
    private UUID payPeriodId;
    private Money totalLoanAmount;
    private Money remainingBalance;
    private boolean active;
    private long version;

    public EmployeeDeduction(
            UUID id,
            UUID employeeId,
            UUID deductionTypeId,
            Money amount,
            boolean recurring,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            UUID payPeriodId,
            Money totalLoanAmount,
            Money remainingBalance,
            boolean active,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.deductionTypeId = Objects.requireNonNull(deductionTypeId, "deductionTypeId must not be null");
        if (amount == null || !amount.isPositive()) {
            throw new ValidationException("Deduction amount must be positive");
        }
        if (recurring && effectiveStart == null) {
            throw new ValidationException("Recurring deduction requires effectiveStart");
        }
        if (!recurring && payPeriodId == null) {
            throw new ValidationException("One-off deduction requires payPeriodId");
        }
        if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            throw new ValidationException("effectiveEnd cannot be before effectiveStart");
        }
        this.amount = amount;
        this.recurring = recurring;
        this.effectiveStart = effectiveStart;
        this.effectiveEnd = effectiveEnd;
        this.payPeriodId = payPeriodId;
        this.totalLoanAmount = totalLoanAmount;
        this.remainingBalance = remainingBalance;
        this.active = active;
        this.version = version;
    }

    public static EmployeeDeduction create(
            UUID employeeId,
            UUID deductionTypeId,
            Money amount,
            boolean recurring,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            UUID payPeriodId,
            Money totalLoanAmount) {
        return new EmployeeDeduction(
                UUID.randomUUID(), employeeId, deductionTypeId, amount, recurring,
                effectiveStart, effectiveEnd, payPeriodId, totalLoanAmount, totalLoanAmount, true, 0L);
    }

    public boolean appliesTo(UUID periodId, LocalDate periodStart, LocalDate periodEnd) {
        if (!active) return false;
        if (remainingBalance != null && !remainingBalance.isPositive()) {
            return false;
        }
        if (!recurring) {
            return periodId.equals(payPeriodId);
        }
        boolean started = !effectiveStart.isAfter(periodEnd);
        boolean notEnded = effectiveEnd == null || !effectiveEnd.isBefore(periodStart);
        return started && notEnded;
    }

    public Money effectiveAmountForPeriod() {
        if (remainingBalance != null) {
            return amount.min(remainingBalance);
        }
        return amount;
    }

    public void recordPayment(Money paid) {
        if (remainingBalance == null) return;
        this.remainingBalance = this.remainingBalance.subtract(paid).max(Money.ZERO);
        if (this.remainingBalance.isZero()) {
            this.active = false;
        }
    }

    public void update(Money amount, LocalDate effectiveStart, LocalDate effectiveEnd, boolean active) {
        if (amount == null || !amount.isPositive()) {
            throw new ValidationException("Deduction amount must be positive");
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
    public UUID getDeductionTypeId() { return deductionTypeId; }
    public Money getAmount() { return amount; }
    public boolean isRecurring() { return recurring; }
    public LocalDate getEffectiveStart() { return effectiveStart; }
    public LocalDate getEffectiveEnd() { return effectiveEnd; }
    public UUID getPayPeriodId() { return payPeriodId; }
    public Money getTotalLoanAmount() { return totalLoanAmount; }
    public Money getRemainingBalance() { return remainingBalance; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
