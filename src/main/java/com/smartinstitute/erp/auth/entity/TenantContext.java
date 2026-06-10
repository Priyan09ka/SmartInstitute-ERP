package com.smartinstitute.erp.auth.entity;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(Long instituteId) {
        CURRENT_TENANT.set(instituteId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}