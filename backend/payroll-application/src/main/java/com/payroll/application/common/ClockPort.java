package com.payroll.application.common;

import java.time.Instant;
import java.time.LocalDate;

public interface ClockPort {
    Instant now();
    LocalDate today();
}
