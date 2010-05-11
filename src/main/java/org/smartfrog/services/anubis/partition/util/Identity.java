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
package org.smartfrog.services.anubis.partition.util;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.WireSizes;

/**
 * The identity of a partiton manager.
 * Identity represents the identity of a partiton manager. The identity
 * is based on a magic number (to allow multiple instances in the same
 * network not to conflict), an integer id, and an epoch number to differentiate
 * incarnations of the same manager.
 */
public class Identity implements Serializable, Cloneable, WireSizes {

  public final int  magic;
  public final int  id;
  public final long epoch;

  static final private int magicIdx = 0;
  static final private int idIdx    = magicIdx + intSz;
  static final private int epochIdx = idIdx + intSz;
  static final public int identityWireSz = epochIdx + longSz;

  /**
   * constructor - takes a given magic number, id and epoch to construct the
   * identity object from.
   */
  public Identity(int magic, int id, long epoch) {
      this.magic = magic;
      this.id    = id;
      this.epoch = epoch;
  }

  public static Identity readWireForm(ByteBuffer bytes, int idx) {
      return new Identity(bytes.getInt(idx + magicIdx),
                          bytes.getInt(idx + idIdx),
                          bytes.getLong(idx + epochIdx));
  }

  public void writeWireForm(ByteBuffer bytes, int idx) {
      bytes.putInt(idx + magicIdx, magic);
      bytes.putInt(idx + idIdx, id);
      bytes.putLong(idx + epochIdx, epoch);
  }

  /**
   * Equality test: does not use epoch.
   * Defines the equality relation for identities - used in containers
   * e.g. hash tables. - note: does not use epoch for equality!!!!
   * TWO IDENTITIES ARE EQUIVALENT IF THE HAVE THE SAME ID AND MAGIC NUMBER
   */
  public boolean equals(Object obj) {
      if( this == obj )
          return true;

      if( obj instanceof Identity ) {
          Identity identity = (Identity)obj;
          return (this.magic == identity.magic) &&
                 (this.id    == identity.id);
      }

      return false;
  }

  /**
   * a hash code for the epoch - used in containers.
   */
  public int hashCode() {
      return (int)(magic + (id * 101));
  }

  /**
   * does the identity match on magic number
   */
  public boolean equalMagic(Identity identity) {
      return this.magic == identity.magic;
  }

  /**
   * does the identity match on magic and id (not epoch)
   */
  public boolean equalId(Identity identity) {
      return this.id == identity.id;
  }

  /**
   * does the identitiy match on epoch
   */
  public boolean equalEpoch(Identity identity) {
      return this.epoch == identity.epoch;
  }

  /**
   * create another identity object like this one.
   */
  public Object clone() {
      try {
          return super.clone();
      } catch (CloneNotSupportedException ex) {
          ex.printStackTrace();
          return null;
      }
  }

  /**
   * for debug purposes
   */
  public String toString() {
      // return "<id=" + id + ", " + magic + ", " + epoch + ">";
      return "<id " + id + " >";
  }


  /**
   * This method is similar to the writeObject() method but is not
   * part of the Serializable interface. This can be used to marshall
   * this object including the super class properties, without using that
   * interface. It does not serialize descriptors, so the use needs to
   * know what objects and what versions to unmarshall from a stream by
   * other means.
   *
   * @param s
   * @throws IOException
   */
    public void writeToObjectStream(ObjectOutputStream s) throws IOException {
        s.writeInt(id);
        s.writeInt(magic);
        s.writeLong(epoch);
    }

    /**
     * This method is similar to the readObject() method but is not
     * part of the Serializable interface. This can be used to unmarshall
     * this object including the super class properties, without using that
     * interface. There are no descriptors, so the use needs to
     * know what objects and what versions to unmarshall from a stream by
     * other means.
     *
     * @param s
     * @throws IOException
     */
    public Identity(ObjectInputStream s) throws IOException, ClassNotFoundException  {
        id    = s.readInt();
        magic = s.readInt();
        epoch = s.readLong();
    }




}
