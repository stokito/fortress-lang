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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.syntax_abstractions.intermediate.Module;

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
		Map<QualifiedIdName, GrammarIndex> m = new HashMap<QualifiedIdName, GrammarIndex>();
		for (GrammarEnv gEnv: envs) {
			for (GrammarIndex g: gEnv.getGrammars()) {
				m.put(g.getName(), g);
			}
		}
		for (GrammarEnv gEnv: envs) {
			for (GrammarIndex g: gEnv.getGrammars()) {
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
		return (new GrammarIndexInitializer()).new Result(envs, ses);
	}
}
