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

import xtc.parser.Action;
import xtc.parser.Binding;
import xtc.parser.Element;
import xtc.parser.FullProduction;
import xtc.parser.NonTerminal;
import xtc.parser.Option;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;
import xtc.tree.Attribute;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;
import com.sun.fortress.syntax_abstractions.util.JavaAstPrettyPrinter;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;
import com.sun.fortress.useful.Pair;


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

    private MemberTranslator() {
        this.productions = new LinkedList<Production>();
        this.errors = new LinkedList<StaticError>();
    }

    /**
     * Translate a collection of grammar members to Rats! productions
     * @param grammarEnv 
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
        NonterminalTranslator nt = new NonterminalTranslator();
        productions.add(member.getAst().accept(nt));
        errors.addAll(nt.errors());
        return new Result(errors);
    }



    private static class NonterminalTranslator extends NodeDepthFirstVisitor<Production> {
        private Collection<StaticError> errors;

        public NonterminalTranslator() {
            this.errors = new LinkedList<StaticError>();
        }

        public Collection<StaticError> errors() {
            return this.errors;
        }

        @Override
        public Production forNonterminalDef(NonterminalDef that) {
            List<Attribute> attr = new LinkedList<Attribute>();
            BaseType type = SyntaxAbstractionUtil.unwrap(that.getAstType());
            String name = that.getHeader().getName().getText(); //toString().replaceAll("\\.", "");
           
            SyntaxDefTranslator.Result sdtr = SyntaxDefTranslator.translate(that); 
            if (!sdtr.isSuccessful()) { for (StaticError e: sdtr.errors()) { this.errors.add(e); } }
            List<Sequence> sequence = sdtr.alternatives();

            // sequence.add(0, getGapSequence(name, type));

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
            BaseType type = SyntaxAbstractionUtil.unwrap(that.getAstType());
            String name = that.getHeader().getName().toString();

            SyntaxDefTranslator.Result sdtr = SyntaxDefTranslator.translate(that); 
            if (!sdtr.isSuccessful()) { for (StaticError e: sdtr.errors()) { this.errors.add(e); } }
            List<Sequence> sequence = sdtr.alternatives();

            // sequence.add(0, getGapSequence(name, type));
            
            Production p = new FullProduction(attr,
                    type.toString(),
                    new NonTerminal(name),
                    new OrderedChoice(sequence));
            p.name = new NonTerminal(name);
            return p;
        }
    }
}
