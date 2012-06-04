/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.BATreeEC;
import com.sun.fortress.useful.Useful;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class OverloadedMethod extends OverloadedFunction implements Method {

    BATreeEC<List<FValue>, List<FType>, MethodClosure> mcache = new BATreeEC<List<FValue>, List<FType>, MethodClosure>(
            FValue.asTypesList);


    public OverloadedMethod(String fnName, Environment within) {
        super(NodeFactory.makeId(NodeFactory.interpreterSpan, fnName), within);
        // TODO Auto-generated constructor stub
    }

    public OverloadedMethod(String fnName, Set<? extends Simple_fcn> ssf, Environment within) {
        super(NodeFactory.makeId(NodeFactory.interpreterSpan, fnName), ssf, within);
        // TODO Auto-generated constructor stub
    }

    /**
     * We separate out getApplicableMethod so that overloaded
     * functional method invocations can perform end-to-end caching
     * of the applicable method.
     */
    public MethodClosure getApplicableMethod(List<FValue> args) {
        MethodClosure best_f = mcache.get(args);
        if (best_f == null) {
            best_f = (MethodClosure) bestMatch(args, overloads);
        }
        return best_f;
    }

    public FValue applyMethod(FObject selfValue, List<FValue> args) {
        Method best_f = getApplicableMethod(args);
        return best_f.applyMethod(selfValue, args);
    }

    public FValue applyMethod(FObject self) {
        return applyMethod(self, Collections.<FValue>emptyList());
    }

    public FValue applyMethod(FObject self, FValue a) {
        return applyMethod(self, Collections.singletonList(a));
    }

    public FValue applyMethod(FObject self, FValue a, FValue b) {
        return applyMethod(self, Useful.list(a, b));
    }

    public FValue applyMethod(FObject self, FValue a, FValue b, FValue c) {
        return applyMethod(self, Useful.list(a, b, c));
    }

    public void bless() {
        overloads = pendingOverloads;
        pendingOverloads = new ArrayList<Overload>();
        super.bless();
    }


}
