/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;

import java.util.List;
import java.util.Set;


public class BuildTraitEnvironment extends BuildEnvironments {

    Set<String> fields;

    Environment methodEnvironment;

    FType definer;

    public BuildTraitEnvironment(Environment within, Environment methodEnvironment, FType definer, Set<String> fields) {
        super(within);
        this.definer = definer;
        this.fields = fields;
        this.methodEnvironment = methodEnvironment;
    }

    protected Simple_fcn newClosure(Environment e, Applicable x) {
        return new TraitMethod(containing, methodEnvironment, x, definer);
    }

    protected GenericMethod newGenericClosure(Environment e, FnDecl x) {
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
            List<LValue> lhs = x.getLhs();
            for (LValue lvb : lhs) {
                Id name = lvb.getName();
                String s = NodeUtil.nameString(name);
                fields.add(s);
            }
        }
        return Boolean.valueOf(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnDecl(com.sun.fortress.interpreter.nodes.FnDecl)
     */
    @Override
    public Boolean forFnDecl(FnDecl x) {
        // This is called from BuildTraitEnvironment
        switch (getPass()) {
            case 1:
                forFnDecl1(x);
                break;
            case 2:
                forFnDecl2(x);
                break;
            case 3:
                forFnDecl3(x);
                break;
            case 4:
                forFnDecl4(x);
                break;
        }
        return Boolean.valueOf(false);
    }

    private void forFnDecl1(FnDecl x) {
        List<StaticParam> optStaticParams = NodeUtil.getStaticParams(x);
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

    private void forFnDecl2(FnDecl x) {
    }

    protected void forFnDecl3(FnDecl x) {
        List<StaticParam> staticParams = NodeUtil.getStaticParams(x);
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

    private void forFnDecl4(FnDecl x) {
    }


}
