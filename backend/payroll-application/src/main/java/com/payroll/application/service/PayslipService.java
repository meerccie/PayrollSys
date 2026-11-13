package com.payroll.application.service;

import com.payroll.application.ports.out.PayPeriodRepository;
import com.payroll.application.ports.out.PayslipPdfRenderer;
import com.payroll.application.ports.out.PayslipRepository;
import com.payroll.application.ports.out.StatutoryTableRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.payroll.PayPeriod;
import com.payroll.domain.payroll.Payslip;
import java.util.List;
import java.util.UUID;

public class PayslipService {

    private final PayslipRepository payslips;
    private final PayPeriodRepository periods;
    private final StatutoryTableRepository statutory;
    private final PayslipPdfRenderer pdfRenderer;

    public PayslipService(
            PayslipRepository payslips,
            PayPeriodRepository periods,
            StatutoryTableRepository statutory,
            PayslipPdfRenderer pdfRenderer) {
        this.payslips = payslips;
        this.periods = periods;
        this.statutory = statutory;
        this.pdfRenderer = pdfRenderer;
    }

    public List<Payslip> listByRun(UUID runId) {
        return payslips.findByPayrollRunId(runId);
    }

    public List<Payslip> listByEmployee(UUID employeeId) {
        return payslips.findByEmployeeId(employeeId);
    }

    public Payslip get(UUID id) {
        return payslips.findById(id).orElseThrow(() -> new EntityNotFoundException("Payslip", id));
    }

    public byte[] renderPdf(UUID id) {
        Payslip slip = get(id);
        PayPeriod period = periods.findById(slip.getPayPeriodId())
                .orElseThrow(() -> new EntityNotFoundException("PayPeriod", slip.getPayPeriodId()));
        return pdfRenderer.render(slip, period, statutory.getCompanyConfig());
    }
}
