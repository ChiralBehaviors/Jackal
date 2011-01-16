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

import static com.hellblazer.slp.ServiceScope.SERVICE_TYPE;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.Times;
import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisStability;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.util.Identity;

import com.fasterxml.uuid.NoArgGenerator;
import com.hellblazer.slp.ServiceEvent;
import com.hellblazer.slp.ServiceEvent.EventType;
import com.hellblazer.slp.ServiceListener;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceURL;
import com.hellblazer.slp.anubis.AnubisScope.Message;
import com.hellblazer.slp.anubis.AnubisScope.MessageType;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class AnubisServiceScopeTest extends TestCase {

    public void testRegister() throws Exception {
        Identity id = new Identity(666, 0, 0);
        Locator mockLocator = mock(Locator.class);
        NoArgGenerator mockGenerator = mock(NoArgGenerator.class);
        String stateName = "fooMeTwice";
        when(mockLocator.getIdentity()).thenReturn(id);
        AnubisScope scope = new AnubisScope(stateName, mockLocator,
                                              new RunImmediate(), mockGenerator);
        scope.openUpdateGate();
        ServiceURL url = new ServiceURL("service:http://foo.bar/");
        UUID registration = UUID.randomUUID();
        when(mockGenerator.generate()).thenReturn(registration);

        UUID returnedRegistration = scope.register(url, null);
        scope.processOneOutboundMessage();

        assertEquals(registration, returnedRegistration);
        verify(mockGenerator).generate();
        verify(mockLocator).getIdentity();
        verify(mockLocator).registerListener(isA(AnubisListener.class));
        ArgumentCaptor<AnubisProvider> providerCapture = ArgumentCaptor.forClass(AnubisProvider.class);
        verify(mockLocator).registerProvider(providerCapture.capture());
        verify(mockLocator).registerStability(isA(AnubisStability.class));
        verifyNoMoreInteractions(mockGenerator, mockLocator);
        AnubisProvider provider = providerCapture.getValue();
        assertNotNull(provider);
        ServiceReferenceImpl ref = (ServiceReferenceImpl) ((Message) provider.getValue()).body;
        assertNotNull(ref);
        assertEquals(url, ref.getUrl());
        assertEquals(id.id, ref.getMember());
        assertEquals(registration, ref.getRegistration());
        assertEquals("service:http", ref.getProperties().get(SERVICE_TYPE));
    }

    public void testGetServiceReference() throws Exception {
        Identity id = new Identity(666, 0, 0);
        Locator mockLocator = mock(Locator.class);
        NoArgGenerator mockGenerator = mock(NoArgGenerator.class);
        String stateName = "fooMeTwice";

        when(mockLocator.getIdentity()).thenReturn(id);
        AnubisScope scope = new AnubisScope(stateName, mockLocator,
                                              new RunImmediate(), mockGenerator);
        scope.openUpdateGate();
        ServiceURL url = new ServiceURL("service:http://foo.bar/");
        UUID registration = UUID.randomUUID();
        when(mockGenerator.generate()).thenReturn(registration);

        scope.register(url, null);
        scope.processOneOutboundMessage();

        ArgumentCaptor<AnubisProvider> providerCapture = ArgumentCaptor.forClass(AnubisProvider.class);
        verify(mockLocator).registerProvider(providerCapture.capture());
        AnubisProvider provider = providerCapture.getValue();
        assertNotNull(provider);
        ServiceReferenceImpl ref = (ServiceReferenceImpl) ((Message) provider.getValue()).body;
        assertNotNull(ref);
        scope.processInbound(new Message(MessageType.REGISTER, ref));
        ServiceReference returnedReference = scope.getServiceReference("service:http");
        assertNotNull(returnedReference);
        assertEquals(ref, returnedReference);
    }

    public void testSetProperties() throws Exception {
        Identity id = new Identity(666, 0, 0);
        Locator mockLocator = mock(Locator.class);
        NoArgGenerator mockGenerator = mock(NoArgGenerator.class);
        String stateName = "fooMeTwice";

        when(mockLocator.getIdentity()).thenReturn(id);
        AnubisScope scope = new AnubisScope(stateName, mockLocator,
                                              new RunImmediate(), mockGenerator);
        scope.openUpdateGate();
        ServiceURL url = new ServiceURL("service:http://foo.bar/");
        UUID registration = UUID.randomUUID();
        when(mockGenerator.generate()).thenReturn(registration);

        UUID returnedRegistration = scope.register(url, null);
        scope.processOneOutboundMessage();

        assertEquals(registration, returnedRegistration);
        verify(mockGenerator).generate();
        verify(mockLocator).getIdentity();
        verify(mockLocator).registerListener(isA(AnubisListener.class));
        ArgumentCaptor<AnubisProvider> providerCapture = ArgumentCaptor.forClass(AnubisProvider.class);
        verify(mockLocator).registerProvider(providerCapture.capture());
        verify(mockLocator).registerStability(isA(AnubisStability.class));
        verifyNoMoreInteractions(mockGenerator, mockLocator);
        AnubisProvider provider = providerCapture.getValue();
        assertNotNull(provider);
        ServiceReferenceImpl ref = (ServiceReferenceImpl) ((Message) provider.getValue()).body;
        assertNotNull(ref);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("foo", "bar");
        properties.put("baz", "bozo");

        scope.setProperties(registration, properties);
        scope.processOneOutboundMessage();

        Map<String, Object> newProperties = ((ServiceReferenceImpl) ((Message) provider.getValue()).body).getProperties();
        assertEquals(4, newProperties.size());
        assertEquals("bar", newProperties.get("foo"));
        assertEquals("bozo", newProperties.get("baz"));
    }

    public void testListener() throws Exception {
        Identity id = new Identity(666, 0, 0);
        Locator mockLocator = mock(Locator.class);
        NoArgGenerator mockGenerator = mock(NoArgGenerator.class);
        String stateName = "fooMeTwice";
        ServiceListener serviceListener = mock(ServiceListener.class);

        when(mockLocator.getIdentity()).thenReturn(id);
        AnubisScope scope = new AnubisScope(stateName, mockLocator,
                                              new RunImmediate(), mockGenerator);
        ServiceURL url = new ServiceURL("service:http://foo.bar/");
        UUID registration1 = UUID.randomUUID();
        UUID registration2 = UUID.randomUUID();

        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(SERVICE_TYPE, "service:http");
        ServiceReferenceImpl reference1 = new ServiceReferenceImpl(
                                                                   url,
                                                                   properties,
                                                                   registration1,
                                                                   0);
        ServiceReferenceImpl reference2 = new ServiceReferenceImpl(
                                                                   url,
                                                                   properties,
                                                                   registration2,
                                                                   0);
        scope.processInbound(new Message(MessageType.REGISTER, reference1));

        scope.addServiceListener(serviceListener,
                                   "(serviceType=service:http)");

        scope.processInbound(new Message(MessageType.REGISTER, reference2));
        scope.processInbound(new Message(MessageType.MODIFY, reference1));
        scope.processInbound(new Message(MessageType.UNREGISTER,
                                           registration1));

        scope.removeServiceListener(serviceListener);

        scope.processInbound(new Message(MessageType.MODIFY, reference2));
        scope.processInbound(new Message(MessageType.UNREGISTER,
                                           registration2));

        ArgumentCaptor<ServiceEvent> eventCaptor = ArgumentCaptor.forClass(ServiceEvent.class);
        verify(serviceListener, new Times(4)).serviceChanged(eventCaptor.capture());
        List<ServiceEvent> events = eventCaptor.getAllValues();
        assertNotNull(events);
        assertEquals(EventType.REGISTERED, events.get(0).getType());
        assertEquals(reference1, events.get(0).getReference());
        assertEquals(EventType.REGISTERED, events.get(1).getType());
        assertEquals(reference2, events.get(1).getReference());
        assertEquals(EventType.MODIFIED, events.get(2).getType());
        assertEquals(reference1, events.get(2).getReference());
        assertEquals(EventType.UNREGISTERED, events.get(3).getType());
        assertEquals(reference1, events.get(3).getReference());
        verifyNoMoreInteractions(serviceListener);
    }

    public void testUnregister() throws Exception {
        Identity id = new Identity(666, 0, 0);
        Locator mockLocator = mock(Locator.class);
        NoArgGenerator mockGenerator = mock(NoArgGenerator.class);
        String stateName = "fooMeTwice";
        when(mockLocator.getIdentity()).thenReturn(id);
        AnubisScope scope = new AnubisScope(stateName, mockLocator,
                                              new RunImmediate(), mockGenerator);
        scope.openUpdateGate();
        ServiceURL url = new ServiceURL("service:http://foo.bar/");
        UUID registration = UUID.randomUUID();
        when(mockGenerator.generate()).thenReturn(registration);

        UUID returnedRegistration = scope.register(url, null);
        scope.processOneOutboundMessage();

        assertEquals(registration, returnedRegistration);
        verify(mockGenerator).generate();
        verify(mockLocator).getIdentity();
        verify(mockLocator).registerListener(isA(AnubisListener.class));
        ArgumentCaptor<AnubisProvider> providerCapture = ArgumentCaptor.forClass(AnubisProvider.class);
        verify(mockLocator).registerProvider(providerCapture.capture());
        verify(mockLocator).registerStability(isA(AnubisStability.class));
        verifyNoMoreInteractions(mockGenerator, mockLocator);
        AnubisProvider provider = providerCapture.getValue();
        assertNotNull(provider);
        ServiceReferenceImpl registered = (ServiceReferenceImpl) ((Message) provider.getValue()).body;
        assertNotNull(registered);

        scope.unregister(returnedRegistration);
        scope.processOneOutboundMessage();

        UUID unregistered = (UUID) ((Message) provider.getValue()).body;
        assertNotNull(unregistered);
        assertEquals(registration, unregistered);
    }
}