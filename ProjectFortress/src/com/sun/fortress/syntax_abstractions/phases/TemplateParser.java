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

import java.io.BufferedReader;
import java.lang.reflect.Method;

import xtc.parser.ParserBase;
import xtc.parser.ParseError;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NodeTransformer;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.NamedTransformerDef;
import com.sun.fortress.nodes.Transformer;
import com.sun.fortress.nodes.UnparsedTransformer;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.ParserMaker;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.syntax_abstractions.rats.util.ParserMediator;

import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

/*
 * Parse pretemplates and replace with real templates
 */
public class TemplateParser {

    public static Api parseTemplates(final ApiIndex api, final NTEnv ntEnv) {
        final Api raw = TemplateParser.rewriteTemplateVars((Api) api.ast(), ntEnv);
        return (Api) raw.accept(new NodeUpdateVisitor() {
                @Override public Node forGrammarDef(GrammarDef that) {
                    if (!that.isNative()){
                        final Class<?> parser = createParser(findGrammar(that));
                        return that.accept(new NodeUpdateVisitor() {
                                @Override 
                                    public Node forUnparsedTransformer(UnparsedTransformer that) {
                                    AbstractNode templateNode = 
                                        parseTemplate(raw.getName(), 
                                                      that.getTransformer(), 
                                                      that.getNonterminal(), 
                                                      parser);
                                    return new NodeTransformer(NodeFactory.makeSpan(templateNode), templateNode);
                                }
                            });
                    } else {
                        return that;
                    }
                }
                private GrammarIndex findGrammar( GrammarDef grammar ){
                    for (GrammarIndex index : api.grammars().values()) {
                        if (index.getName().equals(grammar.getName())) {
                            return index;
                        }
                    }
                    throw new MacroError("Could not find grammar for " + 
                                         grammar.getName());
                }
            });
    }

    private static Api rewriteTemplateVars(Api api, final NTEnv ntEnv) {
        return (Api) api.accept(new NodeUpdateVisitor() {
                @Override public Node forSyntaxDef(SyntaxDef that) {
                    final GapEnv gapEnv = EnvFactory.makeGapEnv(that, ntEnv);
                    Debug.debug(Debug.Type.SYNTAX, 3, "Gap env: " + gapEnv);
                    return that.accept(new NodeUpdateVisitor() {
                            @Override public Node forNamedTransformerDef(NamedTransformerDef that) {
                                TemplateVarRewriter tvs = new TemplateVarRewriter(gapEnv);
                                Transformer transformer = 
                                    (Transformer) that.getTransformer().accept(tvs);
                                return new NamedTransformerDef(NodeFactory.makeSpan(that), that.getName(), 
                                                               that.getParameters(), 
                                                               transformer);
                            }
                        });
                }
            });
    }

    private static Class<?> createParser(GrammarIndex grammar){
        return ParserMaker.parserForGrammar(grammar);
    }

    private static AbstractNode parseTemplate(APIName apiName, String stuff, 
                                              Id nonterminal, Class<?> parserClass){
        try {
            BufferedReader in = Useful.bufferedStringReader(stuff.trim());
            Debug.debug(Debug.Type.SYNTAX, 3,
                        "Parsing template '" + stuff + "' with nonterminal " + nonterminal );
            ParserBase parser = 
                ParserMediator.getParser(apiName, parserClass, in, apiName.toString());
            xtc.parser.Result result =
                (xtc.parser.Result) invokeMethod(parser, ratsParseMethod(nonterminal));
            if (result.hasValue()){
                Object node = ((SemanticValue) result).value;
                Debug.debug( Debug.Type.SYNTAX, 2, "Parsed '" + stuff + "' as node " + node );
                return (AbstractNode) node;
            } else {
                throw new ParserError((ParseError) result, parser);
            }
        } catch (Exception e) {
            throw new MacroError( "Could not parse '" + stuff + "'", e );
        }
    }

    private static Object invokeMethod( Object obj, String name ){
        Option<Method> method = lookupExpression(obj.getClass(), name);
        if ( ! method.isSome() ){
            throw new MacroError("Could not find method " + name + 
                                 " in " + obj.getClass().getName());
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

    private static String ratsParseMethod( Id nonterminal ){
        String str = nonterminal.toString();
        if ( str.startsWith( "FortressSyntax" ) ){
            return "p" + str.substring( str.indexOf(".") + 1 ).replace( '.', '$' );
        } else {
            return "pUSER_" + str.replaceAll("_", "__").replace('.', '_');
        }
    }

    private static Option<Method> lookupExpression(Class<?> parser, String production){
        try {
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
            } else {
                return Option.none();
            }
        } catch (NoSuchMethodException e){
            throw new MacroError(e);
        } catch (SecurityException e){
            throw new MacroError(e);
        }
    }


}
