package com.payroll.domain.leave;

import com.payroll.domain.common.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public final class LeaveType {

    private final UUID id;
    private String code;
    private String name;
    private boolean paid;
    private BigDecimal annualEntitlement;
    private boolean active;
    private long version;

    public LeaveType(UUID id, String code, String name, boolean paid, BigDecimal annualEntitlement, boolean active, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (code == null || code.isBlank()) throw new ValidationException("Leave code is required");
        if (name == null || name.isBlank()) throw new ValidationException("Leave name is required");
        if (annualEntitlement == null || annualEntitlement.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Annual entitlement cannot be negative");
        }
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.paid = paid;
        this.annualEntitlement = annualEntitlement;
        this.active = active;
        this.version = version;
    }

    public static LeaveType create(String code, String name, boolean paid, BigDecimal annualEntitlement) {
        return new LeaveType(UUID.randomUUID(), code, name, paid, annualEntitlement, true, 0L);
    }

    public void update(String code, String name, boolean paid, BigDecimal annualEntitlement, boolean active) {
        if (code == null || code.isBlank()) throw new ValidationException("Leave code is required");
        if (name == null || name.isBlank()) throw new ValidationException("Leave name is required");
        if (annualEntitlement == null || annualEntitlement.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Annual entitlement cannot be negative");
        }
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.paid = paid;
        this.annualEntitlement = annualEntitlement;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isPaid() { return paid; }
    public BigDecimal getAnnualEntitlement() { return annualEntitlement; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
