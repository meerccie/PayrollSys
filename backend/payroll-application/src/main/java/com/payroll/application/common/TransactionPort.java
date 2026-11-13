package com.payroll.application.common;

import java.util.function.Supplier;

public interface TransactionPort {
    <T> T execute(Supplier<T> action);

    default void executeVoid(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
