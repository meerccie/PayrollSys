package com.payroll.domain.leave;

import com.payroll.domain.common.InvalidStateTransitionException;
import com.payroll.domain.common.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class LeaveRequest {

    private final UUID id;
    private final UUID employeeId;
    private final UUID leaveTypeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal days;
    private final boolean halfDay;
    private final String reason;
    private LeaveRequestStatus status;
    private UUID approvedBy;
    private Instant approvedAt;
    private long version;

    public LeaveRequest(
            UUID id,
            UUID employeeId,
            UUID leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal days,
            boolean halfDay,
            String reason,
            LeaveRequestStatus status,
            UUID approvedBy,
            Instant approvedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.leaveTypeId = Objects.requireNonNull(leaveTypeId, "leaveTypeId must not be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date");
        }
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Leave days must be positive");
        }
        this.days = days;
        this.halfDay = halfDay;
        this.reason = reason;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.version = version;
    }

    public static LeaveRequest file(
            UUID employeeId,
            UUID leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal days,
            boolean halfDay,
            String reason) {
        return new LeaveRequest(
                UUID.randomUUID(), employeeId, leaveTypeId, startDate, endDate,
                days, halfDay, reason, LeaveRequestStatus.PENDING, null, null, 0L);
    }

    public void approve(UUID approverId, Instant now) {
        if (status != LeaveRequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only PENDING leave requests can be approved");
        }
        this.status = LeaveRequestStatus.APPROVED;
        this.approvedBy = Objects.requireNonNull(approverId);
        this.approvedAt = Objects.requireNonNull(now);
    }

    public void reject(UUID approverId, Instant now) {
        if (status != LeaveRequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only PENDING leave requests can be rejected");
        }
        this.status = LeaveRequestStatus.REJECTED;
        this.approvedBy = Objects.requireNonNull(approverId);
        this.approvedAt = Objects.requireNonNull(now);
    }

    public void cancel() {
        if (status != LeaveRequestStatus.PENDING && status != LeaveRequestStatus.APPROVED) {
            throw new InvalidStateTransitionException("Only PENDING or APPROVED leave requests can be cancelled");
        }
        this.status = LeaveRequestStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getDays() { return days; }
    public boolean isHalfDay() { return halfDay; }
    public String getReason() { return reason; }
    public LeaveRequestStatus getStatus() { return status; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public long getVersion() { return version; }
}
