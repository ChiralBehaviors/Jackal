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
package org.smartfrog.services.anubis;

import org.smartfrog.services.anubis.locator.AnubisValue;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ValueHistory {
    public final Action action;

    public final String instance;
    public final String name;
    public final long time;
    public final Object value;

    public ValueHistory(AnubisValue av, Action action) {
        name = av.getName();
        time = av.getTime();
        instance = av.getInstance();
        value = av.getValue();
        this.action = action;
    }
}