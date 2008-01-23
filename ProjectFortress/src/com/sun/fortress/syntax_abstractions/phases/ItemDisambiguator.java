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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import com.sun.fortress.compiler.ProductionDisambiguator;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.compiler.typechecker.TypeCheckerResult;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ItemSymbol;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;


public class ItemDisambiguator extends NodeUpdateVisitor {

	public static class Result extends StaticPhaseResult {
		private Collection<SyntaxSymbol> symbols;

		public Result(Collection<SyntaxSymbol> symbols,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.symbols = symbols;
		}

		public Collection<SyntaxSymbol> modules() { return symbols; }
	}

	private Collection<StaticError> _errors;
	private ProductionEnv _currentEnv;

	public ItemDisambiguator(ProductionEnv currentEnv) {
		this._errors = new LinkedList<StaticError>();
		this._currentEnv = currentEnv;
	}

	private void error(String msg, HasAt loc) {
		this._errors.add(StaticError.make(msg, loc));
	}

	@Override
	public SyntaxSymbol forItemSymbol(ItemSymbol that) {
		SyntaxSymbol n = nameResolution(that);
		if (n == null) {
			error("Unknown item symbol: "+that, that);
		}
		return n;
	}

	private SyntaxSymbol nameResolution(ItemSymbol item) {
		if (FortressUtil.validId(item.getItem())) { //TODO A more aquarate method to decide id well formedness

			QualifiedIdName name = makeQualifiedIdName(item.getSpan(), item.getItem());
			//name = ((new ProductionDisambiguator()).new ProductionNameDisambiguator()).handleProductionName(_currentEnv, name);
			
			Set<QualifiedIdName> ss = _currentEnv.declaredProductionNames(name);
			
			if (ss.isEmpty()) {
				ss = _currentEnv.inheritedProductionNames(name);
			}
			
			if (ss.size() == 1) {
				return makeNonterminal(item, name);
			}

			if (ss.size() > 1) {
				error("Production name may refer to: " + NodeUtil.namesString(ss), name);
				return makeNonterminal(item, name);
			}

			if (ss.isEmpty()) {
				return makeKeywordSymbol(item);	
			}
		}
		return makeTokenSymbol(item);
	}

	private NonterminalSymbol makeNonterminal(ItemSymbol that, QualifiedIdName name) {
		return new NonterminalSymbol(that.getSpan(), name);
	}

	private KeywordSymbol makeKeywordSymbol(ItemSymbol item) {
		return new KeywordSymbol(item.getSpan(), item.getItem());
	}

	private TokenSymbol makeTokenSymbol(ItemSymbol item) {
		return new TokenSymbol(item.getSpan(), item.getItem());
	}

	private static QualifiedIdName makeQualifiedIdName(Span span, String item) {
		int lastIndexOf = item.lastIndexOf('.');
		if (lastIndexOf != -1) {
			APIName apiName = NodeFactory.makeAPIName(item.substring(0, lastIndexOf));
			return NodeFactory.makeQualifiedIdName(apiName, NodeFactory.makeId(item.substring(lastIndexOf+1)));
		}
		else {
			return NodeFactory.makeQualifiedIdName(span, item);	
		}
	}

	@Override
	public Node forPrefixedSymbolOnly(final PrefixedSymbol prefix,
			final Option<Id> id_result, SyntaxSymbol symbol_result) {
		
		SyntaxSymbol s = symbol_result;
		Node n = s.accept(new NodeUpdateVisitor(){
			@Override
			public Node forItemSymbol(ItemSymbol that) {
				return handle(that, that.getItem());
			}
			
			@Override
			public Node forKeywordSymbol(KeywordSymbol that) {
				return handle(that, that.getToken());
			}
			
			@Override
			public Node forTokenSymbol(TokenSymbol that) {
				return handle(that, that.getToken());
			}

			private Node handle(SyntaxSymbol that, String s) {
				if (id_result.isNone()) {
					Id var = NodeFactory.makeId(s);
					return new PrefixedSymbol(prefix.getSpan(), Option.wrap(var),that);
				}
				else {
					return new PrefixedSymbol(prefix.getSpan(), id_result,that);
				}
			}
		});
		if (n instanceof SyntaxSymbol) {
			s = (SyntaxSymbol) n; 
			s.getSpan().begin = prefix.getSpan().begin;
			s.getSpan().end = prefix.getSpan().end;
			return s;
		}
		throw new RuntimeException("Prefix symbol contained something different than a syntax symbol: "+ n.getClass().toString());
	}

}
