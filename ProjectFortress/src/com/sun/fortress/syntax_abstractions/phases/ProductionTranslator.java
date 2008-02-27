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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import xtc.parser.TokenValue;
import xtc.tree.Attribute;
import xtc.tree.Node;

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
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NotPredicateSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.syntax_abstractions.rats.util.ProductionEnum;
import com.sun.fortress.syntax_abstractions.util.ActionCreater;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

// Todo: rename to member translator
public class ProductionTranslator {
	private List<Production> productions;
	private Collection<StaticError> errors;
	
	public class Result extends StaticPhaseResult {

		public Result(Iterable<? extends StaticError> errors) {
			super(errors);
		}

		public Result(Collection<StaticError> errors) {
			super(errors);
		}

		public List<Production> productions() { return productions; }

	}
	
	public ProductionTranslator() {
		this.productions = new LinkedList<Production>();
		this.errors = new LinkedList<StaticError>();
	}

	/**
	 * Translate a collection of Fortress productions to Rats! productions
	 * @param 
	 * @param env 
	 * @return
	 */
	public static Result translate(Collection<ProductionIndex<? extends GrammarMemberDecl>> members) {	
		return new ProductionTranslator().doTranslate(members);
	}
	
	private Result doTranslate(
			Collection<ProductionIndex<? extends GrammarMemberDecl>> members) {
		
		for (ProductionIndex<? extends GrammarMemberDecl> member: members) {
			this.translate(member);
		}

		return new Result(errors);
	}

	/**
	 * Translate a Fortress production to a Rats! production 
	 * @param member
	 * @return
	 */
	private Result translate(ProductionIndex<? extends GrammarMemberDecl> member) {
		Collection<StaticError> errors = new LinkedList<StaticError>();
		NonterminalTranslator nt = new NonterminalTranslator(member);
		productions.add(member.getAst().accept(nt));
		errors.addAll(nt.errors());
		return new Result(errors);
	}

	
	
	private static class NonterminalTranslator extends NodeDepthFirstVisitor<Production> {
		private Collection<StaticError> errors;
		private ProductionIndex/*<? extends GrammarMemberDecl>*/ pi; // Unsafe to prevent bug in Java 5 on Solaris 
		
		public NonterminalTranslator(ProductionIndex<? extends GrammarMemberDecl> pi) {
			this.errors = new LinkedList<StaticError>();
			this.pi = pi;
		}
		
		public Collection<StaticError> errors() {
			return this.errors;
		}
				
		@Override
		public Production forNonterminalDef(NonterminalDef that) {
			List<Attribute> attr = new LinkedList<Attribute>();
			TraitType type = SyntaxAbstractionUtil.unwrap(that.getType());
			String name = that.getName().getName().toString();
			
			SyntaxDefTranslator.Result sdtr = SyntaxDefTranslator.translate(that); 
			if (!sdtr.isSuccessful()) { for (StaticError e: sdtr.errors()) { this.errors.add(e); } }
			List<Sequence> sequence = sdtr.alternatives();
			
			Production p = new FullProduction(attr, new FortressTypeToJavaType().analyze(type),
					new NonTerminal(name),
					new OrderedChoice(sequence));
			p.name = new NonTerminal(name);
			return p;
		}

		@Override
		public Production forNonterminalExtensionDef(NonterminalExtensionDef that) {
			throw new RuntimeException("Nonterminal extension definition should not appear"+that);
		}

		@Override
		public Production for_TerminalDef(_TerminalDef that) {
			List<Attribute> attr = new LinkedList<Attribute>();
			TraitType type = SyntaxAbstractionUtil.unwrap(that.getType());
			String name = that.getName().getName().toString();
			
			SyntaxDefTranslator.Result sdtr = SyntaxDefTranslator.translate(that); 
			if (!sdtr.isSuccessful()) { for (StaticError e: sdtr.errors()) { this.errors.add(e); } }
			List<Sequence> sequence = sdtr.alternatives();
			
			Production p = new FullProduction(attr, type.toString(),
					new NonTerminal(name),
					new OrderedChoice(sequence));
			p.name = new NonTerminal(name);
			return p;
		}
		
	}

}
