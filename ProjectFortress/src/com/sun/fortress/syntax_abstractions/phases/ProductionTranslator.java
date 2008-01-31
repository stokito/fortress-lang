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
import xtc.parser.FollowedBy;
import xtc.parser.FullProduction;
import xtc.parser.NonTerminal;
import xtc.parser.NotFollowedBy;
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
import com.sun.fortress.compiler.index.ProductionExtendIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AndPredicateSymbol;
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
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NotPredicateSymbol;
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
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
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
	public static Result translate(Collection<ProductionIndex<? extends NonterminalDecl>> productions, GlobalEnvironment env) {
		ProductionTranslator pt = new ProductionTranslator();
		List<Production> ratsProductions = new LinkedList<Production>();
		Collection<StaticError> errors = new LinkedList<StaticError>();

		for (ProductionIndex<? extends NonterminalDecl> production: productions) {
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
	 * @param p
	 * @return
	 */
	private Result translate(final ProductionIndex<? extends NonterminalDecl> p, GlobalEnvironment env) {
		Collection<StaticError> errors = new LinkedList<StaticError>();
		NonterminalTranslator nt = new NonterminalTranslator(p);
		Production ratsProduction = p.getAst().accept(nt);
		errors.addAll(nt.errors());
		return new Result(FortressUtil.mkList(ratsProduction), errors);
	}

	private class NonterminalTranslator extends NodeDepthFirstVisitor<Production> {
		private Collection<StaticError> errors;
		private ProductionIndex/*<? extends NonterminalDecl>*/ pi; // Unsafe to prevent bug in Java 5 on Solaris 
		
		public NonterminalTranslator(ProductionIndex<? extends NonterminalDecl> pi) {
			this.errors = new LinkedList<StaticError>();
			this.pi = pi;
		}
		
		public Collection<StaticError> errors() {
			return this.errors;
		}
		
		private TraitType unwrap(Option<TraitType> t) {
			if (t.isNone()) {
				throw new RuntimeException("Production type is not defined, malformed AST");
			}
			return Option.unwrap(t);
		}
		
		private List<Sequence> visitSyntaxDefs(Iterable<SyntaxDef> syntaxDefs, String productionName, TraitType type) {
			List<Sequence> sequence = new LinkedList<Sequence>();

			// First translate the syntax definitions
			for (SyntaxDef syntaxDef: syntaxDefs) {
				List<Element> elms = new LinkedList<Element>();
				// Translate the symbols
				for (SyntaxSymbol sym: syntaxDef.getSyntaxSymbols()) {
					elms.addAll(sym.accept(new SymbolTranslator()));
				}		
				String newProductionName = FreshName.getFreshName(productionName).toUpperCase();
				ActionCreater.Result acr = ActionCreater.create(newProductionName, syntaxDef.getTransformationExpression(), type.toString());
				if (!acr.isSuccessful()) { for (StaticError e: acr.errors()) { errors.add(e); } }
				elms.add(acr.action());
				sequence.add(new Sequence(new SequenceName(newProductionName), elms));		
			}
			return sequence;
		}
		
		@Override
		public Production forNonterminalDef(NonterminalDef that) {
			List<Attribute> attr = new LinkedList<Attribute>();
			TraitType type = unwrap(that.getType());
			String name = that.getName().getName().toString();
			List<Sequence> sequence = visitSyntaxDefs(that.getSyntaxDefs(), name, type);
			Production p = new FullProduction(attr, type.toString(),
					new NonTerminal(name),
					new OrderedChoice(sequence));
			p.name = new NonTerminal(name);
			return p;
		}

		@Override
		public Production forNonterminalExtensionDef(NonterminalExtensionDef that) {
			TraitType type = unwrap(that.getType());
			String name = that.getName().getName().toString();
			List<Sequence> sequence = visitSyntaxDefs(that.getSyntaxDefs(), name, type);
			Collection<ProductionIndex<? extends NonterminalDecl>> ls = ((ProductionExtendIndex) this.pi).getExtends();
			QualifiedIdName otherQualifiedName = IterUtil.first(ls).getName();
			Production p = new AlternativeAddition(type.toString(),
					new NonTerminal(name),
					new OrderedChoice(sequence), 
					new SequenceName(ModuleInfo.getExtensionPoint(otherQualifiedName .toString())),false);
			p.name = new NonTerminal(name);
			return p;
		}
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
				// TODO: Error when begin < end
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
			if (symbol_result.isEmpty()) {
				throw new RuntimeException("Malformed variable binding, not bound to any symbol: ");
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
			if (symbol_result.isEmpty()) {
				throw new RuntimeException("Malformed optional symbol, not bound to any symbol: ");
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
			if (symbol_result.isEmpty()) {
				throw new RuntimeException("Malformed repeat-one-or-more symbol, not bound to any symbol: ");
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
			if (symbol_result.isEmpty()) {
				throw new RuntimeException("Malformed repeat symbol, not bound to any symbol: ");
			}
			throw new RuntimeException("Malformed repeat symbol, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> forAndPredicateSymbolOnly(AndPredicateSymbol that,
				List<Element> symbol_result) {
			if (symbol_result.size() == 1) {
				Element e = symbol_result.remove(0);
				symbol_result.add(new FollowedBy(e));
				return symbol_result;
			}
			if (symbol_result.isEmpty()) {
				throw new RuntimeException("Malformed AND predicate symbol, not bound to any symbol: ");
			}
			throw new RuntimeException("Malformed AND predicate symbol, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> forNotPredicateSymbolOnly(NotPredicateSymbol that,
				List<Element> symbol_result) {
			if (symbol_result.size() == 1) {
				Element e = symbol_result.remove(0);
				symbol_result.add(new NotFollowedBy(e));
				return symbol_result;
			}
			if (symbol_result.isEmpty()) {
				throw new RuntimeException("Malformed NOT predicate symbol, not bound to any symbol: ");
			}
			throw new RuntimeException("Malformed NOT predicate symbol, bound to multiple symbols: "+symbol_result);
		}

		@Override
		public List<Element> defaultCase(com.sun.fortress.nodes.Node that) {
			return new LinkedList<Element>();
		}

	}

}
