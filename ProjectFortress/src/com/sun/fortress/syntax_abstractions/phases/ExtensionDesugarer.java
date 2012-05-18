/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

/* ExtensionDesugarer rewrites grammars to satisfy
 * the following postconditions:
 *
 * - A nonterminal has at most one extension node
 *   (multiple extensions in a grammar are combined together)
 * - Every nonterminal with public extensions in some imported grammar
 *   has an extension node
 * - Every extension of a nonterminal mentions every imported grammar
 *   with public extensions exactly once
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

    private void error(String msg, HasAt loc) {
        this._errors.add(StaticError.make(msg, loc));
    }

    public Collection<StaticError> errors() {
        return this._errors;
    }

    /* Get a list of grammars that an imported grammar imports ?? */
    public static List<NonterminalExtensionDef> createImplicitExtensions(List<GrammarIndex> grammars) {
        return rewriteMembers(grammarExtensionMap(grammars), new HashMap<Id, List<NonterminalExtensionDef>>());
    }

    @Override
    public Node forApi(Api that) {
        if (this._globalEnv.definesApi(that.getName())) {
            this._currentApi = this._globalEnv.api(that.getName());
        } else {
            error("Undefined API ", that);
        }
        return super.forApi(that);
    }

    @Override
    public Node forGrammarDecl(GrammarDecl that) {
        Option<GrammarIndex> index = this.grammarIndex(that.getName());
        if (index.isSome()) {
            return rewriteGrammar(that, index.unwrap());
        } else {
            error("Grammar " + that.getName() + " not found", that);
        }
        return super.forGrammarDecl(that);
    }

    private Option<GrammarIndex> grammarIndex(Id name) {
        if (name.getApiName().isSome()) {
            APIName api = name.getApiName().unwrap();
            if (this._globalEnv.definesApi(api)) {
                return Option.some(_globalEnv.api(api).grammars().get(name.getText()));
            } else {
                return Option.none();
            }
        } else {
            return Option.some(((ApiIndex) _currentApi).grammars().get(name));
        }
    }

    private Node rewriteGrammar(GrammarDecl grammar, GrammarIndex index) {
        Debug.debug(Debug.Type.SYNTAX, 1, "Desugaring extensions for grammar " + grammar.getName());

        List<GrammarMemberDecl> members = grammar.getMembers();
        List<GrammarMemberDecl> newMembers = new ArrayList<GrammarMemberDecl>();
        Map<Id, List<NonterminalExtensionDef>> extMap = new HashMap<Id, List<NonterminalExtensionDef>>();

        // Split into defs, extensions
        // Group extensions by extended nonterminal
        split(members, newMembers, extMap);

        // Create mapping (NT => List<GrammarIndex>) of nonterminals
        //   with public extensions in imported grammars
        Map<Id, List<GrammarIndex>> grammarExtensionMap = grammarExtensionMap(index);

        newMembers.addAll(rewriteMembers(grammarExtensionMap, extMap));

        // Recombine into GrammarDecl
        GrammarDecl result = new GrammarDecl(grammar.getInfo(),
                                             grammar.getName(),
                                             grammar.getExtendsClause(),
                                             newMembers,
                                             grammar.getTransformers(),
                                             grammar.isNativeDef());

        // Comment this debug statement out until FortressAstToConcrete supports syntax abstraction nodes.
        // (Necessary because this desugarer is run on imported APIs with grammar definitions.)
        // EricAllen 7/6/2009
        //         Debug.debug(Debug.Type.SYNTAX, 3,
        //                     "Desugared grammar into:\n" + result.accept(new FortressAstToConcrete()));

        return result;
    }

    /* Given a map of nonterminal names to all the grammars that define it and
     * produce a list that contains nonterminals that include the inherited nonterminals
     * in the alternatives of the nonterminal.
     */
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

    /* return a map of nonterminal names to a list of grammars that have public nonterminals
     * why isn't this recursive? does it return the transitive closure
     * of extended grammars?
     */
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

    /* The goal of this function is to construct the complete nonterminal
     * given its name and a list of grammars that it extends. If this nonterminal
     * does not explicitly invoke 'super', then an implicit 'super' is added
     * to the bottom of the nonterminal's alternative.s
     */
    private static NonterminalExtensionDef combine(Id name,
                                                   List<NonterminalExtensionDef> extensions,
                                                   List<GrammarIndex> extendingGrammars) {

        Span theSpan = null;

        Set<Id> availableGrammarNames = new HashSet<Id>();
        Set<Id> usedGrammarNames = new HashSet<Id>();
        /* add all extended grammars to the list of grammar names */
        for (GrammarIndex g : extendingGrammars) {
            availableGrammarNames.add(g.getName());
        }

        Debug.debug(Debug.Type.SYNTAX, 3, "Extensions of " + name + " available from " + availableGrammarNames);

        List<SyntaxDecl> resultDecls = new ArrayList<SyntaxDecl>();
        for (NonterminalExtensionDef extension : extensions) {
            if (theSpan == null) theSpan = NodeUtil.getSpan(extension);
            for (SyntaxDecl decl : extension.getSyntaxDecls()) {
                /* if the syntax is a definition then add it directly */
                if (decl instanceof SyntaxDef) {
                    resultDecls.add(decl);
                    /* otherwise if it invokes an inherited definition then
                    * remove the grammar that contains the alternative from
                    * the list of available grammars and add it to the list of
                    * used grammars.
                    */
                } else if (decl instanceof SuperSyntaxDef) {
                    SuperSyntaxDef ssd = (SuperSyntaxDef) decl;
                    Id superGrammarName = ssd.getGrammarId();
                    if (availableGrammarNames.contains(superGrammarName)) {
                        availableGrammarNames.remove(superGrammarName);
                        usedGrammarNames.add(superGrammarName);
                        resultDecls.add(decl);
                    } else if (usedGrammarNames.contains(superGrammarName)) {
                        throw new MacroError(decl,
                                             "Extensions of " + name + " from " + superGrammarName +
                                             " have already been included.");
                    } else {
                        throw new MacroError(decl, "No extensions of " + name + " in grammar " + ssd.getGrammarId());
                    }
                }
            }
        }
        if (theSpan == null) theSpan = NodeUtil.getSpan(name); // FIXME: maybe pick a better span

        /* add an implicit super alternative for grammars that were not used */
        for (GrammarIndex eg : extendingGrammars) {
            if (availableGrammarNames.contains(eg.getName())) {
                resultDecls.add(new SuperSyntaxDef(NodeFactory.makeSpanInfo(theSpan),
                                                   Option.some("private"),
                                                   name,
                                                   eg.getName()));
            }
        }
        return new NonterminalExtensionDef(NodeFactory.makeSpanInfo(theSpan), name, resultDecls);
    }

    /* returns true if the grammar has any public members (not private/without) */
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
