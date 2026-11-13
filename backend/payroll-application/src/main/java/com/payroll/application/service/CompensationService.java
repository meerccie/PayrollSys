package com.payroll.application.service;

import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.CompensationRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.Money;
import com.payroll.domain.compensation.DeductionType;
import com.payroll.domain.compensation.EarningType;
import com.payroll.domain.compensation.EmployeeDeduction;
import com.payroll.domain.compensation.EmployeeEarning;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CompensationService {

    private final CompensationRepository repo;
    private final TransactionPort tx;

    public CompensationService(CompensationRepository repo, TransactionPort tx) {
        this.repo = repo;
        this.tx = tx;
    }

    public List<EarningType> listEarningTypes() { return repo.findAllEarningTypes(); }

    public EarningType createEarningType(String code, String name, boolean taxable, boolean recurringAllowed) {
        return tx.execute(() -> repo.saveEarningType(EarningType.create(code, name, taxable, recurringAllowed)));
    }

    public EarningType updateEarningType(UUID id, String code, String name, boolean taxable, boolean recurringAllowed, boolean active) {
        return tx.execute(() -> {
            EarningType et = repo.findEarningTypeById(id).orElseThrow(() -> new EntityNotFoundException("EarningType", id));
            et.update(code, name, taxable, recurringAllowed, active);
            return repo.saveEarningType(et);
        });
    }

    public void deleteEarningType(UUID id) {
        tx.executeVoid(() -> repo.deleteEarningTypeById(id));
    }

    public List<DeductionType> listDeductionTypes() { return repo.findAllDeductionTypes(); }

    public DeductionType createDeductionType(String code, String name, boolean recurringAllowed, boolean loanType) {
        return tx.execute(() -> repo.saveDeductionType(DeductionType.create(code, name, recurringAllowed, loanType)));
    }

    public DeductionType updateDeductionType(UUID id, String code, String name, boolean recurringAllowed, boolean loanType, boolean active) {
        return tx.execute(() -> {
            DeductionType dt = repo.findDeductionTypeById(id).orElseThrow(() -> new EntityNotFoundException("DeductionType", id));
            dt.update(code, name, recurringAllowed, loanType, active);
            return repo.saveDeductionType(dt);
        });
    }

    public void deleteDeductionType(UUID id) {
        tx.executeVoid(() -> repo.deleteDeductionTypeById(id));
    }

    public List<EmployeeEarning> listEmployeeEarnings(UUID employeeId) {
        return repo.findEmployeeEarningsByEmployeeId(employeeId);
    }

    public EmployeeEarning addEmployeeEarning(UUID employeeId, UUID earningTypeId, Money amount, boolean recurring, LocalDate effectiveStart, LocalDate effectiveEnd, UUID payPeriodId) {
        return tx.execute(() -> {
            repo.findEarningTypeById(earningTypeId).orElseThrow(() -> new EntityNotFoundException("EarningType", earningTypeId));
            return repo.saveEmployeeEarning(EmployeeEarning.create(employeeId, earningTypeId, amount, recurring, effectiveStart, effectiveEnd, payPeriodId));
        });
    }

    public EmployeeEarning updateEmployeeEarning(UUID id, Money amount, LocalDate effectiveStart, LocalDate effectiveEnd, boolean active) {
        return tx.execute(() -> {
            EmployeeEarning e = repo.findEmployeeEarningById(id).orElseThrow(() -> new EntityNotFoundException("EmployeeEarning", id));
            e.update(amount, effectiveStart, effectiveEnd, active);
            return repo.saveEmployeeEarning(e);
        });
    }

    public void deleteEmployeeEarning(UUID id) {
        tx.executeVoid(() -> repo.deleteEmployeeEarningById(id));
    }

    public List<EmployeeDeduction> listEmployeeDeductions(UUID employeeId) {
        return repo.findEmployeeDeductionsByEmployeeId(employeeId);
    }

    public EmployeeDeduction addEmployeeDeduction(UUID employeeId, UUID deductionTypeId, Money amount, boolean recurring, LocalDate effectiveStart, LocalDate effectiveEnd, UUID payPeriodId, Money totalLoanAmount) {
        return tx.execute(() -> {
            repo.findDeductionTypeById(deductionTypeId).orElseThrow(() -> new EntityNotFoundException("DeductionType", deductionTypeId));
            return repo.saveEmployeeDeduction(EmployeeDeduction.create(employeeId, deductionTypeId, amount, recurring, effectiveStart, effectiveEnd, payPeriodId, totalLoanAmount));
        });
    }

    public EmployeeDeduction updateEmployeeDeduction(UUID id, Money amount, LocalDate effectiveStart, LocalDate effectiveEnd, boolean active) {
        return tx.execute(() -> {
            EmployeeDeduction d = repo.findEmployeeDeductionById(id).orElseThrow(() -> new EntityNotFoundException("EmployeeDeduction", id));
            d.update(amount, effectiveStart, effectiveEnd, active);
            return repo.saveEmployeeDeduction(d);
        });
    }

    public void deleteEmployeeDeduction(UUID id) {
        tx.executeVoid(() -> repo.deleteEmployeeDeductionById(id));
    }
}
