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
package org.smartfrog.services.anubis.partition.diagnostics.console;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.util.Epoch;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.annotations.DeployedPostProcessor;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
@Configuration
public class DiagnosticConsoleConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(
                                               DiagnosticConsoleConfiguration.class);
    }

    @Bean
    public Controller controller() throws UnknownHostException {
        Controller controller = new Controller();
        controller.setAddress(heartbeatGroup());
        controller.setTimer(timer());
        controller.setCheckPeriod(1000);
        controller.setExpirePeriod(10000);
        controller.setIdentity(partitionIdentity());
        controller.setHeartbeatTimeout(heartbeatTimeout());
        controller.setHeartbeatInterval(heartbeatInterval());
        controller.setServer(consoleServer());
        return controller;
    }

    @Bean
    public DeployedPostProcessor deployedPostProcessor() {
        return new DeployedPostProcessor();
    }

    @Bean
    public MulticastAddress heartbeatGroup() throws UnknownHostException {
        return new MulticastAddress(heartbeatGroupMulticastAddress(),
                                    heartbeatGroupPort(), heartbeatGroupTTL());
    }

    public InetAddress heartbeatGroupMulticastAddress()
                                                       throws UnknownHostException {
        return InetAddress.getByName("233.1.2.30");
    }

    public int heartbeatGroupPort() {
        return 1966;
    }

    public int heartbeatGroupTTL() {
        return 1;
    }

    public long heartbeatInterval() {
        return 2000L;
    }

    public long heartbeatTimeout() {
        return 3L;
    }

    @Bean
    public Identity partitionIdentity() throws UnknownHostException {
        return new Identity(magic(), node(), epoch().longValue());
    }

    @Bean
    public Timer timer() {
        return new Timer("Partition timer", true);
    }

    protected ConsoleServer consoleServer() throws UnknownHostException {
        ConsoleServer server = new ConsoleServer();
        server.setCommsExecutor(serverExecutor());
        server.setDispatchExecutor(serverDispatchExecutor());
        return server;
    }

    protected Epoch epoch() {
        return new Epoch();
    }

    protected int magic() {
        return 12345;
    }

    protected int node() throws UnknownHostException {
        return Identity.getProcessUniqueId();
    }

    protected ExecutorService serverDispatchExecutor() {
        return Executors.newFixedThreadPool(serverDispatchPoolSize(),
                                            serverDispatchTheadFactory());
    }

    protected int serverDispatchPoolSize() {
        return 2;
    }

    protected ThreadFactory serverDispatchTheadFactory() {
        return new ThreadFactory() {
            int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r,
                                           "Anubis: Diagnostic server dispatch thread "
                                                   + counter++);
                thread.setDaemon(true);
                thread.setPriority(Thread.MAX_PRIORITY);
                thread.setUncaughtExceptionHandler(uncaughtExceptionHandler());
                return thread;
            }
        };
    }

    protected ExecutorService serverExecutor() {
        int poolSize = serverPoolSize();
        if (poolSize < 3) {
            throw new IllegalArgumentException("Pool size must be >= 3");
        }
        return Executors.newFixedThreadPool(poolSize, serverTheadFactory());
    }

    protected int serverPoolSize() {
        return 5;
    }

    protected ThreadFactory serverTheadFactory() {
        return new ThreadFactory() {
            int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r,
                                           "Anubis: Diagnostic server comms thread "
                                                   + counter++);
                thread.setDaemon(true);
                thread.setPriority(Thread.MAX_PRIORITY);
                thread.setUncaughtExceptionHandler(uncaughtExceptionHandler());
                return thread;
            }
        };
    }

    protected UncaughtExceptionHandler uncaughtExceptionHandler() {
        return new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.err.print("Exception in thread \"" + t.getName() + "\" ");
                e.printStackTrace(System.err);
            }
        };
    }
}
