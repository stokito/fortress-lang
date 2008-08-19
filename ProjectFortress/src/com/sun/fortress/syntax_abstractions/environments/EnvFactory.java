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
import com.sun.fortress.exceptions.MacroError;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.CharacterClassSymbol;
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

public class EnvFactory {

    public static NTEnv makeNTEnv(Collection<GrammarIndex> grammarIndexes) {
        Map<Id, BaseType> typemap = new HashMap<Id, BaseType>();
        for (GrammarIndex gi : grammarIndexes) {
            for (NonterminalIndex<? extends GrammarMemberDecl> ni : gi.getDeclaredNonterminals()) {
                if (!ni.ast().isSome()) continue;
                if (ni.ast().unwrap() instanceof NonterminalDef) {
                    NonterminalDef nd = (NonterminalDef) ni.ast().unwrap();
                    Id name = ni.getName();
                    Option<BaseType> type = nd.getAstType();
                    if (type.isSome()) {
                        typemap.put(name, type.unwrap());
                    } else {
                        throw new RuntimeException("No type for nonterminal " + ni);
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
                        Option<Id> optName = that.getId();
                        if (!optName.isSome()) {
                            throw new MacroError("Prefix symbol without name: " + that);
                        }
                        final Id name = optName.unwrap();
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
}
