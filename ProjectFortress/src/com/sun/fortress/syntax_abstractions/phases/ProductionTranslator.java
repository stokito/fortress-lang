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
import java.util.List;

import xtc.parser.AlternativeAddition;
import xtc.parser.Binding;
import xtc.parser.CharClass;
import xtc.parser.CharRange;
import xtc.parser.Element;
import xtc.parser.FullProduction;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;
import xtc.parser.Terminal;
import xtc.tree.Attribute;
import xtc.tree.Node;
import xtc.type.Type;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BreaklineSymbol;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.CharacterInterval;
import com.sun.fortress.nodes.CharacterSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.syntax_abstractions.rats.util.ProductionEnum;
import com.sun.fortress.syntax_abstractions.util.ActionCreater;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class ProductionTranslator {

	public class Result extends StaticPhaseResult {
		private List<Production> productions;

		public Result(List<Production> productions, 
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.productions = productions;
		}

		public Result(Collection<StaticError> errors) {
			super(errors);
			this.productions = new LinkedList<Production>();
		}

		public List<Production> productions() { return productions; }

	}

	/**
	 * Translate a collection of Fortress productions to Rats! productions
	 * @param 
	 * @param env 
	 * @return
	 */
	public static Result translate(Collection<ProductionIndex> productions, GlobalEnvironment env) {
		ProductionTranslator pt = new ProductionTranslator();
		List<Production> ratsProductions = new LinkedList<Production>();
		Collection<StaticError> errors = new LinkedList<StaticError>();
		
		for (ProductionIndex production: productions) {
			Result result = pt.translate(production, env);
			for (StaticError se: result.errors()) {
				errors.add(se);
			}
			ratsProductions.addAll(result.productions());
		}
		
		return pt.new Result(ratsProductions, errors);
	}

	/**
	 * Translate a Fortress production to a Rats! production 
	 * @param production
	 * @return
	 */
	private Result translate(ProductionIndex production, GlobalEnvironment env) {
		Collection<StaticError> errors = new LinkedList<StaticError>();
		
		List<Sequence> sequence = new LinkedList<Sequence>();
		
		// First translate the syntax definitions
		for (SyntaxDef syntaxDef: production.getSyntaxDefs()) {
			List<Element> elms = new LinkedList<Element>();
			
			// Translate the symbols
			for (SyntaxSymbol sym: syntaxDef.getSyntaxSymbols()) {
				elms.addAll(sym.accept(new SymbolTranslator()));
			}		
			String productionName = FreshName.getFreshName(production.getName().getName().toString()).toUpperCase();
			ActionCreater.Result acr = ActionCreater.create(productionName, syntaxDef.getTransformationExpression(), production.getType().toString());
			if (!acr.isSuccessful()) { for (StaticError e: acr.errors()) { errors.add(e); } }
			elms.add(acr.action());
			sequence.add(new Sequence(new SequenceName(productionName), elms));		
		}
		
		// Then translate the nonterminal definition
		Production ratsProduction = null;
		String currentName = production.getName().getName().toString();
// TODO: extends
//		if (production.getExtends().isSome()) {
//			// If we extend something...
//			QualifiedIdName otherQualifiedName = Option.unwrap(production.getExtends());
//			List<Id> ids = new LinkedList<Id>();
//			ids.addAll(Option.unwrap(otherQualifiedName.getApi()).getIds());
//			Id otherId = null;
//			if (ids.size() > 1) {
//				otherId = ids.remove(ids.size()-1);
//			}
//			APIName apiName = NodeFactory.makeAPIName(ids);
//			ApiIndex otherApi = null;
//			if (env.definesApi(apiName)) {
//				otherApi = env.api(apiName);
//			}
//			else {
//				Collection<StaticError> cs = new LinkedList<StaticError>();
//				cs.add(StaticError.make("Undefined api: "+apiName, otherQualifiedName));
//				return new Result(cs);
//			}
//			GrammarIndex otherGrammar = otherApi.grammars().get(otherId);
//			String otherName = otherQualifiedName.getName().toString();
//
//			String otherType = otherGrammar.productions().get(otherQualifiedName).getType().toString();
//			
//			ratsProduction = new AlternativeAddition(otherType,
//							 						 new NonTerminal(otherName),
//							 						 new OrderedChoice(sequence), 
//							 						 new SequenceName(ModuleInfo.getExtensionPoint(otherQualifiedName.toString())),false);
//			ratsProduction.name = new NonTerminal(currentName);
//		}
//		else {
//			List<Attribute> attr = new LinkedList<Attribute>();
//			String type = production.getType().toString();
//			ratsProduction = new FullProduction(attr, type,
//							 new NonTerminal(currentName),
//							 new OrderedChoice(sequence));
//			ratsProduction.name = new NonTerminal(production.getName().toString());
//		}
		
		List<Production> productions = new LinkedList<Production>();
		productions.add(ratsProduction);
		return new Result(productions, errors);
	}
	
	private static class SymbolTranslator extends NodeDepthFirstVisitor<List<Element>> {

		private List<Element> mkList(Element e) {
			List<Element> els = new LinkedList<Element>();
			els.add(e);
			return els;
		}
		
		@Override
		public List<Element> forNonterminalSymbol(NonterminalSymbol that) {
			return mkList(new NonTerminal(that.getNonterminal().getName().stringName()));
		}	

		@Override
		public List<Element> forKeywordSymbol(KeywordSymbol that) {
			return mkList(new NonTerminal(that.getToken()));
		}
		
		@Override
		public List<Element> forTokenSymbol(TokenSymbol that) {
			return mkList(new NonTerminal(that.getToken()));
		}

		@Override
		public List<Element> forWhitespaceSymbol(WhitespaceSymbol that) {
			return mkList(new NonTerminal("w"));
		}

		@Override
		public List<Element> forBreaklineSymbol(BreaklineSymbol that) {
			return mkList(new NonTerminal("br"));
		}
		
		@Override
		public List<Element> forBackspaceSymbol(BackspaceSymbol that) {
			return mkList(new NonTerminal("backspace"));
		}
		
		@Override
		public List<Element> forNewlineSymbol(NewlineSymbol that) {
			return mkList(new NonTerminal("newline"));
		}

		@Override
		public List<Element> forCarriageReturnSymbol(CarriageReturnSymbol that) {
			return mkList(new NonTerminal("return"));
		}

		@Override
		public List<Element> forFormfeedSymbol(FormfeedSymbol that) {
			return mkList(new NonTerminal("formfeed"));
		}

		@Override
		public List<Element> forTabSymbol(TabSymbol that) {
			return mkList(new NonTerminal("tab"));
		}
		
		@Override
		public List<Element> forCharacterClassSymbol(CharacterClassSymbol that) {
			List<CharRange> crs = new LinkedList<CharRange>();
			final String mess = "Incorrect escape rewrite: ";
			for (CharacterSymbol c: that.getCharacters()) {
				CharRange cr = c.accept(new NodeDepthFirstVisitor<CharRange>() {
					@Override
					public CharRange forCharacterInterval(CharacterInterval that) {
						if (that.getBegin().length() != 1) {
							new RuntimeException(mess +that.getBegin());
						}
						if (that.getEnd().length() != 1) {
							new RuntimeException(mess+that.getEnd());
						}
						return new CharRange(that.getBegin().charAt(0), that.getEnd().charAt(0));
					}

					@Override
					public CharRange forCharSymbol(CharSymbol that) {
						if (that.getString().length() != 1) {
							new RuntimeException(mess+that.getString());
						}
						return new CharRange(that.getString().charAt(0));
					}					
				});
				crs.add(cr);
			}
			return mkList(new CharClass(crs));
		}

		@Override
		public List<Element> forPrefixedSymbolOnly(PrefixedSymbol that,
				Option<List<Element>> id_result, List<Element> symbol_result) {
			if (symbol_result.size() == 1) {
				Element e = symbol_result.remove(0);
				symbol_result.add(new Binding(Option.unwrap(that.getId()).getText(), e));
				return symbol_result;
			}
			throw new RuntimeException("Malformed variable binding, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> forOptionalSymbolOnly(OptionalSymbol that,
				List<Element> symbol_result) {
			if (symbol_result.size() == 1) {
				Element e = symbol_result.remove(0);
				symbol_result.add(new xtc.parser.Option(e));
				return symbol_result;
			}
			throw new RuntimeException("Malformed optional symbol, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> forRepeatOneOrMoreSymbolOnly(
				RepeatOneOrMoreSymbol that, List<Element> symbol_result) {
			if (symbol_result.size() == 1) {
				Element e = symbol_result.remove(0);
				symbol_result.add(new xtc.parser.Repetition(true, e));
				return symbol_result;
			}
			throw new RuntimeException("Malformed repeat-one-or-more symbol, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> forRepeatSymbolOnly(RepeatSymbol that,
				List<Element> symbol_result) {
			if (symbol_result.size() == 1) {
				Element e = symbol_result.remove(0);
				symbol_result.add(new xtc.parser.Repetition(false, e));
				return symbol_result;
			}
			throw new RuntimeException("Malformed repeat symbol, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> defaultCase(com.sun.fortress.nodes.Node that) {
			return new LinkedList<Element>();
		}
		
	}

}
