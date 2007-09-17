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

import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.ReadSet;

import java.lang.reflect.Array;

/**
 * @author mph
 */
public class AtomicArray<T> {
  
  private final T[] array;
  private final T[] shadow;
  private Transaction[] writers;
  private ReadSet[] readers;
  // long version[];
  private final String FORMAT = "Unexpected transaction state: %s";
  
  /** Creates a new instance of AtomicArray */
  public AtomicArray(Class _class, int capacity) {
    array  = (T[]) Array.newInstance(_class, capacity);
    shadow = (T[]) Array.newInstance(_class, capacity);
    readers = new ReadSet[capacity];
    writers = new Transaction[capacity];
    // version = new long[capacity];
    for (int i = 0; i < capacity; i++) {
	readers[i] = new ReadSet();
	writers[i] = Transaction.COMMITTED_TRANS;
        // version[i] = 0;
    }

  }
  
  public T get(int i) {
    Transaction me  = FortressTaskRunner.getTransaction();
    Transaction other = null;
    ContentionManager manager = FortressTaskRunner.getContentionManager();
    while (true) {
      synchronized (readers[i]) {
        other = openRead(me,i);
        if (other == null) {
            return array[i];
        }
      }
      manager.resolveConflict(me, other);
    }
  }
  
  public void set(int i, T value) {
    Transaction me  = FortressTaskRunner.getTransaction();
    Transaction other = null;
    ContentionManager manager = FortressTaskRunner.getContentionManager();
    while (true) {
      synchronized (readers[i]) {
        other = openWrite(me,i);
        if (other == null) {
          array[i] = value;
          return;
        }
      }
      manager.resolveConflict(me, other);
    }
  }

  /**
   * Init is equivalent to set, but fails (returns false) for non-null
   * contents.
   **/
  public boolean init(int i, T value) {
    Transaction me  = FortressTaskRunner.getTransaction();
    Transaction other = null;
    ContentionManager manager = FortressTaskRunner.getContentionManager();
    while (true) {
      synchronized (readers[i]) {
        other = openWrite(me,i);
        /* Note openWrite gives us read permission as well. */
        if (other == null) {
          if (array[i]!=null) return false;
          array[i] = value;
          return true;
        }
      }
      manager.resolveConflict(me, other);
    }
  }

  /**
   * Tries to open object for reading. Returns reference to conflictin transaction, if one exists
   **/
  private Transaction openRead(Transaction me, int i) {
    // not in a transaction
    if (me == null) {	// restore object if latest writer aborted
      if (writers[i].isAborted()) {
        restore(i);
        // version[i]++;
        writers[i] = Transaction.COMMITTED_TRANS;
      }
      return null;
    }
    // Am I still active?
    if (!me.isActive()) {
      throw new AbortedException();
    }
    // Have I already opened this object?
    if (writers[i] == me) {
      return null;
    }
    switch (writers[i].getStatus()) {
      case ACTIVE:
        return writers[i];
      case COMMITTED:
        break;
      case ABORTED:
        restore(i);
        // version[i]++;
        break;
      default:
        throw new PanicException(FORMAT, writers[i].getStatus());
    }
    writers[i] = Transaction.COMMITTED_TRANS;
    readers[i].add(me);
    return null;
  }
  
  /**
   * Tries to open object for reading. Returns reference to conflicting transaction, if one exists
   **/
  public Transaction openWrite(Transaction me, int i) {
    // not in a transaction
    if (me == null) {	// restore object if latest writer aborted
      if (writers[i].isAborted()) {
        restore(i);
        // version[i]++;
        writers[i] = Transaction.COMMITTED_TRANS;
      }
      return null;
    }
    if (!me.validate()) {
      throw new AbortedException();
    }
    if (me == writers[i]) {
      return null;
    }
    for (Transaction reader : readers[i]) {
      if (reader.isActive() && reader != me) {
        return reader;
      }
    }
    readers[i].clear();
    switch (writers[i].getStatus()) {
      case ACTIVE:
        return writers[i];
      case COMMITTED:
        backup(i);
        // version[i]++;
        break;
      case ABORTED:
        restore(i);
        // version[i]++;
        break;
      default:
        throw new PanicException(FORMAT, writers[i].getStatus());
    }
    writers[i] = me;
    return null;
  }
  
  private void restore(int i) {
      array[i] = shadow[i];
  }
  private void backup(int i) {
      shadow[i] = array[i];
  }
  
}
