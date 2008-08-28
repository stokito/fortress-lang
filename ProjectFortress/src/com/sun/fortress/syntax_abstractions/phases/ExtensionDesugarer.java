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
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SuperSyntaxDef;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.tools.FortressAstToConcrete;

import edu.rice.cs.plt.tuple.Option;

/* ExtensionDesugarer rewrites grammars to satisfy the
 * following postconditions:
 * 
 * - A nonterminal has at most one extension node
 *   (multiple extensions in a grammar are combined together)
 * - Every nonterminal with public extensions in some imported grammar
 *   has an extension node
 * - Every extension of a nonterminal mentions every imported grammar
 *   with public extensions exactly once
 *
 *
 * Also provides a public static method used by GrammarComposer
 * to generate implicit extensions.
 */
public class ExtensionDesugarer extends NodeUpdateVisitor {

    private List<StaticError> _errors;
    private GlobalEnvironment _globalEnv;
    private ApiIndex _currentApi;

    public ExtensionDesugarer(GlobalEnvironment env, List<StaticError> errors) {
        this._errors = errors;
        this._globalEnv = env;
    }

    public Collection<StaticError> errors() {
        return this._errors;
    }

    public static List<NonterminalExtensionDef> createImplicitExtensions(List<GrammarIndex> grammars) {
        return rewriteMembers(grammarExtensionMap(grammars), 
                              new HashMap<Id,List<NonterminalExtensionDef>>());
    }

    private void error(String msg, HasAt loc) {
        this._errors.add(StaticError.make(msg, loc));
    }

    private Option<GrammarIndex> grammarIndex(Id name) {
        if (name.getApi().isSome()) {
            APIName api = name.getApi().unwrap();
            if (this._globalEnv.definesApi(api)) {
                return Option.some(_globalEnv.api(api).grammars().get(name.getText()));
            } else {
                return Option.none();
            }
        } else {
            return Option.some(((ApiIndex) _currentApi).grammars().get(name));
        }
    }

    @Override public Node forApi(Api that) {
        if (this._globalEnv.definesApi(that.getName())) {
            this._currentApi = this._globalEnv.api(that.getName());
        } else {
            error("Undefined api ", that);
        }
        return super.forApi(that);
    }

    @Override public Node forGrammarDef(GrammarDef that) {
        Option<GrammarIndex> index = this.grammarIndex(that.getName());
        if (index.isSome()) {
            return rewriteGrammar(that, index.unwrap());
        } else { 
            error("Grammar "+that.getName()+" not found", that);
        }
        return super.forGrammarDef(that);
    }

    private Node rewriteGrammar(GrammarDef grammar, GrammarIndex index) {
        Debug.debug(Debug.Type.SYNTAX, 1,
                    "Desugaring extensions for grammar " + grammar.getName());

        List<GrammarMemberDecl> members = grammar.getMembers();
        List<GrammarMemberDecl> newMembers = new ArrayList<GrammarMemberDecl>();
        Map<Id, List<NonterminalExtensionDef>> extMap = 
            new HashMap<Id, List<NonterminalExtensionDef>>();

        // Split into defs, extensions
        // Group extensions by extended nonterminal
        split(members, newMembers, extMap);

        // Create mapping (NT => List<GrammarIndex>) of nonterminals 
        //   with public extensions in imported grammars
        Map<Id, List<GrammarIndex>> grammarExtensionMap =
            grammarExtensionMap(index);

        newMembers.addAll(rewriteMembers(grammarExtensionMap, extMap));

        // Recombine into GrammarDef
        GrammarDef result =
            new GrammarDef(grammar.getSpan(), grammar.getName(), grammar.getExtends(),
                           newMembers, grammar.getTransformers(), grammar.isNative());
        Debug.debug(Debug.Type.SYNTAX, 3,
                    "Desugared grammar into:\n" + result.accept(new FortressAstToConcrete()));

        return result;
    }

    private static List<NonterminalExtensionDef> rewriteMembers(Map<Id, List<GrammarIndex>> grammarExtensionMap, 
                                                                Map<Id, List<NonterminalExtensionDef>> extMap) {

        List<NonterminalExtensionDef> newMembers = new ArrayList<NonterminalExtensionDef>();

        Set<Id> allExtensionNTs = new HashSet<Id>();
        allExtensionNTs.addAll(extMap.keySet());
        allExtensionNTs.addAll(grammarExtensionMap.keySet());

        // For each nonterminal, complete/create the extension
        for (Id name : allExtensionNTs) {
            List<NonterminalExtensionDef> extensions = extMap.get(name);
            List<GrammarIndex> extendingGrammars = grammarExtensionMap.get(name);
            if (extensions == null) extensions = new LinkedList<NonterminalExtensionDef>();
            if (extendingGrammars == null) extendingGrammars = new LinkedList<GrammarIndex>();

            newMembers.add(combine(name, extensions, extendingGrammars));
        }
        return newMembers;
	}

    private void split(List<GrammarMemberDecl> members,
                       List<GrammarMemberDecl> newMembers,
                       Map<Id, List<NonterminalExtensionDef>> extMap) {
        for (GrammarMemberDecl member : members) {
            if (member instanceof NonterminalDef) {
                newMembers.add(member);
            } else if (member instanceof NonterminalExtensionDef) {
                NonterminalExtensionDef ntExt = (NonterminalExtensionDef) member;
                if (!extMap.containsKey(ntExt.getName())) {
                    extMap.put(ntExt.getName(), new LinkedList<NonterminalExtensionDef>());
                }
                extMap.get(ntExt.getName()).add(ntExt);
            } else {
                throw new MacroError(member, "Unknown kind of grammar member: " + member);
            }
        }
    }

    private static Map<Id, List<GrammarIndex>> grammarExtensionMap(GrammarIndex index) {
        return grammarExtensionMap(index.getExtended());
    }

    private static Map<Id, List<GrammarIndex>> grammarExtensionMap(List<GrammarIndex> extendedGrammars) {
        Map<Id, List<GrammarIndex>> gMap = new HashMap<Id, List<GrammarIndex>>();
        for (GrammarIndex extended : extendedGrammars) {
            for (NonterminalExtendIndex ni : extended.getExtensions()) {
                if (hasPublicParts(ni)) {
                    if (!gMap.containsKey(ni.getName())) {
                        gMap.put(ni.getName(), new LinkedList<GrammarIndex>());
                    }
                    gMap.get(ni.getName()).add(extended);
                }
            }
        }
        return gMap;
    }

    private static NonterminalExtensionDef combine(Id name,
                                                   List<NonterminalExtensionDef> extensions,
                                                   List<GrammarIndex> extendingGrammars) {

        Span theSpan = null;

        Set<Id> availableGrammarNames = new HashSet<Id>();
        Set<Id> usedGrammarNames = new HashSet<Id>();
        for (GrammarIndex g : extendingGrammars) {
            availableGrammarNames.add(g.getName());
        }

        Debug.debug(Debug.Type.SYNTAX, 3,
                    "Extensions of " + name + " available from " + availableGrammarNames);

        List<SyntaxDecl> resultDecls = new ArrayList<SyntaxDecl>();
        for (NonterminalExtensionDef extension : extensions) {
            if (theSpan == null) theSpan = extension.getSpan();
            for (SyntaxDecl decl : extension.getSyntaxDecls()) {
                if (decl instanceof SyntaxDef) {
                    resultDecls.add(decl);
                } else if (decl instanceof SuperSyntaxDef) {
                    SuperSyntaxDef ssd = (SuperSyntaxDef) decl;
                    Id superGrammarName = ssd.getGrammar();
                    if (availableGrammarNames.contains(superGrammarName)) {
                        availableGrammarNames.remove(superGrammarName);
                        usedGrammarNames.add(superGrammarName);
                        resultDecls.add(decl);
                    } else if (usedGrammarNames.contains(superGrammarName)) {
                        throw new MacroError(decl,
                                             "Extensions of " + name +
                                             " from " + superGrammarName +
                                             " have already been included.");
                    } else {
                        throw new MacroError(decl,
                                             "No extensions of " + name + 
                                             " in grammar " + ssd.getGrammar());
                    }
                }
            }
        }
        if (theSpan == null) theSpan = name.getSpan(); // FIXME: maybe pick a better span
        for (GrammarIndex eg : extendingGrammars) {
            if (availableGrammarNames.contains(eg.getName())) {
                resultDecls.add(new SuperSyntaxDef(theSpan, Option.some("private"),
                                                   name, eg.getName()));
            }
        }
        return new NonterminalExtensionDef(theSpan, name, resultDecls);
    }

    private static boolean hasPublicParts(NonterminalExtendIndex ni) {
        NonterminalExtensionDef nd = ni.ast();
        for (SyntaxDecl decl : nd.getSyntaxDecls()) {
            Option<String> modifier = decl.getModifier();
            if (modifier.isSome()) {
                String mod = modifier.unwrap();
                if (mod.equals("private")) {
                    continue;
                } else if (mod.equals("without")) {
                    continue;
                }
            } else {
                // No explicit modifier means public
                return true;
            }
        }
        return false;
    }
}
