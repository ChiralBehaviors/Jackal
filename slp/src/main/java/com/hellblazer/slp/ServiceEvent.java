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
import java.util.UUID;

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
    private final UUID             sequence;
    private final EventType        type;

    public ServiceEvent(EventType type, ServiceReference reference,
                        UUID sequence) {
        this.type = type;
        this.reference = reference;
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServiceEvent other = (ServiceEvent) obj;
        if (sequence == null) {
            if (other.sequence != null) {
                return false;
            }
        } else if (!sequence.equals(other.sequence)) {
            return false;
        }
        return true;
    }

    public ServiceReference getReference() {
        return reference;
    }

    public UUID getSequence() {
        return sequence;
    }

    public EventType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (sequence == null ? 0 : sequence.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ServiceEvent [type=" + type + ", reference=" + reference + "]";
    }
}