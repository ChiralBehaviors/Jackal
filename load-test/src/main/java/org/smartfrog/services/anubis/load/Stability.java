package org.smartfrog.services.anubis.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisStability;

public class Stability extends AnubisStability {
    private final Gate          gate;
    private final String        instance;
    private final static Logger log = LoggerFactory.getLogger(Stability.class.getCanonicalName());

    public Stability(Gate gate, String instance) {
        this.gate = gate;
        this.instance = "<" + instance + ">";
    }

    @Override
    public void stability(boolean isStable, long timeRef) {
        if (isStable) {
            gate.open();
            log.info(instance + " stabilized @ " + timeRef);
        } else {
            gate.close();
            log.info(instance + " destabilized @ " + timeRef);
        }
    }

}
