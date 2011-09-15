/*
 * Copyright (c) 2009, 2011 Hal Hildebrand, all rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hellblazer.jackal.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class SocketOptions {
    private int     backlog             = 0;
    private int     bandwidth           = -1;
    private int     connect_time        = -1;
    private boolean keep_alive          = true;
    private int     latency             = -1;
    private int     linger              = -1;
    private boolean no_delay            = true;
    private int     receive_buffer_size = -1;
    private boolean reuse_address       = false;
    private int     send_buffer_size    = -1;
    private int     timeout             = -1;
    private int     traffic_class       = -1;

    public void configure(Socket socket) throws IOException {
        if (no_delay) {
            socket.setTcpNoDelay(true);
        }
        if (keep_alive) {
            socket.setKeepAlive(true);
        }
        if (linger > 0) {
            socket.setSoLinger(true, linger);
        }
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }
        if (receive_buffer_size > 0) {
            socket.setReceiveBufferSize(receive_buffer_size);
        }
        if (send_buffer_size > 0) {
            socket.setSendBufferSize(send_buffer_size);
        }
        if (traffic_class > 0) {
            socket.setTrafficClass(traffic_class);
        }
    }

    public void configure(ServerSocket socket) throws IOException {
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }
        if (receive_buffer_size > 0) {
            socket.setReceiveBufferSize(receive_buffer_size);
        }
        if (reuse_address) {
            socket.setReuseAddress(reuse_address);
        }
        if (connect_time >= 0 && bandwidth >= 0 && latency >= 0) {
            socket.setPerformancePreferences(connect_time, latency, bandwidth);
        }
    }

    public int getBacklog() {
        return backlog;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getConnect_time() {
        return connect_time;
    }

    public int getLatency() {
        return latency;
    }

    public int getLinger() {
        return linger;
    }

    public int getReceive_buffer_size() {
        return receive_buffer_size;
    }

    public int getSend_buffer_size() {
        return send_buffer_size;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getTraffic_class() {
        return traffic_class;
    }

    public boolean isKeep_alive() {
        return keep_alive;
    }

    public boolean isNo_delay() {
        return no_delay;
    }

    public boolean isReuse_address() {
        return reuse_address;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public void setConnect_time(int connect_time) {
        this.connect_time = connect_time;
    }

    public void setKeep_alive(boolean keep_alive) {
        this.keep_alive = keep_alive;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }

    public void setLinger(int linger) {
        this.linger = linger;
    }

    public void setNo_delay(boolean no_delay) {
        this.no_delay = no_delay;
    }

    public void setReceive_buffer_size(int receive_buffer_size) {
        this.receive_buffer_size = receive_buffer_size;
    }

    public void setReuse_address(boolean reuse_address) {
        this.reuse_address = reuse_address;
    }

    public void setSend_buffer_size(int send_buffer_size) {
        this.send_buffer_size = send_buffer_size;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setTraffic_class(int traffic_class) {
        this.traffic_class = traffic_class;
    }
}