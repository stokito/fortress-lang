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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.BATreeEC;
import com.sun.fortress.useful.HasAt;


public class OverloadedMethod extends OverloadedFunction implements Method {

    BATreeEC<List<FValue>, List<FType>, MethodClosure> mcache =
        new BATreeEC<List<FValue>, List<FType>, MethodClosure>(FValue.asTypesList);


    public OverloadedMethod(String fnName, Environment within) {
        super(NodeFactory.makeId(fnName), within);
        // TODO Auto-generated constructor stub
    }

    public OverloadedMethod(String fnName, Set<? extends Simple_fcn> ssf,
            Environment within) {
        super(NodeFactory.makeId(fnName), ssf, within);
        // TODO Auto-generated constructor stub
    }

    /** We separate out getApplicableMethod so that overloaded
     *  functional method invocations can perform end-to-end caching
     *  of the applicable method.
     */
    public MethodClosure getApplicableMethod(List<FValue> args, HasAt loc,
            Environment envForInference) {
        MethodClosure best_f = mcache.get(args);
        if (best_f == null) {
            best_f = (MethodClosure)bestMatch(args, loc, envForInference, overloads);
        }
        return best_f;
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue,
                              HasAt site, Environment envForInference) {
        Method best_f = getApplicableMethod(args,site,envForInference);
        return best_f.applyMethod(args, selfValue, site, envForInference);
    }

    public void bless() {
        overloads = pendingOverloads;
        pendingOverloads = new ArrayList<Overload>();
        super.bless();
    }


}
