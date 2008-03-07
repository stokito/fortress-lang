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

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.syntax_abstractions.phases.GrammarAnalyzer;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class GrammarIndexInitializer {

	public static class Result extends StaticPhaseResult {
		private Collection<GrammarEnv> envs;

		public Result(Collection<GrammarEnv> envs,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.envs = envs;
		}

		public Collection<GrammarEnv> env() { return envs; }
	}

	public static Result init(Collection<GrammarEnv> envs) {
		Collection<StaticError> ses = new LinkedList<StaticError>();
		initGrammarExtends(envs, ses);
		initNonterminalExtends(envs, ses);
		return new Result(envs, ses);
	}

	/**
	 * Each nonterminal index is linked to the other  
	 * @param envs
	 * @param ses
	 */
	private static void initNonterminalExtends(Collection<GrammarEnv> envs,
			Collection<StaticError> ses) {
		for (GrammarEnv env: envs) {
			for (GrammarIndex g: env.getGrammars()) {
				// Intentional use of raw type to work around a bug in the Java 5 compiler on Solaris: <? extends NonterminalDecl>
				for (NonterminalIndex /*<? extends GrammarMemberDecl> */ n: g.getDeclaredNonterminals()) {
					if (n instanceof NonterminalExtendIndex) {
						Id name = n.getName().getName();
						GrammarAnalyzer<GrammarIndex> ga = new GrammarAnalyzer<GrammarIndex>();
						Collection<NonterminalIndex<? extends GrammarMemberDecl>> s = ga.getOverridingNonterminalIndex(name, g);
						if (s.isEmpty()) {
							ses.add(StaticError.make("Unknown extended nonterminal: "+name+" in grammar: "+g.getName(), n.getAst()));
						}
						((NonterminalExtendIndex) n).addExtendedNonterminals(s);
					}
				}
			}
		}
		
	}

	/**
	 * Each grammar index has a collection of the grammar index' it extends
	 * @param envs
	 * @param ses
	 */
	private static void initGrammarExtends(Collection<GrammarEnv> envs,
			Collection<StaticError> ses) {
		// Record all the grammar names and their grammar index
		Map<QualifiedIdName, GrammarIndex> m = new HashMap<QualifiedIdName, GrammarIndex>();
		for (GrammarEnv gEnv: envs) {
			for (GrammarIndex g: gEnv.getGrammars()) {
				m.put(g.getName(), g);
			}
		}
		// Make sure that a grammar index has a reference to the grammar index's it extends
		for (GrammarEnv gEnv: envs) {
			for (GrammarIndex g: gEnv.getGrammars()) {
				// Init nonterminal envs
				g.setEnv(new NonterminalEnv(g));
				if (g.ast().isSome()) {
					List<GrammarIndex> gs = new LinkedList<GrammarIndex>();
					for (QualifiedIdName otherName: Option.unwrap(g.ast()).getExtends()) {
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
	}
}
