/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.exceptions.UnificationError;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1;
import com.sun.fortress.useful.Useful;

import java.util.List;
import java.util.Set;

public class FTypeOverloadedArrow extends FType {

    private static class Factory implements Factory1<List<FType>, FType> {

        public FType make(List<FType> part1) {
            return new FTypeOverloadedArrow(part1);
        }

    }

    static Memo1<List<FType>, FType> memo = null;

    static public void reset() {
        memo = new Memo1<List<FType>, FType>(new Factory());
    }

    static public FType make(List<FType> l) {
        return memo.make(l);
    }

    List<FType> l;

    private FTypeOverloadedArrow(List<FType> l) {
        super("Overloaded"); // TODO should construct this lazily, better
        this.l = l;
    }

    @Override
    public String toString() {
        return Useful.listInParens(l);
    }

    @Override
    public boolean subtypeOf(FType other) {
        if (commonSubtypeOf(other)) return true;
        if (other instanceof FType) {
            FType fta = (FType) (other);
            for (FType i : l) {
                if (i.subtypeOf(fta)) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean unifyNonVar(Environment env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        // Note that this attempts unification, but then rolls back
        // the results.  This is because we otherwise need to choose
        // the "best" overloading based on the available static types,
        // and that's not always going to be possible.

        // To really do this right I'm now convinced that generic
        // function instantiation (including instantiation of generic
        // arguments), overloading, and unification must all occur as
        // a single monolithic pass.  It's unclear that this is worth
        // the bother if we aren't planning to retain this code after
        // type checking is in place.

        BoundingMap<String, FType, TypeLatticeOps> savedAbm = abm.copy();
        BoundingMap<String, FType, TypeLatticeOps> unifiedAbm = null;
        if (FType.DUMP_UNIFY) System.out.println("\tAttempting to unify overloadings.");
        for (FType t : l) {
            try {
                t.unify(env, tp_set, abm, val);
                if (unifiedAbm != null) return true;
                unifiedAbm = abm.copy();
            }
            catch (UnificationError e) {
                if (FType.DUMP_UNIFY) System.out.println("\tOverloading " + t + " != " + val + ", abm =" + abm);
            }
            finally {
                abm.assign(savedAbm);
            }
        }
        if (unifiedAbm != null) {
            abm.assign(unifiedAbm);
            return true;
        }
        return false;
    }

}
