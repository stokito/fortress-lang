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

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.phases.NonterminalTypeDictionary;

import edu.rice.cs.plt.tuple.Option;

public class TypeCollector extends NodeDepthFirstVisitor<Option<Type>> {

	private TypeCollector() {}

	public static Option<Type> getType(PrefixedSymbol ps) {
		return ps.getSymbol().accept(new TypeCollector());
	}

	@Override
	public Option<Type> defaultCase(Node that) {
		throw new RuntimeException("Unexpected case: "+that.getClass());
	}

	@Override
	public Option<Type> forOptionalSymbol(OptionalSymbol that) {
		return handle(that.getSymbol(), SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.MAYBE);
	}

	@Override
	public Option<Type> forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
		return handle(that.getSymbol(), SyntaxAbstractionUtil.LIST, SyntaxAbstractionUtil.LIST);
	}

	@Override
	public Option<Type> forRepeatSymbol(RepeatSymbol that) {
		return handle(that.getSymbol(), SyntaxAbstractionUtil.LIST, SyntaxAbstractionUtil.LIST);
	}

	@Override 
	public Option<Type> forNonterminalSymbol(NonterminalSymbol that) {
		return NonterminalTypeDictionary.getType(that.getNonterminal().getText());
	}

	@Override
	public Option<Type> forKeywordSymbol(KeywordSymbol that) {
		QualifiedIdName string = NodeFactory.makeQualifiedIdName(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
		return Option.<Type>some(new IdType(string));
	}

	@Override
	public Option<Type> forTokenSymbol(TokenSymbol that) {
		QualifiedIdName string = NodeFactory.makeQualifiedIdName(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
		return Option.<Type>some(new IdType(string));
	}

	private Option<Type> handle(SyntaxSymbol symbol, String api, String id) {
		Option<Type> t = symbol.accept(this);
		if (t.isNone()) {
			return t;
		}
		Type type = Option.unwrap(t);
		QualifiedIdName list = NodeFactory.makeQualifiedIdName(api, id);
		List<StaticArg> args = new LinkedList<StaticArg>();
		args.add(new TypeArg(type));
		return Option.<Type>some(new InstantiatedType(list, args));
	}

}
