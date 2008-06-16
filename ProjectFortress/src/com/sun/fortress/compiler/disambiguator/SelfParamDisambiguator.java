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
package com.sun.fortress.compiler.disambiguator;

import java.util.List;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.compiler.typechecker.TypesUtil;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

/**
 * Replaces implicitly-typed 'self' parameters of methods with explicitly-typed
 * ones. This is now a separate visitor because it needs to occur during
 * disambiguation but before the TypeDisambiguator pass.
 * At parse-time, methods that take the 'self' parameter may not have a
 * type for that parameter. However, at disambiguation time, we can give
 * it one.<br> 
 * {@code trait Foo f(self) : () end}
 * becomes
 * {@code trait Foo f(self:Foo) : () end}
 * This method will only replace parameters "down" to the next
 * object or trait declaration. When then new trait or object is declared,
 * this method will be called again. This method is guaranteed to return
 * the type of node given.
 */
public class SelfParamDisambiguator extends NodeUpdateVisitor {

	private static final String SELF_NAME = "self";
	
	// TODO: Do I need to do the same thing for ObjectExpr? What if there is
	// no type? Do I use the traits that the object extends?
	
	@Override
	public Node forAbsObjectDecl(AbsObjectDecl that) {
    	// Add a type to self parameters of methods
		Type self_type = NodeFactory.makeTraitType(that.getName(),
        		                                   TypeEnv.staticParamsToArgs(that.getStaticParams()));
		AbsObjectDecl that_new = (AbsObjectDecl)this.replaceSelfParamsWithType(that, self_type);
		return super.forAbsObjectDecl(that_new);
	}

	@Override
	public Node forAbsTraitDecl(AbsTraitDecl that) {
    	// Add a type to self parameters of methods
		Type self_type = NodeFactory.makeTraitType(that.getName(),
        		                                   TypeEnv.staticParamsToArgs(that.getStaticParams()));
		AbsTraitDecl that_new = (AbsTraitDecl)this.replaceSelfParamsWithType(that, self_type);
		return super.forAbsTraitDecl(that_new);
	}

	@Override
	public Node forObjectDecl(ObjectDecl that) {
	     // Add a type to self parameters of methods
        Type self_type = NodeFactory.makeTraitType(that.getName(),
        		                                   TypeEnv.staticParamsToArgs(that.getStaticParams()));
    	ObjectDecl that_new = (ObjectDecl)this.replaceSelfParamsWithType(that, self_type);
		return super.forObjectDecl(that_new);
	}

	@Override
	public Node forTraitDecl(TraitDecl that) {
    	// Add a type to self parameters of methods
        Type self_type = NodeFactory.makeTraitType(that.getName(),
        		                                   TypeEnv.staticParamsToArgs(that.getStaticParams()));
    	TraitDecl that_new = (TraitDecl)this.replaceSelfParamsWithType(that, self_type);
		return super.forTraitDecl(that_new);
	}
	
	
	
    @Override
	public Node forObjectExpr(ObjectExpr that) {
    	// Add a type to self parameters of methods
    	Type self_type = TypesUtil.getObjectExprType(that);
    	ObjectExpr that_new = (ObjectExpr)this.replaceSelfParamsWithType(that, self_type);
    	return super.forObjectExpr(that_new);
	}

	/**
     * Replaces Parameters whose name is 'self' with a parameter with
     * the explicit type given. 
     * 
     * @param that
     * @param self_type
     */
    private Node replaceSelfParamsWithType(Node that, final Type self_type) {
    	
    	NodeUpdateVisitor replacer = new NodeUpdateVisitor() {
			
    		@Override
			public Node forNormalParamOnly(NormalParam that,
					List<Modifier> mods_result, Id name_result,
					Option<Type> type_result, Option<Expr> defaultExpr_result) {
    			// my type is broken I need to qualify the type name
    			Option<Type> new_type;
    			if( name_result.getText().equals(SELF_NAME) )
    				new_type = Option.some(self_type);
    			else
    				new_type = type_result;

    			return new NormalParam(that.getSpan(), 
			               that.getMods(),
			               that.getName(),
			               new_type,
			               that.getDefaultExpr());
			}
			// end recurrance here
			@Override public Node forObjectDecl(ObjectDecl that) { return that; }
			@Override public Node forTraitDecl(TraitDecl that) { return that; }
			@Override public Node forObjectExpr(ObjectExpr that) { return that; }
    	};
    	return that.accept(replacer);
    }
}
