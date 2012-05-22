/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.useful.Debug;

import java.util.*;
import java.util.Map.Entry;

/**
 * Syntax abstraction entry point:
 * Part of front-end processing of APIs.
 * <p/>
 * 1) Disambiguate item symbols and rewrite to either nonterminal,
 * keyword or token symbol
 * 2) Disambiguate nonterminal parameters (Not any more)
 * We need to support nonterminal parameters to express
 * desugaring of Fortress comprehensions?
 * In order to support it,
 * the parameters should be handled by TemplateVarRewriter.identifierLoop
 * and Transform should handle it appropriately.
 * 3) Remove whitespace where indicated by no-whitespace symbols
 * 4) Rewrite escaped symbols
 * 5) Desugar extensions: fill in unmentioned imported grammars
 * and collect multiple extensions of same nonterminal together.
 * 6) Name transformers
 * 7) Parse pretemplates and replace with real templates
 * 8) Well-formedness check on template gaps (???)
 */
public class GrammarRewriter {

    public static Collection<Api> rewriteApis(Map<APIName, ApiIndex> map, GlobalEnvironment env) {
        initializeGrammarIndexExtensions(map.values(), env.apis().values());

        List<Api> apis = new ArrayList<Api>();
        for (ApiIndex apii : map.values()) {
            //            Nodes.printNode(apii.ast(), "before-grammar-rewrite.");
            apis.add((Api) apii.ast());
        }

        // Steps 1-6
        List<Api> results1 = rewritePatterns(apis, env);

        // Step 7
        List<Api> results2 = parseTemplates(results1, env);

        return results2;
    }

    private static List<Api> rewritePatterns(List<Api> apis, GlobalEnvironment env) {
        List<Api> results = new ArrayList<Api>();
        List<StaticError> allErrors = new ArrayList<StaticError>();

        for (Api api : apis) {
            //             Nodes.printNode(api, "before-rewritePatterns.");
            List<StaticError> errors = new ArrayList<StaticError>();

            // 1) Disambiguate item symbols and rewrite to either nonterminal,
            //    keyword or token symbol
            api = (Api) api.accept(new ItemDisambiguator(env, errors));
            //             Nodes.printNode(api, "after-ItemDisambiguator.");
            // 2) Disambiguate nonterminal parameters
            // No longer done.

            // 3) Remove whitespace where instructed by non-whitespace symbols
            if (errors.isEmpty()) api = (Api) api.accept(new WhitespaceElimination());
            //             Nodes.printNode(api, "after-WhitespaceElimination.");
            // 4) Rewrite escaped characters
            if (errors.isEmpty()) api = (Api) api.accept(new EscapeRewriter());
            //             Nodes.printNode(api, "after-EscapeRewriter.");
            // 5) Desugar extensions
            if (errors.isEmpty()) api = (Api) api.accept(new ExtensionDesugarer(env, errors));
            //             Nodes.printNode(api, "after-ExtensionDesugarer.");
            // 6) Rewrite transformer names
            if (errors.isEmpty()) api = (Api) api.accept(new RewriteTransformerNames());
            //             Nodes.printNode(api, "after-RewriteTransformerNames.");

            if (errors.isEmpty()) {
                results.add(api);
            } else {
                allErrors.addAll(errors);
            }
            //             Nodes.printNode(api, "after-rewritePatterns.");
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
        for (final ApiIndex api : apiIndexes) {
            //            Nodes.printNode(api.ast(), "before-parseTemplates.");
            results.add(TemplateParser.parseTemplates(api, ntEnv));
            //            Nodes.printNode(api.ast(), "after-parseTemplates.");
        }
        return results;
    }

    private static Collection<ApiIndex> buildApiIndexesOnly(Collection<Api> apis, GlobalEnvironment env) {
        IndexBuilder.ApiResult apiN = IndexBuilder.buildApis(apis, env, System.currentTimeMillis());
        return apiN.apis().values();
    }

    private static NTEnv buildNTEnv(Collection<ApiIndex> apis, GlobalEnvironment env) {
        Map<String, GrammarIndex> grammars = initializeGrammarIndexExtensions(apis, env.apis().values());
        return EnvFactory.makeNTEnv(grammars.values());
    }

    private static Map<String, GrammarIndex> initializeGrammarIndexExtensions(Collection<ApiIndex> apis,
                                                                              Collection<ApiIndex> moreApis) {
        Map<String, GrammarIndex> grammars = new HashMap<String, GrammarIndex>();

        for (ApiIndex a2 : moreApis) {
            for (Entry<String, GrammarIndex> e : a2.grammars().entrySet()) {
                grammars.put(e.getKey(), e.getValue());
                Debug.debug(Debug.Type.SYNTAX, 3, "Add grammar ", e.getKey(), " to ", e.getValue());
            }
        }
        for (ApiIndex a1 : apis) {
            for (Entry<String, GrammarIndex> e : a1.grammars().entrySet()) {
                grammars.put(e.getKey(), e.getValue());
                Debug.debug(Debug.Type.SYNTAX, 3, "Add grammar ", e.getKey(), " to ", e.getValue());
            }
        }
        for (ApiIndex a1 : apis) {
            for (Entry<String, GrammarIndex> e : a1.grammars().entrySet()) {
                GrammarDecl og = e.getValue().ast();
                List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
                for (Id n : og.getExtendsClause()) {
                    GrammarIndex index = grammars.get(n.getText());
                    if (index == null){
                        throw new MacroError("Could not find grammar " + n + " in the extends clause of " + og);
                    }
                    ls.add(index);
                }
                Debug.debug(Debug.Type.SYNTAX, 3, "Grammar ", e.getKey(), " extends ", ls);
                e.getValue().setExtended(ls);
            }
        }
        return grammars;
    }
}
