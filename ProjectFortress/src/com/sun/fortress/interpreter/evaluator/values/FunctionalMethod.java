/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.AssignedList;
import com.sun.fortress.useful.Useful;

import java.util.List;

public class FunctionalMethod extends FunctionClosure implements HasSelfParameter {

    final private int selfParameterIndex;
    final private FTraitOrObjectOrGeneric selfParameterType;
    final private String mname;

    public FunctionalMethod(Environment e,
                            Applicable fndef,
                            int self_parameter_index,
                            FTraitOrObjectOrGeneric self_parameter_type) {
        super(e, fndef, true);
        selfParameterIndex = self_parameter_index;
        selfParameterType = self_parameter_type;
        mname = NodeUtil.nameAsMethod(getDef());
        // TODO Auto-generated constructor stub
    }

    protected FunctionalMethod(Environment e,
                               Applicable fndef,
                               List<FType> static_args,
                               int self_parameter_index,
                               FTraitOrObjectOrGeneric self_parameter_type) {
        super(e, fndef, static_args);
        selfParameterIndex = self_parameter_index;
        selfParameterType = self_parameter_type;
        mname = NodeUtil.nameAsMethod(getDef());
        // TODO Auto-generated constructor stub
    }

    public MethodClosure getApplicableClosure(List<FValue> args0) {
        FValue selfVal = args0.get(selfParameterIndex);
        DottedMethodApplication ma = DottedMethodApplication.make(selfVal, s(def), mname);
        Method cl = ma.getMethod();
        if (cl instanceof MethodClosure) {
            return (MethodClosure) cl;
        } else if (cl instanceof OverloadedMethod) {
            List<FValue> args = Useful.removeIndex(selfParameterIndex, args0);
            OverloadedMethod om = (OverloadedMethod) cl;
            return om.getApplicableMethod(args);
        } else {
            return bug(errorMsg("Functional method resolution for ", this, args0, " yields non-MethodClosure ", cl));
        }
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.FunctionClosure#applyInnerPossiblyGeneric(java.util.List, com.sun.fortress.interpreter.useful.HasAt, com.sun.fortress.interpreter.env.BetterEnv)
     */
    @Override
    public FValue applyInnerPossiblyGeneric(List<FValue> args) {
        FValue selfVal = args.get(selfParameterIndex);
        args = Useful.removeIndex(selfParameterIndex, args);
        return DottedMethodApplication.invokeMethod(selfVal, s(def), mname, args);
    }

    @Override
    public List<Parameter> getParameters() {
        Parameter selfParam = new Parameter("self", selfParameterType, false);
        return new AssignedList<Parameter>(super.getParameters(), selfParameterIndex, selfParam);
    }

    public int hashCode() {
        return def.hashCode() + selfParameterType.hashCode() + (instArgs == null ? 0 : instArgs.hashCode());
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o.getClass().equals(this.getClass())) {
            FunctionalMethod oc = (FunctionalMethod) o;
            return def == oc.def && selfParameterType.equals(oc.selfParameterType) && (instArgs == null ?
                                                                                       (oc.instArgs == null) :
                                                                                       oc.instArgs == null ?
                                                                                       false :
                                                                                       instArgs.equals(oc.instArgs));
        }
        return false;
    }

    public int getSelfParameterIndex() {
        return selfParameterIndex;
    }

    public FTraitOrObjectOrGeneric getSelfParameterType() {
        return selfParameterType;
    }

    public String toString() {
        String res = s(def) + "fn meth(self " + selfParameterIndex + ")";
        if (instArgs != null) res += Useful.listInOxfords(instArgs);
        if (type() != null) {
            res += ":" + type();
        } else {
            res += " [no type]";
        }
        return res + " (" + def.at() + ")";
    }

}
