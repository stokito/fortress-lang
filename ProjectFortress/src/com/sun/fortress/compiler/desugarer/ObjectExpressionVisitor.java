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

package com.sun.fortress.compiler.desugarer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.GenericDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;

public class ObjectExpressionVisitor extends NodeUpdateVisitor {
	private List<ObjectDecl> liftedObjectExprs;
	private Component enclosingComponent;
	private int uniqueId;

	public ObjectExpressionVisitor() {
		uniqueId = 0;
	}

	@Override
	public Node forComponentOnly(Component that, APIName name_result,
			                     List<Import> imports_result, List<Export> exports_result,
			                     List<Decl> decls_result) {
		if(liftedObjectExprs != null) {
			decls_result.addAll(liftedObjectExprs);
		}
		return super.forComponentOnly(that, name_result, imports_result, exports_result, decls_result);
	}

	@Override
	public Node forComponent(Component that) {
		enclosingComponent = that;
		Node returnValue = super.forComponent(that);
		enclosingComponent = null;
		return returnValue;
	}

	@Override
    public Node forObjectExpr(ObjectExpr that) {
		FreeNameCollection freeNames = that.accept(new FreeNameCollector(that));
		FreeNameCollection.printDebug(freeNames);

		ObjectDecl lifted = liftObjectExpr(that, freeNames);  
		if(liftedObjectExprs == null) {
			liftedObjectExprs = new LinkedList<ObjectDecl>();
		}
		liftedObjectExprs.add(lifted);

		TightJuxt callToLifted = makeCallToLiftedObj(lifted, that, freeNames);
		
        return callToLifted;
    }

	private TightJuxt makeCallToLiftedObj(ObjectDecl lifted, ObjectExpr objExpr, FreeNameCollection freeNames) {
		Span span = objExpr.getSpan();
		Id originalName = lifted.getName();
		List<Id> fns = new LinkedList<Id>();
		fns.add(originalName);
		// TODO: Need to figure out what Static params are captured.
		List<StaticArg> staticArgs = Collections.<StaticArg>emptyList(); 
		
		/* Now make the call to construct the lifted object */
		/* Use default value for parenthesized */
		FnRef fnRef = new FnRef(span, originalName, fns, staticArgs);
		VoidLiteralExpr voidLit = new VoidLiteralExpr(span);  /* TODO: this is only if there is no param */
		List<Expr> exprs = new LinkedList<Expr>();
		exprs.add(fnRef);
		exprs.add(voidLit);
		TightJuxt callToConstructor = new TightJuxt(span, objExpr.isParenthesized(), exprs);
		
		return callToConstructor;
	}

	private ObjectDecl liftObjectExpr(ObjectExpr target, FreeNameCollection freeNames) {
		String name = getMangledName(target);
		Span span = target.getSpan();
		Id id = new Id(Option.some(enclosingComponent.getName()), name);
		List<StaticParam> staticParams = getCapturedStaticParams(freeNames);
		List<TraitTypeWhere> extendsClauses = target.getExtendsClause();
		Option<List<Param>> params = getCapturedVars(freeNames);
        List<Decl> decls = target.getDecls();   // FIXME: need to rewrite all decls w/ the freeNames.

        /* Use default value for modifiers, where clauses, throw clauses, contract */
		ObjectDecl lifted = new ObjectDecl(span, id, staticParams, extendsClauses, params, decls);

		return lifted;
	}

	private Option<List<Param>> getCapturedVars(FreeNameCollection freeNames) {
		// TODO: Fill this in - this is more complicated;
		// need to figure out shadowed self via FnRef, FieldRef ... and so on
		// need to box any var that's mutable
		return Option.<List<Param>>some(Collections.<Param>emptyList());
	}

	private List<StaticParam> getCapturedStaticParams(FreeNameCollection freeNames) {
		// TODO: Fill this in - get the VarTypes(?) that's free and generate static param using it
        return Collections.<StaticParam>emptyList();
	}

	private String getMangledName(ObjectExpr target) {
		String componentName = NodeUtil.nameString(enclosingComponent.getName());
		String mangled = componentName + "$" + nextUniqueId();
		return mangled;
	}

	private int nextUniqueId() {
		return uniqueId++;
	}

//	private ObjectDecl createLiftedObjectExpr(ObjectExpr that) {
//	Span span = that.getSpan();
//	Id newId = null;
//	List<TraitTypeWhere> extendsClause = that.getExtendsClause();
//	List<Param> newParameters = new LinkedList<Param>();
//	String enclosedName = null;
//
//	// Set up the enclosing type as a constructor parameter if it exists
//	if (enclosingType != null) {
//		enclosedName = "enclosed$" + enclosedCounter;
//		APIName apiName = enclosingComponent.getName();
//		Type newType = NodeFactory.makeTraitType(name);
//		Param enclosed = new NormalParam(span, new Id(enclosedName), Option.some(newType));
//
//	}
//
//	if (enclosingComponent != null) {
//		APIName apiName = enclosingComponent.getName();
//        String apiNameString = NodeUtil.nameString(apiName);
//        String newName = apiNameString + "$" + span.begin.getLine() + ":" + span.begin.column();
//        Debug.debug(Debug.Type.COMPILER, 1, newName);
//        newId = new Id(newName);
//	} else {
//
//	}
//
//	// Now ensure that all enclosed names are unique
//	if (enclosingType != null) {
//		enclosedCounter++;
//	}
//
//	return null;
//}


//	@Override
//    public Node forTraitDecl(TraitDecl that) {
//    	enclosingType = that;
//    	Node returnValue = super.forTraitDecl(that);
//    	enclosingType = null;
//    	return returnValue;
//    }
//
//	@Override
//    public Node forObjectDecl(ObjectDecl that) {
//    	enclosingType = that;
//    	Node returnValue = super.forObjectDecl(that);
//    	enclosingType = null;
//    	return returnValue;
//    }

}
