package com.payroll.application.service;

import com.payroll.application.common.PageRequest;
import com.payroll.application.common.PageResult;
import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.EmployeeRepository;
import com.payroll.application.ports.out.PayslipRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.Money;
import com.payroll.domain.common.ValidationException;
import com.payroll.domain.employee.BankAccount;
import com.payroll.domain.employee.Employee;
import com.payroll.domain.employee.EmploymentStatus;
import com.payroll.domain.employee.PayBasis;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class EmployeeService {

    public record CreateEmployeeCommand(
            String employeeNo, String firstName, String middleName, String lastName,
            LocalDate birthDate, String email, String phone, String address,
            String tin, String sssNo, String philHealthNo, String pagIbigNo,
            LocalDate hireDate, EmploymentStatus status, PayBasis payBasis, Money baseRate,
            UUID departmentId, UUID positionId, BankAccount bankAccount, Set<DayOfWeek> restDays) {}

    public record UpdateEmployeeCommand(
            String firstName, String middleName, String lastName,
            LocalDate birthDate, String email, String phone, String address,
            String tin, String sssNo, String philHealthNo, String pagIbigNo,
            EmploymentStatus status, PayBasis payBasis, Money baseRate,
            UUID departmentId, UUID positionId, BankAccount bankAccount, Set<DayOfWeek> restDays) {}

    private final EmployeeRepository employees;
    private final PayslipRepository payslips;
    private final TransactionPort tx;

    public EmployeeService(EmployeeRepository employees, PayslipRepository payslips, TransactionPort tx) {
        this.employees = employees;
        this.payslips = payslips;
        this.tx = tx;
    }

    public Employee create(CreateEmployeeCommand cmd) {
        return tx.execute(() -> {
            if (employees.findByEmployeeNo(cmd.employeeNo().trim()).isPresent()) {
                throw new ValidationException("Employee number already exists: " + cmd.employeeNo());
            }
            if (employees.findByEmail(cmd.email().trim().toLowerCase()).isPresent()) {
                throw new ValidationException("Employee email already exists: " + cmd.email());
            }
            Employee emp = Employee.create(
                    cmd.employeeNo(), cmd.firstName(), cmd.middleName(), cmd.lastName(),
                    cmd.birthDate(), cmd.email(), cmd.phone(), cmd.address(),
                    cmd.tin(), cmd.sssNo(), cmd.philHealthNo(), cmd.pagIbigNo(),
                    cmd.hireDate(), cmd.status(), cmd.payBasis(), cmd.baseRate(),
                    cmd.departmentId(), cmd.positionId(), cmd.bankAccount(), cmd.restDays());
            return employees.save(emp);
        });
    }

    public Employee update(UUID id, UpdateEmployeeCommand cmd) {
        return tx.execute(() -> {
            Employee emp = get(id);
            emp.updateProfile(
                    cmd.firstName(), cmd.middleName(), cmd.lastName(),
                    cmd.birthDate(), cmd.email(), cmd.phone(), cmd.address(),
                    cmd.tin(), cmd.sssNo(), cmd.philHealthNo(), cmd.pagIbigNo(),
                    cmd.status(), cmd.payBasis(), cmd.baseRate(),
                    cmd.departmentId(), cmd.positionId(), cmd.bankAccount(), cmd.restDays());
            return employees.save(emp);
        });
    }

    public Employee separate(UUID id, LocalDate separationDate) {
        return tx.execute(() -> {
            Employee emp = get(id);
            emp.separate(separationDate);
            return employees.save(emp);
        });
    }

    public Employee get(UUID id) {
        return employees.findById(id).orElseThrow(() -> new EntityNotFoundException("Employee", id));
    }

    public PageResult<Employee> search(String query, UUID departmentId, EmploymentStatus status, PageRequest pageRequest) {
        return employees.search(query, departmentId, status, pageRequest);
    }

    public void delete(UUID id) {
        tx.executeVoid(() -> {
            get(id);
            if (payslips.existsByEmployeeId(id)) {
                throw new ValidationException("Cannot delete employee with existing payslips; use separate instead");
            }
            employees.deleteById(id);
        });
    }
}
