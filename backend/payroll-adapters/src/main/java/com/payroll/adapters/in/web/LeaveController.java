package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.application.service.LeaveService;
import com.payroll.domain.leave.LeaveBalance;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveRequestStatus;
import com.payroll.domain.leave.LeaveType;
import com.payroll.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    public record LeaveTypeRequest(@NotBlank String code, @NotBlank String name, boolean paid, @NotNull BigDecimal annualEntitlement, boolean active) {}
    public record LeaveTypeDto(UUID id, String code, String name, boolean paid, BigDecimal annualEntitlement, boolean active) {
        static LeaveTypeDto from(LeaveType lt) {
            return new LeaveTypeDto(lt.getId(), lt.getCode(), lt.getName(), lt.isPaid(), lt.getAnnualEntitlement(), lt.isActive());
        }
    }

    public record LeaveBalanceRequest(@NotNull UUID employeeId, @NotNull UUID leaveTypeId, int year, @NotNull BigDecimal entitledDays) {}
    public record LeaveBalanceDto(UUID id, UUID employeeId, UUID leaveTypeId, int year, BigDecimal entitledDays, BigDecimal usedDays, BigDecimal remainingDays) {
        static LeaveBalanceDto from(LeaveBalance b) {
            return new LeaveBalanceDto(b.getId(), b.getEmployeeId(), b.getLeaveTypeId(), b.getYear(), b.getEntitledDays(), b.getUsedDays(), b.remainingDays());
        }
    }

    public record FileLeaveRequest(
            UUID employeeId,
            @NotNull UUID leaveTypeId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull @DecimalMin("0.5") BigDecimal days,
            boolean halfDay,
            String reason) {}

    public record LeaveRequestDto(
            UUID id, UUID employeeId, UUID leaveTypeId, LocalDate startDate, LocalDate endDate,
            BigDecimal days, boolean halfDay, String reason, LeaveRequestStatus status,
            UUID approvedBy, Instant approvedAt) {
        static LeaveRequestDto from(LeaveRequest r) {
            return new LeaveRequestDto(r.getId(), r.getEmployeeId(), r.getLeaveTypeId(), r.getStartDate(),
                    r.getEndDate(), r.getDays(), r.isHalfDay(), r.getReason(), r.getStatus(),
                    r.getApprovedBy(), r.getApprovedAt());
        }
    }

    @GetMapping("/leave-types")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<LeaveTypeDto> listTypes() {
        return leaveService.listLeaveTypes().stream().map(LeaveTypeDto::from).toList();
    }

    @PostMapping("/leave-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveTypeDto createType(@Valid @RequestBody LeaveTypeRequest req) {
        return LeaveTypeDto.from(leaveService.createLeaveType(req.code(), req.name(), req.paid(), req.annualEntitlement()));
    }

    @PutMapping("/leave-types/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveTypeDto updateType(@PathVariable UUID id, @Valid @RequestBody LeaveTypeRequest req) {
        return LeaveTypeDto.from(leaveService.updateLeaveType(id, req.code(), req.name(), req.paid(), req.annualEntitlement(), req.active()));
    }

    @GetMapping("/leave-balances")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<LeaveBalanceDto> getBalances(
            Authentication auth,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(defaultValue = "2026") int year) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID target = p.role() == Role.EMPLOYEE ? p.employeeId() : employeeId;
        if (target == null) throw new AccessDeniedException("Employee ID required");
        return leaveService.getBalances(target, year).stream().map(LeaveBalanceDto::from).toList();
    }

    @PutMapping("/leave-balances")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveBalanceDto setBalance(@Valid @RequestBody LeaveBalanceRequest req) {
        return LeaveBalanceDto.from(leaveService.setBalance(req.employeeId(), req.leaveTypeId(), req.year(), req.entitledDays()));
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<LeaveRequestDto> listRequests(
            Authentication auth,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LeaveRequestStatus status) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID target = p.role() == Role.EMPLOYEE ? p.employeeId() : employeeId;
        return leaveService.listRequests(target, status).stream().map(LeaveRequestDto::from).toList();
    }

    @PostMapping("/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public LeaveRequestDto fileRequest(Authentication auth, @Valid @RequestBody FileLeaveRequest req) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID empId = p.role() == Role.EMPLOYEE ? p.employeeId() : req.employeeId();
        if (empId == null) throw new AccessDeniedException("Employee profile required");
        return LeaveRequestDto.from(leaveService.fileRequest(
                empId, req.leaveTypeId(), req.startDate(), req.endDate(), req.days(), req.halfDay(), req.reason()));
    }

    @PostMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveRequestDto approve(Authentication auth, @PathVariable UUID id) {
        return LeaveRequestDto.from(leaveService.approveRequest(id, AuthPrincipal.from(auth).userId()));
    }

    @PostMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveRequestDto reject(Authentication auth, @PathVariable UUID id) {
        return LeaveRequestDto.from(leaveService.rejectRequest(id, AuthPrincipal.from(auth).userId()));
    }

    @PostMapping("/leave-requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public LeaveRequestDto cancel(@PathVariable UUID id) {
        return LeaveRequestDto.from(leaveService.cancelRequest(id));
    }
}
