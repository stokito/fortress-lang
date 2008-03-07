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

import xtc.parser.FullProduction;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.tree.Attribute;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;


public class MemberTranslator {
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
	
	public MemberTranslator() {
		this.productions = new LinkedList<Production>();
		this.errors = new LinkedList<StaticError>();
	}

	/**
	 * Translate a collection of grammar members to Rats! productions
	 * @param 
	 * @param env 
	 * @return
	 */
	public static Result translate(Collection<NonterminalIndex<? extends GrammarMemberDecl>> members) {	
		return new MemberTranslator().doTranslate(members);
	}
	
	private Result doTranslate(
			Collection<NonterminalIndex<? extends GrammarMemberDecl>> members) {
		
		for (NonterminalIndex<? extends GrammarMemberDecl> member: members) {
			this.translate(member);
		}

		return new Result(errors);
	}

	/**
	 * Translate a grammar member to a Rats! production 
	 * @param member
	 * @return
	 */
	private Result translate(NonterminalIndex<? extends GrammarMemberDecl> member) {
		Collection<StaticError> errors = new LinkedList<StaticError>();
		NonterminalTranslator nt = new NonterminalTranslator(member);
		productions.add(member.getAst().accept(nt));
		errors.addAll(nt.errors());
		return new Result(errors);
	}

	
	
	private static class NonterminalTranslator extends NodeDepthFirstVisitor<Production> {
		private Collection<StaticError> errors;
		private NonterminalIndex/*<? extends GrammarMemberDecl>*/ pi; // Unsafe to prevent bug in Java 5 on Solaris 
		
		public NonterminalTranslator(NonterminalIndex<? extends GrammarMemberDecl> pi) {
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
