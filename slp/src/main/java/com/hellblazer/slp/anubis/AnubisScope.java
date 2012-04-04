/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.slp.anubis;

import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisValue;
import org.smartfrog.services.anubis.partition.Partition;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

import com.fasterxml.uuid.NoArgGenerator;
import com.hellblazer.slp.Filter;
import com.hellblazer.slp.FilterImpl;
import com.hellblazer.slp.InvalidSyntaxException;
import com.hellblazer.slp.ServiceEvent;
import com.hellblazer.slp.ServiceEvent.EventType;
import com.hellblazer.slp.ServiceListener;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceURL;

/**
 * A <link>ServiceScope</link> using Anubis as the underlying support
 * infrastructure. Service events are delivered reliably and in the proper order
 * to all members of a partition. When new members are added to the partition,
 * previous members will synchronize these members with existing service
 * registrations. When members leave, existing members will be notified that the
 * services published by these members have been unregistered.
 * 
 * <p>
 * The interface is non blocking regardless of the stability or instability of
 * the underlying partition's membership view. Service lifecycle events produced
 * by services of this member will only be sent when the partition member view
 * is stable.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class AnubisScope implements ServiceScope {
    static class Gate {
        private int     generation;
        private boolean isOpen;

        // BLOCKS-UNTIL: opened-since(generation on entry)
        public synchronized void await() throws InterruptedException {
            int arrivalGeneration = generation;
            while (!isOpen && arrivalGeneration == generation) {
                wait();
            }
        }

        public synchronized void close() {
            isOpen = false;
        }

        public synchronized void open() {
            ++generation;
            isOpen = true;
            notifyAll();
        }
    }

    static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        final Serializable        body;
        final MessageType         type;

        Message(MessageType type, Serializable body) {
            this.type = type;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Message [type=" + type + ", body=" + body + "]";
        }
    }

    static enum MessageType {
        MODIFY, REGISTER, SYNC, UNREGISTER;
    }

    public static final String                                  MEMBER_IDENTITY = "anubis.member.identity";

    private static final Logger                                 log             = LoggerFactory.getLogger(AnubisScope.class);
    private final ExecutorService                               executor;
    private final int                                           identity;
    private final Map<ServiceListener, Filter>                  listeners       = new ConcurrentHashMap<ServiceListener, Filter>();
    private final Map<UUID, ServiceReferenceImpl>               myServices      = new ConcurrentHashMap<UUID, ServiceReferenceImpl>();
    private final BlockingQueue<Message>                        outboundMsgs    = new LinkedBlockingQueue<Message>();
    private Thread                                              outboundProcessingThread;
    private final Partition                                     partition;
    private final AtomicBoolean                                 run             = new AtomicBoolean(
                                                                                                    false);
    private final ConcurrentHashMap<UUID, ServiceReferenceImpl> systemServices  = new ConcurrentHashMap<UUID, ServiceReferenceImpl>();
    private final Gate                                          updateGate      = new Gate();
    private final NoArgGenerator                                uuidGenerator;
    private volatile View                                       view;
    private final PartitionNotification                         notification;

    public AnubisScope(Identity identity, ExecutorService execService,
                       NoArgGenerator generator, Partition partition) {
        executor = execService;
        this.identity = identity.id;
        uuidGenerator = generator;
        this.partition = partition;
        notification = new PartitionNotification() {
            @Override
            public void objectNotification(Object obj, int sender, long time) {
                if (obj instanceof Message) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("scope %s receiving message %s from %s",
                                                AnubisScope.this.identity, obj,
                                                sender));
                    }
                    processInbound((Message) obj);
                }
            }

            @Override
            public void partitionNotification(View view, int leader) {
                AnubisScope.this.view = view;
                if (view.isStable()) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("stabilizing partition on scope %s",
                                                AnubisScope.this.identity));
                    }
                    sync();
                    openUpdateGate();
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("destabilizing partition on scope %s",
                                                AnubisScope.this.identity));
                    }
                    closeUpdateGate();
                }
            }
        };
        partition.register(notification);
    }

    @Override
    public void addServiceListener(final ServiceListener listener, String query)
                                                                                throws InvalidSyntaxException {
        if (log.isTraceEnabled()) {
            log.trace("adding listener: " + listener + " on query: " + query);
        }
        List<ServiceReference> references;
        listeners.put(listener, new FilterImpl(query));
        references = getServiceReferences(null, query);
        for (ServiceReference reference : references) {
            final ServiceReference ref = reference;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.serviceChanged(new ServiceEvent(
                                                                 EventType.REGISTERED,
                                                                 ref));
                    } catch (Throwable e) {
                        log.error("Error when notifying listener on reference "
                                  + EventType.REGISTERED, e);
                    }
                }
            });
        }

    }

    @Override
    public Filter createFilter(String query) throws InvalidSyntaxException {
        return new FilterImpl(query);
    }

    @Override
    public ServiceReference getServiceReference(String serviceType)
                                                                   throws InvalidSyntaxException {
        if (serviceType == null) {
            serviceType = "*";
        }
        Filter filter = createFilter("(" + SERVICE_TYPE + "=" + serviceType
                                     + ")");
        for (ServiceReference ref : systemServices.values()) {
            if (filter.match(ref)) {
                return ref;
            }
        }
        return null;
    }

    @Override
    public List<ServiceReference> getServiceReferences(String serviceType,
                                                       String query)
                                                                    throws InvalidSyntaxException {
        if (serviceType == null) {
            serviceType = "*";
        }
        Filter filter = createFilter("(&(" + SERVICE_TYPE + "=" + serviceType
                                     + ") " + query + ")");
        ArrayList<ServiceReference> references = new ArrayList<ServiceReference>();
        for (Map.Entry<UUID, ServiceReferenceImpl> entry : systemServices.entrySet()) {
            if (filter.match(entry.getValue())) {
                references.add(entry.getValue());
            }
        }
        return references;
    }

    @Override
    public UUID register(ServiceURL url, Map<String, Object> properties) {
        if (url == null) {
            throw new IllegalArgumentException("Service URL cannot be null");
        }
        UUID registration = uuidGenerator.generate();
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties = new HashMap<String, Object>(properties);
        properties.put(SERVICE_TYPE, url.getServiceType().toString());
        properties.put(MEMBER_IDENTITY, identity);
        ServiceReferenceImpl ref = new ServiceReferenceImpl(url, properties,
                                                            registration,
                                                            identity);
        myServices.put(registration, ref);
        update(new Message(MessageType.REGISTER, ref.clone()));
        return registration;

    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setProperties(UUID serviceRegistration,
                              Map<String, Object> properties) {
        ServiceReferenceImpl ref = myServices.get(serviceRegistration);
        if (ref == null) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("No service registered for %s",
                                        serviceRegistration));
            }
            return;
        }
        properties = new HashMap<String, Object>(properties);
        properties.put(SERVICE_TYPE, ref.currentProperties().get(SERVICE_TYPE));
        properties.put(MEMBER_IDENTITY,
                       ref.currentProperties().get(MEMBER_IDENTITY));
        ref.setProperties(properties);
        update(new Message(MessageType.MODIFY, ref.clone()));
    }

    @Override
    public void unregister(UUID serviceRegistration) {
        myServices.remove(serviceRegistration);
        update(new Message(MessageType.UNREGISTER, serviceRegistration));
    }

    private void send(Message msg) {
        for (int n : view.toBitSet()) {
            if (identity == n) {
                processInbound(msg);
            } else {
                MessageConnection connection = partition.connect(n);
                if (connection == null) {
                    System.out.println(String.format("Node %s cannot connect to: %s",
                                                     identity, n));
                }
                connection.sendObject(msg);
            }
        }
    }

    protected void closeUpdateGate() {
        log.trace("closing update gate");
        updateGate.close();
    }

    protected void memberLeft(AnubisValue value) {
        String instance = value.getInstance();
        if (instance == null) {
            log.error(String.format("No instance value for removed value %s",
                                    value));
            return;
        }
        int index = instance.indexOf('/');
        if (index == -1) {
            log.error(String.format("No member id for instance value %s",
                                    instance));
            return;
        }
        int id = Integer.parseInt(instance.substring(0, index));
        for (Iterator<Map.Entry<UUID, ServiceReferenceImpl>> iterator = systemServices.entrySet().iterator(); iterator.hasNext();) {
            ServiceReferenceImpl ref = iterator.next().getValue();
            if (ref.getMember() == id) {
                iterator.remove();
            }
        }
    }

    protected void openUpdateGate() {
        log.trace("opening update gate");
        updateGate.open();
    }

    /**
     * The state of the system services has changed.
     * 
     * @param value
     */
    protected synchronized void processInbound(Message message) {
        switch (message.type) {
            case REGISTER: {
                ServiceReferenceImpl ref = (ServiceReferenceImpl) message.body;
                ServiceReferenceImpl existing = systemServices.putIfAbsent(ref.getRegistration(),
                                                                           ref);
                if (existing == null) {
                    serviceChanged(ref, EventType.REGISTERED);
                }
                break;
            }
            case MODIFY: {
                ServiceReferenceImpl ref = (ServiceReferenceImpl) message.body;
                ServiceReferenceImpl existing = systemServices.put(ref.getRegistration(),
                                                                   ref);
                if (existing != null) {
                    serviceChanged(ref, EventType.MODIFIED);
                }
                break;
            }
            case UNREGISTER: {
                UUID registration = (UUID) message.body;
                ServiceReferenceImpl systemRef = systemServices.remove(registration);
                ServiceReferenceImpl myRef = myServices.remove(registration);
                if (myRef != null) {
                    serviceChanged(myRef, EventType.UNREGISTERED);
                } else if (systemRef != null) {
                    serviceChanged(systemRef, EventType.UNREGISTERED);
                }
                break;
            }
            case SYNC: {
                @SuppressWarnings("unchecked")
                final List<ServiceReferenceImpl> body = (List<ServiceReferenceImpl>) message.body;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (ServiceReferenceImpl ref : body) {
                            if (systemServices.putIfAbsent(ref.getRegistration(),
                                                           ref) == null) {
                                serviceChanged(ref, EventType.REGISTERED);
                            }
                        }
                    }
                });
                break;
            }
            default: {
                throw new IllegalStateException("Illegal message type: "
                                                + message.type);
            }
        }
    }

    protected void processOneOutboundMessage() throws InterruptedException {
        Message state = outboundMsgs.take();
        updateGate.await();
        send(state);
    }

    protected void serviceChanged(final ServiceReference reference,
                                  final EventType type) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<ServiceListener, Filter> entry : listeners.entrySet()) {
                    if (entry.getValue().match(reference)) {
                        final ServiceListener listener = entry.getKey();
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    listener.serviceChanged(new ServiceEvent(
                                                                             type,
                                                                             reference));
                                } catch (Throwable e) {
                                    log.error("Error when notifying listener on reference "
                                                      + type, e);
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @PostConstruct
    protected void start() {
        if (run.compareAndSet(false, true)) {
            startOutboundProcessingThread();
        }
    }

    protected void startOutboundProcessingThread() {
        outboundProcessingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (log.isTraceEnabled()) {
                    log.trace("State outbound message processing for member: "
                              + identity + " running");
                }
                while (run.get()) {
                    try {
                        processOneOutboundMessage();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "AnubisScope state update thread for member: " + identity);
        outboundProcessingThread.setDaemon(true);
        outboundProcessingThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.warn("Uncaught exception in outbound message processing thread",
                         e);
            }
        });
        outboundProcessingThread.start();
    }

    @PreDestroy
    protected void stop() {
        if (run.compareAndSet(true, false)) {
            partition.deregister(notification);
            if (outboundProcessingThread != null) {
                outboundProcessingThread.interrupt();
            }
        }
    }

    protected void sync() {
        try {
            outboundMsgs.put(new Message(
                                         MessageType.SYNC,
                                         new ArrayList<ServiceReferenceImpl>(
                                                                             myServices.values())));
        } catch (InterruptedException e) {
            return;
        }
    }

    protected void update(Message state) {
        try {
            outboundMsgs.put(state);
        } catch (InterruptedException e) {
            // intentionally no action
        }
    }
}
