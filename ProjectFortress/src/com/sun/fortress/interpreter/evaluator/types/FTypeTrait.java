/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildTraitEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    Environment methodEnv;
    Set<FType> comprises;
    volatile BetterEnv declaredMembersOf;
    volatile protected boolean membersInitialized; // initially false
    protected volatile Set<FType> transitiveComprises;

    public FTypeTrait(String name, Environment interior, HasAt at, List<Decl> members, AbstractNode decl) {
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
            Set<FType> tmp = Collections.unmodifiableSet(new HashSet<FType>(computeTransitiveComprises()));
            synchronized (this) {
                if (transitiveComprises == null) {
                    transitiveComprises = tmp;
                }
            }
        }
        return transitiveComprises;
    }

    protected Set<FType> computeTransitiveComprises() {
        Set<FType> res = new BASet<FType>(FType.comparator);
        if (comprises == null) {
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
        Environment interior = getWithin();
        methodEnv = interior.extend();
        methodEnv.bless();
    }

    public Environment getMethodExecutionEnv() {
        if (methodEnv == null) {
            bug("Internal error, get of unset methodEnv");
        }
        return methodEnv;
    }

    protected void initializeMembers() {
        Environment into = getMembersInternal();
        Environment forTraitMethods = getMethodExecutionEnv();
        List<Decl> defs = getASTmembers();

        BuildTraitEnvironment inner = new BuildTraitEnvironment(into, forTraitMethods, this, null);

        inner.doDefs1234(defs);
        membersInitialized = true;
    }

    public BetterEnv getMembers() {
        if (!membersInitialized) {
            synchronized (this) {
                if (!membersInitialized) {
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
