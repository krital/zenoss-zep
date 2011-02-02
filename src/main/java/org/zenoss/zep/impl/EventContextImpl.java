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
package org.zenoss.zep.impl;

import java.util.HashSet;
import java.util.Set;

import org.zenoss.protobufs.zep.Zep.EventStatus;
import org.zenoss.protobufs.zep.Zep.ZepRawEvent;
import org.zenoss.zep.EventContext;

public class EventContextImpl implements EventContext {

    private EventStatus status;
    private final Set<String> clearClasses = new HashSet<String>();

    public EventContextImpl() {
        this.status = EventStatus.STATUS_NEW;
    }

    public EventContextImpl(ZepRawEvent rawEvent) {
        if (rawEvent == null) {
            throw new NullPointerException();
        }
        this.status = rawEvent.getStatus();
        this.clearClasses.addAll(rawEvent.getClearEventClassList());
    }

    @Override
    public EventStatus getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(EventStatus status) {
        if (status == null) {
            throw new NullPointerException("Status can't be null");
        }
        this.status = status;
    }

    @Override
    public Set<String> getClearClasses() {
        return this.clearClasses;
    }

    @Override
    public String toString() {
        return String.format("EventContextImpl [status=%s, clearClasses=%s]",
                status, clearClasses);
    }

}
