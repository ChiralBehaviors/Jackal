/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

*/
package org.smartfrog.services.anubis.locator;


import org.smartfrog.services.anubis.locator.names.ProviderInstance;

public class AnubisValue {

    private String    name;
    private String    instance;
    private long      time;
    private Object    value;

    public AnubisValue(ProviderInstance i) {
        setName(i.name);
        setInstance(i.instance);
        setTime(i.time);
        setValue(i.value);
    }

    public final String getName()     { return name; }
    public final String getInstance() { return instance; }
    public final long   getTime()     { return time; }
    public final Object getValue()    { return value; }

    public final void   set(long t, ValueData v) {
        setTime(t);
        setValue(v);
    }

    private void setName(String name)         { this.name = name; }
    private void setInstance(String instance) { this.instance = instance; }
    private void setTime(long time)           { this.time = time; }
    private void setValue(ValueData value) {
        this.value = value.getValue();
    }

    public String toString() {
        return "Value [name=" + getName() + ", instance=" + getInstance()
                + ", time=" + getTime() + ", value=" + getValue() + "]";
    }

}
