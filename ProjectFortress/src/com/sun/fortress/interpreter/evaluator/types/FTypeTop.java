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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class FTypeTop extends FTypeTrait {
    public final static FTypeTop ONLY = new FTypeTop();
    private final static List<FType> SingleT = Useful.<FType>list(ONLY);
    private final static Set<FType> SingleSet = Useful.<FType>set(ONLY);

    private FTypeTop() {
        super("Any",BetterEnv.blessedEmpty(),new HasAt.FromString("Built in"),
                Collections.<AbsDeclOrDecl>emptyList(), null); // HACK need a token here.
        membersInitialized = true;
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
        bug("Cannot add exclusions to Top type.");
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
        return (other==ONLY);
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
