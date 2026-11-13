package com.payroll.application.service;

import com.payroll.application.common.ClockPort;
import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.LeaveBalanceRepository;
import com.payroll.application.ports.out.LeaveRequestRepository;
import com.payroll.application.ports.out.LeaveTypeRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.leave.LeaveBalance;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveRequestStatus;
import com.payroll.domain.leave.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class LeaveService {

    private final LeaveTypeRepository leaveTypes;
    private final LeaveBalanceRepository balances;
    private final LeaveRequestRepository requests;
    private final TransactionPort tx;
    private final ClockPort clock;

    public LeaveService(
            LeaveTypeRepository leaveTypes,
            LeaveBalanceRepository balances,
            LeaveRequestRepository requests,
            TransactionPort tx,
            ClockPort clock) {
        this.leaveTypes = leaveTypes;
        this.balances = balances;
        this.requests = requests;
        this.tx = tx;
        this.clock = clock;
    }

    public List<LeaveType> listLeaveTypes() { return leaveTypes.findAll(); }

    public LeaveType createLeaveType(String code, String name, boolean paid, BigDecimal annualEntitlement) {
        return tx.execute(() -> leaveTypes.save(LeaveType.create(code, name, paid, annualEntitlement)));
    }

    public LeaveType updateLeaveType(UUID id, String code, String name, boolean paid, BigDecimal annualEntitlement, boolean active) {
        return tx.execute(() -> {
            LeaveType lt = leaveTypes.findById(id).orElseThrow(() -> new EntityNotFoundException("LeaveType", id));
            lt.update(code, name, paid, annualEntitlement, active);
            return leaveTypes.save(lt);
        });
    }

    public List<LeaveBalance> getBalances(UUID employeeId, int year) {
        return balances.findByEmployeeAndYear(employeeId, year);
    }

    public LeaveBalance setBalance(UUID employeeId, UUID leaveTypeId, int year, BigDecimal entitledDays) {
        return tx.execute(() -> {
            LeaveBalance bal = balances.findByEmployeeAndTypeAndYear(employeeId, leaveTypeId, year)
                    .orElseGet(() -> LeaveBalance.create(employeeId, leaveTypeId, year, entitledDays));
            bal.updateEntitlement(entitledDays);
            return balances.save(bal);
        });
    }

    public List<LeaveRequest> listRequests(UUID employeeId, LeaveRequestStatus status) {
        if (employeeId != null) return requests.findByEmployeeId(employeeId);
        if (status != null) return requests.findByStatus(status);
        return requests.findAll();
    }

    public LeaveRequest fileRequest(UUID employeeId, UUID leaveTypeId, LocalDate startDate, LocalDate endDate, BigDecimal days, boolean halfDay, String reason) {
        return tx.execute(() -> {
            leaveTypes.findById(leaveTypeId).orElseThrow(() -> new EntityNotFoundException("LeaveType", leaveTypeId));
            return requests.save(LeaveRequest.file(employeeId, leaveTypeId, startDate, endDate, days, halfDay, reason));
        });
    }

    public LeaveRequest approveRequest(UUID requestId, UUID approverId) {
        return tx.execute(() -> {
            LeaveRequest req = requests.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
            LeaveType type = leaveTypes.findById(req.getLeaveTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("LeaveType", req.getLeaveTypeId()));
            if (type.isPaid() && type.getAnnualEntitlement().compareTo(BigDecimal.ZERO) > 0) {
                int year = req.getStartDate().getYear();
                LeaveBalance bal = balances.findByEmployeeAndTypeAndYear(req.getEmployeeId(), type.getId(), year)
                        .orElseGet(() -> LeaveBalance.create(req.getEmployeeId(), type.getId(), year, type.getAnnualEntitlement()));
                bal.deduct(req.getDays());
                balances.save(bal);
            }
            req.approve(approverId, clock.now());
            return requests.save(req);
        });
    }

    public LeaveRequest rejectRequest(UUID requestId, UUID approverId) {
        return tx.execute(() -> {
            LeaveRequest req = requests.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
            req.reject(approverId, clock.now());
            return requests.save(req);
        });
    }

    public LeaveRequest cancelRequest(UUID requestId) {
        return tx.execute(() -> {
            LeaveRequest req = requests.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
            boolean wasApproved = req.getStatus() == LeaveRequestStatus.APPROVED;
            req.cancel();
            if (wasApproved) {
                LeaveType type = leaveTypes.findById(req.getLeaveTypeId()).orElse(null);
                if (type != null && type.isPaid()) {
                    balances.findByEmployeeAndTypeAndYear(req.getEmployeeId(), type.getId(), req.getStartDate().getYear())
                            .ifPresent(bal -> {
                                bal.restore(req.getDays());
                                balances.save(bal);
                            });
                }
            }
            return requests.save(req);
        });
    }
}
