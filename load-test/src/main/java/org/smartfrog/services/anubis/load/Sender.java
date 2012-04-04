package org.smartfrog.services.anubis.load;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisProvider;

public class Sender implements Runnable {
    private final Gate           gate;
    private final AnubisProvider provider;
    private final int            waitTime;
    private String               instance;
    private static final Logger  log = LoggerFactory.getLogger(Sender.class.getCanonicalName());

    public Sender(Gate gate, AnubisProvider provider, int waitTime) {
        this.gate = gate;
        this.provider = provider;
        this.waitTime = waitTime;
        instance = "<" + provider.getInstance() + ">";
    }

    @Override
    public void run() {
        Random random = new Random(666);
        int counter = 0;
        while (true) {
            try {
                gate.await();
                provider.setValue(++counter);
                log.info(instance + " sent value: " + counter + " @ "
                         + System.currentTimeMillis());
                Thread.sleep(random.nextInt(waitTime));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

}
