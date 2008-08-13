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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1;
import com.sun.fortress.useful.Useful;

import com.sun.fortress.exceptions.UnificationError;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Environment;

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
            FType fta = (FType)(other);
            for (FType  i : l) {
                if (i.subtypeOf(fta)) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean unifyNonVar(Environment env, Set<String> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
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
        if (FType.DUMP_UNIFY)
            System.out.println("\tAttempting to unify overloadings.");
        for (FType t : l) {
            try {
                t.unify(env,tp_set,abm,val);
                return true;
            } catch (UnificationError e) {
                if (FType.DUMP_UNIFY)
                    System.out.println("\tOverloading "+t+" != "+val+", abm ="+abm);
            } finally {
                abm.assign(savedAbm);
            }
        }
        return false;
    }

}
