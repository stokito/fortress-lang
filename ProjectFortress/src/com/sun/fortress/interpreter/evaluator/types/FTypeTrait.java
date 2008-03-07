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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildTraitEnvironment;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.useful.HasAt;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class FTypeTrait extends FTraitOrObject {

    /**
     * Trait methods run in an environment that
     * was surrounding the trait, plus the parameters
     * and where-clause-types introduced in the trait
     * definition.  A trait method environment does
     * NOT contain the members (methods) of the trait
     * itself; those are obtained by lookup from $self,
     * which is defined as part of method invocation.
     */
    BetterEnv methodEnv;
    volatile BetterEnv declaredMembersOf;
    volatile protected boolean membersInitialized; // initially false

    public FTypeTrait(String name, BetterEnv interior, HasAt at, List<? extends AbsDeclOrDecl> members, AbstractNode decl) {
        super(name, interior, at, members, decl);
        this.declaredMembersOf = new BetterEnv(at);
    }

    protected void finishInitializing() {
        declaredMembersOf.bless();
        BetterEnv interior = getEnv();
        methodEnv = new BetterEnv(interior, interior.getAt());
        methodEnv.bless();
    }

    public BetterEnv getMethodExecutionEnv() {
        if (methodEnv == null) {
            bug("Internal error, get of unset methodEnv");
        }
        return methodEnv;
    }

    protected void initializeMembers() {
        BetterEnv into = getMembersInternal();
        BetterEnv forTraitMethods = getMethodExecutionEnv();
        List<? extends AbsDeclOrDecl> defs = getASTmembers();

        BuildTraitEnvironment inner = new BuildTraitEnvironment(into,
                forTraitMethods, null);

        inner.doDefs1234(defs);
        membersInitialized = true;
    }

    public BetterEnv getMembers() {
        if (! membersInitialized) {
            synchronized (this) {
                if (! membersInitialized) {
                    initializeMembers();
                }
            }
        }
        return declaredMembersOf;
    }

    protected BetterEnv getMembersInternal() {
        return declaredMembersOf;
    }

}
