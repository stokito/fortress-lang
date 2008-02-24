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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.AbstractTupleType;
import com.sun.fortress.nodes.ArgType;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.EmptyLatticeIntervalError;
import com.sun.fortress.useful.Factory2;
import com.sun.fortress.useful.Fn2;
import com.sun.fortress.useful.Memo2;
import com.sun.fortress.useful.Useful;


// TODO Need to memoize this to preserve Equality.
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
        if (other instanceof FTypeDynamic || other instanceof FTypeTop) {
            return Useful.<FType>set(other);
        } else if (other instanceof BottomType) {
            return Useful.<FType>set(this);
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
        if (other instanceof FTypeDynamic || other instanceof FTypeTop) {
            return Useful.<FType>set(this);
        } else if (other instanceof BottomType) {
            return Useful.<FType>set(other);
        } else if (other instanceof FTypeArrow) {
            FTypeArrow fta_other = (FTypeArrow) other;
            return Useful.<FType, FType, FType>setProduct(this.domain.join(fta_other.domain),
                    this.range.meet(fta_other.range), makerObject);
        } else return Collections.<FType>emptySet();
    }

    @Override
    protected boolean unifyNonVar(BetterEnv env, Set<StaticParam> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
        if (FType.DUMP_UNIFY)
            System.out.println("unify arrow "+this+" and "+val+", abm="+abm);
        if (!(val instanceof ArrowType)) {
            if (FType.DUMP_UNIFY)
                System.out.println("       non-arrow");
            return false;
        }
        ArrowType arr = (ArrowType) val;
        try {
            range.unify(env, tp_set, abm, arr.getRange());
            BoundingMap<String, FType, TypeLatticeOps> dual = abm.dual();
            // TODO: Handle domains that are AbstractTupleTypes containing varargs and keywords
            Type valdom = arr.getDomain();

            // Problems with tuples of 1 vs multiple args.
            // Must spot the single-parameter binding to tuple case first.
            if (! (valdom instanceof AbstractTupleType)) {
                domain.unify(env, tp_set, dual, valdom);
            } else if (domain instanceof FTypeTuple) {
                ((FTypeTuple)domain).unifyTuple(env, tp_set, dual, ((AbstractTupleType)valdom).getElements(),
                                                Option.<VarargsType>none());
            } else {
                return false;
            }
        } catch (FortressError p) {
            return false;
        } catch (EmptyLatticeIntervalError p) {
            return false;
        }
        return true;
    }

}
