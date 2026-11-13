package com.payroll.application.common;

public record PageRequest(int page, int size, String sortBy, boolean ascending) {
    public PageRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 20;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, true);
    }
}
