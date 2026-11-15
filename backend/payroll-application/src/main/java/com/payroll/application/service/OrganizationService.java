package com.payroll.application.service;

import com.payroll.application.common.TransactionPort;
import com.payroll.application.ports.out.DepartmentRepository;
import com.payroll.application.ports.out.PositionRepository;
import com.payroll.domain.common.EntityNotFoundException;
import com.payroll.domain.common.ValidationException;
import com.payroll.domain.organization.Department;
import com.payroll.domain.organization.Position;
import java.util.List;
import java.util.UUID;

public class OrganizationService {

    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final TransactionPort tx;

    public OrganizationService(DepartmentRepository departments, PositionRepository positions, TransactionPort tx) {
        this.departments = departments;
        this.positions = positions;
        this.tx = tx;
    }

    public List<Department> listDepartments() { return departments.findAll(); }

    public Department getDepartment(UUID id) {
        return departments.findById(id).orElseThrow(() -> new EntityNotFoundException("Department", id));
    }

    public Department createDepartment(String code, String name) {
        return tx.execute(() -> {
            String normalized = code.trim().toUpperCase();
            if (departments.findByCode(normalized).isPresent()) {
                throw new ValidationException("Department code already exists: " + normalized);
            }
            return departments.save(Department.create(normalized, name));
        });
    }

    public Department updateDepartment(UUID id, String code, String name, boolean active) {
        return tx.execute(() -> {
            Department dept = getDepartment(id);
            String normalized = code.trim().toUpperCase();
            departments.findByCode(normalized)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ValidationException("Department code already exists: " + normalized);
                    });
            dept.update(normalized, name, active);
            return departments.save(dept);
        });
    }

    public void deleteDepartment(UUID id) {
        tx.executeVoid(() -> {
            getDepartment(id);
            departments.deleteById(id);
        });
    }

    public List<Position> listPositions() { return positions.findAll(); }

    public Position getPosition(UUID id) {
        return positions.findById(id).orElseThrow(() -> new EntityNotFoundException("Position", id));
    }

    public Position createPosition(String code, String name) {
        return tx.execute(() -> {
            String normalized = code.trim().toUpperCase();
            if (positions.findByCode(normalized).isPresent()) {
                throw new ValidationException("Position code already exists: " + normalized);
            }
            return positions.save(Position.create(normalized, name));
        });
    }

    public Position updatePosition(UUID id, String code, String name, boolean active) {
        return tx.execute(() -> {
            Position pos = getPosition(id);
            String normalized = code.trim().toUpperCase();
            positions.findByCode(normalized)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ValidationException("Position code already exists: " + normalized);
                    });
            pos.update(normalized, name, active);
            return positions.save(pos);
        });
    }

    public void deletePosition(UUID id) {
        tx.executeVoid(() -> {
            getPosition(id);
            positions.deleteById(id);
        });
    }
}
