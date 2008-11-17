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
package com.sun.fortress.compiler.typechecker;

import java.util.List;

import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Contract;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

/**
 * Inserts inference type variables into the AST where explicit types were not
 * given. This change will be performed before typechecking, so that the
 * typechecker will always expect types to exist.
 */
public class InferenceVarInserter extends NodeUpdateVisitor {

	@Override
	public Node forLValueBindOnly(LValueBind that, Id name_result,
			Option<Type> type_result, List<Modifier> mods_result) {
		if( type_result.isNone() ) {
			Option<Type> new_type = Option.<Type>some(NodeFactory.make_InferenceVarType(that.getName().getSpan()));
			return new LValueBind(that.getSpan(),name_result,new_type,mods_result,that.isMutable());
		}
		else {
			return that;
		}
	}





    @Override
    public Node forAbsFnDeclOnly(AbsFnDecl that, List<Modifier> mods_result,
            IdOrOpOrAnonymousName name_result,
            List<StaticParam> staticParams_result, List<Param> params_result,
            Option<Type> returnType_result,
            Option<List<BaseType>> throwsClause_result,
            Option<WhereClause> where_result, Option<Contract> contract_result, Id unambiguousName_result) {
        // Is return type given?
        // This could be an abstract method in a trait

        Option<Type> new_ret_type =
            returnType_result.isNone() ?
                    Option.<Type>some(NodeFactory.make_InferenceVarType(that.getSpan())) :
                    returnType_result;

        return super.forAbsFnDeclOnly(that, mods_result, name_result,
                staticParams_result, params_result, new_ret_type,
                throwsClause_result, where_result, contract_result, unambiguousName_result);
    }





    @Override
	public Node forFnDefOnly(FnDef that, List<Modifier> mods_result,
			IdOrOpOrAnonymousName name_result,
			List<StaticParam> staticParams_result, List<Param> params_result,
			Option<Type> returnType_result,
			Option<List<BaseType>> throwsClause_result,
			Option<WhereClause> where_result, Option<Contract> contract_result, Id unambiguousName_result, Expr body_result,
                        Option<Id> implementsUnambiguousName_result) {
		// Is the return type given?
		Option<Type> new_ret_type =
			returnType_result.isNone() ?
					Option.<Type>some(NodeFactory.make_InferenceVarType(that.getSpan())) :
					returnType_result;

		return super.forFnDefOnly(that, mods_result, name_result, staticParams_result,
				params_result, new_ret_type, throwsClause_result, where_result,
				contract_result, unambiguousName_result, body_result, implementsUnambiguousName_result);
	}

	@Override
	public Node forNormalParamOnly(NormalParam that,
			List<Modifier> mods_result, Id name_result,
			Option<Type> type_result, Option<Expr> defaultExpr_result) {
		// Is the type given?
		Option<Type> new_type =
			type_result.isNone() ?
					Option.<Type>some(NodeFactory.make_InferenceVarType(that.getSpan())) :
						type_result;

		return super.forNormalParamOnly(that, mods_result, name_result, new_type,
				defaultExpr_result);
	}
}
