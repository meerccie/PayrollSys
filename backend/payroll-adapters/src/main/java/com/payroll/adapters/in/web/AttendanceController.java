package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.application.service.AttendanceService;
import com.payroll.domain.attendance.DayType;
import com.payroll.domain.attendance.Holiday;
import com.payroll.domain.attendance.HolidayType;
import com.payroll.domain.attendance.TimeEntry;
import com.payroll.domain.attendance.TimeEntryStatus;
import com.payroll.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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
            int lateMinutes, int undertimeMinutes, DayType dayType, TimeEntryStatus status) {
        static TimeEntryDto from(TimeEntry e) {
            return new TimeEntryDto(e.getId(), e.getEmployeeId(), e.getWorkDate(), e.getTimeIn(), e.getTimeOut(),
                    e.getRegularHours(), e.getOvertimeHours(), e.getNightDiffHours(),
                    e.getLateMinutes(), e.getUndertimeMinutes(), e.getDayType(), e.getStatus());
        }
    }

    public record HolidayRequest(@NotNull LocalDate holidayDate, @NotBlank String name, @NotNull HolidayType type) {}
    public record HolidayDto(UUID id, LocalDate holidayDate, String name, HolidayType type) {
        static HolidayDto from(Holiday h) { return new HolidayDto(h.getId(), h.getHolidayDate(), h.getName(), h.getType()); }
    }

    @GetMapping("/time-entries")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<TimeEntryDto> listTimeEntries(
            Authentication auth,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        AuthPrincipal p = AuthPrincipal.from(auth);
        UUID targetId = employeeId;
        if (p.role() == Role.EMPLOYEE) {
            if (p.employeeId() == null) throw new AccessDeniedException("No employee profile linked");
            targetId = p.employeeId();
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
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
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
