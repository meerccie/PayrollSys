package com.payroll.adapters.in.web;

import com.payroll.application.service.OrganizationService;
import com.payroll.domain.organization.Department;
import com.payroll.domain.organization.Position;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrganizationController {

    private final OrganizationService orgService;

    public OrganizationController(OrganizationService orgService) {
        this.orgService = orgService;
    }

    public record OrgCreateRequest(@NotBlank String code, @NotBlank String name) {}
    public record OrgUpdateRequest(@NotBlank String code, @NotBlank String name, boolean active) {}
    public record DepartmentDto(UUID id, String code, String name, boolean active, long version) {
        static DepartmentDto from(Department d) { return new DepartmentDto(d.getId(), d.getCode(), d.getName(), d.isActive(), d.getVersion()); }
    }
    public record PositionDto(UUID id, String code, String name, boolean active, long version) {
        static PositionDto from(Position p) { return new PositionDto(p.getId(), p.getCode(), p.getName(), p.isActive(), p.getVersion()); }
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<DepartmentDto> listDepartments() {
        return orgService.listDepartments().stream().map(DepartmentDto::from).toList();
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DepartmentDto createDepartment(@Valid @RequestBody OrgCreateRequest req) {
        return DepartmentDto.from(orgService.createDepartment(req.code(), req.name()));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DepartmentDto updateDepartment(@PathVariable UUID id, @Valid @RequestBody OrgUpdateRequest req) {
        return DepartmentDto.from(orgService.updateDepartment(id, req.code(), req.name(), req.active()));
    }

    @DeleteMapping("/departments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void deleteDepartment(@PathVariable UUID id) {
        orgService.deleteDepartment(id);
    }

    @GetMapping("/positions")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public List<PositionDto> listPositions() {
        return orgService.listPositions().stream().map(PositionDto::from).toList();
    }

    @PostMapping("/positions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PositionDto createPosition(@Valid @RequestBody OrgCreateRequest req) {
        return PositionDto.from(orgService.createPosition(req.code(), req.name()));
    }

    @PutMapping("/positions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PositionDto updatePosition(@PathVariable UUID id, @Valid @RequestBody OrgUpdateRequest req) {
        return PositionDto.from(orgService.updatePosition(id, req.code(), req.name(), req.active()));
    }

    @DeleteMapping("/positions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void deletePosition(@PathVariable UUID id) {
        orgService.deletePosition(id);
    }
}
