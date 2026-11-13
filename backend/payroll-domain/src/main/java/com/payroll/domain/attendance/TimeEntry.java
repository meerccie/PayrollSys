package com.payroll.domain.attendance;

import com.payroll.domain.common.InvalidStateTransitionException;
import com.payroll.domain.common.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public final class TimeEntry {

    private final UUID id;
    private final UUID employeeId;
    private final LocalDate workDate;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal nightDiffHours;
    private int lateMinutes;
    private int undertimeMinutes;
    private DayType dayType;
    private TimeEntryStatus status;
    private long version;

    public TimeEntry(
            UUID id,
            UUID employeeId,
            LocalDate workDate,
            LocalTime timeIn,
            LocalTime timeOut,
            BigDecimal regularHours,
            BigDecimal overtimeHours,
            BigDecimal nightDiffHours,
            int lateMinutes,
            int undertimeMinutes,
            DayType dayType,
            TimeEntryStatus status,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.workDate = Objects.requireNonNull(workDate, "workDate must not be null");
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.regularHours = nonNegative(regularHours, "regularHours");
        this.overtimeHours = nonNegative(overtimeHours, "overtimeHours");
        this.nightDiffHours = nonNegative(nightDiffHours, "nightDiffHours");
        this.lateMinutes = nonNegativeInt(lateMinutes, "lateMinutes");
        this.undertimeMinutes = nonNegativeInt(undertimeMinutes, "undertimeMinutes");
        this.dayType = Objects.requireNonNull(dayType, "dayType must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.version = version;
    }

    public static TimeEntry create(
            UUID employeeId,
            LocalDate workDate,
            LocalTime timeIn,
            LocalTime timeOut,
            BigDecimal regularHours,
            BigDecimal overtimeHours,
            BigDecimal nightDiffHours,
            int lateMinutes,
            int undertimeMinutes,
            DayType dayType) {
        return new TimeEntry(
                UUID.randomUUID(), employeeId, workDate, timeIn, timeOut,
                regularHours, overtimeHours, nightDiffHours,
                lateMinutes, undertimeMinutes, dayType, TimeEntryStatus.PENDING, 0L);
    }

    public void update(
            LocalTime timeIn,
            LocalTime timeOut,
            BigDecimal regularHours,
            BigDecimal overtimeHours,
            BigDecimal nightDiffHours,
            int lateMinutes,
            int undertimeMinutes,
            DayType dayType) {
        if (this.status == TimeEntryStatus.APPROVED) {
            throw new InvalidStateTransitionException("Approved time entry cannot be modified");
        }
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.regularHours = nonNegative(regularHours, "regularHours");
        this.overtimeHours = nonNegative(overtimeHours, "overtimeHours");
        this.nightDiffHours = nonNegative(nightDiffHours, "nightDiffHours");
        this.lateMinutes = nonNegativeInt(lateMinutes, "lateMinutes");
        this.undertimeMinutes = nonNegativeInt(undertimeMinutes, "undertimeMinutes");
        this.dayType = Objects.requireNonNull(dayType, "dayType must not be null");
    }

    public void approve() {
        this.status = TimeEntryStatus.APPROVED;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null) return BigDecimal.ZERO;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(field + " cannot be negative");
        }
        return value;
    }

    private static int nonNegativeInt(int value, String field) {
        if (value < 0) {
            throw new ValidationException(field + " cannot be negative");
        }
        return value;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getTimeIn() { return timeIn; }
    public LocalTime getTimeOut() { return timeOut; }
    public BigDecimal getRegularHours() { return regularHours; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public BigDecimal getNightDiffHours() { return nightDiffHours; }
    public int getLateMinutes() { return lateMinutes; }
    public int getUndertimeMinutes() { return undertimeMinutes; }
    public DayType getDayType() { return dayType; }
    public TimeEntryStatus getStatus() { return status; }
    public long getVersion() { return version; }
}
