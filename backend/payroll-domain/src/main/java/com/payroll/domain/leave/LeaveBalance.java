package com.payroll.domain.leave;

import com.payroll.domain.common.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public final class LeaveBalance {

    private final UUID id;
    private final UUID employeeId;
    private final UUID leaveTypeId;
    private final int year;
    private BigDecimal entitledDays;
    private BigDecimal usedDays;
    private long version;

    public LeaveBalance(UUID id, UUID employeeId, UUID leaveTypeId, int year, BigDecimal entitledDays, BigDecimal usedDays, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.leaveTypeId = Objects.requireNonNull(leaveTypeId, "leaveTypeId must not be null");
        this.year = year;
        this.entitledDays = entitledDays == null ? BigDecimal.ZERO : entitledDays;
        this.usedDays = usedDays == null ? BigDecimal.ZERO : usedDays;
        this.version = version;
    }

    public static LeaveBalance create(UUID employeeId, UUID leaveTypeId, int year, BigDecimal entitledDays) {
        return new LeaveBalance(UUID.randomUUID(), employeeId, leaveTypeId, year, entitledDays, BigDecimal.ZERO, 0L);
    }

    public BigDecimal remainingDays() {
        return entitledDays.subtract(usedDays);
    }

    public void deduct(BigDecimal days) {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Leave days must be positive");
        }
        if (remainingDays().compareTo(days) < 0) {
            throw new ValidationException("Insufficient leave balance. Remaining: " + remainingDays() + ", requested: " + days);
        }
        this.usedDays = this.usedDays.add(days);
    }

    public void restore(BigDecimal days) {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) return;
        this.usedDays = this.usedDays.subtract(days).max(BigDecimal.ZERO);
    }

    public void updateEntitlement(BigDecimal entitledDays) {
        if (entitledDays == null || entitledDays.compareTo(this.usedDays) < 0) {
            throw new ValidationException("Entitled days cannot be less than used days");
        }
        this.entitledDays = entitledDays;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public int getYear() { return year; }
    public BigDecimal getEntitledDays() { return entitledDays; }
    public BigDecimal getUsedDays() { return usedDays; }
    public long getVersion() { return version; }
}
