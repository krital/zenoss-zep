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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.zenoss.protobufs.JsonFormat;
import org.zenoss.protobufs.zep.Zep.ZepConfig;
import org.zenoss.protobufs.zep.Zep.ZepConfig.Builder;
import org.zenoss.zep.Messages;
import org.zenoss.zep.ZepException;
import org.zenoss.zep.dao.ConfigDao;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zenoss.zep.dao.impl.EventConstants.*;

public class ConfigDaoImpl implements ConfigDao {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDao.class);
    private final SimpleJdbcTemplate template;
    private Messages messages;
    private static final int MAX_PARTITIONS = 1000;
    private final int maxEventArchivePurgeIntervalDays;
    private final int maxEventOccurrenceDays;
    private int maxEventArchiveIntervalDays = 30;

    private static final String COLUMN_CONFIG_NAME = "config_name";
    private static final String COLUMN_CONFIG_VALUE = "config_value";

    public ConfigDaoImpl(DataSource ds, PartitionConfig partitionConfig) {
        this.template = new SimpleJdbcTemplate(ds);

        this.maxEventArchivePurgeIntervalDays = calculateMaximumDays(partitionConfig.getConfig(TABLE_EVENT_ARCHIVE));
        logger.info("Maximum archive days: {}", maxEventArchivePurgeIntervalDays);

        this.maxEventOccurrenceDays = calculateMaximumDays(partitionConfig.getConfig(TABLE_EVENT));
        logger.info("Maximum occurrence days: {}", maxEventOccurrenceDays);
    }

    private static int calculateMaximumDays(PartitionTableConfig config) {
        long partitionRange = config.getPartitionUnit().toMinutes(config.getPartitionDuration())
                * MAX_PARTITIONS;
        return (int) TimeUnit.DAYS.convert(partitionRange, TimeUnit.MINUTES);
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    public void setMaxEventArchiveIntervalDays(int maxEventArchiveIntervalDays) {
        this.maxEventArchiveIntervalDays = maxEventArchiveIntervalDays;
    }

    @Override
    @Transactional(readOnly = true)
    public ZepConfig getConfig() throws ZepException {
        try {
            final String sql = "SELECT * FROM config";
            ZepConfig config = template.getJdbcOperations().query(sql, new ZepConfigExtractor());
            validateConfig(config);
            return config;
        } catch (DataAccessException e) {
            throw new ZepException(e);
        }
    }
    
    @Override
    @Transactional
    public void setConfig(ZepConfig config) throws ZepException {
        try {
            validateConfig(config);
            final String sql = "INSERT INTO config (config_name, config_value) VALUES(?, ?) " +
                    "ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)";
            final List<Object[]> records = configToRecords(config);
            if (!records.isEmpty()) {
                this.template.batchUpdate(sql, records);
            }
        } catch (DataAccessException e) {
            throw new ZepException(e);
        }
    }

    @Override
    @Transactional
    public int removeConfigValue(String name) throws ZepException {
        try {
            final String sql = "DELETE FROM config WHERE config_name=:config_name";
            return this.template.update(sql, Collections.singletonMap(COLUMN_CONFIG_NAME, name));
        } catch (DataAccessException e) {
            throw new ZepException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    @Transactional
    public void setConfigValue(String name, ZepConfig config) throws ZepException {
        try {
            validateConfig(config);
            FieldDescriptor field = ZepConfig.getDescriptor().findFieldByName(name);
            if (field == null) {
                throw new ZepException("Invalid field name: " + name);
            }
            Object value = config.getField(field);
            if (value == null) {
                removeConfigValue(name);
            }
            final Map<String,String> fields = new HashMap<String,String>();
            fields.put(COLUMN_CONFIG_NAME, name);
            fields.put(COLUMN_CONFIG_VALUE, valueToString(field, value));
            final String sql = "INSERT INTO config (config_name, config_value) VALUES(:config_name, :config_value) " +
                    "ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)";
            this.template.update(sql, fields);
        } catch (DataAccessException e) {
            throw new ZepException(e.getLocalizedMessage(), e);
        }
    }

    private void validateConfig(ZepConfig config) throws ZepException {
        int ageIntervalMinutes = config.getEventAgeIntervalMinutes();
        if (ageIntervalMinutes < 0) {
            throw new ZepException(messages.getMessage("invalid_event_age_interval"));
        }

        int eventArchivePurgeIntervalDays = config.getEventArchivePurgeIntervalDays();
        if (eventArchivePurgeIntervalDays < 1 || eventArchivePurgeIntervalDays > maxEventArchivePurgeIntervalDays) {
            throw new ZepException(messages.getMessage("invalid_event_archive_purge_interval", 1,
                    maxEventArchivePurgeIntervalDays));
        }

        int eventOccurrencePurgeIntervalDays = config.getEventOccurrencePurgeIntervalDays();
        if (eventOccurrencePurgeIntervalDays < 1 || eventOccurrencePurgeIntervalDays > maxEventOccurrenceDays) {
            throw new ZepException(messages.getMessage("invalid_event_occurrence_purge_interval", 1,
                    maxEventOccurrenceDays));
        }

        int eventArchiveIntervalDays = config.getEventArchiveIntervalDays();
        if (eventArchiveIntervalDays < 1 || eventArchiveIntervalDays > maxEventArchiveIntervalDays) {
            throw new ZepException(messages.getMessage("invalid_event_archive_interval", 1,
                    maxEventArchiveIntervalDays));
        }
    }

    private static List<Object[]> configToRecords(ZepConfig config) throws ZepException {
        final List<Object[]> records = new ArrayList<Object[]>();
        final Descriptor descriptor = config.getDescriptorForType();
        for (FieldDescriptor field : descriptor.getFields()) {
            final Object value = config.getField(field);
            if (value != null) {
                records.add(new Object[] { field.getName(), valueToString(field, value) });
            }
        }
        return records;
    }

    private static String valueToString(FieldDescriptor field, Object value) throws ZepException {
        if (field.isRepeated()) {
            throw new ZepException("Repeated field not supported");
        }
        switch (field.getJavaType()) {
            case BOOLEAN:
                return Boolean.toString((Boolean)value);
            case BYTE_STRING:
                return new String(Base64.encodeBase64(((ByteString)value).toByteArray()), Charset.forName("US-ASCII"));
            case DOUBLE:
                return Double.toString((Double) value);
            case ENUM:
                return Integer.toString(((EnumValueDescriptor) value).getNumber());
            case FLOAT:
                return Float.toString((Float) value);
            case INT:
                return Integer.toString((Integer) value);
            case LONG:
                return Long.toString((Long) value);
            case MESSAGE:
                try {
                    return JsonFormat.writeAsString((Message) value);
                } catch (IOException e) {
                    throw new ZepException(e.getLocalizedMessage(), e);
                }
            case STRING:
                return (String) value;
            default:
                throw new ZepException("Unsupported type: " + field.getType());
        }
    }

    private static Object valueFromString(FieldDescriptor field, Builder builder, String strValue) throws ZepException {
        if (field.isRepeated()) {
            throw new ZepException("Repeated field not supported");
        }
        switch (field.getJavaType()) {
            case BOOLEAN:
                return Boolean.valueOf(strValue);
            case BYTE_STRING:
                return ByteString.copyFrom(Base64.decodeBase64(strValue.getBytes()));
            case DOUBLE:
                return Double.valueOf(strValue);
            case ENUM:
                return field.getEnumType().findValueByNumber(Integer.valueOf(strValue));
            case FLOAT:
                return Float.valueOf(strValue);
            case INT:
                return Integer.valueOf(strValue);
            case LONG:
                return Long.valueOf(strValue);
            case MESSAGE:
                try {
                    return JsonFormat.merge(strValue, builder.newBuilderForField(field));
                } catch (IOException e) {
                    throw new ZepException(e.getLocalizedMessage(), e);
                }
            case STRING:
                return strValue;
            default:
                throw new ZepException("Unsupported type: " + field.getType());
        }
    }

    private static final class ZepConfigExtractor implements ResultSetExtractor<ZepConfig> {
        @Override
        public ZepConfig extractData(ResultSet rs) throws SQLException, DataAccessException {
            Descriptor descriptor = ZepConfig.getDescriptor();
            Builder configBuilder = ZepConfig.newBuilder();
            while (rs.next()) {
                final String fieldName = rs.getString(COLUMN_CONFIG_NAME);
                final String fieldValueStr = rs.getString(COLUMN_CONFIG_VALUE);
                FieldDescriptor field = descriptor.findFieldByName(fieldName);
                if (field == null) {
                    logger.warn("Unrecognized field: {}", fieldName);
                }
                else {
                    try {
                        Object fieldValue = valueFromString(field, configBuilder, fieldValueStr);
                        configBuilder.setField(field, fieldValue);
                    } catch (ZepException e) {
                        throw new SQLException(e.getLocalizedMessage(), e);
                    }
                }
            }
            return configBuilder.build();
        }
    }
}
