package org.smartfrog.services.anubis;

import org.smartfrog.services.anubis.locator.AnubisValue;

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