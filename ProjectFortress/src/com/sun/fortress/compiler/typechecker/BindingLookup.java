/*******************************************************************************
 Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker;

import static edu.rice.cs.plt.tuple.Option.some;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

/**
 * A wrapper around the binding found in the TypeEnv.  Since some bindings
 * do not have an Id to be indexed, there is no way to create the LValue
 * node to represent the binding.  In the case of operators, for example,
 * only a IdOrOpOrAnonymousName exists, so the BindingLookup exports the same methods
 * that LValue does, since an LValue cannot be created.
 */
public class BindingLookup {

    private final IdOrOpOrAnonymousName var;
    private final Option<Type> type;
    private final Modifiers mods;
    private final boolean mutable;

    public BindingLookup(LValue binding) {
        var = binding.getName();
        type = binding.getIdType();
        mods = binding.getMods();
        mutable = binding.isMutable();
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, FnDecl decl) {
        var = _var;
        type = Option.<Type>wrap(TypeEnv.genericArrowFromDecl(decl));
        mods = NodeUtil.getMods(decl);
        mutable = false;
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Collection<FnDecl> decls) {
        var = _var;
        List<Type> overloads = new ArrayList<Type>();
        Modifiers mods = Modifiers.None;
        for (FnDecl decl : decls) {
            overloads.add(TypeEnv.genericArrowFromDecl(decl));
        }
        this.mods = mods;
        type = Option.<Type>some(NodeFactory.makeIntersectionType(NodeFactory.makeSetSpan("impossible", overloads), overloads));
        mutable = false;
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Type _type) {
        this(_var, _type, Modifiers.None, false);
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type) {
        this(_var, _type, Modifiers.None, false);
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Type _type, Modifiers _mods) {
        this(_var, _type, _mods, false);
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type, Modifiers _mods) {
        this(_var, _type, _mods, false);
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Type _type, Modifiers _mods, boolean _mutable) {
        var = _var;
        type = some(_type);
        mods = _mods;
        mutable = _mutable;
    }

    public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type, Modifiers _mods, boolean _mutable) {
        var = _var;
        type = _type;
        mods = _mods;
        mutable = _mutable;
    }

    public IdOrOpOrAnonymousName getVar() { return var; }
    public Option<Type> getType() { return type; }
    public Modifiers getMods() { return mods; }

    public boolean isMutable() {
        if( mutable )
            return true;

        return mods.isMutable();
    }

    @Override
    public String toString() {
        return String.format("%s:%s", var, type);
    }

}