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

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.ReadSet;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FOrdinaryObject;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author Jan-Willem Maessen
 *
 * AtomicArray adapted to FValue
 *
 * Originally based on Christine's AtomicArray, in turn based on
 * Maurice's code.  Now modified beyond all recognition.
 */
public class AtomicFTypeArray extends FOrdinaryObject {
    // We always synchronize with any writer of the following array:
    private final FValue[] array;

    // The following is used lock-free and should eventually be sparse:
    private final AtomicReferenceArray<TransactorRecord> trans;

    private static final String FORMAT = "Unexpected transaction state: %s";
    private static final String FORMAT2 = "Unexpected TransactorRecord: %s";
    public static final boolean TRACE_ARRAY = false;

    public AtomicFTypeArray
            (FType selfType, BetterEnv self_dot_env) {
        super(selfType, BetterEnv.blessedEmpty(), self_dot_env);
        int capacity = self_dot_env.getValue("s0").getInt();
        array        = new FValue[capacity];
        trans        = new AtomicReferenceArray<TransactorRecord>(capacity);
    }

    public FValue get(int i) {
        Transaction me = FortressTaskRunner.getTransaction();

        if (me==null) {
            // In the common case of contention-free non-transactional
            // read, we want to just look at the data and return it
            // without allocating a ReadRecord.
            TransactorRecord orig = trans.get(i);
            // Volatile read happens before regular read.
            FValue res = array[i];
            if (orig==null || orig instanceof ReadRecord) {
                // Must re-check trans.get(i); in order to guarantee
                // that this happens after res is read, we must also
                // write.  Thus we CAS.  Want to use weakCAS to keep
                // this code loop-free, but it doesn't provide
                // ordering guarantees.
                if (trans.compareAndSet(i,orig,orig)) {
                    return res;
                } // else there was contention on the TransactorRecord.
            } // else there is an outstanding write operation; clean it up.
            return getSlowNT(i);
        } // else we need to do a full transctional read.
        return getSlow(i);
    }

    private FValue getSlowNT(int i) {
        while (true) {
            if (TRACE_ARRAY) System.out.println("Slow get("+i+")");
            TransactorRecord orig = trans.get(i);
            TransactorRecord newRec = orig;
            FValue res = array[i];
            if (orig==null) {
                // Momentary hiccup in fast path; retry it.
            } else if (orig instanceof ReadRecord) {
                // Try to clean up outstanding reads.
                newRec = ((ReadRecord)orig).clean();
            } else if (orig instanceof WriteRecord) {
                WriteRecord writeRec = (WriteRecord) orig;
                Transaction writer = writeRec.getTransaction();
                if (writerCompleted(writeRec,writer,i)) {
                    // Have now cleaned up after completed write, so
                    // get rid of it.
                    newRec = null;
                } else {
                    // serialize *before* outstanding writer!
                    res = writeRec.getOldValue();
                }
            } else {
                throw new PanicException(FORMAT2, orig);
            }
            if (trans.compareAndSet(i,orig,newRec)) {
                return res;
            }
        }
    }

    private FValue getSlow(int i) {
        if (TRACE_ARRAY) System.out.println("Transactional get("+i+")");
        while (true) {
            TransactorRecord orig = trans.get(i);
            TransactorRecord readRec;
            if (orig == null) {
                readRec = new ReadRecord(null);
            } else {
                readRec = potentialReadContention(orig,i);
            }
            if (readRec != null && trans.compareAndSet(i,orig,readRec)) {
                FValue res = array[i];
                readRec.completed();
                return res;
            }
            if (TRACE_ARRAY) System.out.println("Retrying get("+i+")");
        }
    }

    public void set(int i, FValue v) {
        while (true) {
            if (TRACE_ARRAY) System.out.println("Trying set("+i+")");
            TransactorRecord orig = trans.get(i);
            if (orig==null || potentialWriteContention(orig,i)) {
                WriteRecord writeRec = new WriteRecord();
                if (trans.compareAndSet(i,orig,writeRec)) {
                    writeRec.setOldValue(array[i]);
                    array[i] = v;
                    writeRec.completed();
                    return;
                }
            }
            if (TRACE_ARRAY) System.out.println("Retrying set("+i+")");
        }
    }

    /** init must only be used to initialize an element, and ought to
     * be the first thing that touches the element.  We take advantage
     * of this to avoid jumping through the usual transactional hoops
     * here.  The price is that we may see violations of the usual
     * transactional semantics if we are racing with an (erroneous)
     * second write or initialization.
     *
     * Thus, we return "true" if we *believe* we successfully wrote to
     * [i] for the first time.  We return "false" if we *know* that
     * our write was not the first successful write to [i].  The
     * resulting operation is *cheap* (especially compared to either
     * get or set).
     *
     * This operation also disrupts racing reads or writes to the
     * array; they are no longer guaranteed to obey the atomicity
     * semantics.  Again, the program is wrong if such reads or writes
     * exist.
     **/
    public boolean init(int i, FValue v) {
        FValue old = array[i];
        array[i] = v;
        /* Ensure subsequent reads see our write.  Also destroys
         * atomicity of anything that was trying to concurrently
         * access this element.  Those accesses ought not to exist
         * anyhow, and ought to fail if they do exist. */
        trans.set(i,null);
        return (old==null);
    }

    private boolean potentialWriteContention(TransactorRecord orig, int i) {
        Transaction me = checkMyValidity();
        if (orig instanceof ReadRecord) {
            ReadRecord rdr = ((ReadRecord)orig).clean();
            while (rdr!=null) {
                Transaction reader = rdr.getTransaction();
                if (reader!=me) {
                    return writeReadContention(me, rdr.getTransaction());
                }
                rdr = rdr.getNext();
                if (rdr == null) break;
                rdr=rdr.clean();
            }
            /* If we get here, all readers are us. */
            return true;
        } else if (orig instanceof WriteRecord) {
            WriteRecord writeRec = (WriteRecord) orig;
            Transaction writer = writeRec.getTransaction();
            if (writer==me || writerCompleted(writeRec,writer,i)) return true;
            return writeWriteContention(me,writer);
        } else {
            throw new PanicException(FORMAT2, orig);
        }
    }

    private TransactorRecord potentialReadContention(TransactorRecord orig, int i) {
        Transaction me = checkMyValidity();
        if (orig instanceof ReadRecord) {
            ReadRecord nxt = ((ReadRecord)orig).clean();
            if (nxt!=null && nxt.getTransaction()==me) return nxt;
            return new ReadRecord(nxt);
        }
        if (orig instanceof WriteRecord) {
            WriteRecord writeRec = (WriteRecord) orig;
            Transaction writer = writeRec.getTransaction();
            if (writer==me) return writeRec;
            if (writerCompleted(writeRec,writer,i)) {
                return new ReadRecord(null);
            }
            return readWriteContention(me, writer);
        }
        throw new PanicException(FORMAT2, orig);
    }

    private Transaction checkMyValidity() {
        Transaction t = FortressTaskRunner.getTransaction();
        if (t==null) return Transaction.COMMITTED_TRANS;
        if (t.isActive()) return t;
        throw new AbortedException();
    }

    private boolean writerCompleted(WriteRecord writeRec, Transaction writer, int i) {
        if (writer!=null) {
            switch (writer.getStatus()) {
            case ACTIVE:
                return false;
            case ABORTED:
                restore(writeRec,i);
                return true;
            case COMMITTED:
                return true;
            default:
                throw new PanicException(FORMAT, writer.getStatus());
            }
        }
        return false;
    }

    private void restore(WriteRecord writeRec, int i) {
        synchronized(writeRec) {
            if (writeRec.mustRestore()) {
                array[i] = writeRec.getOldValue();
                if (TRACE_ARRAY) System.out.println("Restored "+i);
                writeRec.restored();
            }
        }
    }

    private TransactorRecord readWriteContention(Transaction me,
                                                 Transaction writer) {
        if (TRACE_ARRAY) System.out.println("readWriteContention "+me+" and "+writer);
        resolveConflict(me,writer);
        return null;
    }

    private boolean writeReadContention(Transaction me, Transaction reader) {
        if (TRACE_ARRAY) System.out.println("writeReadContention "+me+" and "+reader);
        resolveConflict(me,reader);
        return false;
    }

    private boolean writeWriteContention(Transaction me, Transaction writer) {
        if (TRACE_ARRAY) System.out.println("writeWriteContention "+me+" and "+writer);
        resolveConflict(me,writer);
        return false;
    }

    private void resolveConflict(Transaction me, Transaction other) {
        ContentionManager manager = FortressTaskRunner.getContentionManager();
        manager.resolveConflict(me,other);
    }

}
