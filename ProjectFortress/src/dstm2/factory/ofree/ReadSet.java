/*
 * ReadSet.java
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A.  All rights reserved.  
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document.  In particular, and without limitation, these
 * intellectual property rights may include one or more of the
 * U.S. patents listed at http://www.sun.com/patents and one or more
 * additional patents or pending patent applications in the U.S. and
 * in other countries.
 * 
 * U.S. Government Rights - Commercial software.
 * Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its
 * supplements.  Use is subject to license terms.  Sun, Sun
 * Microsystems, the Sun logo and Java are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.  
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime
 * end uses or end users, whether direct or indirect, are strictly
 * prohibited.  Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion
 * lists, including, but not limited to, the denied persons and
 * specially designated nationals lists is strictly prohibited.
 */

package dstm2.factory.ofree;

import dstm2.Transaction;
import dstm2.factory.ofree.ReadSet;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * ReadSet.java
 * Keep track of transactions that opened this object for READ.
 *
 * @author Maurice Herlihy
 */
public class ReadSet extends AbstractSet<Transaction> {
  
  /**
   * This value is public to facilitate unit testing.
   */
  public static int INITIAL_SIZE = 32;
  /**
   * Number of allocated slots. Must reallocate if actual number of
   * transactions exceeds this size.
   */
  private int size;
  /**
   * Next free slot in array.
   */
  private int next;
  /**
   * Iterates over elements.
   */
  private Transaction elements[];
  
  /**
   * Create ReadSet of default size.
   */
  public ReadSet() {
    this(INITIAL_SIZE);
  }
  /**
   * Create ReadSet of indicated size.
   * @param size Size of readSet to create.
   */
  public ReadSet(int size) {
    this.size = size;
    elements = new Transaction[size];
    next = 0;
  }
  
  /**
   * Initialize one object from another.
   * @param aSet Initialize from here.
   */
  public void copyFrom(ReadSet aSet) {
    if (aSet.size > this.size) {
      elements = new Transaction[aSet.size];
      this.size = aSet.size;
    }
    System.arraycopy(aSet.elements, 0, this.elements, 0, aSet.next);
    this.next = aSet.next;
  }
  
  /**
   * Add a new transaction to the set.
   * @param t Transaction to add.
   * @return Whether this transaction was already present.
   */
  public boolean add(Transaction t) {
    // try to reuse slot
    for (int i = 0; i < next; i++) {
      if (!elements[i].isActive()) {
        elements[i] = t;
        return true;
      } else if (elements[i] == t) {
        return true;
      }
    }
    // check for overflow
    if (next == size) {
      Transaction[] newElements = new Transaction[2 * size];
      System.arraycopy(elements, 0, newElements, 0, size);
      elements = newElements;
      size = 2 * size;
    }
    elements[next++] = t;
    return true;
  }
  
  /**
   * remove transaction from the set.
   * @param t Transaction to remove.
   * @return Whether this transaction was already present.
   */
  public boolean remove(Transaction t) {
    // try to reuse slot
    int i = 0;
    boolean present = false;
    while(i < next) {
      if (elements[i] == t) {
        elements[i] = elements[next-1];
        next--;
        present = true;
      } else {
        i++;
      }
    }
    return present;
  }
  
  /**
   * How many transactions in the set?
   * @return Number of transactions in the set.
   */
  public int size() {
    return next;
  }
  
  /**
   * Iterate over transaction in the set.
   * @return Iterator over transactions in the set.
   */
  public java.util.Iterator<Transaction> iterator() {
    return new Iterator();
  }
  
  /**
   * Inner class that implements iterator.
   */
  private class Iterator implements java.util.Iterator<Transaction> {
    /**
     * Iterator position.
     */
    int pos = 0;
    /**
     * Is there another transaction in the set?
     * @return whether there are more active transactions
     */
    public boolean hasNext() {
//      if (pos == next) {
//        return false;
//      }
//      while (pos < next && !elements[pos].isActive()) {
//        elements[pos] = elements[next-1]; // discard inactive transactions
//        next--;
//      }
      return pos < next;
    }
    
    /**
     * Get next item in the set.
     * @return next transaction in the set.
     */
    public Transaction next() {
      return elements[pos++];
    }
    
    /**
     * Do not call this method.
     */
    public void remove() {
      throw new java.lang.UnsupportedOperationException();
    }
  }
}
