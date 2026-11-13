package com.payroll.domain.compensation;

import com.payroll.domain.common.ValidationException;
import java.util.Objects;
import java.util.UUID;

public final class EarningType {

    private final UUID id;
    private String code;
    private String name;
    private boolean taxable;
    private boolean recurringAllowed;
    private boolean active;
    private long version;

    public EarningType(UUID id, String code, String name, boolean taxable, boolean recurringAllowed, boolean active, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (code == null || code.isBlank()) throw new ValidationException("Earning code is required");
        if (name == null || name.isBlank()) throw new ValidationException("Earning name is required");
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.taxable = taxable;
        this.recurringAllowed = recurringAllowed;
        this.active = active;
        this.version = version;
    }

    public static EarningType create(String code, String name, boolean taxable, boolean recurringAllowed) {
        return new EarningType(UUID.randomUUID(), code, name, taxable, recurringAllowed, true, 0L);
    }

    public void update(String code, String name, boolean taxable, boolean recurringAllowed, boolean active) {
        if (code == null || code.isBlank()) throw new ValidationException("Earning code is required");
        if (name == null || name.isBlank()) throw new ValidationException("Earning name is required");
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.taxable = taxable;
        this.recurringAllowed = recurringAllowed;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isTaxable() { return taxable; }
    public boolean isRecurringAllowed() { return recurringAllowed; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
