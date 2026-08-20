package com.telecom.customer_service.config.datasource;

/**
 * ThreadLocal context holder to explicitly override or track datasource routing keys.
 */
public final class RoutingDataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    private RoutingDataSourceContextHolder() {}

    public static void setDataSourceType(DataSourceType dataSourceType) {
        CONTEXT.set(dataSourceType);
    }

    public static DataSourceType getDataSourceType() {
        return CONTEXT.get();
    }

    public static void clearDataSourceType() {
        CONTEXT.remove();
    }
}
