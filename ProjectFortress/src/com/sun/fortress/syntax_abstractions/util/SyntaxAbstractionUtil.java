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

package com.sun.fortress.syntax_abstractions.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

public class SyntaxAbstractionUtil {

	public static final String FORTRESSAST = "FortressAst";
	public static final String STRINGLITERALEXPR = "StringLiteralExpr";
	public static final String STRINGLITERAL = "StringLiteral";
	public static final String FORTRESSLIBRARY = "FortressLibrary";
	public static final String JUST = "Just";
	public static final String NOTHING = "Nothing";
	public static final String STRING = "String";

	/**
	 * Returns a qualified id name where the grammar name is added to the api.
	 * E.g. api: Foo.Bar, grammar name Baz, and production Gnu gives 
	 * APIName: Foo.Bar.Baz and id: Gnu.
	 */
	public static QualifiedIdName qualifyProductionName(APIName api, Id grammarName, Id productionName) {
		Collection<Id> names = new LinkedList<Id>();
		names.addAll(api.getIds());
		names.add(grammarName);
		APIName apiGrammar = NodeFactory.makeAPIName(names);
		return NodeFactory.makeQualifiedIdName(apiGrammar, productionName);
	}
	
	/**
	 * Create a Java representation of a Fortress AST which when evaluated
	 * instantiates a new object of the given name from the given api, with the given option
	 * as argument.
	 * @param span
	 * @param apiName
	 * @param objectName
	 * @param arg
	 * @return
	 */
	public static Expr makeObjectInstantiation(Span span, String apiName, String objectName, Option<Expr> arg) {
		List<Expr> exprs = new LinkedList<Expr>();
		List<Id> ids = new LinkedList<Id>();
		ids.add(NodeFactory.makeId(span, apiName));
		APIName api = NodeFactory.makeAPIName(span, ids);
		Id typeName = NodeFactory.makeId(span, objectName);
		QualifiedIdName name = NodeFactory.makeQualifiedIdName(span, api, typeName);
		exprs.add(NodeFactory.makeFnRef(span, name));
		if (arg.isSome()) {
			exprs.add(Option.unwrap(arg));
		}
		return NodeFactory.makeTightJuxt(span, exprs);
	}
	
	public static Expr makeObjectInstantiation(Span span, String apiName, String objectName, Option<Expr> arg, List<StaticArg> staticArgs) {
		List<Expr> exprs = new LinkedList<Expr>();
		List<Id> ids = new LinkedList<Id>();
		ids.add(NodeFactory.makeId(span, apiName));
		APIName api = NodeFactory.makeAPIName(span, ids);
		Id typeName = NodeFactory.makeId(span, objectName);
		QualifiedIdName name = NodeFactory.makeQualifiedIdName(span, api, typeName);
		exprs.add(NodeFactory.makeFnRef(span, name, staticArgs));
		if (arg.isSome()) {
			exprs.add(Option.unwrap(arg));
		}
		return NodeFactory.makeTightJuxt(span, exprs);
	}

}
