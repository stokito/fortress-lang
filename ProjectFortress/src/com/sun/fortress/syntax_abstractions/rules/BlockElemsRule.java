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

package com.sun.fortress.syntax_abstractions.rules;

import java.util.LinkedList;
import java.util.List;

import xtc.parser.Analyzer;
import xtc.parser.Element;
import xtc.parser.GrammarVisitor;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.util.Runtime;

import com.sun.fortress.syntax_abstractions.rats.util.ModuleEnum;

public class BlockElemsRule extends Rule {

	public BlockElemsRule(ModuleEnum localdecl,	String s) {
	    Analyzer ana = new Analyzer();
	    Runtime runtime = new xtc.util.Runtime();
		BlockElemsRuleVisitor visitor = new BlockElemsRuleVisitor(runtime, ana, s);
		this.module = localdecl;
		this.ruleRewriter = visitor;
	}

	private class BlockElemsRuleVisitor extends GrammarVisitor {

		private boolean insideBlockElems;
		private boolean insideChoice;
		private String nonTerminal;
		
		public BlockElemsRuleVisitor(Runtime runtime, Analyzer analyzer, String nonTerminal) {
			super(runtime, analyzer);
			this.insideBlockElems = false;
			this.insideChoice = false;
			this.nonTerminal = nonTerminal;
		}

		@Override
		public Element visit(OrderedChoice orderedChoice) {
			boolean insideChoice = this.insideChoice;
			
			if (this.insideBlockElems && this.insideChoice) {
				System.err.println("Before: "+orderedChoice.alternatives);
				for (Sequence s: orderedChoice.alternatives) {
					for (Element e: s.elements) {
						NonTerminal nt = (NonTerminal) e;
						//System.err.println("\""+nt.name+"\"");
					}
				}
				List<Element> elems = new LinkedList<Element>();
				elems.add(new NonTerminal("w"));
				elems.add(new NonTerminal(this.nonTerminal));
				Sequence sequence = new Sequence(elems);
				orderedChoice.alternatives.add(sequence);
				System.err.println("After: "+orderedChoice.alternatives);
			}
			
			if (this.insideBlockElems && !insideChoice) {
				this.insideChoice = true;
			}
			
			Element result = super.visit(orderedChoice);	
			
			if (this.insideBlockElems && !insideChoice) {
				this.insideChoice = false;
			}
			return result;
		}

		@Override
		public Production visit(Production production) {
			if (production.name.name.equals("BlockElems")) {
				this.insideBlockElems = true;
			}
			Production result = super.visit(production); 
			this.insideBlockElems = false;
			return result;
		}
	}
}
