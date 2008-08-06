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

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.intermediate.SyntaxSymbolPrinter;
import com.sun.fortress.syntax_abstractions.util.TypeCollector;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;

public class ComposingSyntaxDeclEnv {

    private final Map<Id, Id> varToNT;
    private final Set<Id> varHasJavaStringType;

    public ComposingSyntaxDeclEnv(SyntaxDef def) {
        varToNT = new HashMap<Id,Id>();
        varHasJavaStringType = new HashSet<Id>();

        for (SyntaxSymbol sym : def.getSyntaxSymbols()) {
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
                                    //Debug.debug(Debug.Type.SYNTAX, 3,
                                    //            "CharClass in " + name + "; " + that);
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
    }

    public Id getNonterminalOfVar(Id var) {
        Id nt = varToNT.get(var);
        if (nt == null) {
            throw new RuntimeException("Not bound to a nonterminal: " + var);
        } else {
            return nt;
        }
    }

    public boolean hasJavaStringType(Id id) {
        return varHasJavaStringType.contains(id);
    }
}
