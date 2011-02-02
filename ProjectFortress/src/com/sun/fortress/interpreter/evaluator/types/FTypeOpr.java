/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1C;

import java.util.Set;

public class FTypeOpr extends FType {
    private FTypeOpr(String s) {
        super(s);
    }

    static Memo1C<String, FType> memo = null;

    private static class Factory implements Factory1<String, FType> {
        public FType make(String part1) {
            return new FTypeOpr(part1);
        }
    }

    public static void reset() {
        memo = new Memo1C<String, FType>(new Factory(), DefaultComparator.V);
    }

    static public FType make(String s) {
        return memo.make(s);
    }

    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.TypeRef)
     */
    @Override
    protected boolean unifyNonVar(Environment env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        bug(val, env, errorMsg("Unimplemented --  unify opr parameter ", this, " and  type argument ", val));
        return false;
    }

    @Override
    public void unifyStaticArg(Environment env,
                               Set<String> tp_set,
                               BoundingMap<String, FType, TypeLatticeOps> abm,
                               StaticArg val) {
        bug(val, env, errorMsg("Unimplemented --  unify opr parameter ", this, " and  type argument ", val));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof FTypeOpr) {
            return getName().equals(((FTypeOpr) other).getName());
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#hashCode()
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }


}
