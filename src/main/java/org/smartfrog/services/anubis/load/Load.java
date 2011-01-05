package org.smartfrog.services.anubis.load;

import org.smartfrog.services.anubis.BasicConfiguration;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Load {
    static int WAIT_TIME = 1500;
    static String STATE_NAME = "Whip It Good";

    public static void main(String[] argv) {
        String stateName = STATE_NAME;
        int waitTime = WAIT_TIME;

        Gate gate = new Gate();
        gate.close();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                                        BasicConfiguration.class);
        AnubisLocator locator = ctx.getBean(AnubisLocator.class);

        AnubisProvider provider = new AnubisProvider(stateName);
        provider.setValue(0);

        locator.registerProvider(provider);
        locator.registerListener(new Receiver(stateName, provider.getInstance()));
        locator.registerStability(new Stability(gate, provider.getInstance()));

        Thread senderThread = new Thread(new Sender(gate, provider, waitTime),
                                         "Sender thread");
        senderThread.start();
    }
}
