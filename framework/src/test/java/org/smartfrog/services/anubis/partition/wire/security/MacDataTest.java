/** 
 * (C) Copyright 2012 Hal Hildebrand, All Rights Reserved
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
package org.smartfrog.services.anubis.partition.wire.security;

import java.nio.ByteBuffer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import static junit.framework.Assert.*;

/**
 * @author hhildebrand
 * 
 */
public class MacDataTest {

    @Test
    public void testMac() throws Exception {
        String macType = "HmacSHA1";
        byte[] keyData2 = { (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad,
                (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad,
                (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad,
                (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad,
                (byte) 0x23, (byte) 0x45, (byte) 0x83, (byte) 0xad };

        SecretKey sk2 = new SecretKeySpec(keyData2, macType);

        int endOffset = 15;
        ByteBuffer data1 = ByteBuffer.wrap(new byte[] {
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
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        ByteBuffer data2 = ByteBuffer.wrap(new byte[] {
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
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        MACData m1a = new MACData();
        MACData m2a = new MACData();
        m2a.setKey(sk2);
        MACData m1b = new MACData();
        MACData m2b = new MACData();
        m2b.setKey(sk2);
        data1.position(endOffset);
        data2.position(endOffset);
        m1a.addMAC(data1);
        m2a.addMAC(data2);

        data1.rewind();
        data2.rewind();
        try { // m1a & m1b
            m1b.checkMAC(data1);
        } catch (SecurityException e) {
            fail("test1 failed");
        }

        try { // m2a & m2b
            m2b.checkMAC(data2);
        } catch (SecurityException e) {
            fail("test2 failed");
        }

        data1.rewind();
        data2.rewind();
        try { // m1a & m2b
            m2b.checkMAC(data1);
            fail("test3 failed");
        } catch (SecurityException e) {
            // expected
        }

        try { // m2a & m1b
            m1b.checkMAC(data2);
            fail("test4 failed");
        } catch (SecurityException e) {
            // expected
        }

        data1.rewind();
        data2.rewind();

        data1.put(3, (byte) 0xff);
        try { // m1a & mod data & m1b
            m1b.checkMAC(data1);
            fail("test5 failed");
        } catch (SecurityException e) {
            // expected
        }

        data2.put(32, (byte) 0xff);
        try { // m2a & mod mac & m2b
            m2b.checkMAC(data2);
            fail("test6 failed");
        } catch (SecurityException e) {
            // expected
        }
    }
}
