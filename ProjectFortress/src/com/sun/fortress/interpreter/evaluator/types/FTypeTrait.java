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

package com.sun.fortress.interpreter.evaluator.types;
import java.util.List;
import java.util.Set;

import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.BASet;
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
    Set<FType> comprises;
    volatile BetterEnv declaredMembersOf;
    volatile protected boolean membersInitialized; // initially false
    protected volatile Set<FType> transitiveComprises;

    public FTypeTrait(String name, BetterEnv interior, HasAt at, List<? extends AbsDeclOrDecl> members, AbstractNode decl) {
        super(name, interior, at, members, decl);
        this.declaredMembersOf = new BetterEnv(at);
    }

    public Set<FType> getComprises() {
        return comprises;
    }

    public void setComprises(Set<FType> c) {
        comprises = c;
    }

    public Set<FType> getTransitiveComprises() {
        if (transitiveComprises == null) {
            Set<FType> tmp = computeTransitiveComprises();
            synchronized(this) {
                if (transitiveComprises == null) {
                    transitiveComprises = tmp;
                }
            }
        }
        return transitiveComprises;
    }

    protected Set<FType> computeTransitiveComprises() {
        Set<FType> res = new BASet<FType>(FType.comparator);
        if (comprises==null) {
            res.add(this);
        } else {
            for (FType c : comprises) {
                res.addAll(c.getTransitiveComprises());
            }
        }
        return res;
    }

    protected void finishInitializing() {
        declaredMembersOf.bless();
        BetterEnv interior = getWithin();
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
                forTraitMethods, this, null);

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
