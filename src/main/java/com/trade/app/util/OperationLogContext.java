package com.trade.app.util;

/**
 * Thread-local operation context to allow nested calls to reuse the same operationId.
 */
public final class OperationLogContext {

    private static final ThreadLocal<String> OPERATION_ID = new ThreadLocal<>();

    private OperationLogContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String getOperationId() {
        return OPERATION_ID.get();
    }

    public static void setOperationId(String operationId) {
        OPERATION_ID.set(operationId);
    }

    public static void clear() {
        OPERATION_ID.remove();
    }
}
