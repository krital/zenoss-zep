/*****************************************************************************
 * 
 * Copyright (C) Zenoss, Inc. 2010, all rights reserved.
 * 
 * This content is made available according to terms specified in
 * License.zenoss under the directory where your Zenoss product is installed.
 * 
 ****************************************************************************/


package org.zenoss.zep.dao.impl;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class PartitionConfig {
    public static final String PARTITION_PREFIX = "partition.";
    
    private final Properties partitionConfig;

    public PartitionConfig(Properties partitioningProperties) {
        this.partitionConfig = partitioningProperties;
    }

    public PartitionTableConfig getConfig(String tableName) {
        int duration = Integer.valueOf(this.partitionConfig.getProperty(PARTITION_PREFIX + tableName + ".duration"));
        TimeUnit unit = TimeUnit.valueOf(this.partitionConfig.getProperty(PARTITION_PREFIX + tableName + ".unit"));
        int initialPastPartitions = Integer.valueOf(
                this.partitionConfig.getProperty(PARTITION_PREFIX + tableName + ".initial_past_partitions"));
        int futurePartitions = Integer.valueOf(
                this.partitionConfig.getProperty(PARTITION_PREFIX + tableName + ".future_partitions"));
        return new PartitionTableConfig(tableName, duration, unit, initialPastPartitions, futurePartitions);
    }
}
