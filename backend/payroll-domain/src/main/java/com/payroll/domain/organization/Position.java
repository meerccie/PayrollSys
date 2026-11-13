package com.payroll.domain.organization;

import com.payroll.domain.common.ValidationException;
import java.util.Objects;
import java.util.UUID;

public final class Position {

    private final UUID id;
    private String code;
    private String name;
    private boolean active;
    private long version;

    public Position(UUID id, String code, String name, boolean active, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireNonBlank(code, "Position code");
        this.name = requireNonBlank(name, "Position name");
        this.active = active;
        this.version = version;
    }

    public static Position create(String code, String name) {
        return new Position(UUID.randomUUID(), code.trim().toUpperCase(), name.trim(), true, 0L);
    }

    public void update(String code, String name, boolean active) {
        this.code = requireNonBlank(code, "Position code").toUpperCase();
        this.name = requireNonBlank(name, "Position name");
        this.active = active;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
