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

package com.sun.fortress.compiler.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalDecl;

import edu.rice.cs.plt.tuple.Option;

public class GrammarIndex {

    private GrammarDef ast;
    private List<NonterminalIndex> members;
    private List<GrammarIndex> extendedGrammars;

    public GrammarIndex(GrammarDef ast, List<NonterminalIndex> members) {
        this.ast = ast;
        this.extendedGrammars = new LinkedList<GrammarIndex>();
        this.members = members;
    }

    public GrammarDef ast() {
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
        for (NonterminalIndex m: this.getDeclaredNonterminals()) {
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
        for (NonterminalIndex index : this.getDeclaredNonterminals()){
            if (index instanceof NonterminalDefIndex) {
                all.add((NonterminalDefIndex) index);
            }
        }
        return all;
    }

    /* returns the list of nonterminal extensions
     */
    public List<NonterminalExtendIndex> getExtensions() {
        List<NonterminalExtendIndex> all = new ArrayList<NonterminalExtendIndex>();
        for (NonterminalIndex index : this.getDeclaredNonterminals()){
            if (index instanceof NonterminalExtendIndex) {
                all.add((NonterminalExtendIndex) index);
            }
        }
        return all;
    }

    public String toString(){
        return getName().toString();
    }
}
