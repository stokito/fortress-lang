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

package com.sun.fortress.interpreter.evaluator;

import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.TraitMethod;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes_util.Applicable;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Voidoid;


public class BuildTraitEnvironment extends BuildEnvironments {

    Set<String> fields;

    Environment methodEnvironment;

    FType definer;

    public BuildTraitEnvironment(Environment within, Environment methodEnvironment,
            FType definer, Set<String> fields) {
        super(within);
        this.definer = definer;
        this.fields = fields;
        this.methodEnvironment = methodEnvironment;
    }

    protected Simple_fcn newClosure(Environment e, Applicable x) {
        return new TraitMethod(containing, methodEnvironment, x, definer);
    }

    protected GenericMethod newGenericClosure(Environment e, FnAbsDeclOrDecl x) {
        return new GenericMethod(containing, methodEnvironment, x, definer, true);
    }

    /**
     * Put a value, perhaps unconditionally depending on subtype's choice
     *
     * @param e
     * @param name
     * @param value
     * @param ft
     */
    protected void putValue(Environment e, String name, FValue value, FType ft) {
        e.putValueRaw(name, value, ft);
    }

    /**
     * Put a value, perhaps unconditionally depending on subtype's choice
     */
    protected void putValue(Environment e, String name, FValue value) {
        e.putValueRaw(name, value);
    }

    public Boolean forVarDecl(VarDecl x) {
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

    protected void forFnDecl3(FnDecl x) {
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
                Fcn fcn = (Fcn) containing.getLeafValue(fname);
                fcn.finishInitializing();
            }
        }
   }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnDecl(com.sun.fortress.interpreter.nodes.AbsFnDecl)
     */
    @Override
    public Boolean forAbsFnDecl(AbsFnDecl x) {
        // This is called from BuildTraitEnvironment
        switch (getPass()) {
        case 1: forAbsFnDecl1(x); break;
        case 2: forAbsFnDecl2(x); break;
        case 3: forAbsFnDecl3(x); break;
        case 4: forAbsFnDecl4(x); break;
        }
       return null;
    }
    private void forAbsFnDecl1(AbsFnDecl x) {


        List<StaticParam> optStaticParams = x.getStaticParams();
        String fname = NodeUtil.nameAsMethod(x);

        if (!optStaticParams.isEmpty()) {
            // GENERIC

                // TODO same treatment as regular functions.
                FValue cl = newGenericClosure(containing, x);
                // LINKER putOrOverloadOrShadowGeneric(x, containing, name, cl);
                bindInto.putValue(fname, cl); // was "shadow"


        } else {
            // NOT GENERIC

                Simple_fcn cl = newClosure(containing, x);
                // LINKER putOrOverloadOrShadow(x, containing, name, cl);
                bindInto.putValue(fname, cl); // was "shadow"

        }

    }
    private void forAbsFnDecl2(AbsFnDecl x) {
    }
    private void forAbsFnDecl3(AbsFnDecl x) {


        List<StaticParam> optStaticParams = x.getStaticParams();
        String fname = NodeUtil.nameAsMethod(x);

        if (!optStaticParams.isEmpty()) {
            // GENERIC


        } else {
            // NOT GENERIC

            Fcn fcn = (Fcn) containing.getLeafValue(fname);
            fcn.finishInitializing();
        }

    }
    private void forAbsFnDecl4(AbsFnDecl x) {
    }


}
