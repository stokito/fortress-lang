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

package com.sun.fortress.syntax_abstractions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.compiler.index.NonterminalDefIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SuperSyntaxDef;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.BaseType;

import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

/* Composes grammars together into a flat PEG.
 */
class GrammarComposer {

    private GrammarComposer() {}

    /* entry point for grammars */
    public static PEG pegForGrammar(GrammarIndex grammar){
        Debug.debug(Debug.Type.SYNTAX, 2, 
                    "GrammarComposer: create parser for grammar " + grammar.getName());
        List<GrammarIndex> imports = grammar.getExtended();
        Debug.debug(Debug.Type.SYNTAX, 2, "Imports: " + imports);
        List<NonterminalDefIndex> definitions = grammar.getDefinitions();
        List<NonterminalExtendIndex> extensions = grammar.getExtensions();
        return pegFor(collectImports(grammar), imports, definitions, extensions);
    }

    /* entry point for components */
    public static PEG pegForComponent(List<GrammarIndex> imports) {
        Debug.debug(Debug.Type.SYNTAX, 2, 
                    "GrammarComposer: create parser for component");
        Debug.debug(Debug.Type.SYNTAX, 2, "Imports: " + imports);
        List<NonterminalDefIndex> definitions = new LinkedList<NonterminalDefIndex>();
        List<NonterminalExtendIndex> extensions = new LinkedList<NonterminalExtendIndex>();
        return pegFor(collectImports(imports), imports, definitions, extensions);
    }

    /* get the complete list of imported grammars */
    private static Set<GrammarIndex> collectImports(GrammarIndex grammar){
        return collectImports(grammar.getExtended());
    }

    /* finds imported grammars as well as their imported gramars */
    private static Set<GrammarIndex> collectImports(List<GrammarIndex> extended) {
        Set<GrammarIndex> all = new HashSet<GrammarIndex>();
        for (GrammarIndex importedGrammar : extended){
            all.add(importedGrammar);
            all.addAll(collectImports(importedGrammar));
        }
        return all;
    }

    /* create a peg and populate it with nonterminals according to the
     * grammar composition rules
     */
    private static PEG pegFor(Set<GrammarIndex> relevant,
                              List<GrammarIndex> imports,
                              Collection<NonterminalDefIndex> definitions,
                              Collection<NonterminalExtendIndex> extensions){

        Debug.debug(Debug.Type.SYNTAX, 2, "Relevant grammars: " + relevant);

        PEG peg = new PEG();

        addGrammarDefsOnly(peg, relevant);
        addDefs(peg, relevant, definitions);

        Set<String> extendedNonterminals = importsExtensionDomain(imports);
        Set<String> extendedExplicit = computeExplicit(extensions);
        Set<String> extendedImplicit = setDifference(extendedNonterminals, extendedExplicit);

        Debug.debug(Debug.Type.SYNTAX, 2, "Extended nonterminals: " + extendedNonterminals);
        Debug.debug(Debug.Type.SYNTAX, 2, "Extended explicit: " + extendedExplicit);
        Debug.debug(Debug.Type.SYNTAX, 2, "Extended implicit: " + extendedImplicit);

        for (NonterminalExtendIndex e : extensions){
            applyExtension(peg, e, relevant);
        }
        for (String implicitNonterminal : extendedImplicit) {
            ; // FIXME: fill this in, and delete next part
        }
        // FIXME: from here
        Collection<NonterminalExtendIndex> implicitExtensions = 
            computeImplicitExtensions(extendedImplicit, imports);
        for (NonterminalExtendIndex e : implicitExtensions){
            applyExtension(peg, e, relevant);
        }
        // FIXME: to here

        computeNativeNonterminals(peg, relevant);
        return peg;
    }

    private static void computeNativeNonterminals(PEG peg, Set<GrammarIndex> relevant) {
        for (GrammarIndex grammar : relevant) {
            GrammarDef grammarDef = grammar.ast();
            Debug.debug(Debug.Type.SYNTAX, 3,
                        "Grammar " + grammarDef + " native?: " + grammarDef.isNative());
            if (grammarDef.isNative()) {
                // A native grammar can only contain nonterminal declarations
                for (GrammarMemberDecl member : grammarDef.getMembers()) {
                    NonterminalHeader header = member.getHeader();
                    peg.nativeNonterminals.add(header.getName());
                }
            }
        }
    }

    /* populate entries for definition sites only */
    private static void addGrammarDefsOnly(PEG peg, Set<GrammarIndex> relevant){
        for (GrammarIndex grammar : relevant){
            addDefs(peg, relevant, grammar.getDefinitions());
        }
    }

    /* populate peg for a single grammar and its definitions */
    private static void addDefs(PEG peg, Set<GrammarIndex> relevant, 
                                Collection<NonterminalDefIndex> nonterminals) {
        for (NonterminalDefIndex nonterminal : nonterminals){
            Id nonterminalName = nonterminal.getName();
            List<SyntaxDef> defs = peg.create(nonterminalName);
            Debug.debug(Debug.Type.SYNTAX, 2, "Add " + nonterminalName + " to peg definition");
            addEntryForDef(defs, relevant, nonterminal);

            Option<BaseType> type = nonterminal.ast().getAstType();
            if (type.isSome()) {
                peg.putType(nonterminalName, type.unwrap());
            } else {
                throw new MacroError(nonterminalName, "No type for nonterminal " + nonterminal);
            }
        }
    }

    /* add choices for a single definition */
    private static void addEntryForDef(final List<SyntaxDef> defs, 
                                       final Collection<GrammarIndex> relevant, 
                                       NonterminalDefIndex nonterminal){
        NonterminalDef ntDef = nonterminal.ast();
        for (SyntaxDecl decl : ntDef.getSyntaxDecls()) {
            resolveAndAdd(defs, relevant, decl);
        }
    }

    /* add choices either defined by the SyntaxDecl or the choices defined by the
     * super non-terminal
     */
    private static void resolveAndAdd(final List<SyntaxDef> defs,
                                      final Collection<GrammarIndex> relevant, 
                                      SyntaxDecl decl){
        if (decl instanceof SyntaxDef) {
            SyntaxDef def = (SyntaxDef) decl;
            Debug.debug(Debug.Type.SYNTAX, 3, "Add choice " + decl);
            defs.add(def);
        } else if (decl instanceof SuperSyntaxDef) {
            SuperSyntaxDef def = (SuperSyntaxDef) decl;
            for (SyntaxDecl extension : 
                     findExtension(def.getNonterminal(), def.getGrammar(), relevant)){
                resolveAndAdd(defs, relevant, extension);
            }
        } else {
            throw new MacroError(decl, "Unknown kind of SyntaxDecl");
        }
    }

    /* return set of nonterminals that are extended by virtue of being
     * in the extends clause of the grammar
     */
    private static List<NonterminalExtendIndex> computeImplicitExtensions(final Set<String> implicit, 
                                                                          List<GrammarIndex> imports){

        final List<NonterminalExtendIndex> all = new ArrayList<NonterminalExtendIndex>();

        for (GrammarIndex imp : imports){
            for (final NonterminalIndex nonterminal : imp.getDeclaredNonterminals()){
                if (nonterminal instanceof NonterminalExtendIndex) {
                    if (implicit.contains(NodeUtil.nameString(nonterminal.getName()))) {
                        all.add((NonterminalExtendIndex) nonterminal);
                    }
                }
            }
        }
        return all;
    }

    /* add choices to nonterminals when the choice is an extension of the nonterminal
     */
    private static Set<String> importsExtensionDomain(Collection<GrammarIndex> imports){
        Set<String> all = new HashSet<String>();
        for (GrammarIndex grammar : imports){
            extensionDomain(all, grammar);
        }
        return all;
    }

    /* only handles nonterminal extensions */
    private static void extensionDomain(final Set<String> domain, GrammarIndex grammar){
        for (NonterminalIndex nonterminal : grammar.getDeclaredNonterminals()){
            nonterminal.ast().accept(new NodeDepthFirstVisitor_void(){
                @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                    domain.add(that.getHeader().getName().toString());
                }
                @Override public void forNonterminalDef(NonterminalDef that) {
                    return;
                }
            });
        }
    }

    /* adds the choices for nonterminal extensions to the definition nonterminals
     */
    private static void applyExtension(final PEG peg, final NonterminalExtendIndex extension, 
                                       final Collection<GrammarIndex> relevant){
        Debug.debug(Debug.Type.SYNTAX, 2, "Apply extensions to " + extension.getName());
        final List<SyntaxDef> exts = peg.getExts(extension.getName());

        extension.ast().accept(new NodeDepthFirstVisitor_void(){
                @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                    for (SyntaxDecl decl : that.getSyntaxDecls()){
                        resolveAndAdd(exts, relevant, decl);
                    }
                }

                @Override public void forNonterminalDef(NonterminalDef that){
                    for (SyntaxDecl decl : that.getSyntaxDecls()){
                        resolveAndAdd(exts, relevant, decl);
                    }
                }
            });
    }

    /* lookup the choices of a nonterminal in a specific grammar
     */
    private static Collection<SyntaxDecl> findExtension(Id nonterminalName, 
                                                        Id grammarName, 
                                                        Collection<GrammarIndex> grammars){
        Debug.debug(Debug.Type.SYNTAX, 3, 
                    "Searching for extension for nonterminal " + nonterminalName +
                    " and grammar " + grammarName);
        for (GrammarIndex grammar : grammars){
            Debug.debug(Debug.Type.SYNTAX, 3,
                         "Checking if grammar " + grammar.getName() +
                         " matches " + grammarName);
            if (grammar.getName().equals(grammarName)){
                Debug.debug(Debug.Type.SYNTAX, 3, ".. yes!");
                Option<NonterminalIndex> nt = grammar.getNonterminalDecl(nonterminalName);
                if (nt.isSome()){
                    return nt.unwrap().getSyntaxDecls();
                }
            } else {
                Debug.debug(Debug.Type.SYNTAX, 3, ".. no");
            }
        }
        throw new MacroError(nonterminalName,
                             "Could not find nonterminal " + nonterminalName +
                             " in grammar " + grammarName);
    }

    // Utilities

    private static <T> Set<T> setDifference(Set<T> s1, Set<T> s2){
        Set<T> all = new HashSet<T>(s1);
        all.removeAll(s2);
        return all;
    }

    /* convert extensions to their names */
    private static Set<String> computeExplicit(Collection<NonterminalExtendIndex> extensions){
        Set<String> all = new HashSet<String>();
        for (NonterminalExtendIndex index : extensions) {
            all.add(index.getName().toString());
        }
        return all;
    }
}
