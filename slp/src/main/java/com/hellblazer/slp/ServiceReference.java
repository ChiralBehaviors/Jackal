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
import java.util.Collections;
import java.util.Map;

/**
 * The ServiceReference represents a registered service within a
 * <link>ServiceScope</link>.
 * 
 * A service is represented by a <link>ServiceURL</link> and a <link>Map</link>
 * of the attributes of the service.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ServiceReference implements Serializable {
    private static final long     serialVersionUID = 1L;
    protected ServiceURL          url;
    protected Map<String, Object> properties;

    /**
     * @param url
     * @param properties
     */
    public ServiceReference(ServiceURL url, Map<String, Object> properties) {
        super();
        this.url = url;
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public ServiceURL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "ServiceReference [url=" + url + ", properties=" + properties
               + "]";
    }
}