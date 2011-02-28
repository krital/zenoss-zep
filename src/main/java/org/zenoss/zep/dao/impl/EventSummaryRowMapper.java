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

import static org.zenoss.zep.dao.impl.EventConstants.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.zenoss.protobufs.JsonFormat;
import org.zenoss.protobufs.zep.Zep.EventNote;
import org.zenoss.protobufs.zep.Zep.EventStatus;
import org.zenoss.protobufs.zep.Zep.EventSummary;

public class EventSummaryRowMapper implements RowMapper<EventSummary> {
    private final EventDaoHelper helper;

    public EventSummaryRowMapper(EventDaoHelper eventDaoHelper) {
        this.helper = eventDaoHelper;
    }

    @Override
    public EventSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        final EventSummary.Builder summaryBuilder = EventSummary.newBuilder();
        summaryBuilder.addOccurrence(helper.eventMapper(rs, true));
        summaryBuilder.setUuid(DaoUtils.uuidFromBytes(rs.getBytes(COLUMN_UUID)));
        summaryBuilder.setStatus(EventStatus.valueOf(rs.getInt(COLUMN_STATUS_ID)));
        summaryBuilder.setFirstSeenTime(rs.getLong(COLUMN_FIRST_SEEN));
        summaryBuilder.setStatusChangeTime(rs.getLong(COLUMN_STATUS_CHANGE));
        summaryBuilder.setLastSeenTime(rs.getLong(COLUMN_LAST_SEEN));
        summaryBuilder.setUpdateTime(rs.getLong(COLUMN_UPDATE_TIME));
        summaryBuilder.setCount(rs.getInt(COLUMN_EVENT_COUNT));
        byte[] acknowledgedByUserUuid = rs.getBytes(COLUMN_ACKNOWLEDGED_BY_USER_UUID);
        if (acknowledgedByUserUuid != null) {
            summaryBuilder.setAcknowledgedByUserUuid(DaoUtils.uuidFromBytes(acknowledgedByUserUuid));
        }
        String acknowledgedByUserName = rs.getString(COLUMN_ACKNOWLEDGED_BY_USER_NAME);
        if (acknowledgedByUserName != null) {
            summaryBuilder.setAcknowledgedByUserName(acknowledgedByUserName);
        }
        byte[] clearedByEventUuid = rs.getBytes(COLUMN_CLEARED_BY_EVENT_UUID);
        if (clearedByEventUuid != null) {
            summaryBuilder.setClearedByEventUuid(DaoUtils.uuidFromBytes(clearedByEventUuid));
        }
        String notesJson = rs.getString(COLUMN_NOTES_JSON);
        if (notesJson != null) {
            try {
                List<EventNote> notes = JsonFormat.mergeAllDelimitedFrom("[" + notesJson + "]",
                        EventNote.getDefaultInstance());
                summaryBuilder.addAllNotes(notes);
            } catch (IOException e) {
                throw new SQLException(e);
            }
        }
        return summaryBuilder.build();
    }
}