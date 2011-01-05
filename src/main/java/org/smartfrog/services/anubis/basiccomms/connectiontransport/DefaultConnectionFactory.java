/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

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

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.basiccomms.connectiontransport;

import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default connection factory is used by ConnectionServer when no other
 * factory has been set. The default immediately terminates connection attempts.
 */

public class DefaultConnectionFactory implements ConnectionFactory {
    private static final Logger log = Logger.getLogger(DefaultConnectionFactory.class.getCanonicalName());

    @Override
    public void createConnection(SocketChannel channel) {
        try {
            if (log.isLoggable(Level.FINER)) {
                log.finer("Immediately closing connection: " + channel);
            }
            channel.close();
        } catch (Exception ex) {
        }
    }

}
