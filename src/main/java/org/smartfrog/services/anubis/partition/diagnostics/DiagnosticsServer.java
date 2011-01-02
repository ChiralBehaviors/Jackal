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

import java.nio.channels.SocketChannel;
import java.util.Collection;

import com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class DiagnosticsServer extends ServerChannelHandler {
    private Diagnostics diagnostics;

    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        DiagnosticsMessageHandler handler = new DiagnosticsMessageHandler(
                                                                          diagnostics,
                                                                          this,
                                                                          accepted);
        diagnostics.newHandler(handler);
        return handler;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DiagnosticsMessageHandler> getOpenHandlers() {
        return (Collection<DiagnosticsMessageHandler>) super.getOpenHandlers();
    }
}
