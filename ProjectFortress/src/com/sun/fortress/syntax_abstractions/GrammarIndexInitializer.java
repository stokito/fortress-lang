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
import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionExtendIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class GrammarIndexInitializer {

	public class Result extends StaticPhaseResult {
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
		initProductionExtends(envs, ses);
		return (new GrammarIndexInitializer()).new Result(envs, ses);
	}

	/**
	 * Each production index is linked to the other  
	 * @param envs
	 * @param ses
	 */
	private static void initProductionExtends(Collection<GrammarEnv> envs,
			Collection<StaticError> ses) {
		for (GrammarEnv env: envs) {
			for (GrammarIndex g: env.getGrammars()) {
				for (ProductionIndex<? extends NonterminalDecl> p: g.productions().values()) {
					if (p instanceof ProductionExtendIndex) {
						Id name = p.getName().getName();
						Set<ProductionIndex<? extends NonterminalDecl>> s = g.env().getExtendedNonterminal(name);
						if (s.isEmpty()) {
							ses.add(StaticError.make("Unknown extended nonterminal: "+name, p.getAst()));
						}
						if (s.size() > 1) {
							ses.add(StaticError.make("Ambiguous extended nonterminal: "+name, p.getAst()));
						}
						((ProductionExtendIndex) p).setExtends(IterUtil.first(s));
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
		// Make sure that a gramar index has a reference to the grammar index's it extends
		for (GrammarEnv gEnv: envs) {
			for (GrammarIndex g: gEnv.getGrammars()) {
				// Init productions envs
				g.setEnv(new ProductionEnv(g));
				if (g.ast().isSome()) {
					List<GrammarIndex> gs = new LinkedList<GrammarIndex>();
					for (QualifiedIdName otherName: Option.unwrap(g.ast()).getExtends()) {
						if (m.containsKey(otherName)) {
							gs.add(m.get(otherName));
						}
					}
					g.setExtendedGrammars(gs);
				}
				else {
					ses.add(StaticError.make("Malformed grammar", g.getName()));
				}
			}
		}
	}
}
