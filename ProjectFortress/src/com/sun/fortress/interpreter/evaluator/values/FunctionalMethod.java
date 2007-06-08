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

/*
 * Created on Apr 23, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.useful.AssignedList;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Useful;

public class FunctionalMethod extends Closure {
    
    int selfParameterIndex;
    private FType selfParameterType;
    private List<FType> cachedDomain;
    
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Closure#applyInner(java.util.List, com.sun.fortress.interpreter.useful.HasAt, com.sun.fortress.interpreter.env.BetterEnv)
     */
    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        // Not quite right
         FObject self = (FObject) args.get(selfParameterIndex);
        args = Useful.removeIndex(selfParameterIndex, args);
        String mname = getDef().nameAsMethod();
        FValue cl = self.getSelfEnv().getValueNull(mname);
        
        // TODO need to common this up with other method dispatch in Evaluator
        
        if (cl instanceof Method) {
            return ((Method) cl).applyMethod(args, self, loc, envForInference);
        }
        return NI.nyi("Functional method apply, method = " + cl);
 
    }

    public FunctionalMethod(BetterEnv e, Applicable fndef, int self_parameter_index, FType self_parameter_type) {
        super(e, fndef);
         selfParameterIndex = self_parameter_index;
         selfParameterType = self_parameter_type;
        // TODO Auto-generated constructor stub
    }
    
    public List<Parameter> getParams() {
        Parameter selfParam = new Parameter("self", selfParameterType, false);
        return new AssignedList<Parameter>(super.getParams(), selfParameterIndex, selfParam);
     }

}
