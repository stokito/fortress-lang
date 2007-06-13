/*
 * AtomicArray.java
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

package dstm2;

import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.PanicException;
import dstm2.factory.ofree.ReadSet;
import java.lang.reflect.Array;

/**
 * @author mph
 */
@atomic public class AtomicArray<T> {
  
  private final T[] array;
  private final T[] shadow;
  private Transaction writer;
  private ReadSet readers;
  long version;
  private final String FORMAT = "Unexpected transaction state: %s";
  
  /** Creates a new instance of AtomicArray */
  public AtomicArray(Class _class, int capacity) {
    array  = (T[]) Array.newInstance(_class, capacity);
    shadow = (T[]) Array.newInstance(_class, capacity);
    writer = Transaction.COMMITTED;
    readers = new ReadSet();
    version = 0;
  }
  
  public T get(int i) {
    Transaction me  = FortressTaskRunner.getTransaction();
    Transaction other = null;
    ContentionManager manager = FortressTaskRunner.getContentionManager();
    while (true) {
      synchronized (this) {
        other = openRead(me);
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
      synchronized (this) {
        other = openWrite(me);
        if (other == null) {
          array[i] = value;
          return;
        }
      }
      manager.resolveConflict(me, other);
    }
  }
  /**
   * Tries to open object for reading. Returns reference to conflictin transaction, if one exists
   **/
  private Transaction openRead(Transaction me) {
    // not in a transaction
    if (me == null) {	// restore object if latest writer aborted
      if (writer.isAborted()) {
        restore();
        version++;
        writer = Transaction.COMMITTED;
      }
      return null;
    }
    // Am I still active?
    if (!me.isActive()) {
      throw new AbortedException();
    }
    // Have I already opened this object?
    if (writer == me) {
      return null;
    }
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        break;
      case ABORTED:
        restore();
        version++;
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = Transaction.COMMITTED;
    readers.add(me);
    return null;
  }
  
  /**
   * Tries to open object for reading. Returns reference to conflicting transaction, if one exists
   **/
  public Transaction openWrite(Transaction me) {
    // not in a transaction
    if (me == null) {	// restore object if latest writer aborted
      if (writer.isAborted()) {
        restore();
        version++;
        writer = Transaction.COMMITTED;
      }
      return null;
    }
    if (!me.validate()) {
      throw new AbortedException();
    }
    if (me == writer) {
      return null;
    }
    for (Transaction reader : readers) {
      if (reader.isActive() && reader != me) {
        return reader;
      }
    }
    readers.clear();
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        backup();
        version++;
        break;
      case ABORTED:
        restore();
        version++;
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = me;
    return null;
  }
  
  private void restore() {
    System.arraycopy(shadow, 0, array, 0, array.length);
  }
  private void backup() {
    System.arraycopy(array, 0, shadow, 0, array.length);
  }
  
}
