package com.payroll.config;

import com.payroll.application.common.ClockPort;
import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.*;
import com.payroll.application.service.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationWiringConfig {

    @Bean
    AuthService authService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            TransactionPort tx,
            ClockPort clock,
            @Value("${app.jwt.refresh-ttl:P7D}") Duration refreshTtl,
            EmployeeRepository employees) {
        return new AuthService(users, refreshTokens, passwordHasher, tokenIssuer, tx, clock, refreshTtl, employees);
    }

    @Bean
    OrganizationService organizationService(DepartmentRepository departments, PositionRepository positions, TransactionPort tx) {
        return new OrganizationService(departments, positions, tx);
    }

    @Bean
    EmployeeService employeeService(EmployeeRepository employees, PayslipRepository payslips, TransactionPort tx) {
        return new EmployeeService(employees, payslips, tx);
    }

    @Bean
    AttendanceService attendanceService(TimeEntryRepository timeEntries, HolidayRepository holidays, EmployeeRepository employees, TransactionPort tx) {
        return new AttendanceService(timeEntries, holidays, employees, tx);
    }

    @Bean
    LeaveService leaveService(
            LeaveTypeRepository leaveTypes,
            LeaveBalanceRepository balances,
            LeaveRequestRepository requests,
            EmployeeRepository employees,
            TimeEntryRepository timeEntries,
            HolidayRepository holidays,
            TransactionPort tx,
            ClockPort clock) {
        return new LeaveService(leaveTypes, balances, requests, employees, timeEntries, holidays, tx, clock);
    }

    @Bean
    CompensationService compensationService(CompensationRepository repo, TransactionPort tx) {
        return new CompensationService(repo, tx);
    }

    @Bean
    PayPeriodService payPeriodService(PayPeriodRepository periods, TransactionPort tx) {
        return new PayPeriodService(periods, tx);
    }

    @Bean
    PayrollRunService payrollRunService(
            PayrollRunRepository runs,
            PayPeriodRepository periods,
            EmployeeRepository employees,
            DepartmentRepository departments,
            PositionRepository positions,
            TimeEntryRepository timeEntries,
            LeaveRequestRepository leaveRequests,
            LeaveTypeRepository leaveTypes,
            CompensationRepository compensation,
            PayslipRepository payslips,
            StatutoryTableRepository statutory,
            TransactionPort tx,
            ClockPort clock) {
        return new PayrollRunService(runs, periods, employees, departments, positions, timeEntries,
                leaveRequests, leaveTypes, compensation, payslips, statutory, tx, clock);
    }

    @Bean
    PayslipService payslipService(PayslipRepository payslips, PayPeriodRepository periods, StatutoryTableRepository statutory, PayslipPdfRenderer pdfRenderer) {
        return new PayslipService(payslips, periods, statutory, pdfRenderer);
    }

    @Bean
    StatutoryService statutoryService(StatutoryTableRepository repo, TransactionPort tx) {
        return new StatutoryService(repo, tx);
    }

    @Bean
    ApplicationRunner bootstrapAdmin(
            AuthService authService,
            @Value("${app.bootstrap.admin-email:}") String email,
            @Value("${app.bootstrap.admin-password:}") String password) {
        return args -> authService.ensureBootstrapAdmin(email, password);
    }
}
