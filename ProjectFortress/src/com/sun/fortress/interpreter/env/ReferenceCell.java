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

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.ReadSet;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;

/**
 * What the interpreter stores mutable things (fields, variables)
 * in.  It will eventually acquire transactional semantics.
 */

public class ReferenceCell extends IndirectionCell {
    private FType theType;
    ValueNode node;

    // for debugging
    static final AtomicInteger counter = new AtomicInteger(0);
    final int id;

    public ReferenceCell() {
        super();
        node = new ValueNode();
        id = counter.getAndIncrement();
    }

    public ReferenceCell(FType t, FValue v) {
        super();
        theType = t;
        node = new ValueNode(v);
        id = counter.getAndIncrement();
    }

    public ReferenceCell(FType t) {
        super();
        theType = t;
        node = new ValueNode();
        id = counter.getAndIncrement();
    }

    public FType getType() { return theType;}
    public String toString() { return "ReferenceCell" + id;}


    private boolean transactionIsCommitted(Transaction w) {
        if (w == null) return true;
        else if (w.isCommitted()) return true;
        return false;
    }

    private boolean transactionIsAbortedOrOrphaned(Transaction w) {
        if (w == null) return false;
        else if (w.isAborted()) return true;
        else if (w.isOrphaned()) return true;
        else return false;
	}

    private void debugPrint(String s) {
        System.out.println(Thread.currentThread().getName() + s);
    }

    private boolean transactionIsNotActive(Transaction w) {
        if (w == null) return false;
        else if (w.isAborted()) return true;
        else if (w.isOrphaned()) return true;
        else if (w.isCommitted()) return true;
        else return false;
    }

    private synchronized void cleanup() {
        Transaction w = node.getWriter();
        FValue prev = node.getValue();
        while (w != null && transactionIsNotActive(w)) {
            if (transactionIsAbortedOrOrphaned(w))  
                node = node.getOld();
            else if (transactionIsCommitted(w)) {
                Transaction p = w.getParent();
                if (p == null) {
                    node = new ValueNode(node.getValue(), node.getOld().getReaders());
                } else  {
                    node = new ValueNode(node.getValue(), p, node.getOld());
                }
            }
            else throw new PanicException("Shouldn't get here");
			if (node != null) 
				w = node.getWriter();
			else w = null;
        }
    }

    public synchronized void assignValue(FValue f2) {
        Transaction me  = FortressTaskRunner.getTransaction();
        cleanup();
        // writer is either active, or null
        if (me == null) { // top level assignment
            ValueNode temp = node;
            node = new ValueNode(f2);
            temp.AbortAllReadersAndWriters();
        } else if (!me.isActive()) {
            throw new AbortedException(me, "Somebody killed me ");
        } else {
            node.resolveReadWriteConflicts();
            cleanup();
            if (!me.isActive())
                throw new AbortedException(me, "Somebody killed me ");      

            Transaction w = node.getWriter();

            if (w == null || w.isAncestorOf(me)) {
                node = new ValueNode(f2, me, node);
                me.addWrite(this, f2);
            } else if (w.isActive()) {
                throw new RuntimeException("How can writer be active after cleanup?");
            }
        }
    }

    public synchronized FValue getValue() {
        Transaction me  = FortressTaskRunner.getTransaction();
        // Top Level transaction 
        if (me == null) {
            node.AbortWriter();
            cleanup();
            return node.getValue();
        }

        if (!me.isActive()) throw new AbortedException(me, "Somebody killed me");   

        cleanup();  

        Transaction w = node.getWriter();
        if (w == me) {
            return node.getValue();
        } else if (w == null || w.isAncestorOf(me)) {
            node.addReader();
            me.addRead(this, node.getValue());
            return node.getValue();
        } else if (w.isActive()) {      
            node.resolveWriteConflict();
        }
        return getValue();
    }

    public FValue getValueNull() {
        return getValue();
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
        return getValue() != null;
    }

}
