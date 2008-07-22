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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;
import com.sun.fortress.useful.Debug;

public class GrammarAnalyzer<T extends Analyzable<T>> {

    /**
     * Returns the set of all methods contained in a the grammar represented by the index.
     * @param name
     * @param a
     * @return
     */
    public Collection<NonterminalIndex<? extends GrammarMemberDecl>> getContainedSet(Analyzable<T> a) {
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> c = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
        c.addAll(getDeclaredSet(a));
        c.addAll(this.getInheritedSet(a));
        Debug.debug( Debug.Type.SYNTAX, 4, "Contained set for " + a + " is " + c );
        return c;
    }

    /**
     * Returns the collection of nonterminal definitions with the same name as
     * provided as argument.
     * @param name
     * @param a
     * @return
     */
    public Set<Id> getContained(String name, Analyzable<T> a) {
        Set<Id> rs = new HashSet<Id>();
        for (NonterminalIndex<? extends GrammarMemberDecl> n: getContainedSet(a)) {
            if (n.getName().getText().equals(name)) {
                rs.add(n.getName());
            }
        }
        return rs;
    }

    public Set<NonterminalIndex<? extends GrammarMemberDecl>> getOverridingNonterminalIndex(
            String name, Analyzable<T> a) {
        Set<NonterminalIndex<? extends GrammarMemberDecl>> rs = new HashSet<NonterminalIndex<? extends GrammarMemberDecl>>();
        for (NonterminalIndex<? extends GrammarMemberDecl> n: getPotentiallyInheritedSet(a)) {
            if (n.getName().getText().equals(name)) {   
                rs.add(n);
            }
        }
        return rs;
            }

    /**
     * Returns the collection of names of inherited nonterminal definitions with the same name as
     * provided as argument.
     * @param name
     * @param a
     * @return
     */
    public Set<Id> getInherited(String name, Analyzable<T> a) {
        Set<Id> rs = new HashSet<Id>();		
        Debug.debug( Debug.Type.SYNTAX, 4, "Search inherited set for " + name );
        for (NonterminalIndex<? extends GrammarMemberDecl> n: getInheritedSet(a)) {
            Debug.debug( Debug.Type.SYNTAX, 4, "Inherit " + n + " = " + name + "?" );
            if (n.getName().getText().equals(name)) {
                rs.add(n.getName());
            }
        }
        return rs;
    }

    /**
     * Returns the collection of declared nonterminal definitions with the same name as
     * provided as argument.
     * @param name
     * @param a
     * @return
     */
    private Set<Id> getDeclared(String name, Analyzable<T> a) {
        Set<Id> rs = new HashSet<Id>();
        for (NonterminalIndex<? extends GrammarMemberDecl> n: getDeclaredSet(a)) {
            if (n.getName().getText().equals(name)) {
                rs.add(n.getName());
            }
        }
        return rs;
    }

    /**
     * Returns a set of all the nonterminals inherited by the grammar represented
     * by the grammar index.
     * @param name
     * @param a
     * @return
     */
    private Collection<NonterminalIndex<? extends GrammarMemberDecl>> getInheritedSet(Analyzable<T> analyzable) {
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> member = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
        Debug.debug( Debug.Type.SYNTAX, 4, "Extended set for " + analyzable + " is " + analyzable.getExtended() );
        for (T gi: analyzable.getExtended()) {
            for (NonterminalIndex<? extends GrammarMemberDecl> nonterminal: this.getContainedSet(gi)) {
                boolean ok = false;
                if (!nonterminal.isPrivate() && !member.contains(nonterminal)) {
                    if (this.getDeclared(nonterminal.getName().getText(), analyzable).isEmpty()) {
                        ok = true;
                        member.add(nonterminal);
                    }
                }
                if ( ok ){
                    Debug.debug( Debug.Type.SYNTAX, 4, nonterminal.getName() + " is inherited" );
                } else {
                    Debug.debug( Debug.Type.SYNTAX, 4, nonterminal.getName() + " is not inherited" );
                }
            }
        }
        return member;
    }

    /**
     * Returns a set of all the nonterminals <b>potentially</b> inherited by
     * the grammar represented by the grammar index.
     * @param name
     * @param a
     * @return
     */
    private Collection<NonterminalIndex<? extends GrammarMemberDecl>> getPotentiallyInheritedSet(Analyzable<T> a) {
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> nonterminals = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
        for (T gi: a.getExtended()) {
            for (NonterminalIndex<? extends GrammarMemberDecl> n: this.getContainedSet(gi)) {
                if (!n.isPrivate()) {  
                    nonterminals.add(n);
                }
            }
        }
        return nonterminals;
    }

    /**
     * Returns a set of nonterminals which are declared in the grammar represented
     * by the grammar index.
     * @param name
     * @param a
     * @return
     */
    private Collection<NonterminalIndex<? extends GrammarMemberDecl>> getDeclaredSet(Analyzable<T> a) {
        final Collection<NonterminalIndex<? extends GrammarMemberDecl>> all = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
        for ( final NonterminalIndex<? extends GrammarMemberDecl> nonterminal : a.getDeclaredNonterminals() ){
            nonterminal.getAst().accept( new NodeDepthFirstVisitor_void(){
                @Override public void forNonterminalDef( NonterminalDef that ){
                    all.add( nonterminal );
                }
            });
        }
        return all;
    }

}
