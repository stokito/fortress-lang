/********************************************************************************
 Copyright 2012, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.runtimeSystem;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.fortress.compiler.codegen.VarCodeGen;
import com.sun.fortress.compiler.runtimeValues.FValue;
import com.sun.fortress.runtimeSystem.FValueHandle;


public class TransactionRecord {
    TransactionRecord parent;
    volatile int snapshot;
    ConcurrentHashMap<FValueHandle, FValue> reads;
    ConcurrentHashMap<FValueHandle, FValue> writes;
    TransactionRecord() {
        parent = null;
        snapshot = 0;
        reads = new ConcurrentHashMap<FValueHandle, FValue>();
        writes = new ConcurrentHashMap<FValueHandle, FValue>();
    }

    TransactionRecord(TransactionRecord p) {
        parent = p;
        snapshot = p.snapshot;
        reads = new ConcurrentHashMap<FValueHandle, FValue>();
        writes = new ConcurrentHashMap<FValueHandle, FValue>();
    }


    void TXBegin() {
        snapshot = parent.snapshot;
        while ((snapshot & 1) != 0)
            snapshot = parent.snapshot;
    }

    static void TXAbort() {
        throw new RuntimeException("Placeholder for now");
    }

    static void checkValue(FValue v1, FValue v2) {
        if (v1 != v2)
            TXAbort();
    }

    int validate() {
       int time = snapshot;
        while ((time & 1) != 0)
            time = snapshot;

        for (FValueHandle handle : reads.keySet()) {
            FValue fv = reads.get(handle);
            handle.checkValue(fv);
        }

        return time;
    }
}
        
    