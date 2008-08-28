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
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.useful.Debug;

/**
 * Syntax abstraction entry point:
 * Part of front-end processing of APIs.
 *
 * 1) Disambiguate item symbols and rewrite to either nonterminal,
 *    keyword or token symbol
 * 2) Disambiguate nonterminal parameters
 * 3) Remove whitespace where indicated by no-whitespace symbols
 * 4) Rewrite escaped symbols
 * 5) Desugar extensions: fill in unmentioned imported grammars
 *    and collect multiple extensions of same nonterminal together.
 * 6) Name transformers
 * 6) Parse pretemplates and replace with real templates
 * 7) Well-formedness check on template gaps (???)
 */
public class GrammarRewriter {

    public static Collection<Api> rewriteApis(Map<APIName, ApiIndex> map, GlobalEnvironment env) {
        initializeGrammarIndexExtensions(map.values(), env.apis().values());

        List<Api> apis = new ArrayList<Api>();
        for (ApiIndex apii : map.values()) { apis.add((Api)apii.ast()); }

        // Steps 1-6
        List<Api> results1 = rewritePatterns(apis, env);

        // Step 7
        List<Api> results2 = parseTemplates(results1, env);

        return results2;
    }

    private static List<Api> rewritePatterns(List<Api> apis, GlobalEnvironment env) {
        List<Api> results = new ArrayList<Api>();
        List<StaticError> allErrors = new ArrayList<StaticError>();

        for (Api api: apis) {
            List<StaticError> errors = new ArrayList<StaticError>();

            // 1) Disambiguate item symbols and rewrite to either nonterminal,
            //    keyword or token symbol
            api = (Api) api.accept(new ItemDisambiguator(env, errors));

            // 2) Disambiguate nonterminal parameters
            // No longer done.

            // 3) Remove whitespace where instructed by non-whitespace symbols
            if (errors.isEmpty()) 
                api = (Api) api.accept(new WhitespaceElimination());

            // 4) Rewrite escaped characters
            if (errors.isEmpty())
                api = (Api) api.accept(new EscapeRewriter());

            // 5) Desugar extensions
            if (errors.isEmpty())
                api = (Api) api.accept(new ExtensionDesugarer(env, errors));

            // 6) Rewrite transformer names
            if (errors.isEmpty())
                api = (Api) api.accept(new RewriteTransformerNames());

            if (errors.isEmpty()) {
                results.add(api);
            } else {
                allErrors.addAll(errors);
            }
        }

        if (allErrors.isEmpty()) {
            return results;
        } else {
            throw new MultipleStaticError(allErrors);
        }
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

    private static Collection<ApiIndex> buildApiIndexesOnly(Collection<Api> apis, 
                                                            GlobalEnvironment env) {
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
