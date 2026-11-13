package com.payroll.adapters.in.web;

import com.payroll.application.ports.out.StatutoryTableRepository;
import com.payroll.application.service.StatutoryService;
import com.payroll.domain.payroll.StatutorySchedule;
import com.payroll.domain.payroll.calc.PayrollSettings;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class StatutoryController {

    private final StatutoryService service;

    public StatutoryController(StatutoryService service) {
        this.service = service;
    }

    public record CompanyConfigRequest(
            @NotBlank String companyName,
            String companyTin,
            String companyAddress,
            @Min(200) int annualWorkDays,
            @NotNull StatutorySchedule statutorySchedule) {}

    @GetMapping("/statutory/sss")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public SssTable getSss(@RequestParam(required = false) LocalDate date) {
        return service.getSssTable(date == null ? LocalDate.now() : date);
    }

    @PostMapping("/statutory/sss")
    @PreAuthorize("hasRole('ADMIN')")
    public SssTable saveSss(@RequestBody SssTable table) {
        return service.saveSssTable(table);
    }

    @GetMapping("/statutory/philhealth")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PhilHealthTable getPhilHealth(@RequestParam(required = false) LocalDate date) {
        return service.getPhilHealthTable(date == null ? LocalDate.now() : date);
    }

    @PostMapping("/statutory/philhealth")
    @PreAuthorize("hasRole('ADMIN')")
    public PhilHealthTable savePhilHealth(@RequestBody PhilHealthTable table) {
        return service.savePhilHealthTable(table);
    }

    @GetMapping("/statutory/pagibig")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PagIbigTable getPagIbig(@RequestParam(required = false) LocalDate date) {
        return service.getPagIbigTable(date == null ? LocalDate.now() : date);
    }

    @PostMapping("/statutory/pagibig")
    @PreAuthorize("hasRole('ADMIN')")
    public PagIbigTable savePagIbig(@RequestBody PagIbigTable table) {
        return service.savePagIbigTable(table);
    }

    @GetMapping("/statutory/tax")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public BirTaxTable getTax(@RequestParam(required = false) LocalDate date) {
        return service.getBirTaxTable(date == null ? LocalDate.now() : date);
    }

    @PostMapping("/statutory/tax")
    @PreAuthorize("hasRole('ADMIN')")
    public BirTaxTable saveTax(@RequestBody BirTaxTable table) {
        return service.saveBirTaxTable(table);
    }

    @GetMapping("/settings/company")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public StatutoryTableRepository.CompanyConfig getCompanyConfig() {
        return service.getCompanyConfig();
    }

    @PutMapping("/settings/company")
    @PreAuthorize("hasRole('ADMIN')")
    public StatutoryTableRepository.CompanyConfig updateCompanyConfig(@Valid @RequestBody CompanyConfigRequest req) {
        return service.updateCompanyConfig(new StatutoryTableRepository.CompanyConfig(
                req.companyName(), req.companyTin(), req.companyAddress(),
                new PayrollSettings(req.annualWorkDays(), req.statutorySchedule())));
    }
}
