package com.payroll.domain.compensation;

import com.payroll.domain.common.ValidationException;
import java.util.Objects;
import java.util.UUID;

public final class DeductionType {

    private final UUID id;
    private String code;
    private String name;
    private boolean recurringAllowed;
    private boolean loanType;
    private boolean active;
    private long version;

    public DeductionType(UUID id, String code, String name, boolean recurringAllowed, boolean loanType, boolean active, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (code == null || code.isBlank()) throw new ValidationException("Deduction code is required");
        if (name == null || name.isBlank()) throw new ValidationException("Deduction name is required");
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.recurringAllowed = recurringAllowed;
        this.loanType = loanType;
        this.active = active;
        this.version = version;
    }

    public static DeductionType create(String code, String name, boolean recurringAllowed, boolean loanType) {
        return new DeductionType(UUID.randomUUID(), code, name, recurringAllowed, loanType, true, 0L);
    }

    public void update(String code, String name, boolean recurringAllowed, boolean loanType, boolean active) {
        if (code == null || code.isBlank()) throw new ValidationException("Deduction code is required");
        if (name == null || name.isBlank()) throw new ValidationException("Deduction name is required");
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.recurringAllowed = recurringAllowed;
        this.loanType = loanType;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isRecurringAllowed() { return recurringAllowed; }
    public boolean isLoanType() { return loanType; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
