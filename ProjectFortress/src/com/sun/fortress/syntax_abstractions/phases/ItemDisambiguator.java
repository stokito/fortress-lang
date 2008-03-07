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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.disambiguator.NonterminalNameDisambiguator;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ItemSymbol;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.IdentifierUtil;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.tuple.Option;

public class ItemDisambiguator extends NodeUpdateVisitor {

	private Collection<StaticError> _errors;
	private GlobalEnvironment _globalEnv;
	private GrammarIndex _currentGrammarIndex;
	private ApiIndex _currentApi;
	private String _currentItem;

	public ItemDisambiguator(GlobalEnvironment env) {
		this._errors = new LinkedList<StaticError>();
		this._globalEnv = env;
	}

	private void error(String msg, HasAt loc) {
		this._errors.add(StaticError.make(msg, loc));
	}

	public Option<GrammarIndex> grammarIndex(final QualifiedIdName name) {
		if (name.getApi().isSome()) {
			APIName n = Option.unwrap(name.getApi());
			if (this._globalEnv.definesApi(n)) {
				return Option.some(_globalEnv.api(n).grammars().get(name));
			}
			else {
				return Option.none();
			}
		}
		return Option.some(((ApiIndex) _currentApi).grammars().get(name));
	}

	public Collection<StaticError> errors() {
		return this._errors;
	}

	@Override
	public Node forApi(Api that) {
		if (this._globalEnv.definesApi(that.getName())) {
			this._currentApi = this._globalEnv.api(that.getName());
		}
		else {
			error("Undefined api ", that);
		}
		return super.forApi(that);
	}

	@Override
	public Node forGrammarDef(GrammarDef that) {
		Option<GrammarIndex> index = this.grammarIndex(that.getName());
		if (index.isSome()) {
			this._currentGrammarIndex = Option.unwrap(index);
		}
		else {
			error("Grammar "+that.getName()+" not found", that); 
		}
		return super.forGrammarDef(that);
	}

	@Override
	public SyntaxSymbol forItemSymbol(ItemSymbol that) {
		SyntaxSymbol n = nameResolution(that);
		if (n == null) {
			error("Unknown item symbol: "+that, that);
		}
		if (n instanceof NonterminalSymbol ||
			n instanceof KeywordSymbol) {
			this._currentItem = that.getItem();
		}
		return n;
	}

	private SyntaxSymbol nameResolution(ItemSymbol item) {
		if (IdentifierUtil.validId(item.getItem())) {
			GrammarAnalyzer<GrammarIndex> ga = new GrammarAnalyzer<GrammarIndex>();
			QualifiedIdName name = makeQualifiedIdName(item.getSpan(), item.getItem());
			NonterminalNameDisambiguator nnd = new NonterminalNameDisambiguator(this._globalEnv);
			Option<QualifiedIdName> oname = nnd.handleNonterminalName(this._currentGrammarIndex.env(), name);
			
			if (oname.isSome()) {
				name = Option.unwrap(oname);
				
				Set<QualifiedIdName> setOfNonterminals = ga.getContained(name.getName(), this._currentGrammarIndex);

				if (setOfNonterminals.size() == 1) {
					this._errors.addAll(nnd.errors());
					return makeNonterminal(item, name);
				}

				if (setOfNonterminals.size() > 1) {
					this._errors.addAll(nnd.errors());
					error("Production name may refer to: " + NodeUtil.namesString(setOfNonterminals), name);
					return makeNonterminal(item, name);
				}
			}
			return makeKeywordSymbol(item);
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
			Option<Id> id_result, SyntaxSymbol symbol_result) {
		String varName = symbol_result.accept(new PrefixHandler());
		if (id_result.isNone()) {
			if (!IdentifierUtil.validId(varName)) {
				return symbol_result;
			}
			return handle(prefix, symbol_result, varName);
		}
		else {
			return new PrefixedSymbol(prefix.getSpan(), id_result, symbol_result);
		}
	}
	
	private Node handle(PrefixedSymbol prefix, SyntaxSymbol that, String varName) {
		Id var = NodeFactory.makeId(varName);
		return new PrefixedSymbol(prefix.getSpan(), Option.wrap(var), that);
	}
	
	private class PrefixHandler extends NodeDepthFirstVisitor<String> {

		
		@Override
		public String defaultCase(Node that) {
			System.err.println(that.getClass());
			return "";
		}

		@Override
		public String forItemSymbol(ItemSymbol that) {
			return that.getItem(); // handle(that, that.getItem());
		}

		@Override
		public String forTokenSymbol(TokenSymbol that) {
			return that.getToken(); // handle(that, that.getItem());
		}
		
		@Override
		public String forNonterminalSymbol(NonterminalSymbol that) {
			return _currentItem; // handle(that, _currentItem);
		}
		
		@Override
		public String forKeywordSymbol(KeywordSymbol that) {
			return that.getToken(); // handle(that, that.getToken());
		}

		@Override
		public String forRepeatOneOrMoreSymbolOnly(RepeatOneOrMoreSymbol that, String symbol_result) {
			return symbol_result;
		}

		@Override
		public String forRepeatSymbolOnly(RepeatSymbol that, String symbol_result) {
			return symbol_result;
		}

		@Override
		public String forOptionalSymbolOnly(OptionalSymbol that,
				String symbol_result) {
			return symbol_result;
		}
	
	}

}
