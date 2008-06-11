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
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NodeVisitor_void;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.syntax_abstractions.GrammarIndexInitializer;
import com.sun.fortress.syntax_abstractions.MacroCompiler.Result;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

/*
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
        List<StaticError> errors = new LinkedList<StaticError>();
        ItemDisambiguator id = new ItemDisambiguator(env);
        errors.addAll(id.errors());
        
        for (ApiIndex api: apis) {
            // 1) Disambiguate item symbols and rewrite to either nonterminal,
            //    keyword or token symbol
            Api idResult = (Api) api.ast().accept(id);
            if (id.errors().isEmpty()) {
                // 2) Disambiguate nonterminal parameters
                NonterminalParameterDisambiguator npd = new NonterminalParameterDisambiguator(env);
                Api npdResult = (Api) idResult.accept(npd);
                errors.addAll(npd.errors());
                
                // 3) Remove whitespace where instructed by non-whitespace symbols
                WhitespaceElimination we = new WhitespaceElimination();
                Api sdResult = (Api) npdResult.accept(we);
                
                // 4) Rewrite escaped characters
                EscapeRewriter escapeRewriter = new EscapeRewriter();
                Api erResult = (Api) sdResult.accept(escapeRewriter);

                // 5) Rewrite terminals to be declared using terminal definitions
                TerminalRewriter terminalRewriter = new TerminalRewriter();
                Api trResult = (Api) erResult.accept(terminalRewriter);

                results.add(trResult);
            }
        }
        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR = IndexBuilder.buildApis(results, System.currentTimeMillis());
        if (!apiIR.isSuccessful()) { return new ApiResult(results, apiIR.errors()); }       
        initializeGrammarIndexExtensions(apiIR.apis().values());
                
        List<Api> rs = new ArrayList<Api>();
        for (ApiIndex api: apiIR.apis().values()) { 
            initGrammarEnv(api.grammars().values());
        }
               
        for (ApiIndex api: apiIR.apis().values()) {
            // 7) Parse content of pretemplates and replace pretemplate 
            // with a real template
            TemplateParser.Result tpr = TemplateParser.parseTemplates((Api)api.ast());
            for (StaticError se: tpr.errors()) { errors.add(se); };
            if (!tpr.isSuccessful()) { return new ApiResult(rs, errors); }
            rs.add(tpr.api);
        }

        return new ApiResult(rs, errors);
    }
    
    private static void initializeGrammarIndexExtensions(Collection<ApiIndex> apis) {
        Map<String, GrammarIndex> grammars = new HashMap<String, GrammarIndex>();
        for (ApiIndex a2: apis) {
            for (Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                grammars.put(e.getKey(), e.getValue());
            }
        }
        for (ApiIndex a1: apis) {
            for (Entry<String,GrammarIndex> e: a1.grammars().entrySet()) {
                Option<GrammarDef> og = e.getValue().ast();
                if (og.isSome()) {
                    List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
                    for (Id n: og.unwrap().getExtends()) {
                        ls.add(grammars.get(n.getText()));
                    }
                    e.getValue().setExtended(ls);
                }
            }
        }
    }
    
    private static void initGrammarEnv(Collection<GrammarIndex> grammarIndexs) {
        for (GrammarIndex g: grammarIndexs) {
            GrammarEnv.add(g);
        }        
    }

}
