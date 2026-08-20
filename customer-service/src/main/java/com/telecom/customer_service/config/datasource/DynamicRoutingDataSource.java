package com.telecom.customer_service.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Dynamic DataSource Router determining whether to route database queries to MASTER or REPLICA.
 * <p>
 * If a current transaction is marked @Transactional(readOnly = true), queries are routed to REPLICA.
 * Otherwise, write operations and non-read-only transactions default to MASTER.
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType explicitType = RoutingDataSourceContextHolder.getDataSourceType();
        if (explicitType != null) {
            return explicitType;
        }

        // Check if current Spring transaction is marked as read-only
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        return isReadOnly ? DataSourceType.REPLICA : DataSourceType.MASTER;
    }
}
