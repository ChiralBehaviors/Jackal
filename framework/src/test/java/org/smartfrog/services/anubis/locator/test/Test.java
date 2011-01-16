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
package org.smartfrog.services.anubis.locator.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisValue;
import org.smartfrog.services.anubis.locator.Locator;

import com.hellblazer.anubis.annotations.Deployed;

@SuppressWarnings("rawtypes")
public class Test {

    class Listener extends AnubisListener {
        public Listener(String name) {
            super(name);
        }

        public void display() {
            String str = "Listener " + getName() + " has values:";
            for (Iterator<?> iter = values().iterator(); iter.hasNext(); str += " "
                                                                                + iter.next().toString()) {
                ;
            }
            driver.println(str);
        }

        @Override
        public void newValue(AnubisValue v) {
            display();
            if (v.getValue().equals("throw")) {
                // this expression intentionally leads to an exception
                @SuppressWarnings("unused")
                int x = ((Integer) null).intValue();
            } else if (v.getValue().equals("wait")) {
                // this expression intentially leads to a pause
                try {
                    wait(1000);
                } catch (Exception ex) {
                }
            }
        }

        @Override
        public void removeValue(AnubisValue v) {
            driver.println("Removing value " + v.toString());
            display();
        }
    }

    class Provider extends AnubisProvider {
        Provider(String name) {
            super(name);
        }
    }

    private Map<String, Provider> providers;
    private Map<String, Set<Listener>> listeners;
    private AnubisLocator locator;
    private Driver driver;
    private String title;

    public Test() {
        this(UUID.randomUUID().toString());
    }

    public Test(String title) {
        this.title = title;
        locator = null;
        driver = null;
        providers = new HashMap<String, Provider>();
        listeners = new HashMap<String, Set<Listener>>();
    }

    public void addProvider(String str) {

        String name = retrieveName(str);
        String value = retrieveValue(str);

        if (name.equals("")) {
            driver.println("Need a name");
            return;
        }

        Provider provider = providers.get(name);
        if (provider == null) {
            provider = new Provider(name);
            provider.setValue(value);
            providers.put(name, provider);
            locator.registerProvider(provider);
            driver.println("Registered provider for " + name + " with value "
                           + value);
        } else {
            provider.setValue(value);
            driver.println("Set value of provider " + name + " to " + value);
        }
    }

    public AnubisLocator getLocator() {
        return locator;
    }

    public String getTitle() {
        return title;
    }

    public void nonBlockAddListener(String str) {

        String name = retrieveName(str);

        if (name.equals("")) {
            driver.println("Need a name");
            return;
        }

        Listener l = new Listener(name);
        if (listeners.containsKey(name)) {
            listeners.get(name).add(l);
        } else {
            Set<Listener> s = new HashSet<Listener>();
            s.add(l);
            listeners.put(name, s);
        }

        locator.registerListener(l);
    }

    public void rapidStates(String str) {

        StringTokenizer tokens = new StringTokenizer(str);

        if (tokens.countTokens() < 2) {
            driver.println("Error - Usage: <name> <sequence of states>");
            return;
        }

        String name = tokens.nextToken();
        if (!providers.containsKey(name)) {
            driver.println("Error - unknown provider name");
            return;
        }

        Provider provider = providers.get(name);
        while (tokens.hasMoreTokens()) {
            provider.setValue(tokens.nextToken());
        }
    }

    public void removeGlobal() {
        if (locator != null && ((Locator) locator).local != null) {
            ((Locator) locator).global.removeDebugFrame();
        }
    }

    public void removeListener(String str) {

        String name = retrieveName(str);

        if (name.equals("")) {
            driver.println("Need a name");
            return;
        }

        if (listeners.containsKey(name)) {

            Set s = listeners.get(name);
            Listener l = (Listener) s.iterator().next();
            s.remove(l);
            if (s.isEmpty()) {
                listeners.remove(name);
            }

            locator.deregisterListener(l);
            driver.println("Deregistered listener for " + name + " -- "
                           + s.size() + " left");

        } else {
            driver.println("There are no listeners for " + name);
        }
    }

    public void removeLocal() {
        if (locator != null && ((Locator) locator).local != null) {
            ((Locator) locator).local.removeDebugFrame();
        }
    }

    public void removeProvider(String str) {

        String name = retrieveName(str);

        if (name.equals("")) {
            driver.println("Need a name");
            return;
        }

        if (providers.containsKey(name)) {
            Provider p = providers.remove(name);
            locator.deregisterProvider(p);
            driver.println("Deregistered provider for " + name);
        } else {
            driver.println("Can't deregister a provider that does not exist");
        }
    }

    public String retrieveName(String str) {
        String trimmed = str.trim();
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex == -1 ? trimmed : trimmed.substring(0, spaceIndex);
    }

    public String retrieveValue(String str) {
        String trimmed = str.trim();
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex == -1 ? "dummy"
                               : trimmed.substring(spaceIndex).trim();
    }

    public void setLocator(AnubisLocator locator) {
        this.locator = locator;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void showGlobal() {
        ((Locator) locator).global.showDebugFrame();
    }

    public void showLocal() {
        ((Locator) locator).local.showDebugFrame();
    }

    /**
     * Implementation of Prim interface.
     * 
     * @throws Exception
     */
    @Deployed
    public void start() {
        driver = new Driver(this, "TestDriver: " + title);
        driver.setVisible(true);
    }

    /**
     * Implementation of Prim interface.
     * 
     * @param tr
     */
    @PreDestroy
    public void terminate() {
        if (driver != null) {
            driver.setVisible(false);
        }
        removeLocal();
        removeGlobal();
        System.exit(0);
    }

}
