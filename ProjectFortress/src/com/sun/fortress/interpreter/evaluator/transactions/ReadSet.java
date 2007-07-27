/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import java.util.AbstractSet;

public class ReadSet extends AbstractSet<Transaction> {

  /**
   * This value is public to facilitate unit testing.
   */
  final static int INITIAL_SIZE = 64;
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
   * @param aSet Initialize from this other object.
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
   * Discard all elements of this set.
   */
  public void clear() {
    next = 0;
  }

  /**
   * discard inactive transactions
   * must be called only while object is locked!
   **/
//  public void clean() {
//    int i = 0;
//    while (i < next && (!elements[i].isActive())) {
//      elements[i] = elements[next-1];
//      next--;
//    }
//  }
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
     * Is there another item in the set?
     * @return whether there are more active transactions
     */
    public boolean hasNext() {
      return pos < next;
    }

    /**
     * Get next item in the set.
     * @return Next item in the set.
     */
    public Transaction next() {
      return ReadSet.this.elements[pos++];
    }

    /**
     * Do not call this method.
     */
    public void remove() {
      throw new java.lang.UnsupportedOperationException();
    }
  }
}
