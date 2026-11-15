package com.payroll.application.service;

import com.payroll.application.common.ClockPort;
import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.EmployeeRepository;
import com.payroll.application.ports.out.PasswordHasher;
import com.payroll.application.ports.out.RefreshTokenRepository;
import com.payroll.application.ports.out.TokenIssuer;
import com.payroll.application.ports.out.UserRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.ValidationException;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.user.Role;
import com.payroll.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public class AuthService {

    public record AuthResult(User user, String accessToken, Instant accessExpiresAt, String refreshToken, Instant refreshExpiresAt) {}

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final TransactionPort tx;
    private final ClockPort clock;
    private final Duration refreshTtl;
    private final EmployeeRepository employees;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            TransactionPort tx,
            ClockPort clock,
            Duration refreshTtl) {
        this(users, refreshTokens, passwordHasher, tokenIssuer, tx, clock, refreshTtl, null);
    }

    public AuthService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            TransactionPort tx,
            ClockPort clock,
            Duration refreshTtl,
            EmployeeRepository employees) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.tx = tx;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
        this.employees = employees;
    }

    public AuthResult login(String email, String rawPassword) {
        return tx.execute(() -> {
            User user = users.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new ValidationException("Invalid email or password"));
            if (!user.isActive() || !passwordHasher.matches(rawPassword, user.getPasswordHash())) {
                throw new ValidationException("Invalid email or password");
            }
            return issueTokens(syncEmployeeLink(user));
        });
    }

    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ValidationException("Refresh token is missing");
        }
        return tx.execute(() -> {
            String hash = sha256(rawRefreshToken);
            var stored = refreshTokens.findByTokenHash(hash)
                    .orElseThrow(() -> new ValidationException("Invalid refresh token"));
            if (stored.revoked() || stored.expiresAt().isBefore(clock.now())) {
                throw new ValidationException("Refresh token expired or revoked");
            }
            refreshTokens.revokeByTokenHash(hash);
            User user = users.findById(stored.userId())
                    .orElseThrow(() -> new EntityNotFoundException("User", stored.userId()));
            if (!user.isActive()) {
                throw new ValidationException("User account is inactive");
            }
            return issueTokens(syncEmployeeLink(user));
        });
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        tx.executeVoid(() -> refreshTokens.revokeByTokenHash(sha256(rawRefreshToken)));
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        tx.executeVoid(() -> {
            User user = users.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User", userId));
            if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
                throw new ValidationException("Current password is incorrect");
            }
            if (newPassword == null || newPassword.length() < 8) {
                throw new ValidationException("New password must be at least 8 characters");
            }
            user.changePasswordHash(passwordHasher.hash(newPassword));
            users.save(user);
            refreshTokens.revokeAllForUser(userId);
        });
    }

    public User getUser(UUID userId) {
        return tx.execute(() -> {
            User user = users.findById(userId).orElseThrow(() -> new EntityNotFoundException("User", userId));
            return syncEmployeeLink(user);
        });
    }

    public UUID resolveEmployeeId(UUID userId, UUID tokenEmployeeId) {
        if (tokenEmployeeId != null) {
            return tokenEmployeeId;
        }
        return getUser(userId).getEmployeeId();
    }

    public List<User> listUsers() {
        return users.findAll();
    }

    public User createUser(String email, String rawPassword, Role role, UUID employeeId) {
        return tx.execute(() -> {
            String normalized = email.trim().toLowerCase();
            if (users.findByEmail(normalized).isPresent()) {
                throw new ValidationException("Email already exists: " + normalized);
            }
            if (rawPassword == null || rawPassword.length() < 8) {
                throw new ValidationException("Password must be at least 8 characters");
            }
            UUID resolvedEmployeeId = employeeId;
            if (resolvedEmployeeId == null && employees != null) {
                resolvedEmployeeId = employees.findByEmail(normalized).map(Employee::getId).orElse(null);
            }
            User user = User.create(normalized, passwordHasher.hash(rawPassword), role, resolvedEmployeeId);
            return users.save(user);
        });
    }

    private User syncEmployeeLink(User user) {
        if (user.getEmployeeId() == null && employees != null) {
            UUID matched = employees.findByEmail(user.getEmail()).map(Employee::getId).orElse(null);
            if (matched != null) {
                user.update(user.getRole(), matched, user.isActive());
                return users.save(user);
            }
        }
        return user;
    }

    public User updateUser(UUID id, Role role, UUID employeeId, boolean active) {
        return tx.execute(() -> {
            User user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("User", id));
            user.update(role, employeeId, active);
            return users.save(user);
        });
    }

    public void deleteUser(UUID id) {
        tx.executeVoid(() -> {
            users.findById(id).orElseThrow(() -> new EntityNotFoundException("User", id));
            users.deleteById(id);
        });
    }

    public void ensureBootstrapAdmin(String email, String rawPassword) {
        tx.executeVoid(() -> {
            if (users.count() == 0 && email != null && !email.isBlank() && rawPassword != null && !rawPassword.isBlank()) {
                User admin = User.create(email, passwordHasher.hash(rawPassword), Role.ADMIN, null);
                users.save(admin);
            }
        });
    }

    private AuthResult issueTokens(User user) {
        TokenIssuer.IssuedAccessToken access = tokenIssuer.issueAccessToken(user);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant refreshExp = clock.now().plus(refreshTtl);
        refreshTokens.save(new RefreshTokenRepository.StoredRefreshToken(
                UUID.randomUUID(), user.getId(), sha256(rawRefresh), refreshExp, false));
        return new AuthResult(user, access.token(), access.expiresAt(), rawRefresh, refreshExp);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
