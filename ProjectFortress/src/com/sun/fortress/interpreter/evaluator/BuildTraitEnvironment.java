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

package com.sun.fortress.interpreter.evaluator;

import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.PartiallyDefinedMethod;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Voidoid;


public class BuildTraitEnvironment extends BuildEnvironments {

    Set<String> fields;

    BetterEnv methodEnvironment;

    public BuildTraitEnvironment(BetterEnv within, BetterEnv methodEnvironment,
            Set<String> fields) {
        super(within);
        this.fields = fields;
        this.methodEnvironment = methodEnvironment;
    }

    protected Simple_fcn newClosure(BetterEnv e, Applicable x) {
        return new PartiallyDefinedMethod(containing, methodEnvironment, x);
    }

    protected GenericMethod newGenericClosure(BetterEnv e, FnAbsDeclOrDecl x) {
        return new GenericMethod(containing, methodEnvironment, x, true);
    }

    /**
     * Put a value, perhaps unconditionally depending on subtype's choice
     *
     * @param e
     * @param name
     * @param value
     * @param ft
     */
    protected void putValue(BetterEnv e, String name, FValue value, FType ft) {
        e.putValueUnconditionally(name, value, ft);
    }

    /**
     * Put a value, perhaps unconditionally depending on subtype's choice
     */
    protected void putValue(BetterEnv e, String name, FValue value) {
        e.putValueUnconditionally(name, value);
    }

    public Voidoid forVarDecl(VarDecl x) {
        if (fields != null) {
            List<LValueBind> lhs = x.getLhs();
            for (LValueBind lvb : lhs) {
                Id name = lvb.getName();
                String s = NodeUtil.nameString(name);
                fields.add(s);
            }
        }
        return null;
    }

    protected void forFnDef3(FnDef x) {
        List<StaticParam> staticParams = x.getStaticParams();
        String fname = NodeUtil.nameAsMethod(x);

        if (!staticParams.isEmpty()) {
            // GENERIC
            // This blows up because the type is not instantiated.
//            {
//                // Why isn't this the right thing to do?
//                // FGenericFunction is (currently) excluded from this treatment.
//                FValue fcn = containing.getValue(fname);
//
//                if (fcn instanceof OverloadedFunction) {
//                    OverloadedFunction og = (OverloadedFunction) fcn;
//                    og.finishInitializing();
//
//                }
//            }

        } else {
            // NOT GENERIC
            {
                Fcn fcn = (Fcn) containing.getValue(fname);

                if (fcn instanceof Closure) {
                    // This is only loosely paired with the
                    // first pass; dealing with overloading tends to
                    // break up the 1-1 relationship between the two.
                    // However, because of the way that scopes nest,
                    // it is possible (I think) that f could be overloaded
                    // in an inner scope but not overloaded in an outer
                    // scope.
                    Closure cl = (Closure) fcn;
                    cl.finishInitializing();
                } else if (fcn instanceof OverloadedFunction) {
                    OverloadedFunction og = (OverloadedFunction) fcn;
                    og.finishInitializing();

                }
            }
        }
   }


}
