/*******************************************************************************
    Copyright 2013, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.runtimeSystem;
import jsr166y.RecursiveAction;

public abstract class FortressAction extends RecursiveAction {
    private static boolean debug = false;
    public Transaction transaction = null;


   private static void debug(String s) {
        if (debug)
            System.out.println("ActionDebug: " + Thread.currentThread().getName() + ":" + s);
    }

    public static void startTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        FortressAction currentAction = ftr.getAction();
        Transaction transaction = Transaction.TXBegin(currentAction.transaction());
        debug("Start transaction ftr = " + ftr + " current action = " + currentAction + " trans = " + transaction);
        currentAction.setTransaction(transaction);
    }

    public static void endTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        FortressAction currentAction = ftr.getAction();
        Transaction transaction = ftr.getAction().transaction();
        debug("End transaction ftr = " + ftr + " current action = " + currentAction + " trans = " + transaction);
        transaction.TXCommit();
        currentAction.setTransaction(transaction.getParent());
    }

    public static void cleanupTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        FortressAction currentAction = ftr.getAction();
        Transaction transaction = ftr.getAction().transaction();
        debug("Cleanup transaction ftr = " + ftr + " current action = " + currentAction + " trans = " + transaction);
        currentAction.setTransaction(null);
    }

    public static Transaction getCurrentTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("getCurrentTransaction: ftr = " + ftr );
        if (ftr.action() != null)
            return ftr.getAction().transaction();
        else return null;
    }

    public static boolean inATransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("inATransaction: ftr = " + ftr + " action = " + ftr.action());
        if (ftr.action() != null) 
            if (ftr.action().transaction() != null)
                return true;
        return false;
    }

    public static void setAction(FortressAction t) {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("setAction: ftr = " + ftr + " action = " + t);
        ftr.setAction(t);
    }

    Transaction transaction() {
        return transaction;
    }

    public void setTransaction(Transaction t) {
        transaction = t;
    }
}