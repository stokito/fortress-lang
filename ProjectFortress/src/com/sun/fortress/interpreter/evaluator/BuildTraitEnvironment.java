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
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.PartiallyDefinedMethod;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.nodes.FnDefOrDecl;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.LValue;
import com.sun.fortress.interpreter.nodes.LValueBind;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.VarDecl;
import com.sun.fortress.interpreter.useful.Voidoid;


public class BuildTraitEnvironment extends BuildEnvironments {

    Set<String> fields;

    BetterEnv methodEnvironment;

    public BuildTraitEnvironment(BetterEnv within, BetterEnv methodEnvironment,
            Set<String> fields) {
        super(within);
        this.fields = fields;
        this.methodEnvironment = methodEnvironment;
    }

    protected Simple_fcn newClosure(BetterEnv e, com.sun.fortress.interpreter.nodes.Applicable x) {
        return new PartiallyDefinedMethod(containing, methodEnvironment, x,
                WellKnownNames.secretSelfName);
    }

    protected GenericMethod newGenericClosure(BetterEnv e, FnDefOrDecl x) {
        return new GenericMethod(containing, methodEnvironment, x,
                WellKnownNames.secretSelfName, true); // TODO need to get
                                                        // notself methods done
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
     *
     * @param e
     * @param name
     * @param value
     * @param ft
     */
    protected void putValue(BetterEnv e, String name, FValue value) {
        e.putValueUnconditionally(name, value);
    }

    public Voidoid forVarDecl(VarDecl x) {
        if (fields != null) {
            List<LValue> lhs = x.getLhs();
            for (LValue lv : lhs) {
                if (lv instanceof LValueBind) {
                    LValueBind lvb = (LValueBind) lv;

                    Id name = lvb.getName();
                    String s = name.getName();
                    fields.add(s);
                }
            }
        }
        return null;
    }

}
