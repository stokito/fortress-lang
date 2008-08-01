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

import java.lang.reflect.Method;

import java.io.BufferedReader;

import xtc.parser.ParserBase;
import xtc.parser.SemanticValue;
import xtc.parser.ParseError;

import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.MacroError;

import com.sun.fortress.syntax_abstractions.rats.util.ParserMediator;

import com.sun.fortress.syntax_abstractions.parser.ImportedApiCollector;

import com.sun.fortress.syntax_abstractions.FileBasedMacroCompiler;
import com.sun.fortress.syntax_abstractions.MacroCompiler;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;

import com.sun.fortress.syntax_abstractions.ComposingMacroCompiler;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbsDecl;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NodeVisitor_void;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.TransformerDef;
import com.sun.fortress.nodes.TransformerNode;
import com.sun.fortress.nodes.TerminalDecl;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.GrammarIndexInitializer;
import com.sun.fortress.syntax_abstractions.MacroCompiler.Result;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Useful;

// import com.sun.fortress.tools.FortressAstToConcrete;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

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
 * 8) Well-formedness check on template gaps
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
        List<StaticError> errors = new LinkedList<StaticError>();
        Collection<ApiIndex> apis = new LinkedList<ApiIndex>();
        apis.addAll(map.values());
        /* why is adding all the env apis necessary? it does redudant work */
        // apis.addAll(env.apis().values());
        errors.addAll(initializeGrammarIndexExtensions(apis, env.apis().values()));

        List<Api> results = new ArrayList<Api>();
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
                //TerminalRewriter terminalRewriter = new TerminalRewriter();
                // Api trResult = (Api) erResult.accept(terminalRewriter);

                results.add(erResult);
            }
        }
        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR = IndexBuilder.buildApis(results, System.currentTimeMillis());
        if (!apiIR.isSuccessful()) { return new ApiResult(results, apiIR.errors()); }       
        initializeGrammarIndexExtensions(apiIR.apis().values(), env.apis().values());

        for (ApiIndex api: apiIR.apis().values()) { 
            initGrammarEnv(api.grammars().values());
        }

        List<Api> i2 = new ArrayList<Api>();
        for (ApiIndex api: apiIR.apis().values()){
            if ( containsGrammar( env, (Api)api.ast()) ){
                Debug.debug( Debug.Type.SYNTAX, 1, "Create parser for " + api.ast().getName() );
                RewriteTransformerNames collector = new RewriteTransformerNames();
                final Api transformed = (Api) api.ast().accept( collector );
                i2.add( transformed );
            } else {
                Debug.debug( Debug.Type.SYNTAX, 1, api.ast().getName() + " doesn't contains grammars" );
                i2.add( (Api) api.ast() );
            }
        }

        List<Api> rs = new ArrayList<Api>();
        IndexBuilder.ApiResult apiN = IndexBuilder.buildApis(i2, System.currentTimeMillis() );
        initializeGrammarIndexExtensions(apiN.apis().values(), env.apis().values());
        for ( final ApiIndex api : apiN.apis().values() ){
                // List<String> names = collector.getNames();
                // Debug.debug( Debug.Type.SYNTAX, 1, "Syntax transformers for " + api.ast().getName() + ": " + names );

            final Api raw = (Api) api.ast().accept( new TemplateParser() );

            // FIXME: Eliminate side effects
            final Option<Class<?>>[] parser = new Option[1];
            parser[0] = Option.none();
            rs.add( (Api) raw.accept( new NodeUpdateVisitor(){
                private GrammarIndex findGrammar( GrammarDef grammar ){
                    for ( GrammarIndex index : api.grammars().values() ){
                        if ( index.getName().equals( grammar.getName() ) ){
                            return index;
                        }
                    }
                    throw new MacroError( "Could not find grammar for " + grammar.getName() );
                }

                @Override public Node forGrammarDef(GrammarDef that) {
                    if ( ! that.isNative() ){
                        parser[ 0 ] = Option.<Class<?>>some(createParser( findGrammar(that) ));
                        return super.forGrammarDef(that);
                    } else {
                        return that;
                    }
                }

                @Override public Node forTransformerDef(TransformerDef that) {
                    try {
                        AbstractNode templateNode = 
                            parseTemplate( raw.getName(), that.getDef(), parser[ 0 ].unwrap() );
                        return new TransformerNode(that.getTransformer(), templateNode, that.getParameters() );
                    } catch ( OptionUnwrapException e ){
                        throw StaticError.make( "No parser created while rewriting api " + raw, "" );
                    }
                }
            }));
        }

        /*
        for (ApiIndex api: apiIR.apis().values()) {
            rs.add( (Api) api.ast() );
        }
        */

        /*
        for (ApiIndex api: apiIR.apis().values()) {
            // 7) Parse content of pretemplates and replace pretemplate 
            // with a real template

            //  Api transfomerApi = TransformerParser.parseTemplates(api.ast());

            TemplateParser.Result tpr = TemplateParser.parseTemplates((Api)api.ast());
            for (StaticError se: tpr.errors()) { errors.add(se); };
            if (!tpr.isSuccessful()) { return new ApiResult(rs, errors); }
            
            rebuildGrammarEnv(tpr.api());
            
            // 8) Well-formedness check on template gaps
            TemplateChecker.Result tcr = TemplateChecker.checkTemplates(env, api, tpr.api());
            for (StaticError se: tcr.errors()) { errors.add(se); };
            if (!tcr.isSuccessful()) { return new ApiResult(rs, errors); }
            rs.add(tcr.api());
        }
        */

        return new ApiResult(rs, errors);
    }

    /* TODO: implement this */
    private static boolean containsGrammar( GlobalEnvironment env, Api api ){
        ImportedApiCollector collector = new ImportedApiCollector(env);
        collector.collectApis(api);
        return (! collector.getGrammars().isEmpty()) ||
               api.accept( new NodeDepthFirstVisitor<Boolean>(){
                   private boolean answer = false;
                   @Override public Boolean defaultCase(Node that) {
                       return answer;
                   }
                   @Override public Boolean forGrammarDef(GrammarDef that){
                       answer = true;
                       return answer;
                   }
               });
    }

    private static Option<Method> lookupExpression(Class parser, String production){
        try{
            /* This is a Rats! specific naming convention. Move it
             * elsewhere?
             */
            String fullName = production;
            // String fullName = "pExprOnly";
            Method found = parser.getDeclaredMethod(fullName, int.class);

            /* method is private by default so we have to make
             * it accessible
             */
            if ( found != null ){
                found.setAccessible(true);
                return Option.wrap(found);
            }
            return Option.none();
        } catch (NoSuchMethodException e){
            throw new MacroError(e);
        } catch (SecurityException e){
            throw new MacroError(e);
        }
    }

    private static Object invokeMethod( Object obj, String name ){
        Option<Method> method = lookupExpression( obj.getClass(), name );
        if ( ! method.isSome() ){
            throw new MacroError( "Could not find method " + name + " in " + obj.getClass().getName() );
        } else {
            try{
                return (xtc.parser.Result) method.unwrap().invoke(obj, 0);
            } catch (IllegalAccessException e){
                throw new MacroError(e);
            } catch (java.lang.reflect.InvocationTargetException e){
                throw new MacroError(e);
            }
        }
    }

    private static AbstractNode parseTemplate( APIName apiName, String stuff, Class<?> parserClass ){
        try{
            BufferedReader in = Useful.bufferedStringReader(stuff.trim());
            Debug.debug( Debug.Type.SYNTAX, 3, "Parsing template '" + stuff + "'" );
            ParserBase parser = ParserMediator.getParser( apiName, parserClass, in, apiName.toString() );
            xtc.parser.Result result = (xtc.parser.Result) invokeMethod( parser, "pExpression$Expr" );
            // xtc.parser.Result result = ParserMediator.parse( parser, "Expression$Expr" );
            if ( result.hasValue() ){
                Object node = ((SemanticValue) result).value;
                Debug.debug( Debug.Type.SYNTAX, 2, "Parsed '" + stuff + "' as node " + node );
//                 Debug.debug( Debug.Type.SYNTAX, 3,
//                              "Template body is: " + 
//                              FortressAstToConcrete.astToString((Node)node));
                return (AbstractNode) node;
            } else {
                throw new ParserError((ParseError) result, parser);
            }
        } catch ( Exception e ){
            throw new MacroError( "Could not parse '" + stuff + "'", e );
        }
    }

    private static SyntaxTransformer createTransformer( SyntaxTransformerCreater creater, APIName apiName, Class<?> parserClass, String def, SyntaxDeclEnv env ){
        try{
            BufferedReader in = Useful.bufferedStringReader(def);
            ParserBase parser = ParserMediator.getParser( apiName, parserClass, in, apiName.toString() );
            xtc.parser.Result result = (xtc.parser.Result) invokeMethod( parser, "pExpression$Expr" );
            // xtc.parser.Result result = ParserMediator.parse( parser, "Expression$Expr" );
            if ( result.hasValue() ){
                Object node = ((SemanticValue) result).value;
                return creater.create( (Node) node, env );
            } else {
                throw new ParserError((ParseError) result, parser);
            }
        } catch ( Exception e ){
            throw new MacroError( "Could not create transformer for '" + def + "'", e );
        }
    }

    private static Class<?> createParser( GrammarIndex grammar ){
        // Compile the syntax abstractions and create a temporary parser
        
        Class<?> temporaryParserClass = ComposingMacroCompiler.compile( grammar );
        return temporaryParserClass;

        /*
        MacroCompiler macroCompiler = new FileBasedMacroCompiler();
        ImportedApiCollector collector = new ImportedApiCollector(env);
        collector.collectApis((Api)api.ast());
        Collection<GrammarIndex> grammars = collector.getGrammars();
        for ( GrammarIndex g : api.grammars().values() ){
            g.isToplevel(true);
        }
        grammars.addAll( api.grammars().values() );
        MacroCompiler.Result tr = macroCompiler.compile(grammars, env);
        // if (!tr.isSuccessful()) { return new Result(tr.errors()); }
        if ( ! tr.isSuccessful() ){
            throw new MultipleStaticError( tr.errors() );
        }

        Class<?> temporaryParserClass = tr.getParserClass(); 
        Debug.debug( Debug.Type.SYNTAX, 2, "Created temporary parser" );
        return temporaryParserClass;
        */

        /*
        BufferedReader in = null; 
        try {
            in = Useful.utf8BufferedFileReader(f);
            ParserBase p =
                ParserMediator.getParser(api_name, temporaryParserClass, in, f.toString());
            CompilationUnit original = Parser.checkResultCU(ParserMediator.parse(), p, f.getName());
        } catch ( IOException e ){
        }
        */
    }

    /*
    private static Class<?> createParser( ApiIndex api, GlobalEnvironment env ){
        // Compile the syntax abstractions and create a temporary parser
        MacroCompiler macroCompiler = new FileBasedMacroCompiler();
        ImportedApiCollector collector = new ImportedApiCollector(env);
        collector.collectApis((Api)api.ast());
        Collection<GrammarIndex> grammars = collector.getGrammars();
        for ( GrammarIndex g : api.grammars().values() ){
            g.isToplevel(true);
        }
        grammars.addAll( api.grammars().values() );
        MacroCompiler.Result tr = macroCompiler.compile(grammars, env);
        // if (!tr.isSuccessful()) { return new Result(tr.errors()); }
        if ( ! tr.isSuccessful() ){
            throw new MultipleStaticError( tr.errors() );
        }

        Class<?> temporaryParserClass = tr.getParserClass(); 
        Debug.debug( Debug.Type.SYNTAX, 2, "Created temporary parser" );
        return temporaryParserClass;

        / *
        BufferedReader in = null; 
        try {
            in = Useful.utf8BufferedFileReader(f);
            ParserBase p =
                ParserMediator.getParser(api_name, temporaryParserClass, in, f.toString());
            CompilationUnit original = Parser.checkResultCU(ParserMediator.parse(), p, f.getName());
        } catch ( IOException e ){
        }
        * /
    }
    */
    
    public static Collection<? extends StaticError> initializeGrammarIndexExtensions(Collection<ApiIndex> apis, Collection<ApiIndex> moreApis ) {
        List<StaticError> errors = new LinkedList<StaticError>();
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
                Option<GrammarDef> og = e.getValue().ast();
                if (og.isSome()) {
                    List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
                    for (Id n: og.unwrap().getExtends()) {
                        ls.add(grammars.get(n.getText()));
                    }
                    Debug.debug( Debug.Type.SYNTAX, 3, "Grammar " + e.getKey() + " extends " + ls );
                    e.getValue().setExtended(ls);
                } else {
                    Debug.debug( Debug.Type.SYNTAX, 3, "Grammar " + e.getKey() + " has no ast" );
                }
            }
        }
        return errors;
    }
    
    private static void initGrammarEnv(Collection<GrammarIndex> grammarIndexs) {
        for (GrammarIndex g: grammarIndexs) {
            GrammarEnv.add(g);
        }        
    }

    private static void rebuildGrammarEnv(Api api) {
        api.accept(new NodeDepthFirstVisitor_void() {
            
            @Override
            public void forNonterminalDef(NonterminalDef that) {
                MemberEnv mEnv = GrammarEnv.getMemberEnv(that.getHeader().getName());
                mEnv.rebuildSyntaxDeclEnvs(that.getSyntaxDefs());
            }

            @Override
            public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                MemberEnv mEnv = GrammarEnv.getMemberEnv(that.getHeader().getName());
                mEnv.rebuildSyntaxDeclEnvs(that.getSyntaxDefs());
            }

            @Override
            public void for_TerminalDef(_TerminalDef that) {
                MemberEnv mEnv = GrammarEnv.getMemberEnv(that.getHeader().getName());
                mEnv.rebuildSyntaxDeclEnvs(FortressUtil.mkList((SyntaxDecl)that.getSyntaxDef()));
            }
            
        });
    }

}
