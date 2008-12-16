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

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Contract;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

/**
 * Inserts inference type variables into the AST where explicit types were not
 * given. This change will be performed before typechecking, so that the
 * typechecker will always expect types to exist.
 */
public class InferenceVarInserter extends NodeUpdateVisitor {

	@Override
	public Node forLValueOnly(LValue that, Id name_result,
                                  Option<Type> type_result) {
		if( type_result.isNone() ) {
			Option<Type> new_type = Option.<Type>some(NodeFactory.make_InferenceVarType(NodeUtil.getSpan(that.getName())));
			return NodeFactory.makeLValue(NodeUtil.getSpan(that), name_result,
                                                      that.getMods(), new_type,
                                                      that.isMutable());
		}
		else {
			return that;
		}
	}

    @Override
    public Node forFnDecl(FnDecl that) {
        FnHeader header = that.getHeader();
        IdOrOpOrAnonymousName name_result = (IdOrOpOrAnonymousName) recur(header.getName());
        List<StaticParam> staticParams_result = recurOnListOfStaticParam(header.getStaticParams());
        Option<WhereClause> where_result = recurOnOptionOfWhereClause(header.getWhereClause());
        Option<List<BaseType>> throwsClause_result = recurOnOptionOfListOfBaseType(header.getThrowsClause());
        Option<Contract> contract_result = recurOnOptionOfContract(header.getContract());
        List<Param> params_result = recurOnListOfParam(header.getParams());
        Option<Type> returnType_result = recurOnOptionOfType(header.getReturnType());
        Id unambiguousName_result = (Id) recur(that.getUnambiguousName());
        Option<Expr> body_result = recurOnOptionOfExpr(that.getBody());
        Option<Id> implementsUnambiguousName_result = recurOnOptionOfId(that.getImplementsUnambiguousName());
        return  forFnDeclOnly(that, name_result, staticParams_result, params_result,
                              returnType_result, throwsClause_result,
                              where_result, contract_result, unambiguousName_result,
                              body_result, implementsUnambiguousName_result);
    }

    public Node forFnDeclOnly(FnDecl that,
                              IdOrOpOrAnonymousName name_result,
                              List<StaticParam> staticParams_result, List<Param> params_result,
                              Option<Type> returnType_result,
                              Option<List<BaseType>> throwsClause_result,
                              Option<WhereClause> where_result,
                              Option<Contract> contract_result,
                              Id unambiguousName_result,
                              Option<Expr> body_result,
                              Option<Id> implementsUnambiguousName_result) {
        // Is the return type given?
        Option<Type> new_ret_type =
            returnType_result.isNone() ?
            Option.<Type>some(NodeFactory.make_InferenceVarType(NodeUtil.getSpan(that))) :
            returnType_result;

        FnHeader header = (FnHeader)forFnHeaderOnly(that.getHeader(),
                                                    staticParams_result,
                                                    name_result,
                                                    where_result, throwsClause_result,
                                                    contract_result,
                                                    params_result, new_ret_type);
        return super.forFnDeclOnly(that, header, unambiguousName_result,
                                   body_result, implementsUnambiguousName_result);
    }

	@Override
	public Node forParamOnly(Param that, Id name_result,
                                 Option<Type> type_result,
                                 Option<Expr> defaultExpr_result,
                                 Option<Type> varargsType_result) {
            if ( ! NodeUtil.isVarargsParam(that) ) {
		// Is the type given?
		Option<Type> new_type =
			type_result.isNone() ?
					Option.<Type>some(NodeFactory.make_InferenceVarType(NodeUtil.getSpan(that))) :
						type_result;

		return super.forParamOnly(that, name_result, new_type,
                                          defaultExpr_result, varargsType_result);
            } else
                return that;
	}
}
