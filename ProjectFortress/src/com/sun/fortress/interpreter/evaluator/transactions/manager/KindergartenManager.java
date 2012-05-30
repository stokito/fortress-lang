/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.transactions.util.FortressRandom;

import java.util.TreeSet;

/**
 * Transactions take turns playing with blocks.
 *
 * @author Bill Scherer
 */
public class KindergartenManager extends BaseManager {
    static final int SLEEP_PERIOD = 1000; // was 100
    static final int MAX_RETRIES = 100; // was 10
    TreeSet<KindergartenManager> otherChildren;
    //FortressRandom random;

    /**
     * Creates new <code>Kindergarten</code> manager
     */
    public KindergartenManager() {
        super();
        otherChildren = new TreeSet<KindergartenManager>();
        otherChildren.add(this);
        //random = new FortressRandom();
    }

    public void resolveConflict(Transaction me, Transaction other) {
        try {
            KindergartenManager otherManager = (KindergartenManager) other.getContentionManager();
            // first, check sharing records.
            if (otherChildren.contains(otherManager)) {
                otherChildren.remove(otherManager);
                other.abort();                      // My turn! My turn!
                return;
            }
            for (int i = 0; i < MAX_RETRIES; i++) {
                this.sleep(SLEEP_PERIOD);
                if (!other.isActive()) {
                    return;
                }
            }
            me.abort(); // give up
            return;
        }
        catch (ClassCastException e) {
            other.abort(); // Oh, other not a Kindergartener. Kill it.
            return;
        }
    }
}
