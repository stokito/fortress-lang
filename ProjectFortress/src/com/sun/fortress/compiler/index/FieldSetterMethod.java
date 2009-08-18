/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.compiler.typechecker.TypesUtil;
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

public class FieldSetterMethod extends Method {

    private final Binding _ast;
    private final Param _param;
    private final Id _declaringTrait;
    private final Option<Type> _selfType;

    public FieldSetterMethod(Binding ast, TraitObjectDecl traitDecl) {
        this(ast,
             NodeFactory.makeParam(NodeUtil.getSpan(ast),
                                   Modifiers.None,
                                   NodeFactory.makeId(NodeUtil.getSpan(ast), "fakeParamForImplicitSetter"),
                                   ast.getIdType()),
             traitDecl);
    }

    public FieldSetterMethod(Binding ast, Param param, TraitObjectDecl traitDecl) {
        _ast = ast;
        _param = param;
        _declaringTrait = NodeUtil.getName(traitDecl);
        _selfType = traitDecl.getSelfType();

        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(Option.<Type>some(Types.VOID)));
    }

    /**
     * Copy another FieldSetterMethod, performing a substitution with the visitor.
     */
    public FieldSetterMethod(FieldSetterMethod that, NodeUpdateVisitor visitor) {
        _ast = (Binding) that._ast.accept(visitor);
        _param = that._param;
        _declaringTrait = that._declaringTrait;
        _selfType = visitor.recurOnOptionOfType(that._selfType);

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public Binding ast() {
        return _ast;
    }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_ast);
    }

    @Override
    public Option<Expr> body() {
        return Option.none();
    }

    @Override
    public List<Param> parameters() {
        return Collections.singletonList(_param);
    }

    @Override
    public List<StaticParam> staticParameters() {
        return Collections.emptyList();
    }

    @Override
    public List<BaseType> thrownTypes() {
        return Collections.emptyList();
    }

    @Override
    public Method instantiate(List<StaticParam> params, List<StaticArg> args) {
        StaticTypeReplacer replacer = new StaticTypeReplacer(params, args);
        return new FieldSetterMethod(this, replacer);
    }

    public Id declaringTrait() {
        return this._declaringTrait;
    }

    public Option<Type> selfType() {
        return _selfType;
    }

    @Override
    public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
        return new FieldSetterMethod(this, visitor);
    }

    @Override
    public Id name() {
        return _ast.getName();
    }

}
