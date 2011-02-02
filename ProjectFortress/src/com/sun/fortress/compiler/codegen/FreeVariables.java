/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.codegen;

import java.util.*;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BASet;

import edu.rice.cs.plt.tuple.Option;

/**
 * Forward analysis to propagate set of locally bound variables
 */
public class FreeVariables extends NodeCollectingVisitor<BASet<IdOrOp>> {

    private final HashMap<Node, BASet<IdOrOp>> fv;

    public FreeVariables() {
        fv = new HashMap<Node, BASet<IdOrOp>>();
    }

    /************************************************************
     * Retrieve computed results
     ************************************************************/
    public BASet<IdOrOp> freeVars(Node n) {
        BASet<IdOrOp> res = fv.get(n);
        return res;
    }

    /************************************************************
     * Helpers
     ************************************************************/

    private static BASet<IdOrOp> set() {
        return new BASet<IdOrOp>(NodeComparator.idOrOpComparer);
    }

    private static BASet<IdOrOp> set(IdOrOp e1) {
        BASet<IdOrOp> res = set();
        res.add(e1);
        return res;
    }

    // private static BASet<IdOrOp> set(IdOrOp e1, IdOrOp e2) {
    //     BASet<IdOrOp> res = set(e1);
    //     res.add(e2);
    //     return res;
    // }

    // public BASet<IdOrOp> combine(BASet<IdOrOp>... sets) {
    //     BASet<IdOrOp> result = set();
    //     for (int i=1; i < sets.length; i++) result.addAll(sets[i]);
    //     return result;
    // }

    static final BASet<IdOrOp>[] blank = new BASet[0];

    public BASet<IdOrOp> combine(List<BASet<IdOrOp>> sets) {
        BASet<IdOrOp> result = set();
        for (BASet<IdOrOp> s : sets) result.addAll(s);
        return result;
    }

    /**
     * Call record every time you compute an analysis result for a node.
     * Intended to be used as you return:
     *     return record(node, answer);
     */
    public BASet<IdOrOp> recur(Node n) {
        BASet<IdOrOp> res = n.accept(this);
        if (res.comparator() != NodeComparator.idOrOpComparer) {
            throw new Error("Wrong comparator on "+ res);
        }
        fv.put(n, res);
        return res;
    }

    /************************************************************
     * Actual visitors
     ************************************************************/

    /** TODO: Visitors for object decls etc. to get fvs during init
     *  and keep distinct from fvs during method call.  Not relevant
     *  for task generation or closure creation, right? */

    public BASet<IdOrOp> forId(Id i) {
        return set(i);
    }

    public BASet<IdOrOp> forOp(Op i) {
        return set(i);
    }

}
