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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.ComponentWrapper;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.rewrite.Desugarer;
import com.sun.fortress.interpreter.rewrite.Desugarer.ArrowOrFunctional;
import com.sun.fortress.interpreter.rewrite.Desugarer.FunctionalMethod;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.AbsVarDecl;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Voidoid;

public class BuildApiEnvironment extends BuildTopLevelEnvironments {
    
    @Override
    public Voidoid forAbsFnDecl(AbsFnDecl x) {
        // super.forAbsFnDecl(x);
        if (getPass() == 1) {
            // TODO the value obtained should be filtered.
            IdOrOpOrAnonymousName id = x.getName();
            String fname = id.stringName();
            FValue fv = exporter.getEnvironment().getValueRaw(fname);
            if (fv != null) {
                bindInto.putValueRaw(fname, fv);
            } else {
                exporter.missingExportedVars.put(fname);
            }
            valNames.add(fname);
            overloadNames.add(fname);
            exporter.overloadableExportedFunction.add(fname);
            api.overloadableExportedFunction.add(fname);
        }
        return null;
    }

    @Override
    public Voidoid forAbsObjectDecl(AbsObjectDecl x) {
        // super.forAbsObjectDecl(x);
        if (getPass() == 1) {
            String fname = NodeUtil.stringName(x.getName());
            valNames.add(fname);
            typeNames.add(fname);
            FValue fv = exporter.getEnvironment().getValueRaw(fname);
            FType ft = exporter.getEnvironment().getTypeNull(fname);
            
            // This is overloadable if the object is NOT a singleton.
            if (x.getParams().isSome()) {
                overloadNames.add(fname);
                exporter.overloadableExportedFunction.add(fname);             
                api.overloadableExportedFunction.add(fname);             
            }
            
            if (fv != null) {
                bindInto.putValueRaw(fname, fv);
            } else {
                exporter.missingExportedVars.put(fname);
            }
            if (ft != null) {
                bindInto.putType(fname, ft);
            } else {
                exporter.missingExportedTypes.put(fname);
            }
            handlePossibleFM(x.getDecls());
            
        }
        return null;
    }

    @Override
    public Voidoid forAbsTraitDecl(AbsTraitDecl x) {
        // super.forAbsTraitDecl(x);
        if (getPass() == 1) {
            String fname = NodeUtil.stringName(x.getName());
            typeNames.add(fname);
            FType ft = exporter.getEnvironment().getTypeNull(fname);
            if (ft != null) {
                bindInto.putType(fname, ft);
            } else {
                exporter.missingExportedTypes.put(fname);
            }
            handlePossibleFM(x.getDecls());
        }
        return null;
    }

    @Override
    public Voidoid forAbsVarDecl(AbsVarDecl x) {
        // super.forAbsVarDecl(x);
        
        if (getPass() == 1) {
            List<LValueBind> lhs = x.getLhs();
            LValueBind lvb = lhs.get(0); // Have desugared to single vars
            Id name = lvb.getName();
            String sname = NodeUtil.stringName(name);
            valNames.add(sname);
            FValue fv = exporter.getEnvironment().getValueRaw(sname);

            if (fv != null) {
                bindInto.putValueRaw(sname, fv);
            } else {
                exporter.missingExportedVars.put(sname);
            }
        }
        return null;
    }

    ComponentWrapper exporter;
    ComponentWrapper api;
    
    @Override
    public void setExporterAndApi(ComponentWrapper exporter, ComponentWrapper api) {
        this.exporter = exporter;
        this.api = api;
    }
    
    public BuildApiEnvironment(Environment within,
            HashMap<String, ComponentWrapper> linker) {
        super(within, linker);
        // TODO Auto-generated constructor stub
    }
    
    private void handlePossibleFM(List<? extends AbsDeclOrDecl> tdecls) {
        for (AbsDeclOrDecl adod : tdecls) {
            Desugarer.ArrowOrFunctional aof = adod
                    .accept(Desugarer.isAnArrowName);
            if (aof == Desugarer.FUNCTIONAL) {
                // Only certain things can be a functional method.
                FnAbsDeclOrDecl fadod = (FnAbsDeclOrDecl) adod;
                String s = fadod.getName().stringName();
                overloadNames.add(s);
                exporter.overloadableExportedFunction.add(s);
                api.overloadableExportedFunction.add(s);
                FValue fv = exporter.getEnvironment().getValueRaw(s);

                if (fv != null) {
                    bindInto.putValueRaw(s, fv);
                } else {
                    exporter.missingExportedVars.put(s);
                }
            }

        }
    }

    
}
