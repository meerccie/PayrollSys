package com.payroll.application.service;

import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.StatutoryTableRepository;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import java.time.LocalDate;

public class StatutoryService {

    private final StatutoryTableRepository repo;
    private final TransactionPort tx;

    public StatutoryService(StatutoryTableRepository repo, TransactionPort tx) {
        this.repo = repo;
        this.tx = tx;
    }

    public SssTable getSssTable(LocalDate date) { return repo.findSssTableOn(date); }
    public PhilHealthTable getPhilHealthTable(LocalDate date) { return repo.findPhilHealthTableOn(date); }
    public PagIbigTable getPagIbigTable(LocalDate date) { return repo.findPagIbigTableOn(date); }
    public BirTaxTable getBirTaxTable(LocalDate date) { return repo.findBirTaxTableOn(date); }

    public SssTable saveSssTable(SssTable table) { return tx.execute(() -> repo.saveSssTable(table)); }
    public PhilHealthTable savePhilHealthTable(PhilHealthTable table) { return tx.execute(() -> repo.savePhilHealthTable(table)); }
    public PagIbigTable savePagIbigTable(PagIbigTable table) { return tx.execute(() -> repo.savePagIbigTable(table)); }
    public BirTaxTable saveBirTaxTable(BirTaxTable table) { return tx.execute(() -> repo.saveBirTaxTable(table)); }

    public StatutoryTableRepository.CompanyConfig getCompanyConfig() { return repo.getCompanyConfig(); }
    public StatutoryTableRepository.CompanyConfig updateCompanyConfig(StatutoryTableRepository.CompanyConfig config) {
        return tx.execute(() -> repo.updateCompanyConfig(config));
    }
}
