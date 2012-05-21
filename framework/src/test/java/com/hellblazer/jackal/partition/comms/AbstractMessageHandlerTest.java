/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.partition.comms;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;
import org.smartfrog.services.anubis.partition.wire.WireSizes;
import org.smartfrog.services.anubis.partition.wire.msg.SerializedMsg;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.pinkie.SocketChannelHandler;

/**
 * @author hhildebrand
 * 
 */
public class AbstractMessageHandlerTest {
    private static Logger log = LoggerFactory.getLogger(AbstractMessageHandlerTest.class);

    private static class InboundMsg {
        final long    order;
        final WireMsg msg;

        public InboundMsg(long order, WireMsg msg) {
            super();
            this.order = order;
            this.msg = msg;
        }
    }

    private static class MsgHandler extends AbstractMessageHandler {
        long             sequence = 0;
        List<InboundMsg> msgs     = new ArrayList<InboundMsg>();

        /**
         * @param wireSecurity
         */
        public MsgHandler(WireSecurity wireSecurity) {
            super(wireSecurity);
        }

        /* (non-Javadoc)
         * @see com.hellblazer.pinkie.CommunicationsHandler#accept(com.hellblazer.pinkie.SocketChannelHandler)
         */
        @Override
        public void accept(SocketChannelHandler handler) {
            this.handler = handler;
        }

        /* (non-Javadoc)
         * @see com.hellblazer.pinkie.CommunicationsHandler#closing()
         */
        @Override
        public void closing() {
        }

        /* (non-Javadoc)
         * @see com.hellblazer.pinkie.CommunicationsHandler#connect(com.hellblazer.pinkie.SocketChannelHandler)
         */
        @Override
        public void connect(SocketChannelHandler handler) {
            this.handler = handler;
        }

        /* (non-Javadoc)
         * @see com.hellblazer.jackal.partition.comms.AbstractMessageHandler#deliverObject(long, java.nio.ByteBuffer)
         */
        @Override
        protected void deliverObject(long order, ByteBuffer readBuffer) {
            try {
                msgs.add(new InboundMsg(order,
                                        wireSecurity.fromWireForm(readBuffer)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /* (non-Javadoc)
         * @see com.hellblazer.jackal.partition.comms.AbstractMessageHandler#getLog()
         */
        @Override
        protected Logger getLog() {
            return log;
        }

        public void send(WireMsg msg) throws WireFormException, IOException {
            sendObject(wireSecurity.toWireForm(msg, bufferPool));
        }

        /* (non-Javadoc)
         * @see com.hellblazer.jackal.partition.comms.AbstractMessageHandler#nextSequence()
         */
        @Override
        protected long nextSequence() {
            return sequence++;
        }

    }

    @Test
    public void testBulkWrite() throws Exception {
        final NoSecurityImpl wireSecurity = new NoSecurityImpl();
        MsgHandler msgHandler = new MsgHandler(wireSecurity);
        SocketChannelHandler handler = mock(SocketChannelHandler.class);
        SocketChannel channel = mock(SocketChannel.class);

        final List<SerializedMsg> msgs = new ArrayList<SerializedMsg>();

        Answer<Long> bulkWrite = new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer[] buffers = (ByteBuffer[]) invocation.getArguments()[0];
                long bytes = 0;
                for (ByteBuffer b : buffers) {
                    bytes += b.remaining();
                }
                assertEquals("Wrong number of buffers", 8, buffers.length);

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[0].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[0].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[0].getInt(), buffers[1].remaining());
                assertEquals("Expected order 0", 0, buffers[0].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[1]));

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[2].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[2].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[2].getInt(), buffers[3].remaining());
                assertEquals("Expected order 1", 1, buffers[2].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[3]));

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[4].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[4].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[4].getInt(), buffers[5].remaining());
                assertEquals("Expected order 2", 2, buffers[4].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[5]));

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[6].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[6].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[6].getInt(), buffers[7].remaining());
                assertEquals("Expected order 3", 3, buffers[6].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[7]));
                return bytes;
            }
        };
        ByteBuffer[] template = new ByteBuffer[0];
        when(handler.getChannel()).thenReturn(channel);
        when(channel.write(any(template.getClass()), anyInt(), anyInt())).thenAnswer(bulkWrite).thenReturn(0L);

        msgHandler.connect(handler);

        SerializedMsg msg1 = new SerializedMsg("Give me Slack");
        SerializedMsg msg2 = new SerializedMsg("Or give me Food");
        SerializedMsg msg3 = new SerializedMsg("Or kill me");
        SerializedMsg msg4 = new SerializedMsg("Hello World");

        msgHandler.send(msg1);
        msgHandler.send(msg2);
        msgHandler.send(msg3);
        msgHandler.send(msg4);

        msgHandler.writeReady();

        assertEquals("Messages were not written", 4, msgs.size());

        assertEquals("Wrong message", msg1.getObject(), msgs.get(0).getObject());
        assertEquals("Wrong message", msg2.getObject(), msgs.get(1).getObject());
        assertEquals("Wrong message", msg3.getObject(), msgs.get(2).getObject());
        assertEquals("Wrong message", msg4.getObject(), msgs.get(3).getObject());
    }

    @Test
    public void testWrite() throws Exception {
        final NoSecurityImpl wireSecurity = new NoSecurityImpl();
        MsgHandler msgHandler = new MsgHandler(wireSecurity);
        SocketChannelHandler handler = mock(SocketChannelHandler.class);
        SocketChannel channel = mock(SocketChannel.class);

        final List<SerializedMsg> msgs = new ArrayList<SerializedMsg>();

        Answer<Long> bulkWrite = new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer[] buffers = (ByteBuffer[]) invocation.getArguments()[0];
                long bytes = 0;
                for (ByteBuffer b : buffers) {
                    bytes += b.remaining();
                }
                assertEquals("Wrong number of buffers", 8, buffers.length);

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[0].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[0].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[0].getInt(), buffers[1].remaining());
                assertEquals("Expected order 0", 0, buffers[0].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[1]));

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[2].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[2].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[2].getInt(), buffers[3].remaining());
                assertEquals("Expected order 1", 1, buffers[2].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[3]));

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[4].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[4].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[4].getInt(), buffers[5].remaining());
                assertEquals("Expected order 2", 2, buffers[4].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[5]));

                assertEquals("Wrong header buffer size",
                             AbstractMessageHandler.HEADER_BYTE_SIZE,
                             buffers[6].remaining());
                assertEquals("Invalid magic", WireSizes.MAGIC_NUMBER,
                             buffers[6].getInt());
                assertEquals("Invalid message buffer length",
                             buffers[6].getInt(), buffers[7].remaining());
                assertEquals("Expected order 3", 3, buffers[6].getLong());
                msgs.add((SerializedMsg) wireSecurity.fromWireForm(buffers[7]));
                return bytes;
            }
        };
        ByteBuffer[] template = new ByteBuffer[0];
        when(handler.getChannel()).thenReturn(channel);
        when(channel.write(any(template.getClass()), anyInt(), anyInt())).thenReturn(0L).thenAnswer(bulkWrite);

        msgHandler.connect(handler);

        SerializedMsg msg1 = new SerializedMsg("Give me Slack");
        SerializedMsg msg2 = new SerializedMsg("Or give me Food");
        SerializedMsg msg3 = new SerializedMsg("Or kill me");
        SerializedMsg msg4 = new SerializedMsg("Hello World");

        msgHandler.send(msg1);
        msgHandler.send(msg2);
        msgHandler.send(msg3);
        msgHandler.send(msg4);

        msgHandler.writeReady();
        assertEquals("Messages were written", 4, msgs.size());

        assertEquals("Wrong message", msg1.getObject(), msgs.get(0).getObject());
        assertEquals("Wrong message", msg2.getObject(), msgs.get(1).getObject());
        assertEquals("Wrong message", msg3.getObject(), msgs.get(2).getObject());
        assertEquals("Wrong message", msg4.getObject(), msgs.get(3).getObject());
    }

    @Test
    public void testBulkRead() throws Exception {
        final NoSecurityImpl wireSecurity = new NoSecurityImpl();
        final MsgHandler msgHandler = new MsgHandler(wireSecurity);
        SocketChannelHandler handler = mock(SocketChannelHandler.class);
        SocketChannel channel = mock(SocketChannel.class);

        final SerializedMsg msg1 = new SerializedMsg("Give me Slack");
        final SerializedMsg msg2 = new SerializedMsg("Or give me Food");
        final SerializedMsg msg3 = new SerializedMsg("Or kill me");
        final SerializedMsg msg4 = new SerializedMsg("Hello World");

        Answer<Long> bulkRead = new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                assertNotNull(buffer);
                long bytes = 0;
                ByteBuffer temp = wireSecurity.toWireForm(msg1,
                                                          msgHandler.bufferPool);
                bytes += temp.remaining()
                         + AbstractMessageHandler.HEADER_BYTE_SIZE;
                buffer.putInt(WireSizes.MAGIC_NUMBER);
                buffer.putInt(temp.remaining());
                buffer.putLong(msgHandler.nextSequence());
                buffer.put(temp);

                temp = wireSecurity.toWireForm(msg2, msgHandler.bufferPool);
                bytes += temp.remaining()
                         + AbstractMessageHandler.HEADER_BYTE_SIZE;
                buffer.putInt(WireSizes.MAGIC_NUMBER);
                buffer.putInt(temp.remaining());
                buffer.putLong(msgHandler.nextSequence());
                buffer.put(temp);

                temp = wireSecurity.toWireForm(msg3, msgHandler.bufferPool);
                bytes += temp.remaining()
                         + AbstractMessageHandler.HEADER_BYTE_SIZE;
                buffer.putInt(WireSizes.MAGIC_NUMBER);
                buffer.putInt(temp.remaining());
                buffer.putLong(msgHandler.nextSequence());
                buffer.put(temp);

                temp = wireSecurity.toWireForm(msg4, msgHandler.bufferPool);
                bytes += temp.remaining()
                         + AbstractMessageHandler.HEADER_BYTE_SIZE;
                buffer.putInt(WireSizes.MAGIC_NUMBER);
                buffer.putInt(temp.remaining());
                buffer.putLong(msgHandler.nextSequence());
                buffer.put(temp);

                return bytes;
            }
        };
        when(handler.getChannel()).thenReturn(channel);
        when(channel.read(any(ByteBuffer.class))).thenAnswer(bulkRead).thenReturn(0);

        msgHandler.connect(handler);

        msgHandler.readReady();
        assertEquals("Messages were not read", 4, msgHandler.msgs.size());

        assertEquals("Wrong message", msg1.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(0).msg).getObject());
        assertEquals("Wrong order", 0, msgHandler.msgs.get(0).order);
        assertEquals("Wrong message", msg2.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(1).msg).getObject());
        assertEquals("Wrong order", 1, msgHandler.msgs.get(1).order);
        assertEquals("Wrong message", msg3.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(2).msg).getObject());
        assertEquals("Wrong order", 2, msgHandler.msgs.get(2).order);
        assertEquals("Wrong message", msg4.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(3).msg).getObject());
        assertEquals("Wrong order", 3, msgHandler.msgs.get(3).order);
    }

    @Test
    public void testPartialReads() throws Exception {
        NoSecurityImpl wireSecurity = new NoSecurityImpl();
        MsgHandler msgHandler = new MsgHandler(wireSecurity);
        SocketChannelHandler handler = mock(SocketChannelHandler.class);
        SocketChannel channel = mock(SocketChannel.class);

        SerializedMsg msg1 = new SerializedMsg("Give me Slack");
        SerializedMsg msg2 = new SerializedMsg("Or give me Food");
        SerializedMsg msg3 = new SerializedMsg("Or kill me");
        SerializedMsg msg4 = new SerializedMsg("Hello World");
        List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();

        int totalBytes = 0;
        ByteBuffer temp = wireSecurity.toWireForm(msg1, msgHandler.bufferPool);
        ByteBuffer header = ByteBuffer.allocate(AbstractMessageHandler.HEADER_BYTE_SIZE);
        bufferList.add(header);
        bufferList.add(temp);
        header.putInt(WireSizes.MAGIC_NUMBER);
        header.putInt(temp.remaining());
        header.putLong(msgHandler.nextSequence());
        header.flip();
        totalBytes += temp.remaining() + header.remaining();

        header = ByteBuffer.allocate(AbstractMessageHandler.HEADER_BYTE_SIZE);
        temp = wireSecurity.toWireForm(msg2, msgHandler.bufferPool);
        header.putInt(WireSizes.MAGIC_NUMBER);
        header.putInt(temp.remaining());
        header.putLong(msgHandler.nextSequence());
        bufferList.add(header);
        bufferList.add(temp);
        header.flip();
        totalBytes += temp.remaining() + header.remaining();

        header = ByteBuffer.allocate(AbstractMessageHandler.HEADER_BYTE_SIZE);
        temp = wireSecurity.toWireForm(msg3, msgHandler.bufferPool);
        header.putInt(WireSizes.MAGIC_NUMBER);
        header.putInt(temp.remaining());
        header.putLong(msgHandler.nextSequence());
        bufferList.add(header);
        bufferList.add(temp);
        header.flip();
        totalBytes += temp.remaining() + header.remaining();

        header = ByteBuffer.allocate(AbstractMessageHandler.HEADER_BYTE_SIZE);
        temp = wireSecurity.toWireForm(msg4, msgHandler.bufferPool);
        header.putInt(WireSizes.MAGIC_NUMBER);
        header.putInt(temp.remaining());
        header.putLong(msgHandler.nextSequence());
        bufferList.add(header);
        bufferList.add(temp);
        header.flip();
        totalBytes += temp.remaining() + header.remaining();

        final ByteBuffer[] buffers = bufferList.toArray(new ByteBuffer[bufferList.size()]);
        final int readLength = 4;

        Answer<Long> bulkRead = new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                assertNotNull(buffer);
                return transfer(readLength, buffers, buffer);
            }
        };
        when(handler.getChannel()).thenReturn(channel);
        when(channel.read(any(ByteBuffer.class))).thenAnswer(bulkRead);

        msgHandler.connect(handler);

        for (int sent = 0; sent < totalBytes; sent += readLength) {
            msgHandler.readReady();
        }
        assertEquals("Messages were not read", 4, msgHandler.msgs.size());

        assertEquals("Wrong message", msg1.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(0).msg).getObject());
        assertEquals("Wrong order", 0, msgHandler.msgs.get(0).order);
        assertEquals("Wrong message", msg2.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(1).msg).getObject());
        assertEquals("Wrong order", 1, msgHandler.msgs.get(1).order);
        assertEquals("Wrong message", msg3.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(2).msg).getObject());
        assertEquals("Wrong order", 2, msgHandler.msgs.get(2).order);
        assertEquals("Wrong message", msg4.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(3).msg).getObject());
        assertEquals("Wrong order", 3, msgHandler.msgs.get(3).order);
    }

    @Test
    public void testLargeRead() throws Exception {
        final NoSecurityImpl wireSecurity = new NoSecurityImpl();
        final MsgHandler msgHandler = new MsgHandler(wireSecurity);
        SocketChannelHandler handler = mock(SocketChannelHandler.class);
        SocketChannel channel = mock(SocketChannel.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                                                               AbstractMessageHandler.READ_BUFFER_SIZE);
        for (int i = 0; i < AbstractMessageHandler.READ_BUFFER_SIZE; i++) {
            baos.write(666);
        }
        byte[] hugeAssByteArray = baos.toByteArray();
        ByteBuffer wireForm = wireSecurity.toWireForm(new SerializedMsg(
                                                                        hugeAssByteArray),
                                                      msgHandler.bufferPool);
        final ByteBuffer hugeAssBuffer = ByteBuffer.allocate(wireForm.remaining()
                                                             + AbstractMessageHandler.HEADER_BYTE_SIZE);
        hugeAssBuffer.putInt(WireSizes.MAGIC_NUMBER);
        hugeAssBuffer.putInt(wireForm.remaining());
        hugeAssBuffer.putLong(0);
        hugeAssBuffer.put(wireForm);
        hugeAssBuffer.flip();

        SerializedMsg msg = new SerializedMsg("Give me Slack");
        final ArrayList<ByteBuffer> smallSend = new ArrayList<ByteBuffer>();
        ByteBuffer temp = wireSecurity.toWireForm(msg, msgHandler.bufferPool);
        ByteBuffer header = ByteBuffer.allocate(AbstractMessageHandler.HEADER_BYTE_SIZE);
        smallSend.add(header);
        smallSend.add(temp);
        header.putInt(WireSizes.MAGIC_NUMBER);
        header.putInt(temp.remaining());
        header.putLong(1);
        header.flip();
        final int smallSendByteSize = temp.remaining() + header.remaining();

        Answer<Long> bulkRead = new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                assertNotNull(buffer);
                int written = 0;
                if (hugeAssBuffer.remaining() > 0) {
                    written = Math.min(buffer.remaining(),
                                       hugeAssBuffer.remaining());
                    buffer.put(hugeAssBuffer.array(), hugeAssBuffer.position(),
                               written);
                    hugeAssBuffer.position(hugeAssBuffer.position() + written);
                } else {
                    transfer(smallSendByteSize,
                             smallSend.toArray(new ByteBuffer[2]), buffer);
                    written = smallSendByteSize;
                }
                return (long) written;
            }
        };
        when(handler.getChannel()).thenReturn(channel);
        when(channel.read(any(ByteBuffer.class))).thenAnswer(bulkRead);

        msgHandler.connect(handler);

        msgHandler.readReady();
        assertEquals("Message was read", 0, msgHandler.msgs.size());
        msgHandler.readReady();
        assertEquals("Message was not read", 1, msgHandler.msgs.size());
        byte[] msgByteArray = (byte[]) ((SerializedMsg) msgHandler.msgs.get(0).msg).getObject();
        assertEquals("Invalid msg size", hugeAssByteArray.length,
                     msgByteArray.length);
        assertTrue("Wrong message",
                   Arrays.equals(hugeAssByteArray, msgByteArray));
        assertEquals("Wrong order", 0, msgHandler.msgs.get(0).order);

        // now see if we can correctly read another message

        msgHandler.readReady();

        assertEquals("Subsequent message was not read", 2,
                     msgHandler.msgs.size());

        assertEquals("Wrong message", msg.getObject(),
                     ((SerializedMsg) msgHandler.msgs.get(1).msg).getObject());
        assertEquals("Wrong order", 1, msgHandler.msgs.get(1).order);
    }

    private static long transfer(int count, ByteBuffer[] input,
                                 ByteBuffer output) throws IOException {
        int read = 0;
        byte[] buf = new byte[count];
        for (ByteBuffer b : input) {
            if (b.hasRemaining()) {
                int length = Math.min(b.remaining(), count - read);
                b.get(buf, read, length);
                read += length;
            }
            if (read == count) {
                break;
            }
        }
        output.put(buf, 0, read);
        return read;
    }
}
