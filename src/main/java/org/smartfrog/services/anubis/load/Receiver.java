package org.smartfrog.services.anubis.load;

import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisValue;

public class Receiver extends AnubisListener {
    private static Logger log = Logger.getLogger(Receiver.class.getCanonicalName());
    private String instance;

    public Receiver(String n, String instance) {
        super(n);
        this.instance = "<" + instance + ">";
    }

    @Override
    public void newValue(AnubisValue value) {
        log.info(instance + " received: " + value);
    }

    @Override
    public void removeValue(AnubisValue value) {
        log.info(instance + " removed: " + value);
    }

}
