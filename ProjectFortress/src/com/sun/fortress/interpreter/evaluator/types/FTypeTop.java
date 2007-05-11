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
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Useful;


public class FTypeTop extends FTypeTrait {
    public final static FTypeTop T = new FTypeTop();
    private final static List<FType> SingleT = Useful.<FType>list(T);
    private final static Set<FType> SingleSet = Useful.<FType>set(T);

    private FTypeTop() {
        super("Any",BetterEnv.empty(),new HasAt.FromString("Built in"));
    }

    @Override
    public List<FType> getExtends() {
        return java.util.Collections.<FType>emptyList();
    }

    @Override
    public List<FType> getTransitiveExtends() {
        return SingleT;
    }

    @Override
    public List<FType> getProperTransitiveExtends() {
        return java.util.Collections.<FType>emptyList();
    }

    @Override
    public Set<FType> getExcludes() {
        return java.util.Collections.<FType>emptySet();
    }

    @Override
    public void addExclude(FType t) {
        throw new InterpreterError("Cannot add exclusions to Top type.");
    }

    @Override
    public boolean excludesOther(FType other) {
        return false;
    }

    @Override
    public boolean subtypeOf(FType other) {
        return commonSubtypeOf(other);
    }

    @Override
    public boolean equals(Object other) {
        return (other==T);
    }

    @Override
    public Set<FType> meet(FType t2) {
        return Useful.set(t2);
    }

    @Override
    public Set<FType> join(FType t2) {
        return SingleSet;
    }

}
