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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.Fun;
import com.sun.fortress.interpreter.nodes.Span;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Useful;


public class Dummy_fcn extends Simple_fcn {
    private List<FType> domain;
    private String allocationSite;
    private FnName fnName  = new Fun(new Span(), "Dummy");

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Fcn#getFnName()
     */
    @Override
    public FnName getFnName() {
        return fnName;
    }

    public Dummy_fcn(List<FType> _params) {
        super(BetterEnv.empty());
        allocationSite = Useful.backtrace(2, 3);
        this.domain = _params;
        setFtype(FTypeArrow.make(FTypeTuple.make(_params), FTypeVoid.T));
    }

    public List<FType> getDomain() {return domain;}

    public FValue applyInner(List<FValue> vals, HasAt loc, BetterEnv envForInference) {
        return NI.nyi("Dummy_fcn.apply_inner");
    }

    public String toString() {
        return "DummyFunc"+Useful.listInParens(domain);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Simple_fcn#at()
     */
    @Override
    String at() {
        return allocationSite;
    }

    @Override
    boolean getFinished() {
        // TODO Auto-generated method stub
        return true;
    }
}
