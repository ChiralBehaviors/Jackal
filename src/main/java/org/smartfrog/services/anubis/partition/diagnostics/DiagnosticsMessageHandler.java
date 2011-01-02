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
package org.smartfrog.services.anubis.partition.diagnostics;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.diagnostics.msg.GetStatsMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.GetThreadsMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.SetIgnoringMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.SetTimingMsg;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.msg.untimed.SerializedMsg;

import com.hellblazer.anubis.basiccomms.nio.AbstractCommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class DiagnosticsMessageHandler extends AbstractCommunicationsHandler
        implements PartitionNotification {
    private static Logger log = Logger.getLogger(DiagnosticsMessageHandler.class.getCanonicalName());
    private Diagnostics diagnostics;

    public DiagnosticsMessageHandler(Diagnostics diagnostics,
                                     ServerChannelHandler handler,
                                     SocketChannel channel) {
        super(handler, channel);
        this.diagnostics = diagnostics;
    }

    @Override
    public void objectNotification(Object obj, int sender, long time) {
    }

    @Override
    public void partitionNotification(View view, int leader) {
        sendObject(new PartitionMsg(view, leader));
    }

    public void sendObject(Object obj) {
        try {
            SerializedMsg msg = new SerializedMsg(obj);
            super.send(msg.toWire());
        } catch (Exception ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "cannot send object " + obj, ex);
            }
        }
    }

    @Override
    protected void closing() {
        diagnostics.closing(this);
    }

    @Override
    protected void deliver(byte[] bytes) {

        SerializedMsg msg = null;
        try {
            msg = (SerializedMsg) Wire.fromWire(bytes);
        } catch (Exception ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Cannot deserialize message bytes", ex);
            }
            return;
        }

        Object obj = msg.getObject();

        if (obj instanceof SetTimingMsg) {
            SetTimingMsg m = (SetTimingMsg) obj;
            diagnostics.setTiming(m.interval, m.timeout);
        } else if (obj instanceof GetStatsMsg) {
            diagnostics.updateStats(this);
        } else if (obj instanceof SetIgnoringMsg) {
            diagnostics.setIgnoring(((SetIgnoringMsg) obj).ignoring);
        } else if (obj instanceof GetThreadsMsg) {
            diagnostics.updateThreads(this);
        } else {
            log.log(Level.SEVERE,
                    "Unrecognised object received in test connection at node"
                            + obj, new Exception());
        }
    }

    @Override
    protected void logClose(String string, IOException e) {
        log.log(Level.WARNING, "Closing diagnostic channel: " + string, e);
    }

    @Override
    protected void terminate() {
    }
}
