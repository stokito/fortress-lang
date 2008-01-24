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

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.useful.HasAt;


public class AnonymousConstructor extends NonPrimitive {

    private HasAt at;
    protected FTypeObject selfType;

    AnonymousConstructor(BetterEnv within, FTypeObject selfType, HasAt at) {
        super(within);
        this.selfType = selfType;
        this.at = at;

    }

    @Override
    public HasAt getAt() {
        return at;
    }

    public String stringName() {
        return "Constructor for " + selfType;
    }

    @Override
    protected void setValueType() {
        // TODO Constructors aren't exposed, do they have a type?
        // yes, they are exposed, and they do.
        setFtype(FTypeArrow.make(getDomain(), selfType));
    }

    @Override
    boolean getFinished() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SimpleName getFnName() {
        // TODO Auto-generated method stub
        return null;
    }

}
