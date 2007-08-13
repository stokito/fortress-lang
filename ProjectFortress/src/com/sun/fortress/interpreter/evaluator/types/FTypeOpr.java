/*
 * Created on Aug 7, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.types;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;

public class FTypeOpr extends FType {
    public FTypeOpr(String s) {
        super(s);
    }

    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.TypeRef)
     */
    @Override
    protected boolean unifyNonVar(BetterEnv env, Set<StaticParam> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
        throw new ProgramError(val,env,
                errorMsg("Unimplemented --  unify opr parameter ", this, " and  type argument ", val));
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
