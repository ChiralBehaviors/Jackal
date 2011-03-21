package org.smartfrog.services.anubis.partition.wire.security;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Calculate/check the MAC associated with data in a byte array. The MAC is
 * assumed to belong at the end of the byte array containing the data
 * <p/>
 * Can statically set a default key that will be used for all subsequent created
 * MACData objects
 * <p/>
 * To cope with distributed key update being non-transactional, it will check
 * the MAC with the current and the last key but generates MAC with the latest
 * key only.
 */
public class MACData {
    private static byte[] defaultKeyData = { (byte) 0x23, (byte) 0x45,
            (byte) 0x83, (byte) 0xad, (byte) 0x23, (byte) 0x46, (byte) 0x83,
            (byte) 0xad, (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad,
            (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad, (byte) 0x23,
            (byte) 0x45, (byte) 0x83, (byte) 0xad };
    private static int    macSize        = 20;        //size in bytes 

    private static String macType        = "HmacSHA1"; //Use HmacSHA512 for better protection

    public static void main(String[] args) {
        //for testing
        try {
            byte[] keyData2 = { (byte) 0x23, (byte) 0x45, (byte) 0x83,
                    (byte) 0xad, (byte) 0x23, (byte) 0x45, (byte) 0x83,
                    (byte) 0xad, (byte) 0x23, (byte) 0x45, (byte) 0x83,
                    (byte) 0xad, (byte) 0x23, (byte) 0x45, (byte) 0x83,
                    (byte) 0xad, (byte) 0x23, (byte) 0x45, (byte) 0x83,
                    (byte) 0xad };

            SecretKey sk2 = new SecretKeySpec(keyData2, macType);

            int startOffset = 0;
            int endOffset = 15;
            byte[] data1 = {
                    //the data
                    (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                    (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14,
                    (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24,
                    (byte) 0x31,
                    (byte) 0x32,
                    (byte) 0x33,
                    (byte) 0x34,
                    //the space for the hmac
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

            byte[] data2 = {
                    //the data
                    (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4,
                    (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14,
                    (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24,
                    (byte) 0x31,
                    (byte) 0x32,
                    (byte) 0x33,
                    (byte) 0x34,
                    //the space for the hmac
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

            MACData m1a = new MACData();
            MACData m2a = new MACData();
            m2a.setKey(sk2);
            MACData m1b = new MACData();
            MACData m2b = new MACData();
            m2b.setKey(sk2);

            m1a.addMAC(data1, startOffset, endOffset);
            m2a.addMAC(data2, startOffset, endOffset);

            try { // m1a & m1b
                m1b.checkMAC(data1, startOffset, endOffset);
                System.out.println("test1 succeeded");
            } catch (SecurityException e) {
                System.out.println("test1 failed");
            }

            try { // m2a & m2b
                m2b.checkMAC(data2, startOffset, endOffset);
                System.out.println("test2 succeeded");
            } catch (SecurityException e) {
                System.out.println("test2 failed");
            }

            try { // m1a & m2b
                m2b.checkMAC(data1, startOffset, endOffset);
                System.out.println("test3 failed");
            } catch (SecurityException e) {
                System.out.println("test3 succeeded");
            }

            try { // m2a & m1b
                m1b.checkMAC(data2, startOffset, endOffset);
                System.out.println("test4 failed");
            } catch (SecurityException e) {
                System.out.println("test4 succeeded");
            }

            try { // m1a & mod data & m1b
                data1[3] = (byte) 0xff;
                m1b.checkMAC(data1, startOffset, endOffset);
                System.out.println("test5 failed");
            } catch (SecurityException e) {
                System.out.println("test5 succeeded");
            }

            try { // m2a & mod mac & m2b
                data2[32] = (byte) 0xff;
                m2b.checkMAC(data2, startOffset, endOffset);
                System.out.println("test6 failed");
            } catch (SecurityException e) {
                System.out.println("test6 succeeded");
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private Mac currentMAC = null;
    private Mac defaultMAC = null;                                      //only to be used if no key explicitly set 
    private Key key        = new SecretKeySpec(defaultKeyData, macType);

    private Mac lastMAC    = null;

    public MACData() throws NoSuchAlgorithmException, InvalidKeyException {
        defaultMAC = Mac.getInstance(macType);
        defaultMAC.init(key);
    }

    /**
     * Adds a MAC of the data to the byte array following the data
     * 
     * @param data
     *            - the buffer holding the data for the MAC, and the MAC
     * @param start
     *            - inclusive start of data
     * @param end
     *            - inclusive end of data, mac added after this
     * @throws javax.crypto.ShortBufferException
     *             - if the buffer is insufficiently long to hold the mac
     * @throws SecurityException
     *             - no key yet provided for MAC calculation
     */
    public synchronized void addMAC(byte[] data, int start, int end)
                                                                    throws ShortBufferException,
                                                                    SecurityException {
        Mac mac = currentMAC == null ? defaultMAC : currentMAC;
        if (mac == null) {
            throw new SecurityException("no key for mac calculation");
        }
        mac.reset();
        mac.update(data, start, end - start + 1);
        mac.doFinal(data, end + 1);
    }

    /**
     * validate a mac that is at the end of a piece of byte array data
     * 
     * @param data
     *            to validate
     * @param start
     *            of the data - inclusive
     * @param end
     *            of the data - inlcusive, mac present after this
     * @throws SecurityException
     *             - the mac does not match
     */
    public synchronized void checkMAC(byte[] data, int start, int end)
                                                                      throws SecurityException {
        if (currentMAC != null) {
            if (validateMac(currentMAC, data, start, end)) {
                return;
            }
        }
        if (lastMAC != null) {
            if (validateMac(lastMAC, data, start, end)) {
                return;
            }
        }
        if (defaultMAC != null) {
            if (validateMac(defaultMAC, data, start, end)) {
                return;
            }
        }
        throw new SecurityException("MAC not valid");
    }

    public synchronized Key getKey() {
        return key;
    }

    /**
     * Return the size, in bytes, of the MAC
     * 
     * @return the size in bytes
     */
    public int getMacSize() {
        return macSize;
    }

    /**
     * Set the current key to use for the MAC. Makes the existing current key
     * the last key. The default key is destroyed for this MACData object.
     * 
     * @param k
     *            the key to use for the MAC
     * @throws InvalidKeyException
     */
    public synchronized void setKey(Key k) throws InvalidKeyException {
        lastMAC = currentMAC;
        defaultMAC = null; //eliminate the default...
        try {
            currentMAC = Mac.getInstance(macType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(); //shouldn't happen as predefined to work...
        }
        currentMAC.init(k);
    }

    private boolean validateMac(Mac m, byte[] data, int start, int end) {
        m.reset();
        m.update(data, start, end - start + 1);
        byte[] checkMAC = m.doFinal();
        int len = checkMAC.length;

        for (int i = 0; i < len; i++) {
            if (checkMAC[i] != data[end + 1 + i]) {
                return false;
            }
        }
        return true;
    }
}