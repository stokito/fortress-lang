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

import java.util.List;

import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;

public final class FreeNameCollector extends NodeDepthFirstVisitor<FreeNameCollection> {

	private FreeNameCollection result;
	private ObjectExpr thisObjExpr;

	public FreeNameCollector(ObjectExpr topObjExpr) {
		result = new FreeNameCollection();
		thisObjExpr = topObjExpr;
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

        // System.err.println("End of FreeNameCollector visit, returning obj: " + that);
        return result;
    }

	@Override
	public FreeNameCollection forVarRef(VarRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forFieldRef(FieldRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forFnRef(FnRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forOpRef(OpRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}


	@Override
	public FreeNameCollection forDimRef(DimRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forIntRef(IntRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forBoolRef(BoolRef that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	@Override
	public FreeNameCollection forVarType(VarType that) {
		if(isDeclaredInObjExpr(that) || isDecalredInSuperType(that) || isDeclareInTopLevel(that)) {
			return FreeNameCollection.EMPTY;
		}

		return result.add(that);
	}

	private boolean isDeclaredInObjExpr(Node that) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isDecalredInSuperType(Node that) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isDeclareInTopLevel(Node that) {
		// TODO Auto-generated method stub
		return false;
	}

}
