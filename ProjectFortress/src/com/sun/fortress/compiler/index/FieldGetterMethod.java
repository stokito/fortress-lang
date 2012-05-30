/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class FieldGetterMethod extends FieldGetterOrSetterMethod {

    /** Create an implicit getter from a variable binding. */
    public FieldGetterMethod(Binding b, TraitObjectDecl traitDecl) {
        super(b, traitDecl);

        // If the Binding has a declared type, use it for the thunk.
        Option<TypeOrPattern> tp = _ast.getIdType();
        if (tp.isSome() && tp.unwrap() instanceof Type)
            _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(NodeUtil.optTypeOrPatternToType(tp)));
    }
    
    /** Create an explicit getter from a function. */
    public FieldGetterMethod(FnDecl f, TraitObjectDecl traitDecl) {
        super(f, makeBinding(f), traitDecl);

        // If the FnDecl has a declared return type, use it for the thunk.
        if (NodeUtil.getReturnType(f).isSome())
            _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(NodeUtil.getReturnType(f)));
    }

    /**
     * Copy another FieldGetterMethod, performing a substitution with the visitor.
     */
    private FieldGetterMethod(FieldGetterMethod that, List<StaticParam> params, StaticTypeReplacer visitor) {
        super(that, params, visitor);
    }

    /** Make a Binding for this getter from the given function. */
    private static Binding makeBinding(FnDecl f) {
        Modifiers mods = NodeUtil.getMods(f);
        return new LValue(f.getInfo(),
                          (Id) NodeUtil.getName(f),
                          mods,
                          NodeUtil.optTypeToTypeOrPattern(NodeUtil.getReturnType(f)),
                          mods.isMutable());
    }

    @Override
    public List<Param> parameters() {
        return Collections.emptyList();
    }

    @Override
    public FieldGetterMethod instantiateTraitStaticParameters(List<StaticParam> params, List<StaticArg> args) {
        return new FieldGetterMethod(this, params, new StaticTypeReplacer(_traitParams, args));
    }
    @Override
    public FieldGetterMethod instantiateTraitStaticParameters(List<StaticParam> params, StaticTypeReplacer str) {
        return new FieldGetterMethod(this, params, str);
    }

    @Override
    public boolean hasDeclaredReturnType() {
      return _ast.getIdType().isSome();
    }
}
