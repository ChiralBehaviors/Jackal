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

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.ControllerConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PartitionManagerUiConfiguration extends ControllerConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(
                                               PartitionManagerUiConfiguration.class);
    }

    @Override
    protected Controller constructController() throws IOException {
        return new GraphicController(timer(), 1000, 300000,
                                     partitionIdentity(), heartbeatTimeout(),
                                     heartbeatInterval(), socketOptions(),
                                     dispatchExecutor(), wireSecurity());
    }

}
