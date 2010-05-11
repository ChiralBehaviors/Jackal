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



/**
 * Abstract base class for the provider object.
 *
 * This is an abstract base class for the provider object. The
 * provider is defined by the user by extendeding this class and
 * can then be registered with the locator. When a named provider
 * registers the locator recognises its presence in the partition.
 *
 * @version 1.0
 */

public class AnubisProvider {
    static private boolean marshallValues = false;
    static public  void    setMarshallValues(boolean marshall) {
        marshallValues = marshall;
    }
    /**
     * The providers name;
     */
    private String name = null;
    /**
     * the locator that this provider is registered with.
     */
    private AnubisLocator locator = null;
    /**
     * The value supplied by the provider.
     */
    private ValueData value = null;
    /**
     * the time the value parameter was set or the time this provider is
     * registered, whichever is more recent. (time taken from system clock).
     */
    private long time = 0;
    /**
     * instance information generated qutomatically by Anubis
     */
    private String instance = null;

    /**
     * constructor - the provider must be named.
     */
    public AnubisProvider(String name) {
            this.name = name;
            setTime(-1);
            setValueObj(ValueData.nullValue());
    }
    /**
     * set the locator - this is used by the locator when the provider is
     * registered.
     */
    public synchronized void setAnubisData(AnubisLocator locator, long time, String instance) {
            this.locator = locator;
            setTime(time);
            setInstance(instance);
    }
    /**
     * setTime is introduced to allow for the SubProcess interface. Remote
     * interfaces need to be re-done - this is a temporary fix.
     * @param time
     */
    protected void setTime(long time) { this.time = time; }
    /**
     * setTime is introduced to allow for the SubProcess interface. Remote
     * interfaces need to be re-done - this is a temporary fix.
     * @param value
     */
    protected void setValueObj(ValueData value) { this.value = value; }
    protected void setInstance(String instance) { this.instance = instance; }
    protected void update() { if( locator != null ) locator.newProviderValue(this); }
    /**
     * to get the assigned name of the provider.
     *
     * @return the providers name
     */
    public synchronized String getName() { return name; }
    /**
     * get the value associated with this provider
     */
    public synchronized Object getValue() { return value.getValue(); }
    /**
     * get the internal representation of the value assocaited with this provider
     */
    public synchronized ValueData getValueData() { return value; }
    /**
     * get the time value
     */
    public synchronized long getTime() { return time; }
    /**
     * get the instance string for this provider
     */
    public synchronized String getInstance() { return instance; }
    /**
     * set the value supplied by the provider.
     */
    public synchronized void setValue(Object value) {
        if( AnubisProvider.marshallValues ) {
            setValueObj(ValueData.newMarshalledValue(value));
        } else {
            setValueObj(ValueData.newValue(value));
        }
        setTime(System.currentTimeMillis());
        update();
    }
    /**
     * abstract method to be defined by the user. This method may be called
     * by the locator periodically to pro-activly check the status of the
     * provider.
     *
     * @return true if the provider is ok, false if not.
     */
    // abstract public boolean anubisLivenessPoll();

    public synchronized String toString() {
        return "Provider " + getName() + "=[" + getValue() + ", " + getTime() + "]";
    }
}
