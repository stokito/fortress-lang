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
import java.util.concurrent.CopyOnWriteArrayList;

public class ReadSet extends AbstractSet<Transaction> {
  private CopyOnWriteArrayList<Transaction> elements;

  public ReadSet() {
      elements = new CopyOnWriteArrayList<Transaction>();
  }
	
  /**
   * Initialize one object from another.
   * @param aSet Initialize from this other object.
   */
  public void copyFrom(ReadSet aSet) {
      elements.addAll(aSet.elements);
  }

  /**
   * Add a new transaction to the set.
   * @param t Transaction to add.
   * @return Whether this transaction was already present.
   */
  public boolean add(Transaction t) {
      boolean res = elements.contains(t);
      elements.addIfAbsent(t);
      return res;
  }

  /**
   * remove transaction from the set.
   * @param t Transaction to remove.
   * @return Whether this transaction was already present.
   */
  public boolean remove(Transaction t) {
      boolean res = elements.contains(t);
      if (res) elements.remove(t);
      return res;
  }

  /**
   * Discard all elements of this set.
   */
  public void clear() {
      elements.clear();
  }

  /**
   * How many transactions in the set?
   * @return Number of transactions in the set.
   */
  public int size() {
      return elements.size();
  }

  /**
   * Iterate over transaction in the set.
   * @return Iterator over transactions in the set.
   */
  public java.util.Iterator<Transaction> iterator() {
    return elements.iterator();
  }
}
