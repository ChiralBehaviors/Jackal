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




import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.smartfrog.services.anubis.partition.wire.WireSizes;



/**
 * The ConnectionComms class is an abstract base class for connection
 * endpoints. The endpoint is used to send and receive objects using
 * object streams over a tcp connection. There are two methods to be
 * implemented: deliverObject is called to handle objects revceived over
 * the connection; is called to indicates that the connection is being closed.
 * Connection closure can occur in response to a remote end closure, a local
 * end closure (shutdown method) or an exception while sending or receving
 * an object.
 */

public abstract class ConnectionComms extends Thread implements WireSizes {


            private SocketChannel      connection;
volatile    private boolean            open;
            private byte[]             headerBytesIn = new byte[HEADER_SIZE];
            private byte[]             headerBytesOut = new byte[HEADER_SIZE];



    /**
     * constructor - creates a tcp connection with the remote address
     * provided as a parameter. If the constructor fails to create
     * the tcp connection it will result in a connection comms object
     * in the closed state: connected() returns false.
     */
    public ConnectionComms(String threadName, ConnectionAddress address)  {

        super(threadName);

        try {

            connection = SocketChannel.open();
            connection.configureBlocking(true);
            connection.connect(new InetSocketAddress(address.ipaddress, address.port));
            connection.socket().setTcpNoDelay(true);
            connection.socket().setKeepAlive(true);
            open = true;

        } catch(Exception ex) {

            // ex.printStackTrace();

            try { connection.close(); }
            catch(Exception ex2) { }
            open = false;

        }

    }



    /**
     * constructor - used to create a ConnectionComms object for an
     * existing tcp socket connection. This is typically used by a connection
     * factory when a ConnectionServer object receives a connection request.
     */

    public ConnectionComms(String threadName, SocketChannel channel) {

        super(threadName);

        connection = channel;

        try {

            connection.socket().setTcpNoDelay(true);
            connection.socket().setKeepAlive(true);
            open = true;

        } catch(Exception ex) {

            // ex.printStackTrace();

            try { connection.close(); }
            catch(Exception ex2) { }
            open = false;

        }

    }



    /**
     * called to indicate that the tcp connection is closing.
     * Not called if the constructor initially fails - i.e. the
     * connection was never open.
     */
    public abstract void closing();

    /**
     * deliverObject is called to deliver an object received on this
     * connection.
     */
    public abstract void deliver(byte[] bytes);


    /**
     * used to send an object on this connection
     */
    public void send(byte[] bytes) {

        try {

            synchronized( connection ) {

                ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytesOut);
                headerBuffer.putInt(0, MAGIC_NUMBER);
                headerBuffer.putInt(4, bytes.length);

                ByteBuffer msgBuffer = ByteBuffer.wrap(bytes);

                connection.write(headerBuffer);
                connection.write(msgBuffer);
            }

        } catch(IOException  ioex) {
            logClose("blocking transport encoutented exception when sending", ioex);
            shutdown();
        }

    }



    /**

     * used to terminate this connection

     */

    public void shutdown() {

        open = false;
        closing();

        try {
            connection.close();
        } catch(Exception ex) {}

    }



    /**
     * returns the state of this connection. Synchronized to avoid
     * reading the status while it is changing.
     */
    public synchronized boolean connected() {
        return open;
    }



    /**
     * The receive object loop forms the main thread loop. When an object is
     * received it is delivered. If the connection is closed at the other end
     * or the read fails the connection is terminated.
     */
    public void run() {

        while(open) {

            try {

                ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytesIn);

                if( connection.read(headerBuffer) < HEADER_SIZE )
                    throw new IOException("not enough bytes reading header");


                if( headerBuffer.getInt(0) != MAGIC_NUMBER )
                    throw new IOException("incorrect magic number in header");

                int length = headerBuffer.getInt(4);

                byte[] msg = new byte[length];
                ByteBuffer msgBuffer = ByteBuffer.wrap(msg);

                if( connection.read(msgBuffer) < length )
                    throw new IOException("not enough bytes reading body");

                deliver(msg);

            } catch(IOException ex) {
                logClose("blocking transport encountered io exception", ex);
                shutdown();
            }

        }

    }


    public void logClose(String reason, Throwable throwable) {
        return;
    }

    /**
     * returns an string representing the status of this thread
     */
    public String getThreadStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.getName()).append(" ............................ ").setLength(30);
        buffer.append(super.isAlive() ? ".. is Alive " : ".. is Dead ");
        buffer.append(open ? ".. running ....." : ".. terminated ..");
        buffer.append( " address = " ).append(connection.socket().getInetAddress()+":"+connection.socket().getPort());
        return buffer.toString();
    }
}

