/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.gossip;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import com.hellblazer.jackal.gossip.SystemView;

import junit.framework.TestCase;

/**
 * Basic testing of the system view
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class SystemViewTest extends TestCase {

    public void testSeedMembers() throws Exception {
        Random random = mock(Random.class);
        when(random.nextInt(4)).thenReturn(2);

        InetSocketAddress local = new InetSocketAddress(1);

        InetSocketAddress seed1 = new InetSocketAddress(2);
        InetSocketAddress seed2 = new InetSocketAddress(3);
        InetSocketAddress seed3 = new InetSocketAddress(4);
        InetSocketAddress seed4 = new InetSocketAddress(5);

        InetSocketAddress live1 = new InetSocketAddress(10);
        InetSocketAddress live2 = new InetSocketAddress(11);
        InetSocketAddress live3 = new InetSocketAddress(12);
        InetSocketAddress live4 = new InetSocketAddress(20);
        InetSocketAddress live5 = new InetSocketAddress(21);
        InetSocketAddress unreachable1 = new InetSocketAddress(13);
        InetSocketAddress unreachable2 = new InetSocketAddress(14);
        InetSocketAddress unreachable3 = new InetSocketAddress(15);
        InetSocketAddress unreachable4 = new InetSocketAddress(16);
        InetSocketAddress unreachable5 = new InetSocketAddress(17);

        Collection<InetSocketAddress> seedHosts = Arrays.asList(seed1, seed2,
                                                                seed3, seed4);
        int quarantineDelay = 30;
        int unreachableDelay = 400;
        SystemView view = new SystemView(random, local, seedHosts,
                                         quarantineDelay, unreachableDelay);
        assertNull(view.getRandomSeedMember(seed1));
        assertEquals(seed3, view.getRandomSeedMember(local));

        view.markAlive(live1);
        view.markAlive(live2);
        view.markAlive(live3);
        view.markAlive(unreachable1);
        view.markAlive(unreachable2);
        view.markAlive(unreachable3);
        view.markAlive(unreachable4);
        view.markAlive(unreachable5);
        view.markDead(unreachable1, 0);
        view.markDead(unreachable2, 0);
        view.markDead(unreachable3, 0);
        view.markDead(unreachable4, 0);
        view.markDead(unreachable5, 0);
        view.cullQuarantined(quarantineDelay + 10);

        when(random.nextDouble()).thenReturn(0.75, 0.45, 0.0);

        assertNull(view.getRandomSeedMember(local));
        assertEquals(seed3, view.getRandomSeedMember(local));

        view.markAlive(live4);
        view.markAlive(live5);
        assertNotNull(view.getRandomSeedMember(local));
    }

    public void testLiveMembers() throws Exception {
        Random random = mock(Random.class);
        when(random.nextInt(3)).thenReturn(2);

        InetSocketAddress local = new InetSocketAddress(1);

        InetSocketAddress seed1 = new InetSocketAddress(2);
        InetSocketAddress seed2 = new InetSocketAddress(3);
        InetSocketAddress seed3 = new InetSocketAddress(4);
        InetSocketAddress seed4 = new InetSocketAddress(5);

        InetSocketAddress live1 = new InetSocketAddress(10);
        InetSocketAddress live2 = new InetSocketAddress(11);
        InetSocketAddress live3 = new InetSocketAddress(12);

        Collection<InetSocketAddress> seedHosts = Arrays.asList(seed1, seed2,
                                                                seed3, seed4);
        int quarantineDelay = 30;
        int unreachableDelay = 400;
        SystemView view = new SystemView(random, local, seedHosts,
                                         quarantineDelay, unreachableDelay);

        view.markAlive(live1);
        view.markAlive(live2);
        view.markAlive(live3);

        assertEquals(live3, view.getRandomLiveMember());
    }

    public void testUnreachableMembers() throws Exception {
        Random random = mock(Random.class);

        InetSocketAddress local = new InetSocketAddress(1);

        InetSocketAddress seed1 = new InetSocketAddress(2);
        InetSocketAddress seed2 = new InetSocketAddress(3);
        InetSocketAddress seed3 = new InetSocketAddress(4);
        InetSocketAddress seed4 = new InetSocketAddress(5);

        InetSocketAddress live1 = new InetSocketAddress(10);
        InetSocketAddress live2 = new InetSocketAddress(11);
        InetSocketAddress live3 = new InetSocketAddress(12);
        InetSocketAddress live4 = new InetSocketAddress(20);
        InetSocketAddress live5 = new InetSocketAddress(21);
        InetSocketAddress live6 = new InetSocketAddress(22);
        InetSocketAddress live7 = new InetSocketAddress(23);
        InetSocketAddress live8 = new InetSocketAddress(24);

        InetSocketAddress unreachable1 = new InetSocketAddress(13);
        InetSocketAddress unreachable2 = new InetSocketAddress(14);
        InetSocketAddress unreachable3 = new InetSocketAddress(15);
        InetSocketAddress unreachable4 = new InetSocketAddress(16);

        Collection<InetSocketAddress> seedHosts = Arrays.asList(seed1, seed2,
                                                                seed3, seed4);
        int quarantineDelay = 30;
        int unreachableDelay = 400;
        SystemView view = new SystemView(random, local, seedHosts,
                                         quarantineDelay, unreachableDelay);

        view.markAlive(live1);
        view.markAlive(live2);
        view.markAlive(live3);
        view.markAlive(live4);
        view.markAlive(live5);
        view.markAlive(live6);
        view.markAlive(live7);
        view.markAlive(live8);
        view.markAlive(unreachable1);
        view.markAlive(unreachable2);
        view.markAlive(unreachable3);
        view.markAlive(unreachable4);
        view.markDead(unreachable1, 0);
        view.markDead(unreachable2, 0);
        view.markDead(unreachable3, 0);
        view.markDead(unreachable4, 0);
        view.cullQuarantined(quarantineDelay + 10);

        when(random.nextInt(4)).thenReturn(2, 3);
        when(random.nextDouble()).thenReturn(0.75, 0.40);

        assertNull(view.getRandomUnreachableMember());
        assertEquals(unreachable3, view.getRandomUnreachableMember());
    }

    public void testQuarantined() throws Exception {
        Random random = mock(Random.class);

        InetSocketAddress local = new InetSocketAddress(1);

        InetSocketAddress seed1 = new InetSocketAddress(2);
        InetSocketAddress seed2 = new InetSocketAddress(3);
        InetSocketAddress seed3 = new InetSocketAddress(4);
        InetSocketAddress seed4 = new InetSocketAddress(5);

        InetSocketAddress live1 = new InetSocketAddress(10);
        InetSocketAddress live2 = new InetSocketAddress(11);
        InetSocketAddress live3 = new InetSocketAddress(12);
        InetSocketAddress live4 = new InetSocketAddress(20);
        InetSocketAddress live5 = new InetSocketAddress(21);
        InetSocketAddress live6 = new InetSocketAddress(22);
        InetSocketAddress live7 = new InetSocketAddress(23);
        InetSocketAddress live8 = new InetSocketAddress(24);

        Collection<InetSocketAddress> seedHosts = Arrays.asList(seed1, seed2,
                                                                seed3, seed4);
        int quarantineDelay = 30;
        int unreachableDelay = 400;
        SystemView view = new SystemView(random, local, seedHosts,
                                         quarantineDelay, unreachableDelay);

        view.markAlive(live1);
        view.markAlive(live2);
        view.markAlive(live3);
        view.markAlive(live4);
        view.markAlive(live5);
        view.markAlive(live6);
        view.markAlive(live7);
        view.markAlive(live8);

        assertFalse(view.isQuarantined(live4));

        view.markDead(live1, 0);
        assertTrue(view.isQuarantined(live1)); 
        view.cullQuarantined(quarantineDelay + 10);
        assertFalse(view.isQuarantined(live1)); 
    }
}
