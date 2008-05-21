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

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.syntax_abstractions.util.TypeCollector;

import edu.rice.cs.plt.tuple.Option;

public class SyntaxDeclEnv {

    private SyntaxDef sd;
    private final Set<Id> anyChars;
    private final Set<Id> characterClasses;
    private final Set<Id> options;
    private final Set<Id> repeats;
    private final Map<Id, Id> varToNonterminalName;
    private final Map<Id, Type> varToType;
    private boolean init;

    public SyntaxDeclEnv(SyntaxDef sd) {
        this.sd = sd;
        this.anyChars = new HashSet<Id>();
        this.characterClasses = new HashSet<Id>();
        this.options = new HashSet<Id>();
        this.repeats = new HashSet<Id>();
        this.varToNonterminalName = new HashMap<Id, Id>();
        this.varToType = new HashMap<Id, Type>();
        this.init = false;
    }

    private void init() {
        for (SyntaxSymbol ss: this.sd.getSyntaxSymbols()) {
            ss.accept(new NodeDepthFirstVisitor_void() {
                @Override
                public void forPrefixedSymbolOnly(PrefixedSymbol that) {
                    assert(that.getId().isSome());
                    final Id id = that.getId().unwrap();
                    /**
                     * At this point the only symbols which should be children of a 
                     * prefix symbol are nonterminal, optional, repeat one or more times, 
                     * repeat, character classes or any chars, thus we only handle these cases. 
                     * In the case of optional, repeat one or more times, and repeat, we are interested 
                     * in the nonterminals they refer to.
                     */
                    that.getSymbol().accept(new NodeDepthFirstVisitor_void() {
                            @Override
                            public void forNonterminalSymbol(NonterminalSymbol that) {
                                varToNonterminalName.put(id, that.getNonterminal());
                            }

                            @Override
                            public void forAnyCharacterSymbol(
                                    AnyCharacterSymbol that) {
                                anyChars.add(id);
                            }

                            @Override
                            public void forCharacterClassSymbol(CharacterClassSymbol that) {
                                characterClasses.add(id);
                            }

                            @Override
                            public void forOptionalSymbol(OptionalSymbol that) {
                                options.add(id);
                                super.forOptionalSymbol(that);
                            }

                            @Override
                            public void forRepeatOneOrMoreSymbol(
                                    RepeatOneOrMoreSymbol that) {
                                repeats.add(id);
                                super.forRepeatOneOrMoreSymbol(that);
                            }

                            @Override
                            public void forRepeatSymbol(RepeatSymbol that) {
                                repeats.add(id);
                                super.forRepeatSymbol(that);
                            }
                            
                        });
                    varToType.put(id, TypeCollector.getType(that));
                    super.forPrefixedSymbolOnly(that);
                }
                });
        }
        this.init = true;
    }

    public boolean contains(Id var) {
        if (!init)
            init();
        return this.varToNonterminalName.containsKey(var) ||
               this.characterClasses.contains(var) ||
               this.anyChars.contains(var);
    }

    public Collection<Id> getVariables() {
        if (!init)
            init();
        Collection<Id> s = new HashSet<Id>();
        s.addAll(this.varToNonterminalName.keySet());
        s.addAll(this.characterClasses);
        s.addAll(this.anyChars);
        return s;
    }

    public boolean isNonterminal(Id var) {
        if (!init)
            init();
        return this.varToNonterminalName.containsKey(var);
    }

    public Id getNonterminalName(Id var) {
        if (!init)
            init();
        return this.varToNonterminalName.get(var);
    }

    public Type getType(Id var) {
        if (!init)
            init();
        return this.varToType.get(var);
    }

    public boolean isAnyChar(Id id) {
        if (!init)
            init();
        return this.anyChars.contains(id);
    }
    
    public boolean isCharacterClass(Id id) {
        if (!init)
            init();
        return this.characterClasses.contains(id);
    }
    
    public boolean isOption(Id id) {
        if (!init)
            init();
        return this.options.contains(id);
    }
    
    public boolean isRepeat(Id id) {
        if (!init)
            init();
        return this.repeats.contains(id);
    }
}
