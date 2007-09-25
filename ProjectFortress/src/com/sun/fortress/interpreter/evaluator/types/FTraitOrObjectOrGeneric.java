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
/*
 * Created on Sep 25, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.FunctionalMethod;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes_util.NodeUtil;

public abstract class FTraitOrObjectOrGeneric extends FType {

    public FTraitOrObjectOrGeneric(String s) {
        super(s);
        // TODO Auto-generated constructor stub
    }

    List<? extends AbsDeclOrDecl> members;

    BetterEnv env;
    
    boolean functionalMethodsFinished;

    public List<? extends AbsDeclOrDecl> getASTmembers() {
        return members;
    }

    @Override
    public BetterEnv getEnv() {
        return env;
    }

   public void initializeFunctionalMethods() {
        FTraitOrObjectOrGeneric x = this;
        BetterEnv topLevel = getEnv();
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
                    {
                        Fcn cl;
                        // If the container is generic, then we create an
                        // empty top-level overloading, to be filled in as
                        // the container is instantiated.
                        
                        // if (x.getStaticParams().isPresent()) {
                        if (x instanceof FTypeGeneric) {
                            cl = new OverloadedFunction(fndod.getName(),
                                    topLevel);
                        } else {
                            // Note that the instantiation of a generic
                            // comes
                            // here too
                            cl = new FunctionalMethod(topLevel, fndod,
                                    spi, x);
                        }

                        // TODO test and other modifiers

                        
                        topLevel.putValueNoShadowFn(fndodname, cl);
                    }
                }
            }
        }
    }

    public void finishFunctionalMethods() {
        if (functionalMethodsFinished)
            return;
        List<? extends AbsDeclOrDecl> defs = members;
        BetterEnv topLevel = getEnv();
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
                    {
                        Fcn fcn = (Fcn) topLevel.getValue(fndodname);

                        if (fcn instanceof Closure) {
                            Closure cl = (Closure) fcn;
                            cl.finishInitializing();
                        } else if (fcn instanceof OverloadedFunction) {
                            // TODO it is correct to do this here, though it
                            // won't work yet.
                            OverloadedFunction og = (OverloadedFunction) fcn;
                            og.finishInitializing();

                        }
                    }
                }
            }
        }
        functionalMethodsFinished = true;
    }

    
}
