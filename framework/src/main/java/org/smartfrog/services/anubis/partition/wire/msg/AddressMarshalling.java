package org.smartfrog.services.anubis.partition.wire.msg;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.wire.WireSizes;

final public class AddressMarshalling implements WireSizes {
    static final private Logger log                     = LoggerFactory.getLogger(AddressMarshalling.class.getCanonicalName());
    static final private int    addressIdx              = intSz;
    static final private int    lengthIdx               = 0;
    static final private int    nullAddress             = 0;
    static final private int    portIdx                 = addressIdx
                                                          + maxInetAddressSz;
    static final public int     connectionAddressWireSz = portIdx + intSz;

    public static InetSocketAddress readWireForm(ByteBuffer bytes, int idx) {
        int length = bytes.getInt(idx + lengthIdx);
        if (length == nullAddress) {
            return null;
        }

        byte[] address = new byte[length];
        for (int i = 0; i < address.length; i++) {
            address[i] = bytes.get(idx + addressIdx + i);
        }
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException ex) {
            log.warn("Unknown host when unmarshalling socket address", ex);
            return null;
        }

        int port = bytes.getInt(idx + portIdx);
        return new InetSocketAddress(inetAddress, port);
    }

    public static void writeNullWireForm(ByteBuffer bytes, int idx) {
        bytes.putInt(idx, nullAddress);
    }

    public static void writeWireForm(InetSocketAddress ipaddress,
                                     ByteBuffer bytes, int idx) {
        byte[] address = ipaddress.getAddress().getAddress();
        bytes.putInt(idx + lengthIdx, address.length);
        for (int i = 0; i < address.length; i++) {
            bytes.put(idx + addressIdx + i, address[i]);
        }
        bytes.putInt(idx + portIdx, ipaddress.getPort());
    }

    private AddressMarshalling() {
        // no instances
    }
}
