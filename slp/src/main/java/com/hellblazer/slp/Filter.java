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

import java.util.Map;

/**
 * The interface to a filter which can be used to match attributes or
 * <link>ServiceReference</>
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface Filter {

    /**
     * Filter using a Map. The Filter is executed using the Map's keys.
     * 
     * @param properties
     *            the map whose keys are used in the match.
     * @return <code>true</code> if the Map's keys match this filter;
     *         <code>false</code> otherwise.
     */
    boolean match(Map<String, Object> properties);

    boolean match(ServiceReference reference);

    /**
     * Filter with case sensitivity using a <tt>Map<String, Object></tt> object.
     * The FilterImpl is executed using the <tt>Map</tt> object's keys and
     * values. The keys are case sensitivley matched with the filter.
     * 
     * @param properties
     *            The <tt>Map</tt> object whose keys are used in the match.
     * 
     * @return <tt>true</tt> if the <tt>Map</tt> object's keys and values match
     *         this filter; <tt>false</tt> otherwise.
     */
    boolean matchCase(Map<String, Object> properties);

}