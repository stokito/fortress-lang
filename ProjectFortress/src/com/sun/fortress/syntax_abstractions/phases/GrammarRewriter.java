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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.QualifiedIdName;

import edu.rice.cs.plt.tuple.Option;

/*
 * 1) Disambiguate item symbols and rewrite to either nonterminal, 
 *    keyword or token symbol
 * 2) Remove whitespace where indicated by no-whitespace symbols
 * 3) Rewrite escaped symbols
 * 4) Create a terminal declaration for each keyword and token symbols,
 *    and rewrite keyword and token symbols to nonterminal symbols, referring 
 *    to the corresponding terminal definitions.
 */
public class GrammarRewriter {

	/** Result of {@link #disambiguateApis}. */
	public static class ApiResult extends StaticPhaseResult {
		private final Iterable<Api> _apis;

		public ApiResult(Iterable<Api> apis, 
				Iterable<? extends StaticError> errors) {
			super(errors);
			_apis = apis;
		}

		public Iterable<Api> apis() { return _apis; }
	}

	public static ApiResult rewriteApis(Map<APIName, ApiIndex> map, GlobalEnvironment env) {
		Collection<ApiIndex> apis = new LinkedList<ApiIndex>();
		apis.addAll(map.values());
		apis.addAll(env.apis().values());
		initializeGrammarIndexExtensions(apis);
		
		List<Api> results = new ArrayList<Api>();
		ItemDisambiguator id = new ItemDisambiguator(env);
		for (ApiIndex api: apis) {
			Api idResult = (Api) api.ast().accept(id);
			if (id.errors().isEmpty()) {
				// Remove whitespace where instructed by non-whitespace symbols
				WhitespaceElimination we = new WhitespaceElimination();
				Api sdResult = (Api) idResult.accept(we);

				// Rewrite escaped characters
				EscapeRewriter escapeRewriter = new EscapeRewriter();
				Api erResult = (Api) sdResult.accept(escapeRewriter);
				
				// Rewrite terminals to be declared using terminal definitions
				TerminalRewriter terminalRewriter = new TerminalRewriter();
				Api trResult = (Api) erResult.accept(terminalRewriter);
				results.add(trResult);
			}
		}
		return new ApiResult(results, id.errors());
	}

	private static void initializeGrammarIndexExtensions(Collection<ApiIndex> apis) {
		Map<QualifiedIdName, GrammarIndex> grammars = new HashMap<QualifiedIdName, GrammarIndex>();
		for (ApiIndex a2: apis) {
			for (Entry<QualifiedIdName,GrammarIndex> e: a2.grammars().entrySet()) {
				grammars.put(e.getKey(), e.getValue());
			}
		}
		
		for (ApiIndex a1: apis) {
			for (Entry<QualifiedIdName,GrammarIndex> e: a1.grammars().entrySet()) {
				Option<GrammarDef> og = e.getValue().ast();
				if (og.isSome()) {
					List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
					for (QualifiedIdName n: Option.unwrap(og).getExtends()) {
						ls.add(grammars.get(n));
					}
					e.getValue().setExtended(ls);
				}
			}
		}		
	}

}
