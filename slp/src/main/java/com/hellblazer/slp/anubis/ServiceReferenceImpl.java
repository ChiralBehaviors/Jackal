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
package com.hellblazer.slp.anubis;

import java.util.Map;
import java.util.UUID;

import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceURL;

/**
 * A specialization of <link>ServiceReference</link> which knows about its
 * member identity and service registration.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ServiceReferenceImpl extends ServiceReference implements Cloneable {
    private static final long serialVersionUID = 1L;

    private final UUID        registration;
    private final int         member;

    public ServiceReferenceImpl(ServiceURL url, Map<String, Object> properties,
                                UUID registration, int member) {
        super(url, properties);
        this.registration = registration;
        this.member = member;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ServiceReferenceImpl) {
            return registration.equals(((ServiceReferenceImpl) obj).registration);
        }
        return false;
    }

    public ServiceReference getServiceReference() {
        return new ServiceReference(url, properties);
    }

    @Override
    public int hashCode() {
        return registration.hashCode();
    }

    @Override
    protected ServiceReferenceImpl clone() {
        try {
            return (ServiceReferenceImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone is not supported", e);
        }
    }

    protected Map<String, Object> currentProperties() {
        return properties;
    }

    protected int getMember() {
        return member;
    }

    protected UUID getRegistration() {
        return registration;
    }

    protected void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
