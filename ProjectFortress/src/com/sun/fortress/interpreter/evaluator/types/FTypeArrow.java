/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.nodes.ArrowType;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.BoundingMap;
import com.sun.fortress.interpreter.useful.Factory2;
import com.sun.fortress.interpreter.useful.Fn2;
import com.sun.fortress.interpreter.useful.Memo2;
import com.sun.fortress.interpreter.useful.Useful;


// TODO Need to memoize this to preserve EQuality.
public class FTypeArrow extends FType {
    private static class Factory implements Factory2<FType, FType, FType> {

        public FType make(FType part1, FType part2) {
            return new FTypeArrow(part1, part2);
        }

    }

    static Memo2<FType, FType, FType> memo = null;

    static public void reset() {
        memo = new Memo2<FType, FType, FType>(new Factory());
    }

    static public FType make(FType d, FType r) {
        return memo.make(d,r);
    }

    public static FType make(List<FType> domain2, FType range2) {
        return make(FTypeTuple.make(domain2), range2);
    }

    private FType domain;
    private FType range;
    private FTypeArrow(FType d, FType r) {
        super("(" + d.getName() + "->" + r.getName() + ")");
        domain = d;
        range = r;
    }
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#subtypeOf(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public boolean subtypeOf(FType other) {
        if (commonSubtypeOf(other)) return true;
        if (other instanceof FTypeArrow) {
            FTypeArrow that = (FTypeArrow) other;
            return this.range.subtypeOf(that.range) &&
                   that.domain.subtypeOf(this.domain);
        }
        return false;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof FTypeArrow)) return false;
        FTypeArrow that = (FTypeArrow) other;
        return this.range.equals(that.range) &&
               this.domain.equals(that.domain);
    }

    public boolean excludesOther(FType other) {
        if (other instanceof FTypeArrow) {
            // TODO Need to refine this, perhaps
            FTypeArrow other_arrow = (FTypeArrow) other;
            return range.excludesOther(other_arrow.range);
        } else {
            // TODO Eventually this will be a trait
            return true;
        }
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#toString()
     */
    @Override
    public String toString() {
        return domain.toString() + "->" + range.toString();
    }

    static Fn2<FType, FType, FType> makerObject = new Fn2<FType, FType, FType>() {
        public FType apply(FType a, FType b) {
            return make(a, b);
        }
    };

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#join(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public Set<FType> join(FType other) {
        if (other instanceof FTypeDynamic) {
            return Useful.<FType>set(other);
        } else if (other instanceof FTypeArrow) {
            FTypeArrow fta_other = (FTypeArrow) other;
            return Useful.<FType, FType, FType>setProduct(this.domain.meet(fta_other.domain),
                    this.range.join(fta_other.range), makerObject);
        } else return Collections.<FType>emptySet();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#meet(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public Set<FType> meet(FType other) {
        if (other instanceof FTypeDynamic) {
            return Useful.<FType>set(this);
        } else if (other instanceof FTypeArrow) {
            FTypeArrow fta_other = (FTypeArrow) other;
            return Useful.<FType, FType, FType>setProduct(this.domain.join(fta_other.domain),
                    this.range.meet(fta_other.range), makerObject);
        } else return Collections.<FType>emptySet();
    }

    @Override
    protected boolean unifyNonVar(BetterEnv env, Set<StaticParam> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, TypeRef val) {
        if (FType.DUMP_UNIFY)
            System.out.println("unify arrow "+this+" and "+val);
        if (!(val instanceof ArrowType)) {
            System.out.println("       non-arrow");
            return false;
        }
        ArrowType arr = (ArrowType) val;
        try {
            range.unify(env, tp_set, abm, arr.getRange());
            BoundingMap<String, FType, TypeLatticeOps> dual = abm.dual();
            List<TypeRef> valdom = arr.getDomain();
            if (domain instanceof FTypeTuple) {
                ((FTypeTuple)domain).unifyTuple(env, tp_set, dual, valdom);
            } else if (valdom.size()==1) {
                domain.unify(env, tp_set, dual, valdom.get(0));
            } else {
                return false;
            }
        } catch (ProgramError p) {
            return false;
        }
        return true;
    }

}
