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

import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.nodes.BaseNatType;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.useful.ABoundingMap;
import com.sun.fortress.interpreter.useful.Factory1;
import com.sun.fortress.interpreter.useful.Memo1;


public class IntNat extends FTypeNat {
    private final static IntNat[] small = new IntNat[1025];

    static {
       for (int i = 0; i < small.length; i++)
           small[i] = new IntNat(i);
    }

    public static void reset() {
        for (int i = 0; i < small.length; i++)
            small[i] = new IntNat(i);
    }

    public static IntNat make(Long ll) {
        long l = ll.longValue();
        if (l < small.length  && l >= 0)
            return small[(int)l];
        return memo.make(ll);
    }

    public static IntNat make(long l) {
        if (l < small.length  && l >= 0)
            return small[(int)l];
        return memo.make(Long.valueOf(l));
    }

    private static class Factory implements Factory1<Long, IntNat> {

        public IntNat make(Long part1) {
            return new IntNat(part1);
        }

    }

    static Memo1<Long, IntNat> memo = new Memo1<Long, IntNat>(new Factory());

    private IntNat(Long l) {
        super("IntNat");
        i = l.longValue();
    }

    private IntNat(long l) {
        super("IntNat");
        i = l;
    }

    String lazyName;

    public String getName() {
        if (lazyName == null)
            lazyName = "nat " + i;
        return lazyName;
    }

    public long getValue() {
        return i.longValue();
    }

    public Long getNumber() {
        return i;
    }

    private Long i;

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof IntNat) {
            return getValue() == ((IntNat) other).getValue();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#hashCode()
     */
    @Override
    public int hashCode() {
        return getNumber().hashCode();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#toString()
     */
    @Override
    public String toString() {
        return String.valueOf(getValue());
    }

    /* @see com.sun.fortress.interpreter.evaluator.types.FTypeNat#unify()
     */
    @Override
    public void unify(BetterEnv env, Set<TypeParam> tp_set,
            ABoundingMap<String, FType, TypeLatticeOps> abm, StaticArg val) {
        if (val instanceof BaseNatType) {
            BaseNatType n = (BaseNatType) val;
            if (n.getValue() != this.getValue()) {
                throw new ProgramError(val,env,"Actual nat type "+this+" does not match declared type "+val);
            }
        } else {
            /* Eventually handle more fiddly cases here. */
            super.unify(env,tp_set,abm,val);
        }
    }
}
