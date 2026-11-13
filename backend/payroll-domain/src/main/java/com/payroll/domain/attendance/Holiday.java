package com.payroll.domain.attendance;

import com.payroll.domain.common.ValidationException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Holiday {

    private final UUID id;
    private LocalDate holidayDate;
    private String name;
    private HolidayType type;
    private long version;

    public Holiday(UUID id, LocalDate holidayDate, String name, HolidayType type, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.holidayDate = Objects.requireNonNull(holidayDate, "holidayDate must not be null");
        if (name == null || name.isBlank()) {
            throw new ValidationException("Holiday name is required");
        }
        this.name = name.trim();
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.version = version;
    }

    public static Holiday create(LocalDate holidayDate, String name, HolidayType type) {
        return new Holiday(UUID.randomUUID(), holidayDate, name, type, 0L);
    }

    public void update(LocalDate holidayDate, String name, HolidayType type) {
        this.holidayDate = Objects.requireNonNull(holidayDate, "holidayDate must not be null");
        if (name == null || name.isBlank()) {
            throw new ValidationException("Holiday name is required");
        }
        this.name = name.trim();
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public UUID getId() { return id; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public String getName() { return name; }
    public HolidayType getType() { return type; }
    public long getVersion() { return version; }
}
