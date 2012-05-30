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
public class FreeVarTypes extends NodeCollectingVisitor<BASet<VarType>> {

    private final HashMap<Node, BASet<VarType>> fv;

    public FreeVarTypes() {
        fv = new HashMap<Node, BASet<VarType>>();
    }

    /************************************************************
     * Retrieve computed results
     ************************************************************/
    public BASet<VarType> freeVarTypes(Node n) {
        BASet<VarType> res = fv.get(n);
        return res;
    }

    /************************************************************
     * Helpers
     ************************************************************/

    private static BASet<VarType> set() {
        return new BASet<VarType>(NodeComparator.varTypeComparer);
    }

    private static BASet<VarType> set(VarType e1) {
        BASet<VarType> res = set();
        res.add(e1);
        return res;
    }

    // private static BASet<VarType> set(VarType e1, VarType e2) {
    //     BASet<VarType> res = set(e1);
    //     res.add(e2);
    //     return res;
    // }

    // public BASet<VarType> combine(BASet<VarType>... sets) {
    //     BASet<VarType> result = set();
    //     for (int i=1; i < sets.length; i++) result.addAll(sets[i]);
    //     return result;
    // }

    static final BASet<VarType>[] blank = new BASet[0];

    public BASet<VarType> combine(List<BASet<VarType>> sets) {
        BASet<VarType> result = set();
        for (BASet<VarType> s : sets) result.addAll(s);
        return result;
    }

    /**
     * Call record every time you compute an analysis result for a node.
     * Intended to be used as you return:
     *     return record(node, answer);
     */
    public BASet<VarType> recur(Node n) {
        BASet<VarType> res = n.accept(this);
        if (res.comparator() != NodeComparator.varTypeComparer) {
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

    public BASet<VarType> forVarType(VarType i) {
        return set(i);
    }

}
