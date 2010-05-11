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
package org.smartfrog.services.anubis.locator.msg;


import java.io.Serializable;

import org.smartfrog.services.anubis.locator.names.ListenerProxy;
import org.smartfrog.services.anubis.locator.names.NameData;
import org.smartfrog.services.anubis.locator.names.ProviderInstance;
import org.smartfrog.services.anubis.locator.names.ProviderProxy;

public class RegisterMsg implements Serializable {
    public static final int LocalRegister      = 1000;
    public static final int GlobalRegister     = 1001;

    public static final int Undefined          = 0;

    public static final int RegisterProvider   = 10;
    public static final int DeregisterProvider = 11;
    public static final int RegisterListener   = 20;
    public static final int DeregisterListener = 21;

    public static final int AddListener        = 100;
    public static final int RemoveListener     = 101;

    public static final int ProviderValue      = 200;
    public static final int ProviderNotPresent = 201;

    public int      register  = Undefined;
    public int      type      = Undefined;
    public NameData data      = null;

    public RegisterMsg(int type, NameData data, int register) {
        this.type     = type;
        this.register = register;
        this.data     = data;
    }

    public static RegisterMsg registerProvider(ProviderProxy provider) {
        return new RegisterMsg(RegisterProvider, provider, GlobalRegister);
    }
    public static RegisterMsg deregisterProvider(ProviderProxy provider) {
        return new RegisterMsg(DeregisterProvider, provider, GlobalRegister);
    }
    public static RegisterMsg registerListener(ListenerProxy listener) {
        return new RegisterMsg(RegisterListener, listener, GlobalRegister);
    }
    public static RegisterMsg deregisterListener(ListenerProxy listener) {
        return new RegisterMsg(DeregisterListener, listener, GlobalRegister);
    }

    public static RegisterMsg addListener(ListenerProxy listener) {
        return new RegisterMsg(AddListener, listener, LocalRegister);
    }
    public static RegisterMsg removeListener(ListenerProxy listener) {
        return new RegisterMsg(RemoveListener, listener, LocalRegister);
    }

    public static RegisterMsg providerValue(ProviderInstance provider) {
        return new RegisterMsg(ProviderValue, provider.copy(), LocalRegister);
    }
    public static RegisterMsg providerNotPresent(ProviderInstance provider) {
        return new RegisterMsg(ProviderNotPresent, provider.copy(), LocalRegister);
    }

    public String toString() {
        String str;
        switch(type) {
            case Undefined:          str = "[Undefined, "; break;

            case RegisterProvider:   str = "[RegisterProvider, "; break;
            case DeregisterProvider: str = "[DeregisterProvider, "; break;
            case RegisterListener:   str = "[RegisterListener, "; break;
            case DeregisterListener: str = "[DeregisterListener, "; break;

            case AddListener:        str = "[AddListener, "; break;
            case RemoveListener:     str = "[RemoveListener, "; break;

            case ProviderValue:      str = "[ProviderValue, "; break;
            case ProviderNotPresent: str = "[ProviderNotPresent, "; break;
            default:                 str = "[??illegal type??, "; break;
        }
        switch(register) {
            case LocalRegister:      str += "LocalRegister"; break;
            case GlobalRegister:     str += "GlobalRegister"; break;
            case Undefined:          str += "Undefined"; break;
            default:                 str += "??illegal recipient??"; break;
        }
        return str + ", data=" + data + "]";
    }
}



