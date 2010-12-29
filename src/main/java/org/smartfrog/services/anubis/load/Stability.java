package org.smartfrog.services.anubis.load;

import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.AnubisStability;

public class Stability extends AnubisStability {
    private final Gate gate;
    private final String instance;
    private final static Logger log = Logger.getLogger(Stability.class.getCanonicalName());
    
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
