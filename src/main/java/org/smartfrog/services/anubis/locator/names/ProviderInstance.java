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
package org.smartfrog.services.anubis.locator.names;


import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.ValueData;

public class ProviderInstance extends NameData {

    public long      time;
    public String    instance;
    public ValueData value;

    public ProviderInstance(AnubisProvider provider, String instance, Integer node) {
        super(provider.getName(), node);
        this.time     = provider.getTime();
        this.instance = instance;
        this.value    = provider.getValueData();
    }

    public ProviderInstance(String name, String instance, Integer node, long time, ValueData value) {
        super(name, node);
        this.time     = time;
        this.instance = instance;
        this.value    = value;
    }

    /**
     * Create a provider proxy that matches this instance
     * @return  proxy provider
     */
    public ProviderProxy proxy() {
        return new ProviderProxy(name, node);
    }

    public ProviderInstance copy() {
        return new ProviderInstance(name, instance, node, time, value);
    }

    /**
     * when a provider instance is compared with another provider instance
     * they must match on name, instance, and node. If it is compared with
     * something that is not a provider instance they are matched
     * according to the super class equals() method (- which matches as name
     * data).
     *
     * @param obj
     * @return  boolean
     */
    public boolean equals(Object obj) {
    if( obj instanceof ProviderInstance )
        return ( super.equals(obj) &&
                 instance.equals( ((ProviderInstance)obj).instance) );
    else
        return super.equals(obj);
    }

    /**
     * hashcode uses the super class nameData hashCode() method. Mixing nameData
     * classes and providerInstance classes in a container will work, but is
     * poor style - it gets confusing.
     *
     * @return  hascode
     */
    public int hashCode() {
        return super.hashCode();
    }

    public String toString() {
        return "ProviderInstance [" + name +
                ": instance=" + instance +
                ", node=" + node +
                ", time=" + time +
                ", value=" + value + "]";
    }

}
