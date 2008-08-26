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

package com.sun.fortress.syntax_abstractions.environments;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import com.sun.fortress.exceptions.MacroError;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.compiler.index.NonterminalDefIndex;

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.phases.VariableCollector;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.useful.Debug;

public class EnvFactory {

    public static NTEnv makeNTEnv(Collection<GrammarIndex> grammarIndexes) {
        Map<Id, BaseType> typemap = new HashMap<Id, BaseType>();
        for (GrammarIndex gi : grammarIndexes) {
            for (NonterminalIndex ni : gi.getDeclaredNonterminals()) {
                if (ni instanceof NonterminalDefIndex) {
                    NonterminalDef nd = ((NonterminalDefIndex) ni).ast();
                    Id name = ni.getName();
                    Option<BaseType> type = nd.getAstType();
                    if (type.isSome()) {
                        typemap.put(name, type.unwrap());
                    } else {
                        throw new MacroError(nd, "No type for nonterminal " + ni);
                    }
                }
            }
        }
        return new NTEnv(typemap);
    }

    public static GapEnv makeGapEnv(SyntaxDef def, NTEnv ntEnv) {
        final Map<Id, Id> varToNT = new HashMap<Id,Id>();
        final Set<Id> varHasJavaStringType = new HashSet<Id>();

        final Map<Id, Depth> varToDepth = new HashMap<Id, Depth>();
        for (SyntaxSymbol sym : def.getSyntaxSymbols()) {
            sym.accept(new VariableCollector(varToDepth));
            sym.accept(new NodeDepthFirstVisitor_void() {
                    @Override public void forPrefixedSymbolOnly(PrefixedSymbol that) {
                        final Id name = that.getId();
                        SyntaxSymbol inner = that.getSymbol();
                        inner.accept(new NodeDepthFirstVisitor_void() {
                                @Override
                                public void forNonterminalSymbol(NonterminalSymbol that) {
                                    varToNT.put(name, that.getNonterminal());
                                }
                                @Override
                                public void forAnyCharacterSymbol(AnyCharacterSymbol that) {
                                    varHasJavaStringType.add(name);
                                }
                                @Override
                                public void forCharacterClassSymbol(CharacterClassSymbol that) {
                                    varHasJavaStringType.add(name);
                                }
                                @Override
                                public void forKeywordSymbol(KeywordSymbol that) {
                                    varHasJavaStringType.add(name);
                                }
                                @Override
                                public void forTokenSymbol(TokenSymbol that) {
                                    varHasJavaStringType.add(name);
                                }
                            });
                    }
                });
        }
        return new GapEnv(ntEnv, varToDepth, varToNT, varHasJavaStringType);
    }

    public static NTEnv makeTestingNTEnv(Map<Id, BaseType> ntToType) {
        return new NTEnv(ntToType);
    }

    public static GapEnv makeTestingGapEnv(NTEnv ntEnv, Map<Id, Depth> varToDepth, 
                                           Map<Id, Id> varToNT, Set<Id> stringVars) {
        return new GapEnv(ntEnv, varToDepth, varToNT, stringVars);
    }

    public static void initializeGrammarIndexExtensions(Collection<ApiIndex> apis, 
                                                        Collection<GrammarIndex> grammars) {
        Map<String, GrammarIndex> grammarMap = new HashMap<String, GrammarIndex>();
        for (ApiIndex a2: apis) {
            for (Map.Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                grammarMap.put(e.getKey(), e.getValue());
            }
        }

        for ( GrammarIndex grammar : grammars ){
            GrammarDef og = grammar.ast();
            List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
            for (Id n: og.getExtends()) {
                Debug.debug(Debug.Type.SYNTAX, 3, "Add grammar " + n.getText() + 
                            "[" + grammarMap.get(n.getText()) +
                            "] to the extends list of " + grammar );
                ls.add(grammarMap.get(n.getText()));
            }
            Debug.debug(Debug.Type.SYNTAX, 3, 
                        "Grammar " + grammar.getName() + " extends " + ls);
            grammar.setExtended(ls);
        }
    }
}
