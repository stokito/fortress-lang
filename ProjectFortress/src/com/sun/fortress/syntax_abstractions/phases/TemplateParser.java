/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.syntax_abstractions.ParserMaker;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Files;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;
import xtc.parser.ParseError;
import xtc.parser.ParserBase;
import xtc.parser.SemanticValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;

/*
 * Parse pretemplates and replace with real templates
 */
public class TemplateParser {

    public static Api parseTemplates(final ApiIndex api, final NTEnv ntEnv) {
        final Api raw = rewriteTemplateVars((Api) api.ast(), ntEnv);
        return (Api) raw.accept(new NodeUpdateVisitor() {
            @Override
            public Node forGrammarDecl(GrammarDecl thatGrammarDecl) {
                if (!thatGrammarDecl.isNativeDef()) {
                    final Class<?> parser = createParser(findGrammar(thatGrammarDecl));
                    return thatGrammarDecl.accept(new NodeUpdateVisitor() {
                        @Override
                        public Node forUnparsedTransformer(UnparsedTransformer that) {
                            AbstractNode templateNode = parseTemplate(raw.getName(),
                                                                      that.getTransformer(),
                                                                      that.getNonterminal(),
                                                                      parser);
                            return new NodeTransformer(templateNode.getInfo(), templateNode);
                        }
                    });
                } else {
                    return thatGrammarDecl;
                }
            }

            private GrammarIndex findGrammar(GrammarDecl grammar) {
                for (GrammarIndex index : api.grammars().values()) {
                    if (index.getName().equals(grammar.getName())) {
                        return index;
                    }
                }
                throw new MacroError("Could not find grammar for " + grammar.getName());
            }
        });
    }

    private static Api rewriteTemplateVars(Api api, final NTEnv ntEnv) {
        return (Api) api.accept(new NodeUpdateVisitor() {
            @Override
            public Node forSyntaxDef(SyntaxDef thatSyntaxDef) {
                final GapEnv gapEnv = EnvFactory.makeGapEnv(thatSyntaxDef, ntEnv);
                Debug.debug(Debug.Type.SYNTAX, 3, "Gap env: ", gapEnv);
                return thatSyntaxDef.accept(new NodeUpdateVisitor() {
                    @Override
                    public Node forNamedTransformerDef(NamedTransformerDef that) {
                        TemplateVarRewriter tvs = new TemplateVarRewriter(gapEnv);
                        Transformer transformer = (Transformer) that.getTransformer().accept(tvs);
                        return new NamedTransformerDef(that.getInfo(),
                                                       that.getName(),
                                                       that.getParameters(),
                                                       transformer);
                    }
                });
            }
        });
    }

    private static Class<?> createParser(GrammarIndex grammar) {
        return ParserMaker.parserForGrammar(grammar);
    }

    private static AbstractNode parseTemplate(APIName apiName, String stuff, Id nonterminal, Class<?> parserClass) {
        try {
            BufferedReader in = Useful.bufferedStringReader(stuff.trim());
            Debug.debug(Debug.Type.SYNTAX, 3, "Parsing template '" + stuff + "' with nonterminal " + nonterminal);
            ParserBase parser = RatsUtil.getParserObject(parserClass, in, NodeUtil.getSpan(apiName).getFileName());
            xtc.parser.Result result = (xtc.parser.Result) invokeMethod(parser, ratsParseMethod(nonterminal));
            if (result.hasValue()) {
                Object node = ((SemanticValue) result).value;
                Debug.debug(Debug.Type.SYNTAX, 2, "Parsed '", stuff, "' as node ", node);
                return (AbstractNode) node;
            } else {
                throw new ParserError((ParseError) result, parser);
            }
        }
        catch (Exception e) {
            throw new MacroError("Could not parse '" + stuff + "'", e);
        }
        finally {
            try {
                Files.rm(ProjectProperties.macroErrorLog(NodeUtil.getSpan(apiName).getFileName() ));
            } catch (IOException e) {}
        }
    }

    private static Object invokeMethod(Object obj, String name) {
        Option<Method> method = lookupExpression(obj.getClass(), name);
        if (method.isNone()) {
            throw new MacroError("Could not find method " + name + " in " + obj.getClass().getName());
        } else {
            try {
                return (xtc.parser.Result) method.unwrap().invoke(obj, 0);
            }
            catch (IllegalAccessException e) {
                throw new MacroError(e);
            }
            catch (java.lang.reflect.InvocationTargetException e) {
                throw new MacroError(e);
            }
        }
    }

    private static Option<Method> lookupExpression(Class<?> parser, String production) {
        try {
            /* This is a Rats! specific naming convention.
             * Move it elsewhere?
             */
            String fullName = production; // for example, "pExprOnly"
            Method found = parser.getDeclaredMethod(fullName, int.class);
            /* method is private by default so we have to make it accessible */
            if (found != null) {
                found.setAccessible(true);
                return Option.wrap(found);
            } else {
                return Option.none();
            }
        }
        catch (NoSuchMethodException e) {
            throw new MacroError(e);
        }
        catch (SecurityException e) {
            throw new MacroError(e);
        }
    }

    private static String ratsParseMethod(Id nonterminal) {
        String str = nonterminal.toString();
        if (str.startsWith("FortressSyntax")) {
            return "p" + str.substring(str.indexOf(".") + 1).replace('.', '$');
        } else {
            return "pUSER_" + str.replace("_", "__").replace('.', '_');
        }
    }
}
