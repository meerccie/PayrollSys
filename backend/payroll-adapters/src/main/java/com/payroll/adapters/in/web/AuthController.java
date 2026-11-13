package com.payroll.adapters.in.web;

import com.payroll.adapters.out.security.AuthPrincipal;
import com.payroll.adapters.out.security.LoginRateLimiter;
import com.payroll.application.service.AuthService;
import com.payroll.domain.user.Role;
import com.payroll.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "payroll_refresh_token";

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            LoginRateLimiter rateLimiter,
            @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.secureCookie = secureCookie;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8) String newPassword) {}
    public record UserDto(UUID id, String email, Role role, UUID employeeId, boolean active) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getEmail(), u.getRole(), u.getEmployeeId(), u.isActive());
        }
    }
    public record AuthResponse(String accessToken, Instant expiresAt, UserDto user) {}

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String ip = httpReq.getRemoteAddr();
        if (!rateLimiter.tryConsume(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        }
        AuthService.AuthResult result = authService.login(req.email(), req.password());
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie clear = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clear.toString()).build();
    }

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.from(auth);
        return UserDto.from(authService.getUser(principal.userId()));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req) {
        AuthPrincipal principal = AuthPrincipal.from(auth);
        authService.changePassword(principal.userId(), req.currentPassword(), req.newPassword());
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthService.AuthResult result) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), result.refreshExpiresAt()).getSeconds());
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(result.accessToken(), result.accessExpiresAt(), UserDto.from(result.user())));
    }
}
