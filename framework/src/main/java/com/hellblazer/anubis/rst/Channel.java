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

package com.hellblazer.anubis.rst;

/**
 * The interface of a channel that connects two members
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface Channel {
    /**
     * Add the channel as a child of the node associated with the channel.
     * 
     * @param child
     *            - the node to add as a child
     */
    void addChild(Channel child);

    /**
     * Answer the color of the channel. The channel is colored RED if the
     * channel is down, or the associated node is colored RED.
     * 
     * @return the color of the channel
     */
    Color getColor();

    /**
     * Answer the index of the node associated with the chanel.
     * 
     * @return the index of the associated node
     */
    int getIndex();

    /**
     * Answer the index of member the node associated with the channel believes
     * is the root of the tree.
     * 
     * @return the root value of the associated node
     */
    int getRoot();

    /**
     * Remove the child from the node associated with the channel
     * 
     * @param child
     */
    void removeChild(Channel child);

    /**
     * Set the color of the channel
     * 
     * @param color
     *            - the color of the channel
     */
    void setColor(Color color);
}
