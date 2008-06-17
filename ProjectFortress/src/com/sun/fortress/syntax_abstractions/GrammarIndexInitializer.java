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

package com.sun.fortress.syntax_abstractions;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.compiler.disambiguator.NonterminalNameDisambiguator;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.GrammarNonterminalIndex;
import com.sun.fortress.compiler.index.GrammarTerminalIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.intermediate.SyntaxSymbolPrinter;
import com.sun.fortress.syntax_abstractions.phases.GrammarAnalyzer;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class GrammarIndexInitializer {

    public static class Result extends StaticPhaseResult {
        private Collection<GrammarIndex> grammarIndexs;

        public Result(Collection<GrammarIndex> grammarIndexs,
                Iterable<? extends StaticError> errors) {
            super(errors);
            this.grammarIndexs = grammarIndexs;
        }

    }

    public static Result init(Collection<GrammarIndex> grammarIndexs) {
        Collection<StaticError> ses = new LinkedList<StaticError>();
        initGrammarExtends(grammarIndexs, ses);
        initNonterminalExtends(grammarIndexs, ses);
        initGrammarEnv(grammarIndexs);
        return new Result(grammarIndexs, ses);
    }

    /**
     * Each nonterminal index is linked to the other
     * @param envs
     * @param ses
     */
    private static void initNonterminalExtends(Collection<GrammarIndex> grammarIndexs,
            Collection<StaticError> ses) {
        for (GrammarIndex g: grammarIndexs) {
            // Intentional use of raw type to work around a bug in the Java 5 compiler on Solaris: <? extends NonterminalDecl>
            for (NonterminalIndex /*<? extends GrammarMemberDecl> */ n: g.getDeclaredNonterminals()) {
                if (n instanceof NonterminalExtendIndex) {
                    Id name = n.getName();
                    GrammarAnalyzer<GrammarIndex> ga = new GrammarAnalyzer<GrammarIndex>();
                    Collection<NonterminalIndex<? extends GrammarMemberDecl>> s = ga.getOverridingNonterminalIndex(name.getText(), g);
                    if (s.isEmpty()) {
                        ses.add(StaticError.make("Unknown extended nonterminal: "+name+" in grammar: "+g.getName(), n.getAst()));
                    }
                }
            }
        }

    }

    /**
     * Each grammar index has a collection of the grammar index' it extends
     * @param grammarIndexs
     * @param ses
     */
    private static void initGrammarExtends(Collection<GrammarIndex> grammarIndexs,
            Collection<StaticError> ses) {
        // Record all the grammar names and their grammar index
        Map<Id, GrammarIndex> m = new HashMap<Id, GrammarIndex>();
        for (GrammarIndex g: grammarIndexs) {
            m.put(g.getName(), g);
        }
        // Make sure that a grammar index has a reference to the grammar index's it extends
        for (GrammarIndex g: grammarIndexs) {
            if (g.ast().isSome()) {
                List<GrammarIndex> gs = new LinkedList<GrammarIndex>();
                for (Id otherName: g.ast().unwrap().getExtends()) {
                    if (m.containsKey(otherName)) {
                        gs.add(m.get(otherName));
                    }
                }
                g.setExtended(gs);
            }
            else {
                ses.add(StaticError.make("Malformed grammar", g.getName()));
            }
        }
    }
    
    private static void initGrammarEnv(Collection<GrammarIndex> grammarIndexs) {
        for (GrammarIndex g: grammarIndexs) {
            for (NonterminalIndex/*<? extends GrammarMemberDecl> Commented out due to a bug in Sun Java 1.5.0_04 on Fedora Linux */ nt: g.getDeclaredNonterminals()) {
                GrammarEnv.add(nt.getName(), new MemberEnv(nt));
            }
        }        
    }
    

}
