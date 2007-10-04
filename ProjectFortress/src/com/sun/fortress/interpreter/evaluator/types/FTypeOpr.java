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

import java.util.List;
import java.util.Set;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1C;
import com.sun.fortress.useful.StringComparer;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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
        memo = new Memo1C<String, FType>( new Factory(), StringComparer.V);
    }
    
    static public FType make(String s) {
        return memo.make(s);
    }
    
    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.TypeRef)
     */
    @Override
    protected boolean unifyNonVar(BetterEnv env, Set<StaticParam> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
        bug(val,env,
                   errorMsg("Unimplemented --  unify opr parameter ", this,
                            " and  type argument ", val));
        return false;
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
