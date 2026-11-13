package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.application.service.PayPeriodService;
import com.payroll.application.service.PayrollRunService;
import com.payroll.application.service.PayslipService;
import com.payroll.domain.payroll.CutoffType;
import com.payroll.domain.payroll.PayFrequency;
import com.payroll.domain.payroll.PayPeriod;
import com.payroll.domain.payroll.PayrollRun;
import com.payroll.domain.payroll.PayrollRunStatus;
import com.payroll.domain.payroll.Payslip;
import com.payroll.domain.payroll.PayslipLine;
import com.payroll.domain.payroll.PayslipLineCategory;
import com.payroll.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PayrollController {

    private final PayPeriodService periodService;
    private final PayrollRunService runService;
    private final PayslipService payslipService;

    public PayrollController(PayPeriodService periodService, PayrollRunService runService, PayslipService payslipService) {
        this.periodService = periodService;
        this.runService = runService;
        this.payslipService = payslipService;
    }

    public record PayPeriodDto(UUID id, LocalDate startDate, LocalDate endDate, LocalDate payDate, PayFrequency frequency, CutoffType cutoff, int year, int periodNumber, boolean closed) {
        static PayPeriodDto from(PayPeriod p) {
            return new PayPeriodDto(p.getId(), p.getStartDate(), p.getEndDate(), p.getPayDate(), p.getFrequency(), p.getCutoff(), p.getYear(), p.getPeriodNumber(), p.isClosed());
        }
    }

    public record CreateRunRequest(@NotNull UUID payPeriodId, String notes) {}
    public record ComputeRunRequest(List<UUID> employeeIds) {}

    public record PayrollRunDto(
            UUID id, UUID payPeriodId, PayrollRunStatus status, int employeeCount,
            BigDecimal totalGrossPay, BigDecimal totalDeductions, BigDecimal totalNetPay,
            BigDecimal totalEmployerContributions, Instant computedAt, UUID approvedBy,
            Instant approvedAt, Instant paidAt, String notes) {
        static PayrollRunDto from(PayrollRun r) {
            return new PayrollRunDto(r.getId(), r.getPayPeriodId(), r.getStatus(), r.getEmployeeCount(),
                    r.getTotalGrossPay().amount(), r.getTotalDeductions().amount(), r.getTotalNetPay().amount(),
                    r.getTotalEmployerContributions().amount(), r.getComputedAt(), r.getApprovedBy(),
                    r.getApprovedAt(), r.getPaidAt(), r.getNotes());
        }
    }

    public record PayslipLineDto(PayslipLineCategory category, String code, String description, BigDecimal quantity, BigDecimal rate, BigDecimal amount, boolean taxable) {
        static PayslipLineDto from(PayslipLine l) {
            return new PayslipLineDto(l.category(), l.code(), l.description(), l.quantity(), l.rate().amount(), l.amount().amount(), l.taxable());
        }
    }

    public record PayslipDto(
            UUID id, UUID payrollRunId, UUID employeeId, UUID payPeriodId,
            String employeeNo, String employeeName, String departmentName, String positionName,
            BigDecimal basicPay, BigDecimal totalEarnings, BigDecimal totalOvertimeAndPremiums,
            BigDecimal totalAbsencesAndLate, BigDecimal grossPay, BigDecimal taxableIncome,
            BigDecimal sssEmployee, BigDecimal philHealthEmployee, BigDecimal pagIbigEmployee,
            BigDecimal withholdingTax, BigDecimal otherDeductions, BigDecimal totalDeductions,
            BigDecimal netPay, BigDecimal sssEmployer, BigDecimal sssEcEmployer,
            BigDecimal philHealthEmployer, BigDecimal pagIbigEmployer,
            boolean hasNetPayWarning, List<PayslipLineDto> lines, Instant generatedAt) {
        static PayslipDto from(Payslip s) {
            return new PayslipDto(
                    s.getId(), s.getPayrollRunId(), s.getEmployeeId(), s.getPayPeriodId(),
                    s.getEmployeeNo(), s.getEmployeeName(), s.getDepartmentName(), s.getPositionName(),
                    s.getBasicPay().amount(), s.getTotalEarnings().amount(),
                    s.getTotalOvertimeAndPremiums().amount(), s.getTotalAbsencesAndLate().amount(),
                    s.getGrossPay().amount(), s.getTaxableIncome().amount(),
                    s.getSssEmployee().amount(), s.getPhilHealthEmployee().amount(),
                    s.getPagIbigEmployee().amount(), s.getWithholdingTax().amount(),
                    s.getOtherDeductions().amount(), s.getTotalDeductions().amount(),
                    s.getNetPay().amount(), s.getSssEmployer().amount(), s.getSssEcEmployer().amount(),
                    s.getPhilHealthEmployer().amount(), s.getPagIbigEmployer().amount(),
                    s.isHasNetPayWarning(),
                    s.getLines().stream().map(PayslipLineDto::from).toList(),
                    s.getGeneratedAt());
        }
    }

    // Pay Periods
    @GetMapping("/pay-periods")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<PayPeriodDto> listPeriods() {
        return periodService.listAll().stream().map(PayPeriodDto::from).toList();
    }

    @PostMapping("/pay-periods/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<PayPeriodDto> generatePeriods(
            @RequestParam int year,
            @RequestParam(defaultValue = "SEMI_MONTHLY") PayFrequency frequency) {
        return periodService.generateForYear(year, frequency).stream().map(PayPeriodDto::from).toList();
    }

    @DeleteMapping("/pay-periods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void deletePeriod(@PathVariable UUID id) {
        periodService.delete(id);
    }

    // Payroll Runs
    @GetMapping("/payroll-runs")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<PayrollRunDto> listRuns() {
        return runService.listRuns().stream().map(PayrollRunDto::from).toList();
    }

    @GetMapping("/payroll-runs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollRunDto getRun(@PathVariable UUID id) {
        return PayrollRunDto.from(runService.getRun(id));
    }

    @PostMapping("/payroll-runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollRunDto createRun(@Valid @RequestBody CreateRunRequest req) {
        return PayrollRunDto.from(runService.createDraft(req.payPeriodId(), req.notes()));
    }

    @PostMapping("/payroll-runs/{id}/compute")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollRunDto compute(@PathVariable UUID id, @RequestBody(required = false) ComputeRunRequest req) {
        return PayrollRunDto.from(runService.compute(id, req == null ? null : req.employeeIds()));
    }

    @PostMapping("/payroll-runs/{id}/reset")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollRunDto reset(@PathVariable UUID id) {
        return PayrollRunDto.from(runService.resetToDraft(id));
    }

    @PostMapping("/payroll-runs/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public PayrollRunDto approve(Authentication auth, @PathVariable UUID id) {
        return PayrollRunDto.from(runService.approve(id, AuthPrincipal.from(auth).userId()));
    }

    @PostMapping("/payroll-runs/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public PayrollRunDto reopen(@PathVariable UUID id) {
        return PayrollRunDto.from(runService.reopen(id));
    }

    @PostMapping("/payroll-runs/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollRunDto markPaid(@PathVariable UUID id) {
        return PayrollRunDto.from(runService.markPaid(id));
    }

    @DeleteMapping("/payroll-runs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void deleteRun(@PathVariable UUID id) {
        runService.deleteDraft(id);
    }

    // Payslips
    @GetMapping("/payroll-runs/{id}/payslips")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<PayslipDto> listPayslipsByRun(@PathVariable UUID id) {
        return payslipService.listByRun(id).stream().map(PayslipDto::from).toList();
    }

    @GetMapping("/me/payslips")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<PayslipDto> myPayslips(Authentication auth) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        if (p.employeeId() == null) return List.of();
        return payslipService.listByEmployee(p.employeeId()).stream().map(PayslipDto::from).toList();
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public PayslipDto getPayslip(Authentication auth, @PathVariable UUID id) {
        Payslip slip = payslipService.get(id);
        enforcePayslipAccess(auth, slip);
        return PayslipDto.from(slip);
    }

    @GetMapping(value = "/payslips/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<byte[]> downloadPayslipPdf(Authentication auth, @PathVariable UUID id) {
        Payslip slip = payslipService.get(id);
        enforcePayslipAccess(auth, slip);
        byte[] pdf = payslipService.renderPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename("payslip-" + slip.getEmployeeNo() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    private static void enforcePayslipAccess(Authentication auth, Payslip slip) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        if (p.role() == Role.EMPLOYEE && !slip.getEmployeeId().equals(p.employeeId())) {
            throw new AccessDeniedException("Cannot view another employee's payslip");
        }
    }
}
