package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.application.service.AttendanceService;
import com.payroll.application.service.AuthService;
import com.payroll.application.service.EmployeeService;
import com.payroll.domain.attendance.DayType;
import com.payroll.domain.attendance.Holiday;
import com.payroll.domain.attendance.HolidayType;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.attendance.TimeEntryStatus;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final EmployeeService employeeService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService, EmployeeService employeeService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
        this.employeeService = employeeService;
    }

    public record TimeEntryRequest(
            @NotNull UUID employeeId,
            @NotNull LocalDate workDate,
            LocalTime timeIn,
            LocalTime timeOut,
            @NotNull BigDecimal regularHours,
            BigDecimal overtimeHours,
            BigDecimal nightDiffHours,
            int lateMinutes,
            int undertimeMinutes,
            DayType dayType) {}

    public record ApproveTimeEntriesRequest(@NotNull List<UUID> ids) {}

    public record TimeEntryDto(
            UUID id, UUID employeeId, LocalDate workDate, LocalTime timeIn, LocalTime timeOut,
            BigDecimal regularHours, BigDecimal overtimeHours, BigDecimal nightDiffHours,
            int lateMinutes, int undertimeMinutes, DayType dayType, TimeEntryStatus status,
            UUID leaveRequestId, String remarks) {
        static TimeEntryDto from(TimeEntry e) {
            return new TimeEntryDto(e.getId(), e.getEmployeeId(), e.getWorkDate(), e.getTimeIn(), e.getTimeOut(),
                    e.getRegularHours(), e.getOvertimeHours(), e.getNightDiffHours(),
                    e.getLateMinutes(), e.getUndertimeMinutes(), e.getDayType(), e.getStatus(),
                    e.getLeaveRequestId(), e.getRemarks());
        }
    }

    public record HolidayRequest(@NotNull LocalDate holidayDate, @NotBlank String name, @NotNull HolidayType type) {}
    public record HolidayDto(UUID id, LocalDate holidayDate, String name, HolidayType type) {
        static HolidayDto from(Holiday h) { return new HolidayDto(h.getId(), h.getHolidayDate(), h.getName(), h.getType()); }
    }

    @GetMapping("/time-entries")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public List<TimeEntryDto> listTimeEntries(
            Authentication auth,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID targetId = employeeId;
        if (p.role() == Role.EMPLOYEE) {
            UUID empId = authService.resolveEmployeeId(p.userId(), p.employeeId());
            if (empId == null) return List.of();
            targetId = empId;
        } else if (p.role() == Role.SUPERVISOR) {
            UUID myEmpId = authService.resolveEmployeeId(p.userId(), p.employeeId());
            if (myEmpId == null) return List.of();
            Set<UUID> allowed = new HashSet<>();
            allowed.add(myEmpId);
            employeeService.listBySupervisor(myEmpId).stream().map(Employee::getId).forEach(allowed::add);
            if (employeeId != null && !allowed.contains(employeeId)) {
                throw new AccessDeniedException("Cannot view time entries outside your team");
            }
            return attendanceService.listTimeEntries(employeeId, from, to).stream()
                    .filter(t -> allowed.contains(t.getEmployeeId()))
                    .map(TimeEntryDto::from)
                    .toList();
        }
        return attendanceService.listTimeEntries(targetId, from, to).stream().map(TimeEntryDto::from).toList();
    }

    @PostMapping("/time-entries")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public TimeEntryDto upsert(@Valid @RequestBody TimeEntryRequest req) {
        return TimeEntryDto.from(attendanceService.upsertTimeEntry(toCmd(req)));
    }

    @PutMapping("/time-entries/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<TimeEntryDto> bulkUpsert(@Valid @RequestBody List<TimeEntryRequest> requests) {
        return attendanceService.bulkUpsert(requests.stream().map(AttendanceController::toCmd).toList())
                .stream().map(TimeEntryDto::from).toList();
    }

    @PostMapping("/time-entries/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<TimeEntryDto> approve(@Valid @RequestBody ApproveTimeEntriesRequest req) {
        return attendanceService.approveTimeEntries(req.ids()).stream().map(TimeEntryDto::from).toList();
    }

    @DeleteMapping("/time-entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void deleteTimeEntry(@PathVariable UUID id) {
        attendanceService.deleteTimeEntry(id);
    }

    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SUPERVISOR','EMPLOYEE')")
    public List<HolidayDto> listHolidays(@RequestParam(defaultValue = "2026") int year) {
        return attendanceService.listHolidays(year).stream().map(HolidayDto::from).toList();
    }

    @PostMapping("/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public HolidayDto createHoliday(@Valid @RequestBody HolidayRequest req) {
        return HolidayDto.from(attendanceService.createHoliday(req.holidayDate(), req.name(), req.type()));
    }

    @PutMapping("/holidays/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public HolidayDto updateHoliday(@PathVariable UUID id, @Valid @RequestBody HolidayRequest req) {
        return HolidayDto.from(attendanceService.updateHoliday(id, req.holidayDate(), req.name(), req.type()));
    }

    @DeleteMapping("/holidays/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void deleteHoliday(@PathVariable UUID id) {
        attendanceService.deleteHoliday(id);
    }

    private static AttendanceService.UpsertTimeEntryCommand toCmd(TimeEntryRequest r) {
        return new AttendanceService.UpsertTimeEntryCommand(
                r.employeeId(), r.workDate(), r.timeIn(), r.timeOut(),
                r.regularHours(), r.overtimeHours(), r.nightDiffHours(),
                r.lateMinutes(), r.undertimeMinutes(), r.dayType());
    }
}
