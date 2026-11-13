package com.payroll.domain.payroll.calc;

import com.payroll.domain.attendance.DayType;
import java.math.BigDecimal;

public record PremiumRateTable(
        BigDecimal regularOvertime,
        BigDecimal restDayOrSpecialWork,
        BigDecimal restDayOrSpecialOvertime,
        BigDecimal regularHolidayWork,
        BigDecimal regularHolidayOvertime,
        BigDecimal restDaySpecialWork,
        BigDecimal restDaySpecialOvertime,
        BigDecimal restDayRegularHolidayWork,
        BigDecimal restDayRegularHolidayOvertime,
        BigDecimal nightDifferential) {

    public static PremiumRateTable standardPh() {
        return new PremiumRateTable(
                new BigDecimal("1.25"),
                new BigDecimal("1.30"),
                new BigDecimal("1.69"),
                new BigDecimal("2.00"),
                new BigDecimal("2.60"),
                new BigDecimal("1.50"),
                new BigDecimal("1.95"),
                new BigDecimal("2.60"),
                new BigDecimal("3.38"),
                new BigDecimal("0.10"));
    }

    public BigDecimal workMultiplier(DayType dayType) {
        return switch (dayType) {
            case REGULAR -> BigDecimal.ONE;
            case REST_DAY, SPECIAL_HOLIDAY -> restDayOrSpecialWork;
            case REGULAR_HOLIDAY -> regularHolidayWork;
            case REST_DAY_SPECIAL_HOLIDAY -> restDaySpecialWork;
            case REST_DAY_REGULAR_HOLIDAY -> restDayRegularHolidayWork;
        };
    }

    public BigDecimal overtimeMultiplier(DayType dayType) {
        return switch (dayType) {
            case REGULAR -> regularOvertime;
            case REST_DAY, SPECIAL_HOLIDAY -> restDayOrSpecialOvertime;
            case REGULAR_HOLIDAY -> regularHolidayOvertime;
            case REST_DAY_SPECIAL_HOLIDAY -> restDaySpecialOvertime;
            case REST_DAY_REGULAR_HOLIDAY -> restDayRegularHolidayOvertime;
        };
    }
}
