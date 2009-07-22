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

import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;

/**
 * Note that this is a {@link Function}, not a {@link Method}, despite the name
 * (methods have distinct receivers).
 */
public class FunctionalMethod extends Function {
    protected final FnDecl _ast;
    protected final Id _declaringTrait;
    protected final List<StaticParam> _traitParams;

    public FunctionalMethod(FnDecl ast, Id declaringTrait, List<StaticParam> traitParams) {
        _ast = ast;
        _declaringTrait = declaringTrait;
        _traitParams = CollectUtil.makeList(IterUtil.map(traitParams, liftStaticParam));
        if (NodeUtil.getReturnType(_ast).isSome())
            putThunk(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    /**
     * Copy another FunctionalMethod, performing a substitution with the visitor.
     */
    public FunctionalMethod(FunctionalMethod that, NodeUpdateVisitor visitor) {
        _ast = (FnDecl)that._ast.accept(visitor);
        _declaringTrait = that._declaringTrait;
        _traitParams = visitor.recurOnListOfStaticParam(that._traitParams);

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public FnDecl ast() { return _ast; }

    @Override
    public Span getSpan() { return NodeUtil.getSpan(_ast); }

    public IdOrOpOrAnonymousName toUndecoratedName() {
        return ast().getHeader().getName();
    }
    
    @Override
    public IdOrOp name() {
        // Functional methods cannot have anonymous names.
        return (IdOrOp) NodeUtil.getName(_ast);
    }
    
    public Id declaringTrait() { return _declaringTrait; }

	@Override
	public Option<Expr> body() {
		return _ast.accept(new NodeDepthFirstVisitor<Option<Expr>>(){
			@Override
			public Option<Expr> defaultCase(Node that) {
				return Option.none();
			}
			@Override
			public Option<Expr> forFnDecl(FnDecl that) {
                            return that.getBody();
			}
		});
	}


	@Override
	public List<Param> parameters() {
		return NodeUtil.getParams(_ast);
	}

    /**
     * Get the static parameters of this method prepended with the declaring
     * trait's static parameters.
     */
	@Override
	public List<StaticParam> staticParameters() {
		return CollectUtil.makeList(IterUtil.compose(_traitParams, NodeUtil.getStaticParams(_ast)));
	}

    /** Get only the explicitly declared static parameters for this method. */
    public List<StaticParam> declaredStaticParameters() {
        return NodeUtil.getStaticParams(_ast);
    }

	@Override
	public List<BaseType> thrownTypes() {
		if(  NodeUtil.getThrowsClause(_ast).isNone() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(NodeUtil.getThrowsClause(_ast).unwrap());
	}

	@Override
	public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
		return new FunctionalMethod(this, visitor);
	}

    @Override
    public boolean hasDeclaredReturnType() {
        return NodeUtil.getReturnType(_ast).isSome();
    }

    @Override
    public String toString() {
        return String.format("%s.%s",
                             _declaringTrait.getText(),
                             super.toString());
    }
}
