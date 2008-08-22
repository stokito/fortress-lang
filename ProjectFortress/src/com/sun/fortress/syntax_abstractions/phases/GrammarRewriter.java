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
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;

/**
 * Syntax abstraction entry point:
 * Part of front-end processing of APIs.
 *
 * 1) Disambiguate item symbols and rewrite to either nonterminal,
 *    keyword or token symbol
 * 2) Disambiguate nonterminal parameters
 * 3) Remove whitespace where indicated by no-whitespace symbols
 * 4) Rewrite escaped symbols
 * 5) Create a terminal declaration for each keyword and token symbols,
 *    and rewrite keyword and token symbols to nonterminal symbols, referring
 *    to the corresponding terminal definitions
 * 6) TODO: Extract subsequences of syntax symbols into a new
 *    nonterminal with a fresh name
 * 7) Parse pretemplates and replace with real templates
 * 8) Well-formedness check on template gaps
 */
public class GrammarRewriter {

    public static Collection<Api> rewriteApis(Map<APIName, ApiIndex> map, GlobalEnvironment env) {
//         Collection<ApiIndex> apis = new LinkedList<ApiIndex>();
//         apis.addAll(map.values());
//         /* why is adding all the env apis necessary? it does redudant work */
//         // apis.addAll(env.apis().values());
        initializeGrammarIndexExtensions(map.values(), env.apis().values());

        List<Api> apis = new ArrayList<Api>();
        for (ApiIndex apii : map.values()) { apis.add((Api)apii.ast()); }
        // for (ApiIndex apii : env.apis().values()) { apis.add((Api)apii.ast(); }

        List<Api> results = rewritePatterns(apis, env);
        List<Api> i2 = rewriteTransformerNames(results, env);
        List<Api> rs = parseTemplates(i2, env);
        return rs;
    }

    private static List<Api> rewritePatterns(List<Api> apis, GlobalEnvironment env) {
        ItemDisambiguator id = new ItemDisambiguator(env);
        List<Api> results = new ArrayList<Api>();

        for (Api api: apis) {
            // 1) Disambiguate item symbols and rewrite to either nonterminal,
            //    keyword or token symbol
            Api idResult = (Api) api.accept(id);
            if (id.errors().isEmpty()) {
                // 2) Disambiguate nonterminal parameters
                // No longer done.
                Api npdResult = idResult;

                // 3) Remove whitespace where instructed by non-whitespace symbols
                WhitespaceElimination we = new WhitespaceElimination();
                Api sdResult = (Api) npdResult.accept(we);

                // 4) Rewrite escaped characters
                EscapeRewriter escapeRewriter = new EscapeRewriter();
                Api erResult = (Api) sdResult.accept(escapeRewriter);

                // 5) Rewrite terminals to be declared using terminal definitions
                //TerminalRewriter terminalRewriter = new TerminalRewriter();
                // Api trResult = (Api) erResult.accept(terminalRewriter);

                results.add(erResult);
            }
        }
        return results;
    }

    private static List<Api> rewriteTransformerNames(List<Api> apis, GlobalEnvironment env) {
        List<Api> results = new ArrayList<Api>();
        for (Api api: apis) {
            Debug.debug(Debug.Type.SYNTAX, 1, "Name transformers in " + api.getName());
            RewriteTransformerNames collector = new RewriteTransformerNames();
            final Api transformed = (Api) api.accept(collector);
            results.add(transformed);
        }
        return results;
    }

    private static List<Api> parseTemplates(List<Api> apis, GlobalEnvironment env) {
        Collection<ApiIndex> apiIndexes = buildApiIndexesOnly(apis, env);
        NTEnv ntEnv = buildNTEnv(apiIndexes, env);

        List<Api> results = new ArrayList<Api>();
        for (final ApiIndex api : apiIndexes){
            results.add(TemplateParser.parseTemplates(api, ntEnv));
        }
        return results;
    }


    private static Collection<ApiIndex> buildApiIndexes(Collection<Api> apis, GlobalEnvironment env) {
        IndexBuilder.ApiResult apiN = IndexBuilder.buildApis(apis, System.currentTimeMillis() );
        initializeGrammarIndexExtensions(apiN.apis().values(), env.apis().values());
        return apiN.apis().values();
    }

    private static Collection<ApiIndex> buildApiIndexesOnly(Collection<Api> apis, GlobalEnvironment env) {
        IndexBuilder.ApiResult apiN = IndexBuilder.buildApis(apis, System.currentTimeMillis() );
        return apiN.apis().values();
    }

    private static NTEnv buildNTEnv(Collection<ApiIndex> apis, GlobalEnvironment env) {
        Map<String, GrammarIndex> grammars = 
            initializeGrammarIndexes(apis, env.apis().values());
        return EnvFactory.makeNTEnv(grammars.values());
    }

    private static void initializeGrammarIndexExtensions(Collection<ApiIndex> apis, 
                                                        Collection<ApiIndex> moreApis ) {
        initializeGrammarIndexes(apis, moreApis);
    }

    private static Map<String, GrammarIndex> initializeGrammarIndexes(Collection<ApiIndex> apis,
                                                                      Collection<ApiIndex> moreApis) {
        Map<String, GrammarIndex> grammars = new HashMap<String, GrammarIndex>();

        for (ApiIndex a2: moreApis) {
            for (Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                grammars.put(e.getKey(), e.getValue());
            }
        }
        for (ApiIndex a2: apis) {
            for (Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                grammars.put(e.getKey(), e.getValue());
            }
        }
        for (ApiIndex a1: apis) {
            for (Entry<String,GrammarIndex> e: a1.grammars().entrySet()) {
                GrammarDef og = e.getValue().ast();
                List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
                for (Id n: og.getExtends()) {
                    ls.add(grammars.get(n.getText()));
                }
                Debug.debug( Debug.Type.SYNTAX, 3, "Grammar " + e.getKey() + " extends " + ls );
                e.getValue().setExtended(ls);
            }
        }
        return grammars;
    }
}
