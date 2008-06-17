 /*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

import java.util.Set;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.ReadSet;
import com.sun.fortress.interpreter.evaluator.transactions.Recoverable;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;

/**
 * What the interpreter stores mutable things (fields, variables)
 * in.  It will eventually acquire transactional semantics.
 */
public class ReferenceCell extends IndirectionCell {
    private FType theType;
    protected FNode node;
    Recoverable rnode;
    ContentionManager manager;
    Transaction writer;
    ReadSet readers;

    private final static String FORMAT = "Unexpected transaction state: %s";
    /**
     * A transaction switches to exclusive mode after being aborted this many times.
     */
    public static final int CONFLICT_THRESHOLD = 8;

    public ReferenceCell(FType t, FValue v) {
        super();
        theType = t;
        node = new FNode(v);
        rnode = (Recoverable)node;
        manager = FortressTaskRunner.getContentionManager();
        writer = Transaction.COMMITTED_TRANS;
        readers = new ReadSet();
    }

    public ReferenceCell(FType t) {
        super();
        theType = t;
        node = new FNode();
        rnode = (Recoverable)node;
        manager = FortressTaskRunner.getContentionManager();
        writer = Transaction.COMMITTED_TRANS;
        readers = new ReadSet();
    }

    public ReferenceCell() {
        super();
        node = new FNode();
        rnode = (Recoverable)node;
        manager = FortressTaskRunner.getContentionManager();
        writer = Transaction.COMMITTED_TRANS;
        readers = new ReadSet();
    }

    public String toString() {
        return node.toString();
    }

    private void debugPrint(String s) {
        System.out.println(Thread.currentThread().getName() + s);
    }

    public void assignValue(FValue f2) {
        Transaction me  = FortressTaskRunner.getTransaction();
        Transaction other = null;
        Set<Transaction> others = null;
        while (true) {
            synchronized (this) {
                if (me == null) {
                    others = readWriteConflict(me);
                    for (Transaction t : others) {
                        t.abort();
                    }
                    writer.abort();
                    rnode.recover();
                    writer = Transaction.COMMITTED_TRANS;
                    node.setValue(f2);
                    return;
                } else if (!me.isActive()) {
                    return;
                }
                others = readWriteConflict(me);
                if (others.isEmpty()) {
                    other = openWrite(me);
                    if (other == null) {
                        node.setValue(f2);
                        return;
                    }
                }
            }
            if (others != null) {
                manager.resolveConflict(me, others);
            } else if (other != null) {
                manager.resolveConflict(me, other);
            }
        }
    }

    public FType getType() {
        return theType;
    }

    class FNode implements Recoverable {
        FValue val;
        FValue oldVal;

        void setValue(FValue value) {
            val = value;
        }

        FNode(FValue value) {val = value;}
        FNode() {}
        FValue getValue() {
            return val;
        }
        FValue getOldValue() { return oldVal;}

        public void backup() { oldVal = val; }
        public void recover() { val = oldVal; }
        public String toString() {
            return String.valueOf(val);
            }
    }

    public void storeValue(FValue f2) {
        if (node.getValue() != null)
            bug("Internal error, second store of indirection cell");
        assignValue(f2);
    }

    public void storeType(FType f2) {
        if (theType != null)
            bug("Internal error, second store of type");
        theType = f2;
    }

    public boolean isInitialized() {
        return node.getValue() != null;
    }

    public FValue getValue() {
        FValue the_value = getValueNull();
        if (the_value == null) {
            return error("Attempt to read uninitialized variable");
        }

        return the_value;
    }

    public FValue getValueNull() {
        Transaction me  = FortressTaskRunner.getTransaction();
        Transaction other = null;
        while (true) {
            synchronized (this) {
                other = openRead(me);
                //other = openWrite(me);
                if (other == null) {
                    return node.getValue();
                }
            }
            manager.resolveConflict(me, other);
        }
    }

  /**
   * Tries to open object for reading. Returns reference to conflicting transaction, if one exists
   **/
  public Transaction openRead(Transaction me) {
    // not in a transaction?
    if (me == null) {   // restore object if latest writer aborted
      if (writer.isAborted()) {
        rnode.recover();
        writer = Transaction.COMMITTED_TRANS;
      } else if (writer.isActive()) {
          writer.abort();
          rnode.recover();
          writer = Transaction.COMMITTED_TRANS;
      }

      return null;
    }
    if (me.attempts > CONFLICT_THRESHOLD) {
      return openWrite(me);
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
        rnode.recover();
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = Transaction.COMMITTED_TRANS;
    readers.add(me);
    manager.openSucceeded();
    return null;
  }

  /**
   * Tries to open object for reading.
   * Returns reference to conflicting transaction, if one exists
   **/
  Transaction openWrite(Transaction me) {
    boolean cacheHit = false;  // already open for read?
    // not in a transaction?
    if (me == null) {   // restore object if latest writer aborted
      if (writer.isAborted()) {
        rnode.recover();
        writer = Transaction.COMMITTED_TRANS;
      } else if (writer.isActive()) {
          writer.abort();
          rnode.recover();
          writer = Transaction.COMMITTED_TRANS;
      }
      return null;
    }
    if (!me.isActive()) {
      throw new AbortedException();
    }
    if (me == writer) {
      return null;
    }
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        rnode.backup();
        break;
      case ABORTED:
        rnode.recover();
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = me;
    if (!cacheHit) {
      me.memRefs++;
      manager.openSucceeded();
    }
    return null;
  }

  public Set<Transaction> readWriteConflict(Transaction me) {
    for (Transaction reader : readers) {
      if (reader.isActive() && reader != me) {
        return readers;
      }
    }
    readers.clear();
    return readers;
  }

}
