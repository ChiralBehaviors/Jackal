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

import java.net.InetSocketAddress;

import com.hellblazer.anubis.rst.Channel;
import com.hellblazer.anubis.rst.ThisChannel;

/**
 * Implementation of the channel representing the local node.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class LocalChannel extends AbstractChannel implements ThisChannel {

    public LocalChannel(int index, UdpService udpService,
                        InetSocketAddress memberAddress) {
        super(index, udpService, memberAddress);
    }

    @Override
    public void addChild(Channel child) {
        if (child == this) {
            return; // Can't be a child of one's self.
        }
    }

    @Override
    public void markGreen() {
        // TODO Auto-generated method stub

    }

    @Override
    public void markRed() {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeChild(Channel child) {
        if (child == this) {
            return; // Can't be a child of one's self.
        }
    }

    @Override
    public void setRoot(int root) {
        // TODO Auto-generated method stub

    }

}
