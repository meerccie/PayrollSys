package com.payroll.domain.employee;

import com.payroll.domain.common.InvalidStateTransitionException;
import com.payroll.domain.common.Money;
import com.payroll.domain.common.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Employee {

    private final UUID id;
    private String employeeNo;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String address;
    private String tin;
    private String sssNo;
    private String philHealthNo;
    private String pagIbigNo;
    private LocalDate hireDate;
    private LocalDate separationDate;
    private EmploymentStatus status;
    private PayBasis payBasis;
    private Money baseRate;
    private UUID departmentId;
    private UUID positionId;
    private BankAccount bankAccount;
    private Set<DayOfWeek> restDays;
    private long version;

    public Employee(
            UUID id,
            String employeeNo,
            String firstName,
            String middleName,
            String lastName,
            LocalDate birthDate,
            String email,
            String phone,
            String address,
            String tin,
            String sssNo,
            String philHealthNo,
            String pagIbigNo,
            LocalDate hireDate,
            LocalDate separationDate,
            EmploymentStatus status,
            PayBasis payBasis,
            Money baseRate,
            UUID departmentId,
            UUID positionId,
            BankAccount bankAccount,
            Set<DayOfWeek> restDays,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeNo = requireNonBlank(employeeNo, "Employee number");
        this.firstName = requireNonBlank(firstName, "First name");
        this.middleName = middleName == null ? null : middleName.trim();
        this.lastName = requireNonBlank(lastName, "Last name");
        this.birthDate = birthDate;
        this.email = requireNonBlank(email, "Email").toLowerCase();
        this.phone = phone;
        this.address = address;
        this.tin = tin;
        this.sssNo = sssNo;
        this.philHealthNo = philHealthNo;
        this.pagIbigNo = pagIbigNo;
        this.hireDate = Objects.requireNonNull(hireDate, "hireDate must not be null");
        this.separationDate = separationDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.payBasis = Objects.requireNonNull(payBasis, "payBasis must not be null");
        this.baseRate = validateRate(baseRate);
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.bankAccount = bankAccount;
        this.restDays = restDays == null || restDays.isEmpty()
                ? EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                : EnumSet.copyOf(restDays);
        this.version = version;
    }

    public static Employee create(
            String employeeNo,
            String firstName,
            String middleName,
            String lastName,
            LocalDate birthDate,
            String email,
            String phone,
            String address,
            String tin,
            String sssNo,
            String philHealthNo,
            String pagIbigNo,
            LocalDate hireDate,
            EmploymentStatus status,
            PayBasis payBasis,
            Money baseRate,
            UUID departmentId,
            UUID positionId,
            BankAccount bankAccount,
            Set<DayOfWeek> restDays) {
        if (status == EmploymentStatus.SEPARATED) {
            throw new ValidationException("Cannot create an already separated employee");
        }
        return new Employee(
                UUID.randomUUID(), employeeNo, firstName, middleName, lastName, birthDate,
                email, phone, address, tin, sssNo, philHealthNo, pagIbigNo,
                hireDate, null, status, payBasis, baseRate, departmentId, positionId,
                bankAccount, restDays, 0L);
    }

    public void updateProfile(
            String firstName,
            String middleName,
            String lastName,
            LocalDate birthDate,
            String email,
            String phone,
            String address,
            String tin,
            String sssNo,
            String philHealthNo,
            String pagIbigNo,
            EmploymentStatus status,
            PayBasis payBasis,
            Money baseRate,
            UUID departmentId,
            UUID positionId,
            BankAccount bankAccount,
            Set<DayOfWeek> restDays) {
        if (this.status == EmploymentStatus.SEPARATED) {
            throw new InvalidStateTransitionException("Cannot update a separated employee");
        }
        if (status == EmploymentStatus.SEPARATED) {
            throw new InvalidStateTransitionException("Use separate() to separate an employee");
        }
        this.firstName = requireNonBlank(firstName, "First name");
        this.middleName = middleName == null ? null : middleName.trim();
        this.lastName = requireNonBlank(lastName, "Last name");
        this.birthDate = birthDate;
        this.email = requireNonBlank(email, "Email").toLowerCase();
        this.phone = phone;
        this.address = address;
        this.tin = tin;
        this.sssNo = sssNo;
        this.philHealthNo = philHealthNo;
        this.pagIbigNo = pagIbigNo;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.payBasis = Objects.requireNonNull(payBasis, "payBasis must not be null");
        this.baseRate = validateRate(baseRate);
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.bankAccount = bankAccount;
        if (restDays != null && !restDays.isEmpty()) {
            this.restDays = EnumSet.copyOf(restDays);
        }
    }

    public void separate(LocalDate separationDate) {
        Objects.requireNonNull(separationDate, "separationDate must not be null");
        if (this.status == EmploymentStatus.SEPARATED) {
            throw new InvalidStateTransitionException("Employee is already separated");
        }
        if (separationDate.isBefore(hireDate)) {
            throw new ValidationException("Separation date cannot be before hire date");
        }
        this.status = EmploymentStatus.SEPARATED;
        this.separationDate = separationDate;
    }

    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(hireDate)) {
            return false;
        }
        return separationDate == null || !date.isAfter(separationDate);
    }

    public boolean isRestDay(DayOfWeek dayOfWeek) {
        return restDays.contains(dayOfWeek);
    }

    /**
     * Daily rate: for DAILY pay basis, returns baseRate; for MONTHLY, (baseRate * 12) / annualWorkDays.
     */
    public Money dailyRate(int annualWorkDays) {
        if (payBasis == PayBasis.DAILY) {
            return baseRate;
        }
        BigDecimal daily = baseRate.amount()
                .multiply(BigDecimal.valueOf(12))
                .divide(BigDecimal.valueOf(annualWorkDays), 4, RoundingMode.HALF_UP);
        return Money.of(daily);
    }

    /**
     * Hourly rate based on an 8-hour workday.
     */
    public Money hourlyRate(int annualWorkDays) {
        BigDecimal daily = payBasis == PayBasis.DAILY
                ? baseRate.amount()
                : baseRate.amount().multiply(BigDecimal.valueOf(12))
                        .divide(BigDecimal.valueOf(annualWorkDays), 6, RoundingMode.HALF_UP);
        return Money.of(daily.divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP));
    }

    /**
     * Monthly equivalent compensation used as the basis for statutory contributions.
     */
    public Money monthlyEquivalent(int annualWorkDays) {
        if (payBasis == PayBasis.MONTHLY) {
            return baseRate;
        }
        BigDecimal monthly = baseRate.amount()
                .multiply(BigDecimal.valueOf(annualWorkDays))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        return Money.of(monthly);
    }

    public String fullName() {
        if (middleName == null || middleName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName + " " + middleName + " " + lastName;
    }

    private static Money validateRate(Money rate) {
        Objects.requireNonNull(rate, "baseRate must not be null");
        if (!rate.isPositive()) {
            throw new ValidationException("Base rate must be positive");
        }
        return rate;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getEmployeeNo() { return employeeNo; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getTin() { return tin; }
    public String getSssNo() { return sssNo; }
    public String getPhilHealthNo() { return philHealthNo; }
    public String getPagIbigNo() { return pagIbigNo; }
    public LocalDate getHireDate() { return hireDate; }
    public LocalDate getSeparationDate() { return separationDate; }
    public EmploymentStatus getStatus() { return status; }
    public PayBasis getPayBasis() { return payBasis; }
    public Money getBaseRate() { return baseRate; }
    public UUID getDepartmentId() { return departmentId; }
    public UUID getPositionId() { return positionId; }
    public BankAccount getBankAccount() { return bankAccount; }
    public Set<DayOfWeek> getRestDays() { return Set.copyOf(restDays); }
    public long getVersion() { return version; }
}
