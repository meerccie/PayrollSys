package com.payroll.application.service;

import com.payroll.application.common.ClockPort;
import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.CompensationRepository;
import com.payroll.application.ports.out.DepartmentRepository;
import com.payroll.application.ports.out.EmployeeRepository;
import com.payroll.application.ports.out.LeaveRequestRepository;
import com.payroll.application.ports.out.LeaveTypeRepository;
import com.payroll.application.ports.out.PayPeriodRepository;
import com.payroll.application.ports.out.PayrollRunRepository;
import com.payroll.application.ports.out.PayslipRepository;
import com.payroll.application.ports.out.PositionRepository;
import com.payroll.application.ports.out.StatutoryTableRepository;
import com.payroll.application.ports.out.TimeEntryRepository;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.Money;
import com.payroll.domain.common.ValidationException;
import com.payroll.domain.compensation.DeductionType;
import com.payroll.domain.compensation.EarningType;
import com.payroll.domain.compensation.EmployeeDeduction;
import com.payroll.domain.compensation.EmployeeEarning;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveType;
import com.payroll.domain.organization.Department;
import com.payroll.domain.organization.Position;
import com.payroll.domain.payroll.PayPeriod;
import com.payroll.domain.payroll.PayrollRun;
import com.payroll.domain.payroll.PayrollRunStatus;
import com.payroll.domain.payroll.Payslip;
import com.payroll.domain.payroll.calc.PayrollCalculator;
import com.payroll.domain.payroll.calc.PayrollContext;
import com.payroll.domain.payroll.calc.PremiumRateTable;
import com.payroll.domain.payroll.statutory.BirTaxTable;
import com.payroll.domain.payroll.statutory.PagIbigTable;
import com.payroll.domain.payroll.statutory.PhilHealthTable;
import com.payroll.domain.payroll.statutory.SssTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PayrollRunService {

    private final PayrollRunRepository runs;
    private final PayPeriodRepository periods;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final TimeEntryRepository timeEntries;
    private final LeaveRequestRepository leaveRequests;
    private final LeaveTypeRepository leaveTypes;
    private final CompensationRepository compensation;
    private final PayslipRepository payslips;
    private final StatutoryTableRepository statutory;
    private final TransactionPort tx;
    private final ClockPort clock;
    private final PayrollCalculator calculator = new PayrollCalculator();

    public PayrollRunService(
            PayrollRunRepository runs,
            PayPeriodRepository periods,
            EmployeeRepository employees,
            DepartmentRepository departments,
            PositionRepository positions,
            TimeEntryRepository timeEntries,
            LeaveRequestRepository leaveRequests,
            LeaveTypeRepository leaveTypes,
            CompensationRepository compensation,
            PayslipRepository payslips,
            StatutoryTableRepository statutory,
            TransactionPort tx,
            ClockPort clock) {
        this.runs = runs;
        this.periods = periods;
        this.employees = employees;
        this.departments = departments;
        this.positions = positions;
        this.timeEntries = timeEntries;
        this.leaveRequests = leaveRequests;
        this.leaveTypes = leaveTypes;
        this.compensation = compensation;
        this.payslips = payslips;
        this.statutory = statutory;
        this.tx = tx;
        this.clock = clock;
    }

    public List<PayrollRun> listRuns() { return runs.findAll(); }

    public PayrollRun getRun(UUID id) {
        return runs.findById(id).orElseThrow(() -> new EntityNotFoundException("PayrollRun", id));
    }

    public PayrollRun createDraft(UUID payPeriodId, String notes) {
        return tx.execute(() -> {
            periods.findById(payPeriodId).orElseThrow(() -> new EntityNotFoundException("PayPeriod", payPeriodId));
            if (runs.findByPayPeriodId(payPeriodId).isPresent()) {
                throw new ValidationException("A payroll run already exists for this pay period");
            }
            return runs.save(PayrollRun.createDraft(payPeriodId, notes));
        });
    }

    public PayrollRun compute(UUID runId, List<UUID> employeeIds) {
        return tx.execute(() -> {
            PayrollRun run = getRun(runId);
            PayPeriod period = periods.findById(run.getPayPeriodId())
                    .orElseThrow(() -> new EntityNotFoundException("PayPeriod", run.getPayPeriodId()));

            List<Employee> targetEmployees = employees.findAllActive().stream()
                    .filter(e -> e.isActiveOn(period.getEndDate()))
                    .filter(e -> employeeIds == null || employeeIds.isEmpty() || employeeIds.contains(e.getId()))
                    .toList();

            Map<UUID, Department> deptMap = departments.findAll().stream()
                    .collect(Collectors.toMap(Department::getId, Function.identity()));
            Map<UUID, Position> posMap = positions.findAll().stream()
                    .collect(Collectors.toMap(Position::getId, Function.identity()));
            Map<UUID, LeaveType> leaveTypeMap = leaveTypes.findAll().stream()
                    .collect(Collectors.toMap(LeaveType::getId, Function.identity()));
            Map<UUID, EarningType> earningTypeMap = compensation.findAllEarningTypes().stream()
                    .collect(Collectors.toMap(EarningType::getId, Function.identity()));
            Map<UUID, DeductionType> deductionTypeMap = compensation.findAllDeductionTypes().stream()
                    .collect(Collectors.toMap(DeductionType::getId, Function.identity()));

            SssTable sssTable = statutory.findSssTableOn(period.getEndDate());
            PhilHealthTable philHealthTable = statutory.findPhilHealthTableOn(period.getEndDate());
            PagIbigTable pagIbigTable = statutory.findPagIbigTableOn(period.getEndDate());
            BirTaxTable birTaxTable = statutory.findBirTaxTableOn(period.getEndDate());
            var companyConfig = statutory.getCompanyConfig();

            payslips.deleteByPayrollRunId(runId);

            List<Payslip> generated = new ArrayList<>();
            Money totalGross = Money.ZERO;
            Money totalDed = Money.ZERO;
            Money totalNet = Money.ZERO;
            Money totalEr = Money.ZERO;

            for (Employee emp : targetEmployees) {
                List<TimeEntry> empEntries = timeEntries.findByEmployeeAndDateBetween(
                        emp.getId(), period.getStartDate(), period.getEndDate());
                List<LeaveRequest> empLeaves = leaveRequests.findApprovedForEmployeeOverlapping(
                        emp.getId(), period.getStartDate(), period.getEndDate());
                List<EmployeeEarning> empEarnings = compensation.findEmployeeEarningsByEmployeeId(emp.getId());
                List<EmployeeDeduction> empDeductions = compensation.findEmployeeDeductionsByEmployeeId(emp.getId());

                String deptName = emp.getDepartmentId() != null && deptMap.containsKey(emp.getDepartmentId())
                        ? deptMap.get(emp.getDepartmentId()).getName() : null;
                String posName = emp.getPositionId() != null && posMap.containsKey(emp.getPositionId())
                        ? posMap.get(emp.getPositionId()).getName() : null;

                PayrollContext ctx = new PayrollContext(
                        run.getId(), period, emp, deptName, posName,
                        empEntries, empLeaves, leaveTypeMap,
                        empEarnings, earningTypeMap,
                        empDeductions, deductionTypeMap,
                        sssTable, philHealthTable, pagIbigTable, birTaxTable,
                        PremiumRateTable.standardPh(), companyConfig.settings(), clock.now());

                Payslip slip = calculator.calculate(ctx);
                generated.add(slip);
                totalGross = totalGross.add(slip.getGrossPay());
                totalDed = totalDed.add(slip.getTotalDeductions());
                totalNet = totalNet.add(slip.getNetPay());
                totalEr = totalEr.add(slip.totalEmployerContributions());
            }

            payslips.saveAll(generated);
            run.markComputed(generated.size(), totalGross, totalDed, totalNet, totalEr, clock.now());
            return runs.save(run);
        });
    }

    public PayrollRun resetToDraft(UUID runId) {
        return tx.execute(() -> {
            PayrollRun run = getRun(runId);
            run.resetToDraft();
            payslips.deleteByPayrollRunId(runId);
            return runs.save(run);
        });
    }

    public PayrollRun approve(UUID runId, UUID approverId) {
        return tx.execute(() -> {
            PayrollRun run = getRun(runId);
            run.approve(approverId, clock.now());
            PayPeriod period = periods.findById(run.getPayPeriodId())
                    .orElseThrow(() -> new EntityNotFoundException("PayPeriod", run.getPayPeriodId()));
            period.close();
            periods.save(period);
            return runs.save(run);
        });
    }

    public PayrollRun reopen(UUID runId) {
        return tx.execute(() -> {
            PayrollRun run = getRun(runId);
            run.reopen();
            PayPeriod period = periods.findById(run.getPayPeriodId())
                    .orElseThrow(() -> new EntityNotFoundException("PayPeriod", run.getPayPeriodId()));
            period.reopen();
            periods.save(period);
            return runs.save(run);
        });
    }

    public PayrollRun markPaid(UUID runId) {
        return tx.execute(() -> {
            PayrollRun run = getRun(runId);
            run.markPaid(clock.now());
            return runs.save(run);
        });
    }

    public void deleteDraft(UUID runId) {
        tx.executeVoid(() -> {
            PayrollRun run = getRun(runId);
            if (run.getStatus() != PayrollRunStatus.DRAFT) {
                throw new ValidationException("Only DRAFT payroll runs can be deleted");
            }
            runs.deleteById(runId);
        });
    }
}
