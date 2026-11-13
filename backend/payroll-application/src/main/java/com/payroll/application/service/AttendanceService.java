package com.payroll.application.service;

import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.EmployeeRepository;
import com.payroll.application.ports.out.HolidayRepository;
import com.payroll.application.ports.out.TimeEntryRepository;
import com.payroll.domain.attendance.DayType;
import com.payroll.domain.attendance.Holiday;
import com.payroll.domain.attendance.HolidayType;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.employee.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AttendanceService {

    public record UpsertTimeEntryCommand(
            UUID employeeId,
            LocalDate workDate,
            LocalTime timeIn,
            LocalTime timeOut,
            BigDecimal regularHours,
            BigDecimal overtimeHours,
            BigDecimal nightDiffHours,
            int lateMinutes,
            int undertimeMinutes,
            DayType dayType) {}

    private final TimeEntryRepository timeEntries;
    private final HolidayRepository holidays;
    private final EmployeeRepository employees;
    private final TransactionPort tx;

    public AttendanceService(
            TimeEntryRepository timeEntries,
            HolidayRepository holidays,
            EmployeeRepository employees,
            TransactionPort tx) {
        this.timeEntries = timeEntries;
        this.holidays = holidays;
        this.employees = employees;
        this.tx = tx;
    }

    public List<TimeEntry> listTimeEntries(UUID employeeId, LocalDate from, LocalDate to) {
        if (employeeId != null) {
            return timeEntries.findByEmployeeAndDateBetween(employeeId, from, to);
        }
        return timeEntries.findByDateBetween(from, to);
    }

    public TimeEntry upsertTimeEntry(UpsertTimeEntryCommand cmd) {
        return tx.execute(() -> {
            Employee emp = employees.findById(cmd.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("Employee", cmd.employeeId()));
            DayType resolvedDayType = cmd.dayType() != null ? cmd.dayType() : resolveDayType(emp, cmd.workDate());
            Optional<TimeEntry> existing = timeEntries.findByEmployeeAndDate(cmd.employeeId(), cmd.workDate());
            if (existing.isPresent()) {
                TimeEntry entry = existing.get();
                entry.update(cmd.timeIn(), cmd.timeOut(), cmd.regularHours(), cmd.overtimeHours(),
                        cmd.nightDiffHours(), cmd.lateMinutes(), cmd.undertimeMinutes(), resolvedDayType);
                return timeEntries.save(entry);
            }
            TimeEntry entry = TimeEntry.create(
                    cmd.employeeId(), cmd.workDate(), cmd.timeIn(), cmd.timeOut(),
                    cmd.regularHours(), cmd.overtimeHours(), cmd.nightDiffHours(),
                    cmd.lateMinutes(), cmd.undertimeMinutes(), resolvedDayType);
            return timeEntries.save(entry);
        });
    }

    public List<TimeEntry> bulkUpsert(List<UpsertTimeEntryCommand> commands) {
        return tx.execute(() -> {
            List<TimeEntry> saved = new ArrayList<>();
            for (UpsertTimeEntryCommand cmd : commands) {
                saved.add(upsertTimeEntry(cmd));
            }
            return saved;
        });
    }

    public List<TimeEntry> approveTimeEntries(List<UUID> ids) {
        return tx.execute(() -> {
            List<TimeEntry> approved = new ArrayList<>();
            for (UUID id : ids) {
                TimeEntry entry = timeEntries.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("TimeEntry", id));
                entry.approve();
                approved.add(timeEntries.save(entry));
            }
            return approved;
        });
    }

    public void deleteTimeEntry(UUID id) {
        tx.executeVoid(() -> {
            timeEntries.findById(id).orElseThrow(() -> new EntityNotFoundException("TimeEntry", id));
            timeEntries.deleteById(id);
        });
    }

    public List<Holiday> listHolidays(int year) {
        return holidays.findAllByYear(year);
    }

    public Holiday createHoliday(LocalDate date, String name, HolidayType type) {
        return tx.execute(() -> holidays.save(Holiday.create(date, name, type)));
    }

    public Holiday updateHoliday(UUID id, LocalDate date, String name, HolidayType type) {
        return tx.execute(() -> {
            Holiday h = holidays.findById(id).orElseThrow(() -> new EntityNotFoundException("Holiday", id));
            h.update(date, name, type);
            return holidays.save(h);
        });
    }

    public void deleteHoliday(UUID id) {
        tx.executeVoid(() -> {
            holidays.findById(id).orElseThrow(() -> new EntityNotFoundException("Holiday", id));
            holidays.deleteById(id);
        });
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
