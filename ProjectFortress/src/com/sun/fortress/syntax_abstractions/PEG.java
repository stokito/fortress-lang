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

/*
 * Class which builds a table of pieces of Rats! AST which corresponds the macro 
 * declarations given as input.
 * The Rats! ASTs are combined to Rats! modules which are written to files on the
 * file system.
 * 
 */

package com.sun.fortress.syntax_abstractions;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.SyntaxDef;

import com.sun.fortress.useful.Debug;

/* Contains a mapping from a nonterminal name to its production
 * Also contains a mapping from nonterminal name to its type
 */
class PEG extends NTEnv {

    Map<Id, List<SyntaxDef>> entries = new HashMap<Id,List<SyntaxDef>>();
    PEG() { }

    /* has the side effect of creating an entry in the peg with an
     * empty definition list
     */
    public List<SyntaxDef> create(Id nt) {
        if (entries.containsKey(nt)) {
            throw new RuntimeException("PEG already has an entry for " + nt);
        }
        List<SyntaxDef> defs = new LinkedList<SyntaxDef>();
        entries.put(nt, defs);
        return defs;
    }

    /* remove a nonterminal from the set of productions
     */
    private void remove(Id nt){
        entries.remove(nt);
    }

    /* remove nonterminals with empty definitions
     */
    public void removeEmptyNonterminals(){
        Set<Id> all = new HashSet<Id>( entries.keySet() );
        for (Id nt : all){
            if (entries.get(nt).isEmpty()){
                Debug.debug( Debug.Type.SYNTAX, 2, "Removing empty nonterminal " + nt );
                entries.remove(nt);
            }
        }
    }

    /* return the list of productions for a nonterminal */
    public List<SyntaxDef> get(Id nt) {
        if (!entries.containsKey(nt)) {
            throw new RuntimeException("PEG has no entry for " + nt);
        }
        return entries.get(nt);
    }

    public Set<Id> keySet() {
        return entries.keySet();
    }

    /* map a nonterminal name to its type */
    public void putType(Id nt, BaseType type) {
        if (ntToType.containsKey(nt)) {
            throw new RuntimeException("PEG already has a type for " + nt);
        }
        ntToType.put(nt, type);
    }
}
