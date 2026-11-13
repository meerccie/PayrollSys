package com.payroll.application.service;

import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.PayPeriodRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.ValidationException;
import com.payroll.domain.payroll.CutoffType;
import com.payroll.domain.payroll.PayFrequency;
import com.payroll.domain.payroll.PayPeriod;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PayPeriodService {

    private final PayPeriodRepository periods;
    private final TransactionPort tx;

    public PayPeriodService(PayPeriodRepository periods, TransactionPort tx) {
        this.periods = periods;
        this.tx = tx;
    }

    public List<PayPeriod> listAll() {
        return periods.findAll();
    }

    public PayPeriod get(UUID id) {
        return periods.findById(id).orElseThrow(() -> new EntityNotFoundException("PayPeriod", id));
    }

    public List<PayPeriod> generateForYear(int year, PayFrequency frequency) {
        return tx.execute(() -> {
            List<PayPeriod> existing = periods.findByYearAndFrequency(year, frequency);
            if (!existing.isEmpty()) {
                throw new ValidationException("Pay periods already generated for " + year + " (" + frequency + ")");
            }
            List<PayPeriod> generated = new ArrayList<>();
            int num = 1;
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                LocalDate first = ym.atDay(1);
                LocalDate last = ym.atEndOfMonth();
                if (frequency == PayFrequency.MONTHLY) {
                    generated.add(PayPeriod.create(first, last, last, frequency, CutoffType.FULL_MONTH, year, num++));
                } else {
                    LocalDate mid = ym.atDay(15);
                    LocalDate sixteenth = ym.atDay(16);
                    generated.add(PayPeriod.create(first, mid, mid, frequency, CutoffType.FIRST, year, num++));
                    generated.add(PayPeriod.create(sixteenth, last, last, frequency, CutoffType.SECOND, year, num++));
                }
            }
            return periods.saveAll(generated);
        });
    }

    public void delete(UUID id) {
        tx.executeVoid(() -> {
            PayPeriod period = get(id);
            if (period.isClosed()) {
                throw new ValidationException("Cannot delete a closed pay period");
            }
            periods.deleteById(id);
        });
    }
}
