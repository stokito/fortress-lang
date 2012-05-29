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
import java.util.Stack;

import com.sun.fortress.compiler.runtimeValues.MutableFValue;
import com.sun.fortress.compiler.runtimeValues.FValue;

public class Transaction {

    private Transaction parent;
    private static AtomicInteger global_lock = new AtomicInteger(0);
    private AtomicInteger snapshot;
    Hashtable<MutableFValue, FValue> reads;
    Hashtable<MutableFValue, FValue> writes;
    private Transaction topLevelTransaction;
    private static boolean debug = true;
    private static AtomicInteger counter = new AtomicInteger(0);
    private int transactionNumber;

    private String ancestrialString() {
        Transaction current = this;
        Stack transactionHistory = new Stack();
        while (current != null) {
            transactionHistory.push(current.transactionNumber);
            current = current.parent;
        }

        String result = "";
        Boolean first = true;
        while (!transactionHistory.empty()) {
            if (first) {
                result = result + transactionHistory.pop();
                first = false;
            } else {
                result = result +  ":=>" + transactionHistory.pop();
            }
        }
        return result;
    }
        

    private void debug(String s) {
        if (debug)
            System.out.println("TransactionDebug: " + Thread.currentThread().getName() + " Transaction: " + ancestrialString() + " : " +  s);
    }

    public String toString() {
        return "Transaction: " + transactionNumber + " parent = " + parent;
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

    public boolean isTopLevelTransaction() {
        if (parent == null)
            return true;
        else return false;
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
        this.parent = parent;
        this.snapshot = new AtomicInteger(parent.snapshot.get());
        this.reads = new Hashtable<MutableFValue, FValue>();
        this.writes = new Hashtable<MutableFValue, FValue>();
        this.topLevelTransaction = parent.topLevelTransaction;
        this.transactionNumber = counter.getAndIncrement();
    }

    private Transaction(int snapshot) {
        this.parent = null;
        this.snapshot = new AtomicInteger(snapshot);
        this.reads = new Hashtable<MutableFValue, FValue>();
        this.writes = new Hashtable<MutableFValue, FValue>();
        this.topLevelTransaction = this;
        this.transactionNumber = counter.getAndIncrement();
    }        

    private void TXAbort() {
        debug("TXAbort");
        throw new TransactionAbortException();
    }

    public static Transaction TXBegin(Transaction parent) {
        Transaction result;
        if (parent == null) {
            result = new Transaction(snapshotGlobalLock());
        } else {
            result = new Transaction(parent);
            //throw new RuntimeException(Thread.currentThread().getName() + ": Only single level transactions for now");
        }
        result.debug("TXBegin:");
        return result;
    }

    private FValue AncestrialWrite(MutableFValue v) {
        if (writes.containsKey(v))
            return writes.get(v);
        else if (parent == null)
            return null;
        else return parent.AncestrialWrite(v);
    }

    public FValue TXRead(MutableFValue v) {
        FValue AncestrialValue = AncestrialWrite(v);
        if (AncestrialValue != null) {
            debug("TXRead: v = " + v + " ancestrial val = " + AncestrialValue );
            reads.put(v,AncestrialValue);
            return AncestrialValue;
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
        if (this == topLevelTransaction) 
            return TXValidateHelperTopLevelTransaction();
        else return TXValidateHelperNestedTransaction();
    }
     
    public int TXValidateHelperTopLevelTransaction() {
        debug("TXValidateTopLevel");
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

    public int TXValidateHelperNestedTransaction() {
        debug("TXValidateNested");
        int time = waitForGlobalLock();
        for (Map.Entry<MutableFValue,FValue> entry : reads.entrySet()) {
            MutableFValue key = entry.getKey();
            FValue val = entry.getValue();
            FValue ancestrialWrite = parent.AncestrialWrite(key);
            debug("TXValidateNested: key = " + key + " val = " + val + " ancestrialWrite = " + ancestrialWrite);
            if (ancestrialWrite == null) {
                if (key.getValue() != val)
                    TXAbort();
            } else {
                if (ancestrialWrite != val)
                    TXAbort();
            }
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
        debug("TXWrite: v = " + v + " f = " + f + " reads.getValue = " + reads.get(v) + " writes.getValue = " + writes.get(v));
        writes.put(v, f);
    }
    
    public void TXCommit() {
        debug("TXCommit: reads = " + reads + " writers = " + writes);
        if (writes.isEmpty())
            return;

        int time = snapshot.get();
            
        while (! getGlobalLock(time)) {
            time = TXValidate();
            debug("TXCommitFail: time " + time);
            snapshot.set(time);
        }
            
        if (this == topLevelTransaction) {
       
            for (Map.Entry<MutableFValue,FValue> entry : writes.entrySet()) {
                MutableFValue key = entry.getKey();
                FValue val = entry.getValue();
                debug("TXCommitting:  time = " + time + " key = " + key + " val = " + val);
                key.setValue(val);
            }
       
        } else {

            for (Map.Entry<MutableFValue,FValue> entry : reads.entrySet()) {
                MutableFValue key = entry.getKey();
                FValue val = entry.getValue();
                FValue ancestrialValue = parent.AncestrialWrite(key);
                if (ancestrialValue == null) {
                    parent.reads.put(key, val);
                    debug("TXCommitting: read to parent: time = " + time + " key = " + key + " val = " + val);
                }
             }


            for (Map.Entry<MutableFValue,FValue> entry : writes.entrySet()) {
                MutableFValue key = entry.getKey();
                FValue val = entry.getValue();
                parent.writes.put(key, val);
                debug("TXCommitting: write to parent: time = " + time + " key = " + key + " val = " + val);
            }
        }

        if (! releaseGlobalLock(time+1))
            throw new RuntimeException("Who messed with a committing transaction?");
    }

}