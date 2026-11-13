package com.payroll.domain.user;

import com.payroll.domain.common.ValidationException;
import java.util.Objects;
import java.util.UUID;

public final class User {

    private final UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private UUID employeeId;
    private boolean active;
    private long version;

    public User(UUID id, String email, String passwordHash, Role role, UUID employeeId, boolean active, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        this.email = email.trim().toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.employeeId = employeeId;
        this.active = active;
        this.version = version;
    }

    public static User create(String email, String passwordHash, Role role, UUID employeeId) {
        return new User(UUID.randomUUID(), email, passwordHash, role, employeeId, true, 0L);
    }

    public void update(Role role, UUID employeeId, boolean active) {
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.employeeId = employeeId;
        this.active = active;
    }

    public void changePasswordHash(String newHash) {
        if (newHash == null || newHash.isBlank()) {
            throw new ValidationException("Password hash is required");
        }
        this.passwordHash = newHash;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UUID getEmployeeId() { return employeeId; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
