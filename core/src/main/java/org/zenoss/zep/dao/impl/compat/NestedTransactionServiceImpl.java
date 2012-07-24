/*****************************************************************************
 * 
 * Copyright (C) Zenoss, Inc. 2011, all rights reserved.
 * 
 * This content is made available according to terms specified in
 * License.zenoss under the directory where your Zenoss product is installed.
 * 
 ****************************************************************************/


package org.zenoss.zep.dao.impl.compat;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Implemented of NestedTransactionService.
 */
public class NestedTransactionServiceImpl implements NestedTransactionService {

    private final SimpleJdbcTemplate template;
    private final NestedTransactionContext context;

    public NestedTransactionServiceImpl(DataSource dataSource) {
        this.template = new SimpleJdbcTemplate(dataSource);
        this.context = new NestedTransactionContext() {
            @Override
            public SimpleJdbcTemplate getSimpleJdbcTemplate() {
                return template;
            }
        };
    }

    @Override
    @Transactional(propagation = Propagation.NESTED)
    public <T> T executeInNestedTransaction(final NestedTransactionCallback<T> callback) throws DataAccessException {
        return callback.doInNestedTransaction(this.context);
    }
}
