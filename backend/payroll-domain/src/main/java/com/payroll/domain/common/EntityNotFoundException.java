package com.payroll.domain.common;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, Object id) {
        super(entityName + " not found: " + id);
    }
}
