/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.syntax_abstractions.phases.ExtensionDesugarer;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

/* Composes grammars together into a flat PEG. */
class GrammarComposer {

    private Map<Id, GrammarIndex> grammarMap;

    private GrammarComposer(Collection<GrammarIndex> grammars) {
        Map<Id, GrammarIndex> grammarMap = new HashMap<Id, GrammarIndex>();
        for (GrammarIndex g : grammars) {
            grammarMap.put(g.getName(), g);
        }
        this.grammarMap = grammarMap;
    }

    /* entry point for grammars */
    public static PEG pegForGrammar(GrammarIndex grammar) {
        Debug.debug(Debug.Type.SYNTAX, 2, "GrammarComposer: create parser for grammar " + grammar.getName());
        List<NonterminalDef> definitions = grammar.getDefinitionAsts();
        List<NonterminalExtensionDef> extensions = grammar.getExtensionAsts();
        GrammarComposer composer = new GrammarComposer(collectImports(grammar));
        return composer.compose(definitions, extensions);
    }

    /* Entry point for components. Creates a peg given a list of
     * grammars
     * @imports - list of immediately imported api's that contain grammars
     */
    public static PEG pegForComponent(List<GrammarIndex> imports) {
        Debug.debug(Debug.Type.SYNTAX, 2, "GrammarComposer: create parser for component");
        Debug.debug(Debug.Type.SYNTAX, 2, "Imports: ", imports);
        List<NonterminalDef> definitions = new LinkedList<NonterminalDef>();
        List<NonterminalExtensionDef> extensions = ExtensionDesugarer.createImplicitExtensions(imports);
        GrammarComposer composer = new GrammarComposer(collectImports(imports));
        return composer.compose(definitions, extensions);
    }

    /* get the complete list of imported grammars */
    private static Set<GrammarIndex> collectImports(GrammarIndex grammar) {
        return collectImports(grammar.getExtended());
    }

    /* finds extended grammars as well as their extended gramars.
     * The transitive closure of all extended grammars.
     */
    private static Set<GrammarIndex> collectImports(List<GrammarIndex> extended) {
        Set<GrammarIndex> all = new HashSet<GrammarIndex>();
        for (GrammarIndex extendedGrammar : extended) {
            all.add(extendedGrammar);
            all.addAll(collectImports(extendedGrammar));
        }
        return all;
    }

    /* create a peg and populate it with nonterminals according to the
     * grammar composition rules
     */
    private PEG compose(Collection<NonterminalDef> definitions, Collection<NonterminalExtensionDef> extensions) {
        PEG peg = new PEG();
        /* adds nonterminal definitions for 2nd generation extended grammars */
        addGrammarDefsOnly(peg);
        /* adds nonterminal definitions for immediately extended grammars */
        addDefs(peg, definitions);
        /* modify nonterminals to include extended alternatives */
        for (NonterminalExtensionDef e : extensions) {
            applyExtension(peg, e);
        }
        computeNativeNonterminals(peg);
        return peg;
    }

    /* add all nonterminals that belong to a native grammar to the PEG */
    private void computeNativeNonterminals(PEG peg) {
        for (GrammarIndex grammar : grammarMap.values()) {
            GrammarDecl grammarDef = grammar.ast();
            Debug.debug(Debug.Type.SYNTAX,
                        3,
                        "Grammar " + grammarDef.getName() + " native?: " + grammarDef.isNativeDef());
            if (grammarDef.isNativeDef()) {
                // A native grammar can only contain nonterminal declarations
                for (GrammarMemberDecl member : grammarDef.getMembers()) {
                    peg.nativeNonterminals.add(member.getName());
                }
            }
        }
    }

    /* populate entries for definition sites only */
    private void addGrammarDefsOnly(PEG peg) {
        for (GrammarIndex grammar : grammarMap.values()) {
            addDefs(peg, grammar.getDefinitionAsts());
        }
    }

    /* populate peg for a single grammar and its definitions */
    private void addDefs(PEG peg, Collection<NonterminalDef> nonterminals) {
        for (NonterminalDef nonterminal : nonterminals) {
            Id nonterminalName = nonterminal.getName();
            List<SyntaxDef> defs = peg.create(nonterminalName);
            Debug.debug(Debug.Type.SYNTAX, 2, "Add ", nonterminalName, " to peg definition");
            addEntryForDef(defs, nonterminal);

            Option<BaseType> type = nonterminal.getAstType();
            if (type.isSome()) {
                peg.putType(nonterminalName, type.unwrap());
            } else {
                throw new MacroError(nonterminalName, "No type for nonterminal " + nonterminal);
            }
        }
    }

    /* add choices for a single definition */
    private void addEntryForDef(final List<SyntaxDef> defs, NonterminalDef nonterminal) {
        for (SyntaxDecl decl : nonterminal.getSyntaxDecls()) {
            resolveAndAdd(defs, decl);
        }
    }

    /* add choices for nonterminal extensions to the definition nonterminals */
    private void applyExtension(PEG peg, NonterminalExtensionDef ext) {
        Debug.debug(Debug.Type.SYNTAX, 2, "Apply extensions to ", ext.getName());
        List<SyntaxDef> exts = peg.getExts(ext.getName());
        for (SyntaxDecl decl : ext.getSyntaxDecls()) {
            resolveAndAdd(exts, decl);
        }
    }

    /* add choices either defined by the SyntaxDecl or the choices defined by the
     * super non-terminal
     */
    private void resolveAndAdd(List<SyntaxDef> defs, SyntaxDecl decl) {
        resolveAndAdd(defs, decl, true);
    }

    private void resolveAndAdd(List<SyntaxDef> defs, SyntaxDecl decl, boolean includePrivate) {
        String modifier = decl.getModifier().unwrap("public");
        if (modifier.equals("private") && !includePrivate) return;
        if (modifier.equals("without")) return;
        //   "without" only applies to SuperSyntaxDef, but no harm testing here

        if (decl instanceof SyntaxDef) {
            SyntaxDef def = (SyntaxDef) decl;
            Debug.debug(Debug.Type.SYNTAX, 3, "Add choice ", decl);
            defs.add(def);
        } else if (decl instanceof SuperSyntaxDef) {
            SuperSyntaxDef def = (SuperSyntaxDef) decl;
            for (SyntaxDecl extension : findExtension(def.getNonterminal(), def.getGrammarId())) {
                resolveAndAdd(defs, extension, false);
            }
        } else {
            throw new MacroError(decl, "Unknown kind of SyntaxDecl");
        }
    }

    /* lookup the choices of a nonterminal in a specific grammar */
    private Collection<SyntaxDecl> findExtension(Id nonterminalName, Id grammarName) {
        Debug.debug(Debug.Type.SYNTAX,
                    3,
                    "Searching for extension for nonterminal " + nonterminalName + " and grammar " + grammarName);
        GrammarIndex grammar = grammarMap.get(grammarName);
        if (grammar == null) {
            throw new MacroError(grammarName, "Could not find grammar: " + grammarName);
        }

        Option<NonterminalIndex> nt = grammar.getNonterminalDecl(nonterminalName);
        if (nt.isSome()) {
            return nt.unwrap().getSyntaxDecls();
        } else {
            throw new MacroError(nonterminalName,
                                 "Could not find nonterminal " + nonterminalName + " in grammar " + grammarName);
        }
    }
}
