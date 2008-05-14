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
    private final Map<Id, Id> varToNonterminalName;
    private final Map<Id, Type> varToType;
    private boolean init;

    public SyntaxDeclEnv(SyntaxDef sd) {
        this.sd = sd;
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
                    Id id = that.getId().unwrap();

                    Id nonterminalName = that.getSymbol().accept(new NameCollector());
                    varToNonterminalName.put(id, nonterminalName);
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
        return this.varToNonterminalName.containsKey(var);
    }

    public Collection<Id> getVariables() {
        if (!init)
            init();
        return this.varToNonterminalName.keySet();
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

    /**
     * At this point the only symbols which should be children of a 
     * prefix symbol are nonterminal, optional, repeat one or more times, 
     * or repeat, thus we only handle these cases. 
     */
    private static class NameCollector extends NodeDepthFirstVisitor<Id> {

        @Override
        public Id forNonterminalSymbol(NonterminalSymbol that) {
            return that.getNonterminal();
        }

        @Override
        public Id forOptionalSymbolOnly(OptionalSymbol that,
                Id symbol_result) {
            return symbol_result;
        }

        @Override
        public Id forRepeatOneOrMoreSymbolOnly(
                RepeatOneOrMoreSymbol that, Id symbol_result) {
            return symbol_result;
        }

        @Override
        public Id forRepeatSymbolOnly(RepeatSymbol that,
                Id symbol_result) {
            return symbol_result;
        }
    }
}
