/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class FieldSetterMethod extends FieldGetterOrSetterMethod {

    private final Param _param;

    public FieldSetterMethod(Binding b, TraitObjectDecl traitDecl) {
        super(b, traitDecl);
        _param = NodeFactory.makeParam(
            NodeUtil.getSpan(b),
            Modifiers.None,
            NodeFactory.makeId(NodeUtil.getSpan(b), "fakeParamForImplicitSetter"),
            b.getIdType());

        // Return type is always VOID.
        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(Option.<Type>some(Types.VOID)));
    }

    /** Create an implicit setter from a variable binding. */
    public FieldSetterMethod(FnDecl f, TraitObjectDecl traitDecl) {
        super(f, makeBinding(f), traitDecl);
        _param = NodeUtil.getParams(f).get(0);

        // Return type is always VOID.
        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(Option.<Type>some(Types.VOID)));
    }

    /**
     * Copy another FieldSetterMethod, performing a substitution with the visitor.
     */
    private FieldSetterMethod(FieldSetterMethod that, List<StaticParam> params, StaticTypeReplacer visitor) {
        super(that, params, visitor);
        _param = (Param) that._param.accept(visitor);
    }
    
    /** Make a Binding for this setter from the given function. */
    private static Binding makeBinding(FnDecl f) {
        Param p = NodeUtil.getParams(f).get(0);
        Modifiers mods = NodeUtil.getMods(f);
        return new LValue(f.getInfo(),
                          (Id) NodeUtil.getName(f),
                          mods,
                          p.getIdType(),
                          mods.isMutable());
    }

    @Override
    public List<Param> parameters() {
        return Collections.singletonList(_param);
    }

    @Override
    public FieldSetterMethod instantiateTraitStaticParameters(List<StaticParam> params, List<StaticArg> args) {
        return new FieldSetterMethod(this, params, new StaticTypeReplacer(_traitParams, args));
    }

    @Override
    public FieldSetterMethod instantiateTraitStaticParameters(List<StaticParam> params, StaticTypeReplacer str) {
        return new FieldSetterMethod(this, params, str);
    }
    
}
