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

package com.sun.fortress.compiler.index;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.GetterSetter;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.tuple.Option;

public class FieldGetterMethod extends Method {

    private final GetterSetter _ast;
    private final Id _declaringTrait;

    public FieldGetterMethod(GetterSetter ast, Id declaringTrait) {
        _ast = ast;
        _declaringTrait = declaringTrait;
    }

    public GetterSetter ast() { return _ast; }

	@Override
	public Option<Expr> body() {
		return Option.none();
	}

	@Override
	public List<Param> parameters() {
		return Collections.emptyList();
	}

	@Override
	public List<StaticParam> staticParameters() {
		return Collections.emptyList();
	}

	@Override
	public Iterable<BaseType> thrownTypes() {
		return Collections.emptyList();
	}

	@Override
	public Functional instantiate(List<StaticParam> params, List<StaticArg> args) {
		return this;
	}

	@Override
	public Type getReturnType() {
		return _ast.getType().unwrap();
	}

	@Override
	public Id getDeclaringTrait() {
		return this._declaringTrait;
	}

	@Override
	public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
		return new FieldGetterMethod((GetterSetter)this._ast.accept(visitor), this._declaringTrait);
	}


}
