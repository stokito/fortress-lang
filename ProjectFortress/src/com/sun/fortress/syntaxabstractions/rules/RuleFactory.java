package com.sun.fortress.syntaxabstractions.rules;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.Analyzer;
import xtc.util.Runtime;

import com.sun.fortress.syntaxabstractions.rats.util.ModuleEnum;
import com.sun.fortress.syntaxabstractions.rules.Rule;
import com.sun.fortress.syntaxabstractions.util.SyntaxParam;

public class RuleFactory {

	public static Collection<Rule> getRules(String nonterminalName, int inx, List<SyntaxParam> syntaxParams) {
		Collection<Rule> cs = new LinkedList<Rule>();
		if (nonterminalName.equals("BlockElems")) {
			if (syntaxParams.size() > inx+1) {
				cs.add(getBlockElemsRule(syntaxParams.get(inx+1)));	
			}			
		}
		return cs;
	}
	
	private static Rule getBlockElemsRule(SyntaxParam syntaxParam) {
		return new BlockElemsRule(ModuleEnum.LOCALDECL, syntaxParam.getIdentifier());
	}
	
	

}
