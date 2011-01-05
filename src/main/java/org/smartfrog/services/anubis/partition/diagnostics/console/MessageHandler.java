/** (C) Copyright 2010 Hal Hildebrand, all rights reserved.

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.smartfrog.services.anubis.partition.diagnostics.console;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.msg.untimed.SerializedMsg;

import com.hellblazer.anubis.basiccomms.nio.AbstractCommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class MessageHandler extends AbstractCommunicationsHandler {
    private static Logger log = Logger.getLogger(MessageHandler.class.getCanonicalName());

    private final Controller controller;
    private final NodeData node;

    public MessageHandler(Controller controller, NodeData node,
                          ServerChannelHandler handler, SocketChannel channel) {
        super(handler, channel);
        this.controller = controller;
        this.node = node;
    }

    public void sendObject(Object obj) {
        SerializedMsg msg = new SerializedMsg(obj);
        try {
            super.send(msg.toWire());
        } catch (Exception ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Cannot sent diagnostics object " + obj,
                        ex);
            }
        }
    }

    @Override
    protected void closing() {
        controller.disconnectNode(node);
    }

    @Override
    protected void deliver(byte[] bytes) {
        SerializedMsg msg = null;
        try {
            msg = (SerializedMsg) Wire.fromWire(bytes);
            Object obj = msg.getObject();

            controller.deliverObject(obj, node);
        } catch (Exception ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING,
                        "Cannot deliver diagnostics message bytes", ex);
            }
        }
    }

    @Override
    protected void logClose(String string, IOException e) {
        log.log(Level.FINE, "closing message handler - " + string, e);
    }

    @Override
    protected void terminate() {
        close();
    }

}
