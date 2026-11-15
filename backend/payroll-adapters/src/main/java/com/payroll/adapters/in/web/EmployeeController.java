package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.application.common.PageRequest;
import com.payroll.application.common.PageResult;
import com.payroll.application.service.AuthService;
import com.payroll.application.service.EmployeeService;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.Money;
import com.payroll.domain.employee.BankAccount;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.employee.EmploymentStatus;
import com.payroll.domain.employee.PayBasis;
import com.payroll.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AuthService authService;

    public EmployeeController(EmployeeService employeeService, AuthService authService) {
        this.employeeService = employeeService;
        this.authService = authService;
    }

    public record CreateEmployeeRequest(
            @NotBlank String employeeNo,
            @NotBlank String firstName,
            String middleName,
            @NotBlank String lastName,
            LocalDate birthDate,
            @NotBlank @Email String email,
            String phone,
            String address,
            String tin,
            String sssNo,
            String philHealthNo,
            String pagIbigNo,
            @NotNull LocalDate hireDate,
            @NotNull EmploymentStatus status,
            @NotNull PayBasis payBasis,
            @NotNull @DecimalMin("0.01") BigDecimal baseRate,
            UUID departmentId,
            UUID positionId,
            UUID supervisorId,
            String bankName,
            String bankAccountNo,
            String bankAccountName,
            Set<DayOfWeek> restDays) {}

    public record UpdateEmployeeRequest(
            @NotBlank String firstName,
            String middleName,
            @NotBlank String lastName,
            LocalDate birthDate,
            @NotBlank @Email String email,
            String phone,
            String address,
            String tin,
            String sssNo,
            String philHealthNo,
            String pagIbigNo,
            @NotNull EmploymentStatus status,
            @NotNull PayBasis payBasis,
            @NotNull @DecimalMin("0.01") BigDecimal baseRate,
            UUID departmentId,
            UUID positionId,
            UUID supervisorId,
            String bankName,
            String bankAccountNo,
            String bankAccountName,
            Set<DayOfWeek> restDays) {}

    public record SeparateRequest(@NotNull LocalDate separationDate) {}
    public record ReinstateRequest(EmploymentStatus status) {}

    public record EmployeeDto(
            UUID id, String employeeNo, String firstName, String middleName, String lastName,
            String fullName, LocalDate birthDate, String email, String phone, String address,
            String tin, String sssNo, String philHealthNo, String pagIbigNo,
            LocalDate hireDate, LocalDate separationDate, EmploymentStatus status,
            PayBasis payBasis, BigDecimal baseRate, UUID departmentId, UUID positionId, UUID supervisorId,
            String bankName, String bankAccountNo, String bankAccountName,
            Set<DayOfWeek> restDays, long version) {
        static EmployeeDto from(Employee e) {
            return new EmployeeDto(
                    e.getId(), e.getEmployeeNo(), e.getFirstName(), e.getMiddleName(), e.getLastName(),
                    e.fullName(), e.getBirthDate(), e.getEmail(), e.getPhone(), e.getAddress(),
                    e.getTin(), e.getSssNo(), e.getPhilHealthNo(), e.getPagIbigNo(),
                    e.getHireDate(), e.getSeparationDate(), e.getStatus(), e.getPayBasis(),
                    e.getBaseRate().amount(), e.getDepartmentId(), e.getPositionId(), e.getSupervisorId(),
                    e.getBankAccount() == null ? null : e.getBankAccount().bankName(),
                    e.getBankAccount() == null ? null : e.getBankAccount().accountNumber(),
                    e.getBankAccount() == null ? null : e.getBankAccount().accountName(),
                    e.getRestDays(), e.getVersion());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR')")
    public PageResult<EmployeeDto> list(
            Authentication auth,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "true") boolean asc) {
        AuthPrincipal principal = AuthPrincipal.from(auth);
        if (principal.role() == Role.SUPERVISOR) {
            UUID myEmpId = authService.resolveEmployeeId(principal.userId(), principal.employeeId());
            if (myEmpId == null) {
                return new PageResult<>(List.of(), page, size, 0);
            }
            Map<UUID, Employee> teamMap = new LinkedHashMap<>();
            teamMap.put(myEmpId, employeeService.get(myEmpId));
            for (Employee report : employeeService.listBySupervisor(myEmpId)) {
                teamMap.put(report.getId(), report);
            }
            List<EmployeeDto> filtered = teamMap.values().stream()
                    .filter(e -> !activeOnly || e.getStatus() != EmploymentStatus.SEPARATED)
                    .filter(e -> status == null || e.getStatus() == status)
                    .filter(e -> departmentId == null || departmentId.equals(e.getDepartmentId()))
                    .sorted(Comparator.comparing(Employee::getEmployeeNo))
                    .map(EmployeeDto::from)
                    .toList();
            return new PageResult<>(filtered, 0, Math.max(filtered.size(), 1), filtered.size());
        }
        PageResult<Employee> result = employeeService.search(q, departmentId, status, activeOnly, new PageRequest(page, size, sort, asc));
        return new PageResult<>(result.content().stream().map(EmployeeDto::from).toList(),
                result.page(), result.size(), result.totalElements());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public EmployeeDto me(Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.from(auth);
        UUID empId = authService.resolveEmployeeId(principal.userId(), principal.employeeId());
        if (empId == null) {
            throw new EntityNotFoundException("Employee profile for user", principal.userId());
        }
        return EmployeeDto.from(employeeService.get(empId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeDto get(@PathVariable UUID id) {
        return EmployeeDto.from(employeeService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeDto create(@Valid @RequestBody CreateEmployeeRequest req) {
        BankAccount bank = req.bankName() == null && req.bankAccountNo() == null
                ? null
                : new BankAccount(req.bankName(), req.bankAccountNo(), req.bankAccountName());
        Employee created = employeeService.create(new EmployeeService.CreateEmployeeCommand(
                req.employeeNo(), req.firstName(), req.middleName(), req.lastName(),
                req.birthDate(), req.email(), req.phone(), req.address(),
                req.tin(), req.sssNo(), req.philHealthNo(), req.pagIbigNo(),
                req.hireDate(), req.status(), req.payBasis(), Money.of(req.baseRate()),
                req.departmentId(), req.positionId(), req.supervisorId(), bank, req.restDays()));
        return EmployeeDto.from(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeDto update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest req) {
        BankAccount bank = req.bankName() == null && req.bankAccountNo() == null
                ? null
                : new BankAccount(req.bankName(), req.bankAccountNo(), req.bankAccountName());
        Employee updated = employeeService.update(id, new EmployeeService.UpdateEmployeeCommand(
                req.firstName(), req.middleName(), req.lastName(),
                req.birthDate(), req.email(), req.phone(), req.address(),
                req.tin(), req.sssNo(), req.philHealthNo(), req.pagIbigNo(),
                req.status(), req.payBasis(), Money.of(req.baseRate()),
                req.departmentId(), req.positionId(), req.supervisorId(), bank, req.restDays()));
        return EmployeeDto.from(updated);
    }

    @PostMapping("/{id}/separate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeDto separate(@PathVariable UUID id, @Valid @RequestBody SeparateRequest req) {
        return EmployeeDto.from(employeeService.separate(id, req.separationDate()));
    }

    @PostMapping("/{id}/reinstate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeDto reinstate(@PathVariable UUID id, @RequestBody(required = false) ReinstateRequest req) {
        return EmployeeDto.from(employeeService.reinstate(id, req == null ? null : req.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void delete(@PathVariable UUID id) {
        employeeService.delete(id);
    }
}
