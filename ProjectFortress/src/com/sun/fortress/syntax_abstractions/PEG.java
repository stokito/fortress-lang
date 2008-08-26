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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.SyntaxDef;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.useful.Debug;

/* Contains a mapping from a nonterminal name to its production
 * Also contains a mapping from nonterminal name to its type
 */
class PEG extends NTEnv {

    private final Map<Id, List<SyntaxDef>> defEntries = new HashMap<Id,List<SyntaxDef>>();
    private final Map<Id, List<SyntaxDef>> extEntries = new HashMap<Id,List<SyntaxDef>>();
    public final Set<Id> nativeNonterminals = new HashSet<Id>();

    PEG() { }

    /* has the side effect of creating an entry in the peg with an
     * empty definition list
     */
    public List<SyntaxDef> create(Id nt) {
        if (defEntries.containsKey(nt)) {
            throw new MacroError(nt, "PEG already has an entry for " + nt);
        }
        List<SyntaxDef> defs = new ArrayList<SyntaxDef>();
        List<SyntaxDef> exts = new ArrayList<SyntaxDef>();
        defEntries.put(nt, defs);
        extEntries.put(nt, exts);
        return defs;
    }

    /* remove nonterminals with empty definitions
     */
    public void removeEmptyNonterminals(){
        Set<Id> all = new HashSet<Id>(defEntries.keySet());
        for (Id nt : all){
            if (defEntries.get(nt).isEmpty() && extEntries.get(nt).isEmpty()) {
                Debug.debug(Debug.Type.SYNTAX, 2, "Removing empty nonterminal " + nt);
                defEntries.remove(nt);
                extEntries.remove(nt);
            }
        }
    }

    public List<SyntaxDef> getAll(Id nt) {
        List<SyntaxDef> all = new ArrayList<SyntaxDef>();
        all.addAll(getDefs(nt));
        all.addAll(getExts(nt));
        return all;
    }

    /* return the list of productions for a nonterminal */
    public List<SyntaxDef> getDefs(Id nt) {
        if (!defEntries.containsKey(nt)) {
            throw new MacroError(nt, "PEG has no entry for " + nt);
        }
        return defEntries.get(nt);
    }

    /* return the list of productions for a nonterminal */
    public List<SyntaxDef> getExts(Id nt) {
        if (!extEntries.containsKey(nt)) {
            throw new MacroError(nt, "PEG has no entry for " + nt);
        }
        return extEntries.get(nt);
    }

    public Set<Id> keySet() {
        return defEntries.keySet();
    }

    /* map a nonterminal name to its type */
    public void putType(Id nt, BaseType type) {
        if (ntToType.containsKey(nt)) {
            throw new MacroError(nt, "PEG already has a type for " + nt);
        }
        ntToType.put(nt, type);
    }
}
