/*
 * This program is part of Zenoss Core, an open source monitoring platform.
 * Copyright (C) 2010, Zenoss Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 * 
 * For complete information please visit: http://www.zenoss.com/oss/
 */
package org.zenoss.zep.dao.impl;

import java.util.concurrent.TimeUnit;

public class PartitionTableConfig {

    private final String tableName;
    private final int partitionDuration;
    private final TimeUnit partitionUnit;
    private final int initialPastPartitions;
    private final int futurePartitions;

    public PartitionTableConfig(String tableName, int partitionDuration,
            TimeUnit partitionUnit, int initialPastPartitions,
            int futurePartitions) {
        if (tableName == null || partitionDuration <= 0
                || partitionUnit == null || initialPastPartitions < 0
                || futurePartitions < 0
                || (initialPastPartitions == 0 && futurePartitions == 0)) {
            throw new IllegalArgumentException();
        }
        this.tableName = tableName;
        this.partitionDuration = partitionDuration;
        this.partitionUnit = partitionUnit;
        this.initialPastPartitions = initialPastPartitions;
        this.futurePartitions = futurePartitions;
    }

    public String getTableName() {
        return tableName;
    }

    public int getPartitionDuration() {
        return partitionDuration;
    }

    public TimeUnit getPartitionUnit() {
        return partitionUnit;
    }

    public int getInitialPastPartitions() {
        return initialPastPartitions;
    }

    public int getFuturePartitions() {
        return futurePartitions;
    }

    @Override
    public String toString() {
        return String.format("PartitionTableConfig [tableName=%s, "
                + "partitionDuration=%s, partitionUnit=%s, "
                + "initialPastPartitions=%s, futurePartitions=%s]", tableName,
                partitionDuration, partitionUnit, initialPastPartitions,
                futurePartitions);
    }
}