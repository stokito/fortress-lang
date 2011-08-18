/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1;

import java.util.Set;


public class IntNat extends FTypeNat {
    private final static IntNat[] small = new IntNat[1025];

    static {
        for (int i = 0; i < small.length; i++) {
            small[i] = new IntNat(i);
        }
    }

    public static void reset() {
        for (int i = 0; i < small.length; i++) {
            small[i] = new IntNat(i);
        }
    }

    public static IntNat make(Long ll) {
        long l = ll.longValue();
        if (l < small.length && l >= 0) return small[(int) l];
        return memo.make(ll);
    }

    public static IntNat make(long l) {
        if (l < small.length && l >= 0) return small[(int) l];
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
        if (lazyName == null) lazyName = "nat " + i;
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

    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.Type)
     */
    @Override
    protected boolean unifyNonVar(Environment env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        if (FType.DUMP_UNIFY) System.out.println("unifying IntNat " + this + " and " + val);
        return false;
    }

    @Override
    public void unifyStaticArg(Environment env,
                               Set<String> tp_set,
                               BoundingMap<String, FType, TypeLatticeOps> abm,
                               StaticArg val) {
        if (FType.DUMP_UNIFY) System.out.println(
                "unifying IntNat " + this + " and " + val.getClass().getSimpleName() + " " + val);
        if (val instanceof IntArg) {
            IntExpr n = ((IntArg) val).getIntVal();
            if (n instanceof IntBase) {
                if (((IntBase) n).getIntVal().getIntVal().intValue() == this.getValue()) {
                    // no error
                    return;
                }
            } else if (n instanceof IntRef) {
                String nm = ((IntRef) n).getName().getText();
                abm.joinPut(nm, this);
                return;
            }
        }
        super.unifyStaticArg(env, tp_set, abm, val);
    }

}
