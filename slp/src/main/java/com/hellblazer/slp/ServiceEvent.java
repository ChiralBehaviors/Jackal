/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.slp;

import java.io.Serializable;

/**
 * The service lifecycle event.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ServiceEvent implements Serializable {
    public enum EventType {
        MODIFIED, REGISTERED, UNREGISTERED
    }

    private static final long      serialVersionUID = 1L;
    private final ServiceReference reference;
    private final EventType        type;

    public ServiceEvent(EventType type, ServiceReference reference) {
        this.type = type;
        this.reference = reference;
    }

    public ServiceReference getReference() {
        return reference;
    }

    public EventType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ServiceEvent [type=" + type + ", reference=" + reference + "]";
    }
}