package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.application.service.AuthService;
import com.payroll.application.service.EmployeeService;
import com.payroll.application.service.LeaveService;
import com.payroll.domain.employee.Employee;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final AuthService authService;
    private final EmployeeService employeeService;

    public LeaveController(LeaveService leaveService, AuthService authService, EmployeeService employeeService) {
        this.leaveService = leaveService;
        this.authService = authService;
        this.employeeService = employeeService;
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

    public record WorkflowActionRequest(String remarks, String reason) {}

    public record LeaveRequestDto(
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
            String rejectionReason) {
        static LeaveRequestDto from(LeaveRequest r) {
            return new LeaveRequestDto(
                    r.getId(), r.getEmployeeId(), r.getLeaveTypeId(), r.getStartDate(),
                    r.getEndDate(), r.getDays(), r.isHalfDay(), r.getReason(), r.getStatus(),
                    r.getSupervisorBy(), r.getSupervisorAt(), r.getSupervisorRemarks(),
                    r.getHrBy(), r.getHrAt(), r.getHrRemarks(),
                    r.getApprovedBy(), r.getApprovedAt(), r.getRejectionReason());
        }
    }

    @GetMapping("/leave-types")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
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
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public List<LeaveBalanceDto> getBalances(
            Authentication auth,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(defaultValue = "2026") int year) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID target;
        if (p.role() == Role.EMPLOYEE) {
            target = authService.resolveEmployeeId(p.userId(), p.employeeId());
        } else if (p.role() == Role.SUPERVISOR) {
            UUID myEmpId = authService.resolveEmployeeId(p.userId(), p.employeeId());
            if (myEmpId == null) throw new AccessDeniedException("Employee profile required");
            if (employeeId == null || employeeId.equals(myEmpId)) {
                target = myEmpId;
            } else {
                Employee emp = employeeService.get(employeeId);
                if (!myEmpId.equals(emp.getSupervisorId())) {
                    throw new AccessDeniedException("Cannot view leave balances outside your team");
                }
                target = employeeId;
            }
        } else {
            target = employeeId != null ? employeeId : authService.resolveEmployeeId(p.userId(), p.employeeId());
        }
        if (target == null) throw new AccessDeniedException("Employee ID required");
        return leaveService.getBalances(target, year).stream().map(LeaveBalanceDto::from).toList();
    }

    @PutMapping("/leave-balances")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveBalanceDto setBalance(@Valid @RequestBody LeaveBalanceRequest req) {
        return LeaveBalanceDto.from(leaveService.setBalance(req.employeeId(), req.leaveTypeId(), req.year(), req.entitledDays()));
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public List<LeaveRequestDto> listRequests(
            Authentication auth,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LeaveRequestStatus status) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        if (p.role() == Role.EMPLOYEE) {
            UUID empId = authService.resolveEmployeeId(p.userId(), p.employeeId());
            if (empId == null) return List.of();
            return leaveService.listRequests(empId, status).stream().map(LeaveRequestDto::from).toList();
        }
        if (p.role() == Role.SUPERVISOR) {
            UUID myEmpId = authService.resolveEmployeeId(p.userId(), p.employeeId());
            if (myEmpId == null) return List.of();
            Set<UUID> visibleEmployees = new HashSet<>();
            visibleEmployees.add(myEmpId);
            employeeService.listBySupervisor(myEmpId).stream().map(Employee::getId).forEach(visibleEmployees::add);
            if (employeeId != null) {
                if (!visibleEmployees.contains(employeeId)) {
                    throw new AccessDeniedException("Cannot view leave requests outside your team");
                }
                return leaveService.listRequests(employeeId, status).stream().map(LeaveRequestDto::from).toList();
            }
            return leaveService.listRequests(null, status).stream()
                    .filter(r -> visibleEmployees.contains(r.getEmployeeId()))
                    .sorted(Comparator.comparing(LeaveRequest::getStartDate).reversed())
                    .map(LeaveRequestDto::from)
                    .toList();
        }
        return leaveService.listRequests(employeeId, status).stream().map(LeaveRequestDto::from).toList();
    }

    @PostMapping("/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public LeaveRequestDto fileRequest(Authentication auth, @Valid @RequestBody FileLeaveRequest req) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID empId = (p.role() == Role.EMPLOYEE || p.role() == Role.SUPERVISOR)
                ? authService.resolveEmployeeId(p.userId(), p.employeeId())
                : req.employeeId();
        if (empId == null) throw new AccessDeniedException("Employee profile required");
        return LeaveRequestDto.from(leaveService.fileRequest(
                empId, req.leaveTypeId(), req.startDate(), req.endDate(), req.days(), req.halfDay(), req.reason()));
    }

    @PostMapping("/leave-requests/{id}/recommend")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public LeaveRequestDto recommend(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowActionRequest body) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        LeaveRequest existing = leaveService.getRequest(id);
        enforceSupervisorAuthority(p, existing.getEmployeeId());
        return LeaveRequestDto.from(leaveService.recommendRequest(
                id, p.userId(), body == null ? null : body.remarks()));
    }

    @PostMapping("/leave-requests/{id}/hr-verify")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveRequestDto hrVerify(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowActionRequest body) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        LeaveRequest existing = leaveService.getRequest(id);
        enforceNotSelfUnlessAdmin(p, existing.getEmployeeId());
        return LeaveRequestDto.from(leaveService.hrVerifyRequest(
                id, p.userId(), body == null ? null : body.remarks()));
    }

    @PostMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public LeaveRequestDto approve(Authentication auth, @PathVariable UUID id) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        LeaveRequest existing = leaveService.getRequest(id);
        enforceSupervisorAuthority(p, existing.getEmployeeId());
        return LeaveRequestDto.from(leaveService.approveRequest(id, p.userId()));
    }

    @PostMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR')")
    public LeaveRequestDto reject(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowActionRequest body) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        LeaveRequest existing = leaveService.getRequest(id);
        enforceNotSelfUnlessAdmin(p, existing.getEmployeeId());
        if (p.role() == Role.SUPERVISOR) {
            if (existing.getStatus() == LeaveRequestStatus.SUPERVISOR_RECOMMENDED) {
                throw new AccessDeniedException("HR department handles review at this stage");
            }
            enforceSupervisorAuthority(p, existing.getEmployeeId());
        } else if (p.role() == Role.HR && existing.getStatus() == LeaveRequestStatus.HR_VERIFIED) {
            throw new AccessDeniedException("Authorized approving officer handles final review at this stage");
        }
        String rejectionReason = body == null ? null : (body.reason() != null ? body.reason() : body.remarks());
        return LeaveRequestDto.from(leaveService.rejectRequest(id, p.userId(), rejectionReason));
    }

    @PostMapping("/leave-requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public LeaveRequestDto cancel(Authentication auth, @PathVariable UUID id) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        if (p.role() == Role.EMPLOYEE || p.role() == Role.SUPERVISOR) {
            UUID myEmpId = authService.resolveEmployeeId(p.userId(), p.employeeId());
            LeaveRequest existing = leaveService.getRequest(id);
            if (!existing.getEmployeeId().equals(myEmpId)) {
                throw new AccessDeniedException("Cannot cancel another employee's leave request");
            }
        }
        return LeaveRequestDto.from(leaveService.cancelRequest(id));
    }

    private void enforceNotSelfUnlessAdmin(AuthPrincipal p, UUID employeeId) {
        if (p.role() == Role.ADMIN) return;
        UUID actorEmpId = authService.resolveEmployeeId(p.userId(), p.employeeId());
        if (actorEmpId != null && actorEmpId.equals(employeeId)) {
            throw new AccessDeniedException("You cannot approve or reject your own leave request");
        }
    }

    private void enforceSupervisorAuthority(AuthPrincipal p, UUID employeeId) {
        if (p.role() == Role.ADMIN) return;
        UUID supervisorEmpId = authService.resolveEmployeeId(p.userId(), p.employeeId());
        if (supervisorEmpId == null) {
            throw new AccessDeniedException("Supervisor account must be linked to an employee profile");
        }
        if (supervisorEmpId.equals(employeeId)) {
            throw new AccessDeniedException("You cannot review or approve your own leave request");
        }
        Employee emp = employeeService.get(employeeId);
        if (emp.getSupervisorId() != null && !emp.getSupervisorId().equals(supervisorEmpId)) {
            throw new AccessDeniedException("Only the employee's immediate supervisor can perform this action");
        }
    }
}
