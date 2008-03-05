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
import java.util.ArrayList;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.VarAbsDeclOrDecl;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;


public class FTypeObject extends FTraitOrObject {

    // names of fields
    // including both the field declarations in the object declaration
    // and the non-transient value parameters of the object constructor
    List<Id> fields = new ArrayList<Id>();
    // names of methods
    // including coercions, getters, setters, operator methods,
    // functional methods, and dotted methods
    List<SimpleName> methods = new ArrayList<SimpleName>();

    public FTypeObject(String name, BetterEnv env, HasAt at,
                       Option<List<Param>> params,
                       List<? extends AbsDeclOrDecl> members, AbstractNode def) {
        super(name, env, at, members, def);
        for(AbsDeclOrDecl v : members) {
            if (v instanceof VarAbsDeclOrDecl) {
                for (LValueBind lhs : ((VarAbsDeclOrDecl)v).getLhs()) {
                    fields.add(lhs.getName());
                }
                if (params.isSome()) {
                    for (Param p : Option.unwrap(params)) {
                        if (!NodeUtil.isTransient(p))
                            fields.add(p.getName());
                    }
                }
            } else if (v instanceof FnAbsDeclOrDecl) {
                methods.add(((FnAbsDeclOrDecl)v).getName());
            }
        }
        cannotBeExtended = true;
    }

    @Override
    protected void finishInitializing() {
        // TODO Auto-generated method stub

    }

}
