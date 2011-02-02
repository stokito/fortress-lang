/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import java.util.List;


public class Dummy_fcn extends Simple_fcn {
    private List<FType> domain;
    private String allocationSite;
    private IdOrOpOrAnonymousName fnName = NodeFactory.makeId(NodeFactory.interpreterSpan, "Dummy");

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Fcn#getFnName()
     */
    @Override
    public IdOrOpOrAnonymousName getFnName() {
        return fnName;
    }

    public String stringName() {
        return fnName.stringName();
    }

    public Dummy_fcn(List<FType> _params) {
        super(BetterEnv.blessedEmpty());
        allocationSite = Useful.backtrace(2, 3);
        this.domain = _params;
        setFtype(FTypeArrow.make(_params, FTypeVoid.ONLY));
    }

    public List<FType> getDomain() {
        return domain;
    }

    public FValue applyInnerPossiblyGeneric(List<FValue> vals) {
        return NI.nyi("Dummy_fcn.apply_inner");
    }

    public String toString() {
        return "DummyFunc" + Useful.listInParens(domain);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Simple_fcn#at()
     */
    @Override
    public String at() {
        return allocationSite;
    }

    @Override
    boolean getFinished() {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean seqv(FValue v) {
        return false;
    }
}
