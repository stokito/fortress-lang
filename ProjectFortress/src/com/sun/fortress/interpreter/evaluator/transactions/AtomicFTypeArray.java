/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.values.FNativeObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author Jan-Willem Maessen
 *         <p/>
 *         AtomicArray adapted to FValue
 *         <p/>
 *         Originally based on Christine's AtomicArray, in turn based on
 *         Maurice's code.  Now modified beyond all recognition.
 */
public class AtomicFTypeArray extends FNativeObject {
    // The native constructor, containing type and method information:
    private final NativeConstructor con;

    // This contains the actual array data.
    private final AtomicReferenceArray<FValue> array;
    // This contains slot access metadata.  It should be made sparse
    // in some fashion.
    private final AtomicReferenceArray<TransactorRecord> trans;

    private static final String FORMAT = "Unexpected transaction state: %s";
    private static final String FORMAT2 = "Unexpected TransactorRecord: %s";
    public static final boolean TRACE_ARRAY = false;

    public AtomicFTypeArray(NativeConstructor con, int capacity) {
        super(con);
        this.con = con;

        array = new AtomicReferenceArray<FValue>(capacity);
        trans = new AtomicReferenceArray<TransactorRecord>(capacity);
        FortressTaskRunner.debugPrintln("AtomicFTypeArray con = " + con + " capacity = " + capacity);

    }

    public NativeConstructor getConstructor() {
        return this.con;
    }

    public boolean seqv(FValue v) {
        return v == this;
    }

    public FValue get(int i) {
        Transaction me = FortressTaskRunner.getTransaction();

        if (me == null) {
            // In the common case of contention-free non-transactional
            // read, we want to just look at the data and return it
            // without allocating a ReadRecord.
            TransactorRecord orig = trans.get(i);
            // Volatile read happens before volatile read.
            FValue res = array.get(i);
            if (orig == null || orig instanceof ReadRecord) {
                // Must re-check trans.get(i); we had to make array
                // an AtomicReferenceArray in order to guarantee that
                // this second check happens after res is read.
                if (orig == trans.get(i)) {
                    return res;
                } // else there was contention on the TransactorRecord.
            } // else there is an outstanding write operation; clean it up.
        } // else we need to do a full transactional read.
        return getSlow(i);
    }

    private FValue getSlowNT(int i) {
        while (true) {
            if (TRACE_ARRAY) System.out.println("Slow get(" + i + ")");
            TransactorRecord orig = trans.get(i);
            TransactorRecord newRec = orig;
            FValue res = array.get(i);
            if (orig == null) {
                // Momentary hiccup in fast path; retry it.
            } else if (orig instanceof ReadRecord) {
                // Try to clean up outstanding reads.
                newRec = ((ReadRecord) orig).clean();
            } else if (orig instanceof WriteRecord) {
                WriteRecord writeRec = (WriteRecord) orig;
                Transaction writer = writeRec.getTransaction();
                if (writerCompleted(writeRec, writer, i)) {
                    // Have now cleaned up after completed write, so
                    // get rid of it.
                    newRec = null;
                } else {
                    // THIS LINE IS SUSPECT; TALK TO VICTOR AGAIN AND/OR
                    // WORK OUT SOME MORE.  MAY NEED TO AWAIT WRITER.
                    // serialize *before* outstanding writer!
                    res = writeRec.getOldValue();
                }
            } else {
                throw new PanicException(FORMAT2, orig);
            }
            if (trans.compareAndSet(i, orig, newRec)) {
                return res;
            }
        }
    }

    private FValue getSlow(int i) {
        if (TRACE_ARRAY) System.out.println("Transactional get(" + i + ")");
        while (true) {
            TransactorRecord orig = trans.get(i);
            TransactorRecord readRec;
            if (orig == null) {
                readRec = new ReadRecord(null);
            } else {
                readRec = potentialReadContention(orig, i);
            }
            if (readRec != null && trans.compareAndSet(i, orig, readRec)) {
                FValue res = array.get(i);
                readRec.completed();
                return res;
            }
            if (TRACE_ARRAY) System.out.println("Retrying get(" + i + ")");
        }
    }

    public void set(int i, FValue v) {
        while (true) {
            if (TRACE_ARRAY) System.out.println("Trying set(" + i + ")");
            TransactorRecord orig = trans.get(i);
            if (orig == null || potentialWriteContention(orig, i)) {
                WriteRecord writeRec = new WriteRecord();
                if (trans.compareAndSet(i, orig, writeRec)) {
                    writeRec.setOldValue(array.get(i));
                    array.set(i, v);
                    writeRec.completed();
                    return;
                }
            }
            if (TRACE_ARRAY) System.out.println("Retrying set(" + i + ")");
        }
    }

    /**
     * init must only be used to initialize an element, and ought to
     * be the first thing that touches the element.  We take advantage
     * of this to avoid jumping through the usual transactional hoops
     * here.  The price is that we may see violations of the usual
     * transactional semantics if we are racing with an (erroneous)
     * second write or initialization.
     * <p/>
     * Thus, we return "true" if we *believe* we successfully wrote to
     * [i] for the first time.  We return "false" if we *know* that
     * our write was not the first successful write to [i].  The
     * resulting operation is *cheap* (especially compared to either
     * get or set).
     * <p/>
     * This operation also disrupts racing reads or writes to the
     * array; they are no longer guaranteed to obey the atomicity
     * semantics.  Again, the program is wrong if such reads or writes
     * exist.
     */
    public boolean init(int i, FValue v) {
        FValue old = array.get(i);
        array.set(i, v);
        /* Ensure subsequent reads see our write.  Also destroys
         * atomicity of anything that was trying to concurrently
         * access this element.  Those accesses ought not to exist
         * anyhow, and ought to fail if they do exist. */
        trans.set(i, null);
        return (old == null);
    }

    private boolean potentialWriteContention(TransactorRecord orig, int i) {
        Transaction me = checkMyValidity();
        if (orig instanceof ReadRecord) {
            ReadRecord rdr = ((ReadRecord) orig).clean();
            while (rdr != null) {
                Transaction reader = rdr.getTransaction();
                if (reader != me) {
                    return writeReadContention(me, rdr.getTransaction());
                }
                rdr = rdr.getNext();
                if (rdr == null) break;
                rdr = rdr.clean();
            }
            /* If we get here, all readers are us. */
            return true;
        } else if (orig instanceof WriteRecord) {
            WriteRecord writeRec = (WriteRecord) orig;
            Transaction writer = writeRec.getTransaction();
            if (writer == me || writerCompleted(writeRec, writer, i)) return true;
            return writeWriteContention(me, writer);
        } else {
            throw new PanicException(FORMAT2, orig);
        }
    }

    private TransactorRecord potentialReadContention(TransactorRecord orig, int i) {
        Transaction me = checkMyValidity();
        if (orig instanceof ReadRecord) {
            ReadRecord nxt = ((ReadRecord) orig).clean();
            if (nxt != null && nxt.getTransaction() == me) return nxt;
            return new ReadRecord(nxt);
        }
        if (orig instanceof WriteRecord) {
            WriteRecord writeRec = (WriteRecord) orig;
            Transaction writer = writeRec.getTransaction();
            if (writer == me) return writeRec;
            if (writerCompleted(writeRec, writer, i)) {
                return new ReadRecord(null);
            }
            return readWriteContention(me, writer);
        }
        throw new PanicException(FORMAT2, orig);
    }

    private Transaction checkMyValidity() {
        Transaction t = FortressTaskRunner.getTransaction();
        if (t == null || t.isActive()) return t;
        throw new AbortedException(t);
    }

    private boolean writerCompleted(WriteRecord writeRec, Transaction writer, int i) {
        return false;
    }
    //         if (writer!=null) {
    //             switch (writer.getStatus()) {
    //             case ACTIVE:
    //                 return false;
    //             case ABORTED:
    //                 restore(writeRec,i);
    //                 return true;
    //             case COMMITTED:
    //                 return true;
    //             default:
    //                 throw new PanicException(FORMAT, writer.getStatus());
    //             }
    //         }
    //         return false;
    //     }

    private void restore(WriteRecord writeRec, int i) {
        synchronized (writeRec) {
            if (writeRec.mustRestore()) {
                array.set(i, writeRec.getOldValue());
                if (TRACE_ARRAY) System.out.println("Restored " + i);
                writeRec.restored();
            }
        }
    }

    private TransactorRecord readWriteContention(Transaction me, Transaction writer) {
        if (TRACE_ARRAY) System.out.println("readWriteContention " + me + " and " + writer);
        resolveConflict(me, writer);
        return null;
    }

    private boolean writeReadContention(Transaction me, Transaction reader) {
        if (TRACE_ARRAY) System.out.println("writeReadContention " + me + " and " + reader);
        resolveConflict(me, reader);
        return false;
    }

    private boolean writeWriteContention(Transaction me, Transaction writer) {
        if (TRACE_ARRAY) System.out.println("writeWriteContention " + me + " and " + writer);
        resolveConflict(me, writer);
        return false;
    }

    private void resolveConflict(Transaction me, Transaction other) {
        ContentionManager manager = FortressTaskRunner.getContentionManager();
        manager.resolveConflict(me, other);
    }

}
