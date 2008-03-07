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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import xtc.parser.Binding;
import xtc.parser.CharClass;
import xtc.parser.CharRange;
import xtc.parser.Element;
import xtc.parser.FollowedBy;
import xtc.parser.Module;
import xtc.parser.NonTerminal;
import xtc.parser.NotFollowedBy;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.AndPredicateSymbol;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BreaklineSymbol;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.CharacterInterval;
import com.sun.fortress.nodes.CharacterSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NotPredicateSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.util.ActionCreater;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

import edu.rice.cs.plt.tuple.Option;

public class SyntaxDefTranslator extends NodeDepthFirstVisitor<List<Sequence>>{
	
	private Iterable<? extends StaticError> errors;
	
	public class Result extends StaticPhaseResult {
		private List<Sequence> alternatives;
		private Set<String> keywords;
		private Collection<Module> keywordModules;

		public Result(List<Sequence> alternatives, Set<String> keywords,
				Collection<Module> keywordModules, Iterable<? extends StaticError> errors) {
			super(errors);
			this.alternatives = alternatives;
			this.keywords = keywords;
			this.keywordModules = keywordModules;
		}

		public Result(Iterable<? extends StaticError> errors) {
			super(errors);
		}

		public List<Sequence> alternatives() { return alternatives; }

		public Set<String> keywords() { return keywords; }
		
		public Collection<Module> keywordModules() { return this.keywordModules; }
	}
	
	public SyntaxDefTranslator() {
		this.errors = new LinkedList<StaticError>();
	}
	
	public static Result translate(NonterminalIndex<? extends GrammarMemberDecl> member) {
		return new SyntaxDefTranslator().visit(member.getAst());
	}

	public static Result translate(GrammarMemberDecl member) {
		return new SyntaxDefTranslator().visit(member);
	}
	
	public Result visit(GrammarMemberDecl member) {
		List<Sequence> sequence = member.accept(this);
		return new Result(sequence, new HashSet<String>(), new LinkedList<Module>(), this.errors);
	}
		
	@Override
	public List<Sequence> forNonterminalDef(NonterminalDef that) {
		TraitType type = SyntaxAbstractionUtil.unwrap(that.getType());
		String name = that.getName().getName().toString();
		return visitSyntaxDefs(that.getSyntaxDefs(), name, type);
	}

	@Override
	public List<Sequence> forNonterminalExtensionDef(NonterminalExtensionDef that) {
		throw new RuntimeException("Nonterminal extension definitions should have been eliminated by now");
	}

	@Override
	public List<Sequence> for_TerminalDef(_TerminalDef that) {
		TraitType type = SyntaxAbstractionUtil.unwrap(that.getType());
		String name = that.getName().getName().toString();
		List<Sequence> sequences = FortressUtil.mkList(visitSyntaxDef(that.getSyntaxDef(), name, type));
		return sequences;
	}
	
	
	private List<Sequence> visitSyntaxDefs(Iterable<SyntaxDef> syntaxDefs,
			String name, TraitType type) {
		List<Sequence> sequence = new LinkedList<Sequence>();
		for (SyntaxDef syntaxDef: syntaxDefs) {
			sequence.add(visitSyntaxDef(syntaxDef, name, type));
		}
		return sequence;
	}

	
	private Sequence visitSyntaxDef(SyntaxDef syntaxDef, String name, TraitType type) {
		List<Element> elms = new LinkedList<Element>();
		// Translate the symbols
		Collection<PrefixedSymbol> boundVariables = new LinkedList<PrefixedSymbol>();
		for (SyntaxSymbol sym: syntaxDef.getSyntaxSymbols()) {
			elms.addAll(sym.accept(new SymbolTranslator()));
			boundVariables.addAll(sym.accept(new VariableCollector()));
		}
		String newName = FreshName.getFreshName(name).toUpperCase();
		ActionCreater.Result acr = ActionCreater.create(newName, syntaxDef.getTransformationExpression(), type, boundVariables);
		if (!acr.isSuccessful()) { new Result(acr.errors()); }
		elms.add(acr.action());
		return new Sequence(new SequenceName(newName), elms);
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
			return mkList(new xtc.parser.StringLiteral(that.getToken()));
		}

		@Override
		public List<Element> forTokenSymbol(TokenSymbol that) {
			return mkList(new xtc.parser.StringLiteral(that.getToken()));
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
				if (that.getId().isSome()) {
					throw new RuntimeException("Malformed variable binding, bound to nonsensible symbol: "+Option.unwrap(that.getId()).getText() + " "+that.getSymbol());
				}
				else {
					throw new RuntimeException("Malformed variable binding, bound to nonsensible symbol, no identifier: "+that.getSymbol());
				}
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
