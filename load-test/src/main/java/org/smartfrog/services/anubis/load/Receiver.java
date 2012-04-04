package org.smartfrog.services.anubis.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisValue;

public class Receiver extends AnubisListener {
    private static final Logger log = LoggerFactory.getLogger(Receiver.class.getCanonicalName());
    private String              instance;

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
