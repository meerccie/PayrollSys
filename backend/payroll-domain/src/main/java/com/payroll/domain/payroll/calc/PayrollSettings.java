package com.payroll.domain.payroll.calc;

import com.payroll.domain.payroll.StatutorySchedule;

public record PayrollSettings(int annualWorkDays, StatutorySchedule statutorySchedule) {

    public static PayrollSettings defaultPh() {
        return new PayrollSettings(261, StatutorySchedule.SPLIT);
    }
}
