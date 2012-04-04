/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
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
package org.smartfrog.services.anubis.partition.test.controller.gui;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
import com.hellblazer.jackal.configuration.MulticastHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.MulticastSnoopConfig;
import com.hellblazer.pinkie.SocketOptions;

@Configuration
@Import({ GraphicControllerConfig.class, MulticastSnoopConfig.class,
         MulticastHeartbeatAndDiscoveryConfig.class })
public class PartitionManagerUiConfiguration {

    public static void main(String[] argv) throws InterruptedException {
        new AnnotationConfigApplicationContext(
                                               PartitionManagerUiConfiguration.class);
        while (true) {
            Thread.sleep(50000);
        }
    }

    @Bean
    public HeartbeatConfiguration heartbeatConfig() {
        return new HeartbeatConfiguration(3000, 2);
    }

    @Bean
    public MulticastAddress heartbeatGroup() throws UnknownHostException {
        return new MulticastAddress(InetAddress.getByName("233.1.2.30"), 1966,
                                    0);
    }

    @Bean(name = "multicastInterface")
    public InetAddress multicastInterface() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public Identity partitionIdentity() {
        return new Identity(getMagic(), node(), System.currentTimeMillis());
    }

    @Bean
    public SocketOptions socketOptions() {
        return new SocketOptions();
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }

    protected int getMagic() {
        try {
            return Identity.getMagicFromLocalIpAddress();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int node() {
        return 2047;
    }

}
