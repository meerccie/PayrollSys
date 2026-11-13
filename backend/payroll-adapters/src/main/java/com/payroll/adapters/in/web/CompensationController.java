package com.payroll.adapters.in.web;

import com.payroll.application.service.CompensationService;
import com.payroll.domain.common.Money;
import com.payroll.domain.compensation.DeductionType;
import com.payroll.domain.compensation.EarningType;
import com.payroll.domain.compensation.EmployeeDeduction;
import com.payroll.domain.compensation.EmployeeEarning;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN','HR')")
public class CompensationController {

    private final CompensationService service;

    public CompensationController(CompensationService service) {
        this.service = service;
    }

    public record EarningTypeRequest(@NotBlank String code, @NotBlank String name, boolean taxable, boolean recurringAllowed, boolean active) {}
    public record EarningTypeDto(UUID id, String code, String name, boolean taxable, boolean recurringAllowed, boolean active) {
        static EarningTypeDto from(EarningType t) { return new EarningTypeDto(t.getId(), t.getCode(), t.getName(), t.isTaxable(), t.isRecurringAllowed(), t.isActive()); }
    }

    public record DeductionTypeRequest(@NotBlank String code, @NotBlank String name, boolean recurringAllowed, boolean loanType, boolean active) {}
    public record DeductionTypeDto(UUID id, String code, String name, boolean recurringAllowed, boolean loanType, boolean active) {
        static DeductionTypeDto from(DeductionType t) { return new DeductionTypeDto(t.getId(), t.getCode(), t.getName(), t.isRecurringAllowed(), t.isLoanType(), t.isActive()); }
    }

    public record EmployeeEarningRequest(@NotNull UUID earningTypeId, @NotNull @DecimalMin("0.01") BigDecimal amount, boolean recurring, LocalDate effectiveStart, LocalDate effectiveEnd, UUID payPeriodId) {}
    public record EmployeeEarningDto(UUID id, UUID employeeId, UUID earningTypeId, BigDecimal amount, boolean recurring, LocalDate effectiveStart, LocalDate effectiveEnd, UUID payPeriodId, boolean active) {
        static EmployeeEarningDto from(EmployeeEarning e) {
            return new EmployeeEarningDto(e.getId(), e.getEmployeeId(), e.getEarningTypeId(), e.getAmount().amount(), e.isRecurring(), e.getEffectiveStart(), e.getEffectiveEnd(), e.getPayPeriodId(), e.isActive());
        }
    }

    public record EmployeeDeductionRequest(@NotNull UUID deductionTypeId, @NotNull @DecimalMin("0.01") BigDecimal amount, boolean recurring, LocalDate effectiveStart, LocalDate effectiveEnd, UUID payPeriodId, BigDecimal totalLoanAmount) {}
    public record EmployeeDeductionDto(UUID id, UUID employeeId, UUID deductionTypeId, BigDecimal amount, boolean recurring, LocalDate effectiveStart, LocalDate effectiveEnd, UUID payPeriodId, BigDecimal totalLoanAmount, BigDecimal remainingBalance, boolean active) {
        static EmployeeDeductionDto from(EmployeeDeduction d) {
            return new EmployeeDeductionDto(d.getId(), d.getEmployeeId(), d.getDeductionTypeId(), d.getAmount().amount(), d.isRecurring(), d.getEffectiveStart(), d.getEffectiveEnd(), d.getPayPeriodId(),
                    d.getTotalLoanAmount() == null ? null : d.getTotalLoanAmount().amount(),
                    d.getRemainingBalance() == null ? null : d.getRemainingBalance().amount(),
                    d.isActive());
        }
    }

    @GetMapping("/earning-types")
    public List<EarningTypeDto> listEarningTypes() { return service.listEarningTypes().stream().map(EarningTypeDto::from).toList(); }

    @PostMapping("/earning-types")
    @ResponseStatus(HttpStatus.CREATED)
    public EarningTypeDto createEarningType(@Valid @RequestBody EarningTypeRequest req) {
        return EarningTypeDto.from(service.createEarningType(req.code(), req.name(), req.taxable(), req.recurringAllowed()));
    }

    @PutMapping("/earning-types/{id}")
    public EarningTypeDto updateEarningType(@PathVariable UUID id, @Valid @RequestBody EarningTypeRequest req) {
        return EarningTypeDto.from(service.updateEarningType(id, req.code(), req.name(), req.taxable(), req.recurringAllowed(), req.active()));
    }

    @DeleteMapping("/earning-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEarningType(@PathVariable UUID id) { service.deleteEarningType(id); }

    @GetMapping("/deduction-types")
    public List<DeductionTypeDto> listDeductionTypes() { return service.listDeductionTypes().stream().map(DeductionTypeDto::from).toList(); }

    @PostMapping("/deduction-types")
    @ResponseStatus(HttpStatus.CREATED)
    public DeductionTypeDto createDeductionType(@Valid @RequestBody DeductionTypeRequest req) {
        return DeductionTypeDto.from(service.createDeductionType(req.code(), req.name(), req.recurringAllowed(), req.loanType()));
    }

    @PutMapping("/deduction-types/{id}")
    public DeductionTypeDto updateDeductionType(@PathVariable UUID id, @Valid @RequestBody DeductionTypeRequest req) {
        return DeductionTypeDto.from(service.updateDeductionType(id, req.code(), req.name(), req.recurringAllowed(), req.loanType(), req.active()));
    }

    @DeleteMapping("/deduction-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeductionType(@PathVariable UUID id) { service.deleteDeductionType(id); }

    @GetMapping("/employees/{employeeId}/earnings")
    public List<EmployeeEarningDto> listEmployeeEarnings(@PathVariable UUID employeeId) {
        return service.listEmployeeEarnings(employeeId).stream().map(EmployeeEarningDto::from).toList();
    }

    @PostMapping("/employees/{employeeId}/earnings")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeEarningDto addEmployeeEarning(@PathVariable UUID employeeId, @Valid @RequestBody EmployeeEarningRequest req) {
        return EmployeeEarningDto.from(service.addEmployeeEarning(employeeId, req.earningTypeId(), Money.of(req.amount()), req.recurring(), req.effectiveStart(), req.effectiveEnd(), req.payPeriodId()));
    }

    @DeleteMapping("/employees/{employeeId}/earnings/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployeeEarning(@PathVariable UUID employeeId, @PathVariable UUID itemId) {
        service.deleteEmployeeEarning(itemId);
    }

    @GetMapping("/employees/{employeeId}/deductions")
    public List<EmployeeDeductionDto> listEmployeeDeductions(@PathVariable UUID employeeId) {
        return service.listEmployeeDeductions(employeeId).stream().map(EmployeeDeductionDto::from).toList();
    }

    @PostMapping("/employees/{employeeId}/deductions")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDeductionDto addEmployeeDeduction(@PathVariable UUID employeeId, @Valid @RequestBody EmployeeDeductionRequest req) {
        Money loan = req.totalLoanAmount() == null ? null : Money.of(req.totalLoanAmount());
        return EmployeeDeductionDto.from(service.addEmployeeDeduction(employeeId, req.deductionTypeId(), Money.of(req.amount()), req.recurring(), req.effectiveStart(), req.effectiveEnd(), req.payPeriodId(), loan));
    }

    @DeleteMapping("/employees/{employeeId}/deductions/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployeeDeduction(@PathVariable UUID employeeId, @PathVariable UUID itemId) {
        service.deleteEmployeeDeduction(itemId);
    }
}
