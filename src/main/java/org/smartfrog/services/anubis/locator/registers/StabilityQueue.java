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
package org.smartfrog.services.anubis.locator.registers;


import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.locator.util.BlockingQueue;

abstract public class StabilityQueue {

    private class Notification {
        View view; int leader;
        public Notification(View view, int leader) {
            this.view = view; this.leader = leader;
        }
    }

    private class RequestServer extends Thread {
        private boolean           running = false;
        public RequestServer() { super("Anubis: Locator Stability Queue Server"); }
        public void run() {
            running = true;
            while( running ) {
                Notification notification = (Notification)requests.get();
                if( notification != null )
                    doit(notification.view, notification.leader);
            }
        }
        public void terminate() { running = false; }
    }

    private BlockingQueue requests = new BlockingQueue();
    private RequestServer server   = new RequestServer();

    public      StabilityQueue()           { }
    public void start()                    { server.start(); }
    public void terminate()                { server.terminate(); requests.deactivate(); }
    public void put(View view, int leader) { requests.put(new Notification(view, leader)); }

    abstract public void doit(View view, int leader);
}
