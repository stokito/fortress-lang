/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GrammarIndex {

    private GrammarDecl ast;
    private List<NonterminalIndex> members;
    private List<GrammarIndex> extendedGrammars;

    public GrammarIndex(GrammarDecl ast, List<NonterminalIndex> members) {
        this.ast = ast;
        this.extendedGrammars = new LinkedList<GrammarIndex>();
        this.members = members;
    }

    public GrammarDecl ast() {
        return this.ast;
    }

    public List<NonterminalIndex> getDeclaredNonterminals() {
        return this.members;
    }

    public void setExtended(List<GrammarIndex> gs) {
        this.extendedGrammars = gs;
    }

    public List<GrammarIndex> getExtended() {
        return this.extendedGrammars;
    }

    public Id getName() {
        return this.ast().getName();
    }

    public Option<NonterminalIndex> getNonterminalDecl(Id name) {
        for (NonterminalIndex m : this.getDeclaredNonterminals()) {
            if (name.getText().equals(m.getName().getText())) {
                if (m instanceof NonterminalIndex) {
                    return Option.<NonterminalIndex>some((NonterminalIndex) m);
                }
            }
        }
        return Option.none();
    }

    /* returns the set of nonterminal definitions
     */
    public List<NonterminalDefIndex> getDefinitions() {
        List<NonterminalDefIndex> all = new ArrayList<NonterminalDefIndex>();
        for (NonterminalIndex index : this.getDeclaredNonterminals()) {
            if (index instanceof NonterminalDefIndex) {
                all.add((NonterminalDefIndex) index);
            }
        }
        return all;
    }

    public List<NonterminalDef> getDefinitionAsts() {
        List<NonterminalDef> all = new ArrayList<NonterminalDef>();
        for (NonterminalIndex index : this.getDeclaredNonterminals()) {
            if (index instanceof NonterminalDefIndex) {
                all.add(((NonterminalDefIndex) index).ast());
            }
        }
        return all;
    }


    /* returns the list of nonterminal extensions
     */
    public List<NonterminalExtendIndex> getExtensions() {
        List<NonterminalExtendIndex> all = new ArrayList<NonterminalExtendIndex>();
        for (NonterminalIndex index : this.getDeclaredNonterminals()) {
            if (index instanceof NonterminalExtendIndex) {
                all.add((NonterminalExtendIndex) index);
            }
        }
        return all;
    }

    public List<NonterminalExtensionDef> getExtensionAsts() {
        List<NonterminalExtensionDef> all = new ArrayList<NonterminalExtensionDef>();
        for (NonterminalIndex index : this.getDeclaredNonterminals()) {
            if (index instanceof NonterminalExtendIndex) {
                all.add(((NonterminalExtendIndex) index).ast());
            }
        }
        return all;
    }

    public String toString() {
        return getName().toString();
    }
}
