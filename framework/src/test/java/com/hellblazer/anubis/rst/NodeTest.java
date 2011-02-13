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

import static com.hellblazer.anubis.rst.Color.GREEN;
import static com.hellblazer.anubis.rst.Color.RED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mockito.internal.verification.Times;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class NodeTest extends TestCase {

    /**
     * Node state is green, parent is self, and parent in adjacent set
     */
    public void testColorRedCase1() {
        Channel selfChannel = mock(Channel.class);
        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);

        Node node = new Node(selfChannel, members);

        assertFalse(node.colorRed());

        verify(selfChannel, new Times(2)).getColor();
        verify(selfChannel).getIndex();

        verifyNoMoreInteractions(selfChannel);
    }

    /**
     * Node state is green, parent is not self, parent is green, and parent in
     * adjacent set
     */
    public void testColorRedCase2() throws Exception {
        Channel selfChannel = mock(Channel.class);
        Channel parentChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);
        members.put(1, parentChannel);

        when(parentChannel.getColor()).thenReturn(GREEN);
        when(parentChannel.getIndex()).thenReturn(1);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);

        Node node = new Node(selfChannel, members);
        setParent(node, parentChannel);

        assertFalse(node.colorRed());

        verify(selfChannel).getColor();
        verify(parentChannel).getColor();
        verify(parentChannel).getIndex();

        verifyNoMoreInteractions(selfChannel, parentChannel);
    }

    /**
     * Node state is green, parent is not self, parent is red, and parent in
     * adjacent set
     */
    public void testColorRedCase3() throws Exception {
        Channel selfChannel = mock(Channel.class);
        Channel parentChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);
        members.put(1, parentChannel);

        when(parentChannel.getColor()).thenReturn(RED);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);

        Node node = new Node(selfChannel, members);
        setParent(node, parentChannel);

        assertTrue(node.colorRed());

        verify(selfChannel).getColor();
        verify(selfChannel).setColor(RED);
        verify(parentChannel).getColor();

        verifyNoMoreInteractions(selfChannel, parentChannel);
    }

    /**
     * Node state is green, parent is not self, parent is gree, and parent not
     * in adjacent set
     */
    public void testColorRedCase4() throws Exception {
        Channel selfChannel = mock(Channel.class);
        Channel parentChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(parentChannel.getColor()).thenReturn(RED);
        when(parentChannel.getIndex()).thenReturn(1);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);

        Node node = new Node(selfChannel, members);
        setParent(node, parentChannel);

        assertTrue(node.colorRed());

        verify(selfChannel).getColor();
        verify(selfChannel).setColor(RED);
        verify(parentChannel).getColor();

        verifyNoMoreInteractions(selfChannel, parentChannel);
    }

    /**
     * Node state is green, no children
     */
    public void testDisownParentCase1() throws Exception {
        Channel selfChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(selfChannel.getColor()).thenReturn(GREEN);

        Node node = new Node(selfChannel, members);

        assertFalse(node.disownParent());

        verify(selfChannel).getColor();

        verifyNoMoreInteractions(selfChannel);
    }

    /**
     * Node state is red, no children
     */
    public void testDisownParentCase2() throws Exception {
        Channel selfChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(selfChannel.getColor()).thenReturn(RED);

        Node node = new Node(selfChannel, members);

        assertTrue(node.disownParent());

        verify(selfChannel).getColor();
        verify(selfChannel).setColor(GREEN);

        verifyNoMoreInteractions(selfChannel);
    }

    /**
     * Node state is red, node has children
     */
    public void testDisownParentCase3() throws Exception {
        Channel selfChannel = mock(Channel.class);
        Channel childChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(selfChannel.getColor()).thenReturn(RED);

        Node node = new Node(selfChannel, members);
        node.addChild(childChannel);

        assertFalse(node.disownParent());

        verify(selfChannel).getColor();

        verifyNoMoreInteractions(selfChannel, childChannel);
    }

    /**
     * Node state is red
     */
    public void testMergeCase1() {
        Channel selfChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(selfChannel.getColor()).thenReturn(RED);

        Node node = new Node(selfChannel, members);

        assertFalse(node.merge());

        verify(selfChannel).getColor();

        verifyNoMoreInteractions(selfChannel);
    }

    /**
     * Node state is green, no members in adjacent set
     */
    public void testMergeCase2() {
        Channel selfChannel = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);
        when(selfChannel.getRoot()).thenReturn(0);

        Node node = new Node(selfChannel, members);

        assertFalse(node.merge());

        verify(selfChannel, new Times(2)).getColor();
        verify(selfChannel).getIndex();
        verify(selfChannel).getRoot();

        verifyNoMoreInteractions(selfChannel);
    }

    /**
     * Node state is green, members in adjacent set, all members green, node's
     * root > all other members
     */
    public void testMergeCase3() {
        Channel selfChannel = mock(Channel.class);
        Channel member1 = mock(Channel.class);
        Channel member2 = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(5, selfChannel);
        members.put(4, member1);
        members.put(3, member2);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(5);
        when(selfChannel.getRoot()).thenReturn(5);
        when(member1.getColor()).thenReturn(GREEN);
        when(member2.getColor()).thenReturn(GREEN);
        when(member1.getRoot()).thenReturn(4);
        when(member2.getRoot()).thenReturn(3);

        Node node = new Node(selfChannel, members);

        assertFalse(node.merge());

        verify(selfChannel, new Times(2)).getColor();
        verify(selfChannel, new Times(3)).getIndex();
        verify(selfChannel).getRoot();
        verify(member1).getColor();
        verify(member2).getColor();
        verify(member1).getRoot();
        verify(member2).getRoot();

        verifyNoMoreInteractions(selfChannel, member1, member2);
    }

    /**
     * Node state is green, members in adjacent set, all members green, node's
     * root is not less than member1's root
     */
    public void testMergeCase4() {
        Channel selfChannel = mock(Channel.class);
        Channel member1 = mock(Channel.class);
        Channel member2 = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);
        members.put(1, member1);
        members.put(2, member2);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);
        when(selfChannel.getRoot()).thenReturn(0);
        when(member1.getColor()).thenReturn(GREEN);
        when(member2.getColor()).thenReturn(GREEN);
        when(member1.getIndex()).thenReturn(1);
        when(member2.getIndex()).thenReturn(2);
        when(member1.getRoot()).thenReturn(1);
        when(member2.getRoot()).thenReturn(2);

        Node node = new Node(selfChannel, members);

        assertTrue(node.merge());

        verify(selfChannel, new Times(2)).getColor();
        verify(selfChannel, new Times(2)).getIndex();
        verify(selfChannel).getRoot();
        verify(selfChannel).removeChild(selfChannel);
        verify(member1).getIndex();
        verify(member1).getColor();
        verify(member2).getColor();
        verify(member1, new Times(2)).getRoot();
        verify(member2, new Times(2)).getRoot();
        verify(member2).addChild(selfChannel);

        verifyNoMoreInteractions(selfChannel, member1, member2);
    }

    /**
     * Node state is green, members in adjacent set, node's root is not less
     * than member1's root, state of member with highest root is red
     */
    public void testMergeCase5() {
        Channel selfChannel = mock(Channel.class);
        Channel member1 = mock(Channel.class);
        Channel member2 = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);
        members.put(1, member1);
        members.put(2, member2);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);
        when(selfChannel.getRoot()).thenReturn(0);
        when(member1.getColor()).thenReturn(GREEN);
        when(member2.getColor()).thenReturn(RED);
        when(member1.getIndex()).thenReturn(1);
        when(member2.getIndex()).thenReturn(2);
        when(member1.getRoot()).thenReturn(1);
        when(member2.getRoot()).thenReturn(2);

        Node node = new Node(selfChannel, members);

        assertTrue(node.merge());

        verify(selfChannel, new Times(2)).getColor();
        verify(selfChannel, new Times(2)).getIndex();
        verify(selfChannel).getRoot();
        verify(selfChannel).removeChild(selfChannel);
        verify(member1).getColor();
        verify(member2).getColor();
        verify(member1, new Times(2)).getRoot();
        verify(member1).addChild(selfChannel);

        verifyNoMoreInteractions(selfChannel, member1, member2);
    }

    /**
     * Node state is green, members in adjacent set, node's root is not less
     * than member2's root, member1's root is higher, but member1's state is red
     */
    public void testMergeCase6() {
        Channel selfChannel = mock(Channel.class);
        Channel member1 = mock(Channel.class);
        Channel member2 = mock(Channel.class);

        Map<Integer, Channel> members = new HashMap<Integer, Channel>();
        members.put(0, selfChannel);
        members.put(1, member1);
        members.put(2, member2);

        when(selfChannel.getColor()).thenReturn(GREEN);
        when(selfChannel.getIndex()).thenReturn(0);
        when(selfChannel.getRoot()).thenReturn(0);
        when(member1.getColor()).thenReturn(RED);
        when(member2.getColor()).thenReturn(GREEN);
        when(member1.getIndex()).thenReturn(1);
        when(member2.getIndex()).thenReturn(2);
        when(member1.getRoot()).thenReturn(1);
        when(member2.getRoot()).thenReturn(2);

        Node node = new Node(selfChannel, members);

        assertTrue(node.merge());

        verify(selfChannel, new Times(2)).getColor();
        verify(selfChannel, new Times(2)).getIndex();
        verify(selfChannel).getRoot();
        verify(selfChannel).removeChild(selfChannel); 
        verify(member1).getColor();
        verify(member2).getColor(); 
        verify(member2, new Times(2)).getRoot();
        verify(member2).addChild(selfChannel);

        verifyNoMoreInteractions(selfChannel, member1, member2);
    }

    void setParent(Node node, Channel channel) throws Exception {
        Field parentField = Node.class.getDeclaredField("parent");
        parentField.setAccessible(true);
        parentField.set(node, channel);
    }
}
