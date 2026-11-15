package com.payroll.application.service;

import com.payroll.application.common.ClockPort;
import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.EmployeeRepository;
import com.payroll.application.ports.out.HolidayRepository;
import com.payroll.application.ports.out.LeaveBalanceRepository;
import com.payroll.application.ports.out.LeaveRequestRepository;
import com.payroll.application.ports.out.LeaveTypeRepository;
import com.payroll.application.ports.out.TimeEntryRepository;
import com.payroll.domain.attendance.DayType;
import com.payroll.domain.attendance.Holiday;
import com.payroll.domain.attendance.HolidayType;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.ValidationException;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.employee.EmploymentStatus;
import com.payroll.domain.leave.LeaveBalance;
import com.payroll.domain.leave.LeaveRequest;
import com.payroll.domain.leave.LeaveRequestStatus;
import com.payroll.domain.leave.LeaveType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LeaveService {

    private final LeaveTypeRepository leaveTypes;
    private final LeaveBalanceRepository balances;
    private final LeaveRequestRepository requests;
    private final EmployeeRepository employees;
    private final TimeEntryRepository timeEntries;
    private final HolidayRepository holidays;
    private final TransactionPort tx;
    private final ClockPort clock;

    public LeaveService(
            LeaveTypeRepository leaveTypes,
            LeaveBalanceRepository balances,
            LeaveRequestRepository requests,
            EmployeeRepository employees,
            TimeEntryRepository timeEntries,
            HolidayRepository holidays,
            TransactionPort tx,
            ClockPort clock) {
        this.leaveTypes = leaveTypes;
        this.balances = balances;
        this.requests = requests;
        this.employees = employees;
        this.timeEntries = timeEntries;
        this.holidays = holidays;
        this.tx = tx;
        this.clock = clock;
    }

    public List<LeaveType> listLeaveTypes() {
        return leaveTypes.findAll();
    }

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
        List<LeaveBalance> persisted = balances.findByEmployeeAndYear(employeeId, year);
        Map<UUID, LeaveBalance> byType = persisted.stream()
                .collect(Collectors.toMap(LeaveBalance::getLeaveTypeId, b -> b, (a, b) -> a));

        List<LeaveBalance> result = new ArrayList<>(persisted);
        for (LeaveType lt : leaveTypes.findAll()) {
            if (lt.isActive() && !byType.containsKey(lt.getId())) {
                UUID syntheticId = UUID.nameUUIDFromBytes(
                        (employeeId + ":" + lt.getId() + ":" + year).getBytes(StandardCharsets.UTF_8));
                result.add(new LeaveBalance(
                        syntheticId, employeeId, lt.getId(), year,
                        lt.getAnnualEntitlement(), BigDecimal.ZERO, 0L));
            }
        }
        return result;
    }

    public LeaveBalance setBalance(UUID employeeId, UUID leaveTypeId, int year, BigDecimal entitledDays) {
        return tx.execute(() -> {
            employees.findById(employeeId).orElseThrow(() -> new EntityNotFoundException("Employee", employeeId));
            leaveTypes.findById(leaveTypeId).orElseThrow(() -> new EntityNotFoundException("LeaveType", leaveTypeId));
            LeaveBalance bal = balances.findByEmployeeAndTypeAndYear(employeeId, leaveTypeId, year)
                    .orElseGet(() -> LeaveBalance.create(employeeId, leaveTypeId, year, entitledDays));
            bal.updateEntitlement(entitledDays);
            return balances.save(bal);
        });
    }

    public List<LeaveRequest> listRequests(UUID employeeId, LeaveRequestStatus status) {
        if (employeeId != null) {
            List<LeaveRequest> list = requests.findByEmployeeId(employeeId);
            if (status != null) {
                return list.stream().filter(r -> r.getStatus() == status).toList();
            }
            return list;
        }
        if (status != null) return requests.findByStatus(status);
        return requests.findAll();
    }

    public LeaveRequest getRequest(UUID requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
    }

    public LeaveRequest fileRequest(
            UUID employeeId,
            UUID leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal days,
            boolean halfDay,
            String reason) {
        return tx.execute(() -> {
            Employee emp = employees.findById(employeeId)
                    .orElseThrow(() -> new EntityNotFoundException("Employee", employeeId));
            if (emp.getStatus() == EmploymentStatus.SEPARATED) {
                throw new ValidationException("Separated employees cannot file leave requests");
            }
            LeaveType type = leaveTypes.findById(leaveTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveType", leaveTypeId));
            if (!type.isActive()) {
                throw new ValidationException("Selected leave policy is inactive");
            }
            return requests.save(LeaveRequest.file(employeeId, leaveTypeId, startDate, endDate, days, halfDay, reason));
        });
    }

    /** Stage 2: Immediate Supervisor reviews and recommends approval. */
    public LeaveRequest recommendRequest(UUID requestId, UUID supervisorUserId, String remarks) {
        return tx.execute(() -> {
            LeaveRequest req = requests.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
            req.recommendBySupervisor(supervisorUserId, clock.now(), remarks);
            return requests.save(req);
        });
    }

    /** Stage 3: HR verifies leave balance and policy compliance, then records for final approval. */
    public LeaveRequest hrVerifyRequest(UUID requestId, UUID hrUserId, String remarks) {
        return tx.execute(() -> {
            LeaveRequest req = requests.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
            LeaveType type = leaveTypes.findById(req.getLeaveTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("LeaveType", req.getLeaveTypeId()));
            if (!type.isActive()) {
                throw new ValidationException("Leave policy " + type.getName() + " is inactive");
            }
            if (type.isPaid() && type.getAnnualEntitlement().compareTo(BigDecimal.ZERO) > 0) {
                int year = req.getStartDate().getYear();
                LeaveBalance bal = balances.findByEmployeeAndTypeAndYear(req.getEmployeeId(), type.getId(), year)
                        .orElseGet(() -> LeaveBalance.create(req.getEmployeeId(), type.getId(), year, type.getAnnualEntitlement()));
                if (bal.remainingDays().compareTo(req.getDays()) < 0) {
                    throw new ValidationException("Insufficient leave balance. Remaining: "
                            + bal.remainingDays() + ", requested: " + req.getDays());
                }
            }
            req.verifyByHr(hrUserId, clock.now(), remarks);
            return requests.save(req);
        });
    }

    /** Stage 4: Authorized approving officer gives final approval; updates leave credits and attendance records. */
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
            LeaveRequest saved = requests.save(req);
            syncApprovedLeaveAttendance(saved, type);
            return saved;
        });
    }

    public LeaveRequest rejectRequest(UUID requestId, UUID approverId) {
        return rejectRequest(requestId, approverId, null);
    }

    public LeaveRequest rejectRequest(UUID requestId, UUID approverId, String rejectionReason) {
        return tx.execute(() -> {
            LeaveRequest req = requests.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("LeaveRequest", requestId));
            req.reject(approverId, clock.now(), rejectionReason);
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
                timeEntries.deleteByLeaveRequestId(requestId);
            }
            return requests.save(req);
        });
    }

    private void syncApprovedLeaveAttendance(LeaveRequest req, LeaveType type) {
        Employee emp = employees.findById(req.getEmployeeId()).orElse(null);
        if (emp == null) return;

        List<LocalDate> workDates = new ArrayList<>();
        for (LocalDate d = req.getStartDate(); !d.isAfter(req.getEndDate()); d = d.plusDays(1)) {
            if (!emp.isRestDay(d.getDayOfWeek())) {
                workDates.add(d);
            }
        }
        if (workDates.isEmpty()) {
            workDates.add(req.getStartDate());
        }

        String remark = "On Leave: " + type.getName() + (req.isHalfDay() ? " (Half-Day)" : "");
        if (remark.length() > 160) {
            remark = remark.substring(0, 160);
        }

        for (LocalDate date : workDates) {
            if (timeEntries.findByEmployeeAndDate(emp.getId(), date).isEmpty()) {
                DayType dayType = resolveDayType(emp, date);
                timeEntries.save(TimeEntry.forLeave(emp.getId(), date, dayType, req.getId(), remark));
            }
        }
    }

    private DayType resolveDayType(Employee emp, LocalDate date) {
        boolean restDay = emp.isRestDay(date.getDayOfWeek());
        Optional<Holiday> holiday = holidays.findByDate(date);
        if (holiday.isEmpty()) {
            return restDay ? DayType.REST_DAY : DayType.REGULAR;
        }
        HolidayType type = holiday.get().getType();
        if (type == HolidayType.REGULAR) {
            return restDay ? DayType.REST_DAY_REGULAR_HOLIDAY : DayType.REGULAR_HOLIDAY;
        }
        return restDay ? DayType.REST_DAY_SPECIAL_HOLIDAY : DayType.SPECIAL_HOLIDAY;
    }
}
