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

    private Option<GrammarDef> ast;

    private Collection<NonterminalIndex<? extends GrammarMemberDecl>> members;

    private Collection<GrammarIndex> extendedGrammars;

    private boolean isToplevel;

    public GrammarIndex(Option<GrammarDef> ast, 
                        Set<NonterminalIndex<? extends GrammarMemberDecl>> members) {
        this.ast = ast;
        this.extendedGrammars = new LinkedList<GrammarIndex>();
        this.members = members;
        this.isToplevel = false;
    }

    public Option<GrammarDef> ast() {
        return this.ast;
    }

    public Collection<NonterminalIndex<? extends GrammarMemberDecl>> getDeclaredNonterminals() {
        return this.members;
    }

    public void setExtended(Collection<GrammarIndex> gs) {
        this.extendedGrammars = gs;
    }

    public Collection<GrammarIndex> getExtended() {
        return this.extendedGrammars;
    }

    public Id getName() {
        if (this.ast().isSome()) {
            return this.ast().unwrap().getName();
        }
        throw new RuntimeException("No name for grammar: "+this.hashCode());
    }

    public Option<GrammarNonterminalIndex<? extends NonterminalDecl>> getNonterminalDecl(Id name) {
        for (NonterminalIndex<? extends GrammarMemberDecl> m: this.getDeclaredNonterminals()) {
            if (name.getText().equals(m.getName().getText())) {
                if (m.ast().isSome()) {
                    if (m instanceof GrammarNonterminalIndex) {
                        return Option.<GrammarNonterminalIndex<? extends NonterminalDecl>>some((GrammarNonterminalIndex) m);
                    }
                }
            }
        }
        return Option.none();
    }

    public void isToplevel(boolean isTopLevel) {
        this.isToplevel = isTopLevel;
    }
    
    public boolean isToplevel() {
        return this.isToplevel;
    }

    public String toString(){
        return getName().toString();
    }

}
