/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
 * Class traversing a macro declaration and creates a corresponding Rats!
 * macro declaration
 * 
 */

package com.sun.fortress.syntaxabstractions.old;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import xtc.parser.Action;
import xtc.parser.Element;
import xtc.parser.NonTerminal;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;

import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.syntaxabstractions.rats.util.FreshName;
import com.sun.fortress.syntaxabstractions.rats.util.ModuleEnum;
import com.sun.fortress.syntaxabstractions.rats.util.ModuleInfo;
import com.sun.fortress.syntaxabstractions.rats.util.ProductionEnum;
import com.sun.fortress.syntaxabstractions.rules.RuleFactory;
import com.sun.fortress.syntaxabstractions.util.ActionCreater;
import com.sun.fortress.syntaxabstractions.util.SyntaxParam;

import edu.rice.cs.plt.tuple.Option;

public class RatsMacroTranslator extends NodeDepthFirstVisitor_void {

	private RatsMacroDecl ratsMacroDecl;
	/* keywords are collected so they can be written out into the Keyword.rats file */
	private Set<String> keywords;
		
	public RatsMacroTranslator() {
		super();
		this.keywords = new HashSet<String>();
	}

	public RatsMacroDecl getMacroDecl() {
		return this.ratsMacroDecl;
	}

	@Override
	public void forSyntaxDef(SyntaxDef that) {
//		ProductionEnum production = ModuleInfo.getProductionFromSyntaxParamType(Option.unwrap(that.getTypedSyntaxParam().getB()));
//		ModuleEnum module = ModuleInfo.getModuleFromProduction(production);
//		
//		ratsMacroDecl = new RatsMacroDecl(module, production);
//		
//		// Which other Rats! modules does this module depend on?
//		DependencyResolver dependencyResolver = new DependencyResolver();
//		dependencyResolver.resolveDependencies(that.getBody().getSyntaxHeaderFront());
//		ratsMacroDecl.addParameters(dependencyResolver.getParameters());
//		ratsMacroDecl.addDependencies(dependencyResolver.getDependencies());
//		
//		SyntaxParamCollector syntaxParamCollector = new SyntaxParamCollector();
//		List<SyntaxParam> syntaxParams = syntaxParamCollector.getSyntaxParams(that.getBody().getSyntaxHeaderFront());
//		for (SyntaxParam syntaxParam: syntaxParams) {
//			if (syntaxParam.isKeyword()) {
//				this.keywords.add(syntaxParam.getIdName().stringName());
//			}
//		}
//		
//		List<Sequence> seq = new LinkedList<Sequence>();
//
//		List<Element> elms = new LinkedList<Element>();
//		for (int inx = 0; inx < syntaxParams.size(); inx++) {
//			SyntaxParam syntaxParam = syntaxParams.get(inx);
//			if (syntaxParam.isKeyword()) {
//				elms.add(new NonTerminal(syntaxParam.getIdentifier()));
//			}
//			else {
//				String nonterminalName = syntaxParam.getType().getName().stringName();
//				ratsMacroDecl.addRules(RuleFactory.getRules(nonterminalName, inx, syntaxParams));
//				elms.add(new NonTerminal(nonterminalName));
//			}
//			if (inx != syntaxParams.size()-1) {
//				elms.add(new NonTerminal("w"));
//			}
//		}
//		elms.add(FortressObjectTranslator.translate(that.getBody().getBody()));	
//
//		seq.add(new Sequence(new SequenceName(FreshName.getFreshName(production.toString()).toUpperCase()), elms )); // TODO: generate freshname
//		ratsMacroDecl.setSequence(seq);
	}

	/**
	 * Returns a list of keywords
	 * @return
	 */
	public Set<String> getKeywords() {
		return keywords;
	}


	
}
