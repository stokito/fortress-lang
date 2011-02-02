/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;


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
        return memo.make(d, r);
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

    public FType getRange() {
        return range;
    }

    public FType getDomain() {
        return domain;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#subtypeOf(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public boolean subtypeOf(FType other) {
        if (commonSubtypeOf(other)) return true;
        if (other instanceof FTypeArrow) {
            FTypeArrow that = (FTypeArrow) other;
            return this.range.subtypeOf(that.range) && that.domain.subtypeOf(this.domain);
        }
        return false;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FTypeArrow)) return false;
        FTypeArrow that = (FTypeArrow) other;
        return this.range.equals(that.range) && this.domain.equals(that.domain);
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
        if (other instanceof FTypeTop) {
            return Useful.<FType>set(other);
        } else if (other instanceof BottomType) {
            return Useful.<FType>set(this);
        } else if (other instanceof FTypeArrow) {
            FTypeArrow fta_other = (FTypeArrow) other;
            return Useful.<FType, FType, FType>setProduct(this.domain.meet(fta_other.domain),
                                                          this.range.join(fta_other.range),
                                                          makerObject);
        } else return Collections.<FType>emptySet();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#meet(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public Set<FType> meet(FType other) {
        if (other instanceof FTypeTop) {
            return Useful.<FType>set(this);
        } else if (other instanceof BottomType) {
            return Useful.<FType>set(other);
        } else if (other instanceof FTypeArrow) {
            FTypeArrow fta_other = (FTypeArrow) other;
            return Useful.<FType, FType, FType>setProduct(this.domain.join(fta_other.domain),
                                                          this.range.meet(fta_other.range),
                                                          makerObject);
        } else return Collections.<FType>emptySet();
    }

    @Override
    protected boolean unifyNonVar(Environment env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        if (FType.DUMP_UNIFY) System.out.println("unify arrow " + this + " and " + val + ", abm=" + abm);
        if (!(val instanceof ArrowType)) {
            if (FType.DUMP_UNIFY) System.out.println("       non-arrow");
            return false;
        }
        ArrowType arr = (ArrowType) val;
        try {
            range.unify(env, tp_set, abm, arr.getRange());
            BoundingMap<String, FType, TypeLatticeOps> dual = abm.dual();
            Type valdom = arr.getDomain();
            if (valdom instanceof TupleType && !((TupleType) valdom).getKeywords().isEmpty()) {
                return false;
                // TODO: handle domains containing keywords
            } else {
                domain.unify(env, tp_set, dual, Types.stripKeywords(valdom));
            }
        }
        catch (FortressException p) {
            return false;
        }
        catch (EmptyLatticeIntervalError p) {
            return false;
        }
        return true;
    }

}
