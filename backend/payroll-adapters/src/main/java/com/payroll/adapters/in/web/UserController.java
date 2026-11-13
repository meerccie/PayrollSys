package com.payroll.adapters.in.web;

import com.payroll.application.service.AuthService;
import com.payroll.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotNull Role role,
            UUID employeeId) {}

    public record UpdateUserRequest(
            @NotNull Role role,
            UUID employeeId,
            boolean active) {}

    @GetMapping
    public List<AuthController.UserDto> list() {
        return authService.listUsers().stream().map(AuthController.UserDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthController.UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return AuthController.UserDto.from(
                authService.createUser(req.email(), req.password(), req.role(), req.employeeId()));
    }

    @PutMapping("/{id}")
    public AuthController.UserDto update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return AuthController.UserDto.from(authService.updateUser(id, req.role(), req.employeeId(), req.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        authService.deleteUser(id);
    }
}
