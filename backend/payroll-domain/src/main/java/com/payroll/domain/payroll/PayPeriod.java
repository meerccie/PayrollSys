package com.payroll.domain.payroll;

import com.payroll.domain.common.ValidationException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class PayPeriod {

    private final UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate payDate;
    private PayFrequency frequency;
    private CutoffType cutoff;
    private int year;
    private int periodNumber;
    private boolean closed;
    private long version;

    public PayPeriod(
            UUID id,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate payDate,
            PayFrequency frequency,
            CutoffType cutoff,
            int year,
            int periodNumber,
            boolean closed,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.payDate = Objects.requireNonNull(payDate, "payDate must not be null");
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("Pay period endDate cannot be before startDate");
        }
        this.frequency = Objects.requireNonNull(frequency, "frequency must not be null");
        this.cutoff = Objects.requireNonNull(cutoff, "cutoff must not be null");
        this.year = year;
        this.periodNumber = periodNumber;
        this.closed = closed;
        this.version = version;
    }

    public static PayPeriod create(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate payDate,
            PayFrequency frequency,
            CutoffType cutoff,
            int year,
            int periodNumber) {
        return new PayPeriod(UUID.randomUUID(), startDate, endDate, payDate, frequency, cutoff, year, periodNumber, false, 0L);
    }

    public void close() {
        this.closed = true;
    }

    public void reopen() {
        this.closed = false;
    }

    public UUID getId() { return id; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getPayDate() { return payDate; }
    public PayFrequency getFrequency() { return frequency; }
    public CutoffType getCutoff() { return cutoff; }
    public int getYear() { return year; }
    public int getPeriodNumber() { return periodNumber; }
    public boolean isClosed() { return closed; }
    public long getVersion() { return version; }
}
