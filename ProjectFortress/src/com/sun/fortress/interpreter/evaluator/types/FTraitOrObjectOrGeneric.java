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
/*
 * Created on Sep 25, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;
import java.util.SortedSet;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.FunctionalMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionalMethod;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierValue;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Useful;

public abstract class FTraitOrObjectOrGeneric extends FType {

    protected FTraitOrObjectOrGeneric(String s, Environment env, AbstractNode def) {
        super(s);
        this.env = env;
        this.decl = def;
        boolean isValueType = false;
        if (def instanceof TraitObjectAbsDeclOrDecl)
            for (Modifier mod : ((TraitObjectAbsDeclOrDecl) def).getMods())
                if (mod instanceof ModifierValue) {
                    isValueType = true;
                    break;
                }
        this.isValueType = isValueType;
    }

    List<? extends AbsDeclOrDecl> members;

    Environment env;

    boolean functionalMethodsFinished;

    final private AbstractNode decl;

    final private boolean isValueType;

    @Override public boolean isValueType() {
        return this.isValueType;
    }

    public List<? extends AbsDeclOrDecl> getASTmembers() {
        return members;
    }

    @Override
    final public Environment getWithin() {
        return env;
    }

    public  AbstractNode getDecl() {
        return decl;
    }

    public void initializeFunctionalMethods() {
        initializeFunctionalMethods(getWithin());

    }
    public final void initializeFunctionalMethods(Environment topLevel) {
        if (isSymbolic)
            return;
        FTraitOrObjectOrGeneric x = this;
        // List<? extends AbsDeclOrDecl> defs = members;

        SortedSet<FnAbsDeclOrDecl> defs = Useful
                .<AbsDeclOrDecl, FnAbsDeclOrDecl> filteredSortedSet(members,
                        new Fn<AbsDeclOrDecl, FnAbsDeclOrDecl>() {
                            @Override
                            public FnAbsDeclOrDecl apply(AbsDeclOrDecl x) {
                                if (x instanceof FnAbsDeclOrDecl)
                                    return (FnAbsDeclOrDecl) x;
                                return null;
                            }
                        }, NodeComparator.fnAbsDeclOrDeclComparer);

        if (x instanceof FTypeGeneric) {

            for (FnAbsDeclOrDecl dod : defs) {

                int spi = NodeUtil.selfParameterIndex((FnAbsDeclOrDecl) dod);
                if (spi >= 0) {
                    // If it is a functional method, it is definitely a
                    // FnAbsDeclOrDecl
                    FnAbsDeclOrDecl fndod = (FnAbsDeclOrDecl) dod;
                    String fndodname = NodeUtil.nameString(fndod.getName());
                    // cl = new OverloadedFunction(fndod.getName(),
                    // getEnv());

                    // If the container is generic, then we create an
                    // empty top-level overloading, to be filled in as
                    // the container is instantiated.
                    Fcn cl = new GenericFunctionalMethod(getWithin(), fndod,
                                                         spi, (FTypeGeneric)x);

                    topLevel.putValueNoShadowFn(fndodname, cl);
                    // TODO test and other modifiers

                }
            }
        } else {

            for (FnAbsDeclOrDecl dod : defs) {

                int spi = NodeUtil.selfParameterIndex((FnAbsDeclOrDecl) dod);
                if (spi >= 0) {
                    // If it is a functional method, it is definitely a
                    // FnAbsDeclOrDecl
                    FnAbsDeclOrDecl fndod = (FnAbsDeclOrDecl) dod;
                    String fndodname = NodeUtil.nameString(fndod.getName());

                    Fcn cl = new FunctionalMethod(getWithin(), fndod, spi, x);
                    if (x instanceof GenericTypeInstance) {
                        topLevel.putFunctionalMethodInstance(fndodname, cl);
                    } else {
                        topLevel.putValueNoShadowFn(fndodname, cl);
                    }
                }
            }
        }
    }


    public void finishFunctionalMethods() {
        if (functionalMethodsFinished)
            return;
        Environment topLevel = getWithin();
        finishFunctionalMethods(topLevel);
        functionalMethodsFinished = true;
    }

    public void finishFunctionalMethods(Environment topLevel) {
        if (isSymbolic)
            return;

        topLevel = topLevel.getTopLevel();
        
        List<? extends AbsDeclOrDecl> defs = members;

        for (AbsDeclOrDecl dod : defs) {
            // Filter out non-functions.
            if (dod instanceof FnAbsDeclOrDecl) {
                int spi = NodeUtil
                        .selfParameterIndex((FnAbsDeclOrDecl) dod);
                if (spi >= 0) {
                    // If it is a functional method, it is definitely a
                    // FnAbsDeclOrDecl
                    FnAbsDeclOrDecl fndod = (FnAbsDeclOrDecl) dod;
                    // System.err.println("Functional method " + dod + "
                    // pass
                    // "+pass);
                    String fndodname = NodeUtil.nameString(fndod.getName());

                    Fcn fcn = (Fcn) topLevel.getRootValue(fndodname);

                    fcn.finishInitializing();
                }
            }
        }
    }

}
