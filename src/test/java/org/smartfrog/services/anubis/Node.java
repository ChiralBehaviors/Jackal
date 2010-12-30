package org.smartfrog.services.anubis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisStability;
import org.smartfrog.services.anubis.locator.AnubisValue;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Node {
    static Random RANDOM = new Random(666);
    private AnnotationConfigApplicationContext context;
    private int maxSleep;
    private AnubisProvider provider;
    private int messagesToSend;
    private CyclicBarrier barrier;
    private String instance; 
    private CountDownLatch latch = new CountDownLatch(1);

    private ArrayList<SendHistory> sendHistory = new ArrayList<SendHistory>();
    private ConcurrentHashMap<String, List<ValueHistory>> receiveHistory = new ConcurrentHashMap<String, List<ValueHistory>>();
    private ArrayList<StabilityHistory> stabilityHistory = new ArrayList<StabilityHistory>();
    private CyclicBarrier startBarrier;

    Node(AnnotationConfigApplicationContext context, String stateName)
                                                                      throws Exception {
        this.context = context;
        
        PartitionManager pm = context.getBean(PartitionManager.class);
        pm.register(new PartitionNotification() {
            
            @Override
            public void partitionNotification(View view, int leader) {
                if (view.isStable() && view.cardinality() == 7) {
                    System.out.println("Launching");
                    latch.countDown();
                } else { 
                    System.out.println("Not launching: " + view);
                }
            }
            
            @Override
            public void objectNotification(Object obj, int sender, long time) {
                // TODO Auto-generated method stub
                
            }
        });

        AnubisListener listener = new AnubisListener(stateName) {
            @Override
            public void newValue(AnubisValue value) {
                List<ValueHistory> newHistory = new CopyOnWriteArrayList<ValueHistory>();
                List<ValueHistory> history = receiveHistory.putIfAbsent(value.getInstance(),
                                                                        newHistory);
                if (history == null) {
                    history = newHistory;
                }
                history.add(new ValueHistory(value, Action.NEW));
            }

            @Override
            public void removeValue(AnubisValue value) {
                List<ValueHistory> newHistory = new CopyOnWriteArrayList<ValueHistory>();
                List<ValueHistory> history = receiveHistory.putIfAbsent(value.getInstance(),
                                                                        newHistory);
                if (history == null) {
                    history = newHistory;
                }
                history.add(new ValueHistory(value, Action.REMOVE));
            }
        };
        provider = new AnubisProvider(stateName);
        AnubisStability stability = new AnubisStability() {
            @Override
            public void stability(boolean stable, long timeRef) {
                stabilityHistory.add(new StabilityHistory(stable, timeRef));
            }
        };

        AnubisLocator locator = context.getBean(AnubisLocator.class);
        locator.registerStability(stability);
        locator.registerListener(listener);
        locator.registerProvider(provider); 
        instance = provider.getInstance();
    }

    public String getInstance() {
        return instance;
    }

    public List<SendHistory> getSendHistory() {
        return sendHistory;
    }

    public List<ValueHistory> getValueHistory(String instance) {
        return receiveHistory.get(instance);
    }

    public void setEndBarrier(CyclicBarrier barrier) {
        this.barrier = barrier;
    }

    public void setMaxSleep(int maxSleep) {
        this.maxSleep = maxSleep;
    }

    public void setMessagesToSend(int messagesToSend) {
        this.messagesToSend = messagesToSend;
    }

    public void setStartBarrier(CyclicBarrier startBarrier) {
        this.startBarrier = startBarrier;
    }

    public void shutDown() {
        context.close();
    }

    public void start() {
        Thread daemon = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                    startBarrier.await();
                } catch (InterruptedException e1) {
                    return;
                } catch (BrokenBarrierException e1) {
                    e1.printStackTrace();
                    return;
                }
                for (int counter = 0; counter < messagesToSend; counter++) {
                    try {
                        Thread.sleep(RANDOM.nextInt(maxSleep));
                    } catch (InterruptedException e) {
                        return;
                    }
                    SendHistory sent = new SendHistory(
                                                       System.currentTimeMillis(),
                                                       counter);
                    provider.setValue(sent.value);
                    sendHistory.add(sent);
                }
                try {
                    Thread.sleep(2000);
                    barrier.await();
                } catch (InterruptedException e) {
                    return;
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
        }, "Run Thread for: " + context.getBean(Identity.class).toString());

        daemon.setDaemon(true);
        daemon.start();
    }
}