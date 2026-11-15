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
    private UUID supervisorBy;
    private Instant supervisorAt;
    private String supervisorRemarks;
    private UUID hrBy;
    private Instant hrAt;
    private String hrRemarks;
    private UUID approvedBy;
    private Instant approvedAt;
    private String rejectionReason;
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
        this(id, employeeId, leaveTypeId, startDate, endDate, days, halfDay, reason, status,
                null, null, null, null, null, null, approvedBy, approvedAt, null, version);
    }

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
            UUID supervisorBy,
            Instant supervisorAt,
            String supervisorRemarks,
            UUID hrBy,
            Instant hrAt,
            String hrRemarks,
            UUID approvedBy,
            Instant approvedAt,
            String rejectionReason,
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
        this.supervisorBy = supervisorBy;
        this.supervisorAt = supervisorAt;
        this.supervisorRemarks = supervisorRemarks;
        this.hrBy = hrBy;
        this.hrAt = hrAt;
        this.hrRemarks = hrRemarks;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
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

    public void recommendBySupervisor(UUID supervisorUserId, Instant now, String remarks) {
        if (status != LeaveRequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only PENDING leave requests can be recommended by supervisor");
        }
        this.status = LeaveRequestStatus.SUPERVISOR_RECOMMENDED;
        this.supervisorBy = Objects.requireNonNull(supervisorUserId, "supervisorUserId must not be null");
        this.supervisorAt = Objects.requireNonNull(now, "now must not be null");
        this.supervisorRemarks = trimOrNull(remarks);
    }

    public void verifyByHr(UUID hrUserId, Instant now, String remarks) {
        if (status != LeaveRequestStatus.SUPERVISOR_RECOMMENDED) {
            throw new InvalidStateTransitionException(
                    "Only SUPERVISOR_RECOMMENDED leave requests can be verified by HR");
        }
        this.status = LeaveRequestStatus.HR_VERIFIED;
        this.hrBy = Objects.requireNonNull(hrUserId, "hrUserId must not be null");
        this.hrAt = Objects.requireNonNull(now, "now must not be null");
        this.hrRemarks = trimOrNull(remarks);
    }

    public void approve(UUID approverId, Instant now) {
        if (status != LeaveRequestStatus.HR_VERIFIED) {
            throw new InvalidStateTransitionException(
                    "Only HR_VERIFIED leave requests can receive final approval");
        }
        this.status = LeaveRequestStatus.APPROVED;
        this.approvedBy = Objects.requireNonNull(approverId, "approverId must not be null");
        this.approvedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void reject(UUID approverId, Instant now) {
        reject(approverId, now, null);
    }

    public void reject(UUID approverId, Instant now, String rejectionReason) {
        if (status != LeaveRequestStatus.PENDING
                && status != LeaveRequestStatus.SUPERVISOR_RECOMMENDED
                && status != LeaveRequestStatus.HR_VERIFIED) {
            throw new InvalidStateTransitionException("Only in-review leave requests can be rejected");
        }
        this.status = LeaveRequestStatus.REJECTED;
        this.approvedBy = Objects.requireNonNull(approverId, "approverId must not be null");
        this.approvedAt = Objects.requireNonNull(now, "now must not be null");
        this.rejectionReason = trimOrNull(rejectionReason);
    }

    public void cancel() {
        if (status != LeaveRequestStatus.PENDING
                && status != LeaveRequestStatus.SUPERVISOR_RECOMMENDED
                && status != LeaveRequestStatus.HR_VERIFIED
                && status != LeaveRequestStatus.APPROVED) {
            throw new InvalidStateTransitionException("Only active or approved leave requests can be cancelled");
        }
        this.status = LeaveRequestStatus.CANCELLED;
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
    public UUID getSupervisorBy() { return supervisorBy; }
    public Instant getSupervisorAt() { return supervisorAt; }
    public String getSupervisorRemarks() { return supervisorRemarks; }
    public UUID getHrBy() { return hrBy; }
    public Instant getHrAt() { return hrAt; }
    public String getHrRemarks() { return hrRemarks; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public long getVersion() { return version; }
}
