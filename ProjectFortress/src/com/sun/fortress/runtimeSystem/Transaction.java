/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.fortress.compiler.runtimeValues.MutableFValue;
import com.sun.fortress.compiler.runtimeValues.FValue;

public class Transaction {

    private Transaction parent;
    private static AtomicInteger global_lock = new AtomicInteger(0);
    private AtomicInteger snapshot;
    Hashtable<MutableFValue, FValue> reads;
    Hashtable<MutableFValue, FValue> writes;
    private Transaction topLevelTransaction;
    private static boolean debug = false;

    private static void debug(String s) {
        if (debug)
            System.out.println("TransactionDebug: " + Thread.currentThread().getName() + ":" + s);
    }

    public Transaction getParent() {
        return parent;
    }

    private static boolean globalLockHeld() {
        return ((global_lock.get() & 1) != 0);
    }

    private static boolean globalLockFree() {
        return !globalLockHeld();
    }

    private static int waitForGlobalLock() {
        int temp = global_lock.get();
        while ((temp & 1) != 0)
            temp = global_lock.get();
        return temp;
    }

    private static boolean getGlobalLock(int i) {
        if (( i & 1) == 0)
            return global_lock.compareAndSet(i, i+1);
        else throw new RuntimeException(Thread.currentThread().getName() + "Tried to get an already held lock");
    }

    private static boolean releaseGlobalLock(int i) {
        if ((i & 1) != 0)
            return global_lock.compareAndSet(i, i+1);
        else throw new RuntimeException(Thread.currentThread().getName() + "Tried to release unheld lock");
    }

    private static int snapshotGlobalLock() {
        int time = global_lock.get();
        while ((time & 1) != 0)
            time = global_lock.get();
        return time;
    }

    private Transaction(Transaction parent) {
        debug("Creating a new transaction with parent " + parent);
        this.parent = parent;
        this.snapshot = new AtomicInteger(parent.snapshot.get());
        this.reads = new Hashtable<MutableFValue, FValue>();
        this.writes = new Hashtable<MutableFValue, FValue>();
        this.topLevelTransaction = parent.topLevelTransaction;
    }

    private Transaction(int snapshot) {
        debug("Creating a new top level transaction");
        this.parent = null;
        this.snapshot = new AtomicInteger(snapshot);
        this.reads = new Hashtable<MutableFValue, FValue>();
        this.writes = new Hashtable<MutableFValue, FValue>();
        this.topLevelTransaction = this;
    }        

    private void TXAbort() {
        debug("TXAbort");
        throw new TransactionAbortException();
    }

    public static Transaction TXBegin(Transaction parent) {
        debug("TXBegin");
        if (parent == null) {
            return new Transaction(snapshotGlobalLock());
        } else {
            throw new RuntimeException(Thread.currentThread().getName() + ": Only single level transactions for now");
        }
    }


    public FValue TXRead(MutableFValue v) {
        if (writes.containsKey(v)) {
            debug("TXRead: v = " + v + " val = " + writes.get(v));
            return writes.get(v);
        } else {
            FValue val = v.getValue();
            
            while (snapshot.get() != global_lock.get()) {
                snapshot.set(TXValidate());
                val = v.getValue();
            }
            debug("TXRead: v = " + v + " val = " + val);
            reads.put(v, val);
            return val;
        }
    }

    public int TXValidateHelper() {
        debug("TXValidate");
        int time = waitForGlobalLock();

        for (Map.Entry<MutableFValue,FValue> entry : reads.entrySet()) {
            MutableFValue key = entry.getKey();
            FValue val = entry.getValue();
            debug("TXValidate: key = " + key + " val = " + val);
            if (key.getValue() != val)
                TXAbort();
        }

        if (time == waitForGlobalLock())
            return time;
        else return TXValidateHelper();
    }

    public int TXValidate() {
        int time = TXValidateHelper();
        if ((time & 1) != 0)
            throw new RuntimeException(Thread.currentThread().getName() + " TXValidate should always be even");
        return time;
    }

    public void TXWrite(MutableFValue v, FValue f) {
        debug("TXWrite: v = " + v + " f = " + f);
        writes.put(v, f);
    }
    
    public void TXCommit() {
        debug("TXCommit: reads = " + reads + " writers = " + writes);
        if (writes.isEmpty())
            return;

        if (this == topLevelTransaction) {
            int time = snapshot.get();
            
            while (! getGlobalLock(time)) {
                time = TXValidate();
                debug("TXCommitFail: time " + time);
                snapshot.set(time);
            }
            
            for (Map.Entry<MutableFValue,FValue> entry : writes.entrySet()) {
                MutableFValue key = entry.getKey();
                FValue val = entry.getValue();
                debug("TXCommitting:  time = " + time + " key = " + key + " val = " + val);
                key.setValue(val);
            }
            
            if (! releaseGlobalLock(time+1))
                throw new RuntimeException("Who messed with a committing transaction?");

        } else {
            throw new RuntimeException("Only doing top level transactions");

//         if (parent != topLevelTransaction) {
//             for (Map.Entry<MutableFValue,FValue> entry : writes.entrySet()) {
//                 MutableFValue key = entry.getKey();
//                 FValue val = entry.getValue();
//                 parent.writes.put(key, val);
//             }

//         } else 
//         parent.snapshot.set(snapshot.get() + 2);
        }
    }

}