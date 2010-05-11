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
package org.smartfrog.services.anubis.partition.wire;

/**
 * This interface is a collection of useful size values for the wire form.
 *
 * @author not attributable
 * @version 1.0
 */
public interface WireSizes {

    final int intSz = 4;
    final int longSz = 8;
    final int booleanSz = intSz;
    final int booleanTrueValue = 1;
    final int booleanFalseValue = 0;
    final int inetV4AddressSz = 4;
    final int inetV6AddressSz = 16;
    final int maxInetAddressSz = inetV6AddressSz;
    final int UNDEFINED_SIZE = -1;

    final int MAGIC_NUMBER = 24051967;
    final int magicSz = intSz;
    final int HEADER_SIZE = magicSz + intSz;
}
