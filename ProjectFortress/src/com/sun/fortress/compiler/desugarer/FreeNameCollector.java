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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.compiler.typechecker.TypeEnv.BindingLookup;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public final class FreeNameCollector extends NodeDepthFirstVisitor<FreeNameCollection> {

	private FreeNameCollection result;
	private ObjectExpr thisObjExpr;
	// TODO: Maybe we do want to use the Span as a key for this??
	private Map<Pair<Node,Span>, TypeEnv> in_typeEnvAtNode;
	private Map<Span, TypeEnv> typeEnvAtNode;
	private Stack<Node> scopeStack;
	
	private static final int DEBUG_LEVEL = 1;
	
	// This class is used to collect names captured by object expression.
	// The assumption is that, the top node accepting this visitor is always
	// type ObjectExpr.
	// A reference is considered free if it is not declared within thisObjExpr, 
	// not inherited by thisObjExpr, AND not declared at top level environment.
	public FreeNameCollector(Stack<Node> scopeStack, Map<Pair<Node,Span>, TypeEnv> typeEnvAtNode) {
		result = new FreeNameCollection();
		thisObjExpr = (ObjectExpr) scopeStack.peek();  
		this.in_typeEnvAtNode = typeEnvAtNode;
		
		this.typeEnvAtNode = new HashMap<Span,TypeEnv>();
		for(Pair<Node, Span> n : in_typeEnvAtNode.keySet()) { // FIXME: Temp hack to use Map<Span,TypeEnv>
			this.typeEnvAtNode.put(n.second(), in_typeEnvAtNode.get(n));
		}
		this.scopeStack = scopeStack;
	}

	public FreeNameCollection getResult() {
		return result;
	}

	@Override
	public FreeNameCollection defaultCase(Node that) {
		return FreeNameCollection.EMPTY;
	}

	@Override
    public FreeNameCollection forObjectExpr(ObjectExpr that) {
		// TODO: if(thisObjExpr.equals(that) && thisObjExpr.getSpan().equals(that.getSpan()))
		// need to do something different if it's an ObjectExpr nested inside another ObjectExpr
		// System.err.println("In FreeNameCollector, obj: " + that);

        List<FreeNameCollection> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
        List<FreeNameCollection> decls_result = recurOnListOfDecl(that.getDecls());

        for(FreeNameCollection c : extendsClause_result) {
        	result = result.composeResult(c);
        }
        for(FreeNameCollection c : decls_result) {
        	result = result.composeResult(c);
        }

        return super.forObjectExprOnly(that, extendsClause_result, decls_result);
    }

	@Override
	public FreeNameCollection forVarRef(VarRef that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getVar()) || isDeclareInTopLevel(that.getVar())) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forFieldRef(FieldRef that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getField()) || isDeclareInTopLevel(that.getField())) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forFnRef(FnRef that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getOriginalName()) || isDeclareInTopLevel(that.getOriginalName())) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

//	@Override
//	public FreeNameCollection forOpRef(OpRef that) {
//		if(isDeclaredInObjExpr(that.) || isDeclareInTopLevel(that)) {
//			return FreeNameCollection.EMPTY;
//		}
//
//		return result.add(that);
//	}
//
//
	
	@Override
	public FreeNameCollection forDimRef(DimRef that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getName()) || isDeclareInTopLevel(that.getName())) {
			// FIXME: I put this in, but the TypeEnv doesn't actually contain DimRef
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forIntRef(IntRef that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getName()) || isDeclareInTopLevel(that.getName())) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forBoolRef(BoolRef that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getName()) || isDeclareInTopLevel(that.getName())) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forVarType(VarType that) {
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "FreeNameCollector visiting ", that);
		if(isDeclaredInObjExpr(that.getName()) || isDeclareInTopLevel(that.getName())) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	private boolean isDeclaredInObjExpr(Id id) { 
		TypeEnv objExprTypeEnv = 
			typeEnvAtNode.get(thisObjExpr.getSpan());  // FIXME: change if going back to Pair<Node,Span> key
		Option<BindingLookup> binding = objExprTypeEnv.binding(id);

		// The typeEnv are things visible outside of object expression
		// Since we have already passed type checking, we don't need to worry 
		// about undefined variable.  If the binding is not found, the reference
		// must be declared / inherited within the object expression.
		if(binding.isNone()) {
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, 
					    id, " is declared in ", thisObjExpr);
			return true;  
		}

		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, 
			        id, " is NOT declared in ", thisObjExpr);
		return false;
	}

	private boolean isDeclareInTopLevel(Id id) {
		// The first node in this stack must be declared at a top level
		// Its corresonding environment must be the top level environment
		Node topLevelNode = scopeStack.get(0);  
		
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "top level node is: ", topLevelNode);
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "its span is: ", topLevelNode.getSpan());
		
		// FIXME: change if going back to Pair<Node,Span> key
		TypeEnv topLevelEnv = typeEnvAtNode.get(topLevelNode.getSpan());

		if(topLevelEnv == null) {
			DebugTypeEnvAtNode();
		}
		
		Option<BindingLookup> binding = topLevelEnv.binding(id);
		if(binding.isNone()) { 
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, 
			        id, " is NOT declared in top level env.");
			return false;
		}
		
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, 
		        id, " is declared in top level env.");
		return true;
	}
	
	private void DebugTypeEnvAtNode() {
		int i = 0;
		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "Debuggging typeEnvAtNode ... ");
		for(Pair<Node,Span> n : in_typeEnvAtNode.keySet()) {
			i++;
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "key ", i, ": ", n.first());
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "\t Its span is ", n.second());
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "\t Are they equal? ", n.first().getSpan().equals(n.second()));
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, "\t It's env contains: ", typeEnvAtNode.get(n));
		}
	}

}
