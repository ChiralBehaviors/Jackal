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
package com.hellblazer.anubis.rst.udp;

import static com.hellblazer.anubis.rst.udp.AbstractChannel.Color.GREEN;
import static com.hellblazer.anubis.rst.udp.AbstractChannel.Color.RED;

import java.net.InetSocketAddress;

import com.hellblazer.anubis.rst.Channel;

/**
 * The the common implementation of a UDP based message channel to another node.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
abstract public class AbstractChannel implements Channel {
    static enum Color {
        RED, GREEN;
    }

    protected final UdpService service;
    protected final InetSocketAddress address;
    protected volatile Color color;
    protected final int id;
    protected volatile int root = -1;

    public AbstractChannel(int index, UdpService udpService,
                           InetSocketAddress memberAddress) {
        id = index;
        service = udpService;
        address = memberAddress;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getRoot() {
        return root;
    }

    @Override
    public boolean isGreen() {
        return color == GREEN;
    }

    @Override
    public boolean isRed() {
        return color == RED;
    }

}