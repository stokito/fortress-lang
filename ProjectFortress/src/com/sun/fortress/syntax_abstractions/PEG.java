/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Class which builds a table of pieces of Rats! AST which correspond to the macro
 * declarations given as input.
 * The Rats! ASTs are combined to Rats! modules which are written to files on the
 * file system.
 */
package com.sun.fortress.syntax_abstractions;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.useful.Debug;

import java.util.*;

/* Contains a mapping from a nonterminal name to its production.
 * Also contains a mapping from nonterminal name to its type.
 */
class PEG extends NTEnv {

    private final Map<Id, List<SyntaxDef>> defEntries = new HashMap<Id, List<SyntaxDef>>();
    private final Map<Id, List<SyntaxDef>> extEntries = new HashMap<Id, List<SyntaxDef>>();
    public final Set<Id> nativeNonterminals = new HashSet<Id>();

    PEG() {
    }

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

    /* remove nonterminals with empty definitions */
    public void removeEmptyNonterminals() {
        Set<Id> all = new HashSet<Id>(defEntries.keySet());
        for (Id nt : all) {
            if (defEntries.get(nt).isEmpty() && extEntries.get(nt).isEmpty()) {
                Debug.debug(Debug.Type.SYNTAX, 2, "Removing empty nonterminal ", nt);
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
