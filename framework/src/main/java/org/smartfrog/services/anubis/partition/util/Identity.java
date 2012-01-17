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
package org.smartfrog.services.anubis.partition.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import org.smartfrog.services.anubis.partition.wire.WireSizes;

/**
 * The identity of a partiton manager. Identity represents the identity of a
 * partiton manager. The identity is based on a magic number (to allow multiple
 * instances in the same network not to conflict), an integer id, and an epoch
 * number to differentiate incarnations of the same manager.
 */
public class Identity implements Serializable, Cloneable, WireSizes {
    public static int         MAX_ID           = 2047;             // The maximum id possible

    static final private int  magicIdx         = 0;
    static final private int  idIdx            = magicIdx + intSz;
    static final private int  epochIdx         = idIdx + intSz;
    static final public int   identityWireSz   = epochIdx + longSz;
    private static final long serialVersionUID = 1L;

    public static int getIdFromLocalIpAddress() throws IOException {
        DataInputStream dis = new DataInputStream(
                                                  new ByteArrayInputStream(
                                                                           getInterfaceHardwareAddress()));
        return dis.readInt();
    }

    public static byte[] getInterfaceHardwareAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface nint = en.nextElement();
                if (!nint.isLoopback()) {
                    return nint.getHardwareAddress();
                }
            }
            throw new IllegalStateException("No network interface");
        } catch (java.net.SocketException e) {
            throw new IllegalStateException("No network interface", e);
        }
    }

    public static int getMagicFromLocalIpAddress() throws IOException {
        DataInputStream dis = new DataInputStream(
                                                  new ByteArrayInputStream(
                                                                           getInterfaceHardwareAddress()));
        return dis.readInt();
    }

    public static int getPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.substring(0, name.indexOf('@'));
        return Integer.parseInt(pid);
    }

    public static int getProcessUniqueId() throws IOException {
        int ip = getIdFromLocalIpAddress();
        int pid = getPID();
        return (pid ^ ip) & MAX_ID;
    }

    public static Identity readWireForm(ByteBuffer bytes, int idx) {
        return new Identity(bytes.getInt(idx + magicIdx),
                            bytes.getInt(idx + idIdx),
                            bytes.getLong(idx + epochIdx));
    }

    public final long epoch;
    public final int  id;

    public final int  magic;

    public Identity(ByteBuffer buffer) {
        magic = buffer.getInt();
        id = buffer.getInt();
        assert id <= MAX_ID;
        epoch = buffer.getLong();
    }

    /**
     * constructor - takes a given magic number, id and epoch to construct the
     * identity object from.
     */
    public Identity(int magic, int id, long epoch) {
        if (id > MAX_ID) {
            throw new IllegalArgumentException("Id cannot be > " + MAX_ID);
        }
        this.magic = magic;
        this.id = id;
        this.epoch = epoch;
    }

    /**
     * This method is similar to the readObject() method but is not part of the
     * Serializable interface. This can be used to unmarshall this object
     * including the super class properties, without using that interface. There
     * are no descriptors, so the user needs to know what objects and what
     * versions to unmarshall from a stream by other means.
     * 
     * @param s
     * @throws IOException
     */
    public Identity(ObjectInputStream s) throws IOException {
        id = s.readInt();
        assert id < MAX_ID;
        magic = s.readInt();
        epoch = s.readLong();
    }

    /**
     * create another identity object like this one.
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * does the identitiy match on epoch
     */
    public boolean equalEpoch(Identity identity) {
        return epoch == identity.epoch;
    }

    /**
     * does the identity match on magic and id (not epoch)
     */
    public boolean equalId(Identity identity) {
        return id == identity.id;
    }

    /**
     * does the identity match on magic number
     */
    public boolean equalMagic(Identity identity) {
        return magic == identity.magic;
    }

    /**
     * Equality test: does not use epoch. Defines the equality relation for
     * identities - used in containers e.g. hash tables. - note: does not use
     * epoch for equality!!!! TWO IDENTITIES ARE EQUIVALENT IF THE HAVE THE SAME
     * ID AND MAGIC NUMBER
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Identity) {
            Identity identity = (Identity) obj;
            return magic == identity.magic && id == identity.id;
        }

        return false;
    }

    /**
     * a hash code for the epoch - used in containers.
     */
    @Override
    public int hashCode() {
        return magic + id * 101;
    }

    /**
     * for debug purposes
     */
    @Override
    public String toString() {
        // return "<id=" + id + ", " + magic + ", " + epoch + ">";
        return "<id " + (id >= 0 ? id : "unknown") + " >";
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.putInt(magic);
        buffer.putInt(id);
        buffer.putLong(epoch);
    }

    /**
     * This method is similar to the writeObject() method but is not part of the
     * Serializable interface. This can be used to marshall this object
     * including the super class properties, without using that interface. It
     * does not serialize descriptors, so the use needs to know what objects and
     * what versions to unmarshall from a stream by other means.
     * 
     * @param s
     * @throws IOException
     */
    public void writeToObjectStream(ObjectOutputStream s) throws IOException {
        s.writeInt(id);
        s.writeInt(magic);
        s.writeLong(epoch);
    }

    public void writeWireForm(ByteBuffer bytes, int idx) {
        bytes.putInt(idx + magicIdx, magic);
        bytes.putInt(idx + idIdx, id);
        bytes.putLong(idx + epochIdx, epoch);
    }

    protected void readHeader() {
    }

    protected void readMessage() {
    }

    protected void writeHeader() {
    }

    protected void writeMessage() {
    }
}
