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

package com.sun.fortress.syntax_abstractions.rats;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.tools.javac.parser.Keywords;

import xtc.parser.*;
import xtc.tree.Attribute;

public class KeywordModule {

	public void addKeywords(Set<String> keywords, String tempDir) {
		String filename = tempDir+RatsUtil.getModulePath()+"Keyword.rats";
		Module module = RatsUtil.getRatsModule(filename);

		// Adding keyword to the set of FORTRESS_KEYWORDS
		List<String> code = module.body.code;
		String insertString = "";
		int inx = 0;
		// Locate insertion point
		for (inx=0; inx<code.size(); inx++) {
			if (code.get(inx).contains("}")) {
				insertString = code.get(inx);
				break;
			}
		}
		for (String keyword: keywords) {
			int insertPoint = insertString.indexOf("}");
			if (insertPoint < 0) {
				throw new RuntimeException("Expected to add keyword, but found no insert point "+insertString);
			}
			else {
				if (!com.sun.fortress.parser.Fortress.FORTRESS_KEYWORDS.contains(keyword)) {
					insertString = insertString.substring(0, insertPoint) + ", \""+keyword+"\""+insertString.substring(insertPoint);
				}
			}
		}
		code.remove(inx);
		code.add(inx, insertString);

		// Adding production for keyword
		for (String keyword: keywords) {
			if (!com.sun.fortress.parser.Fortress.FORTRESS_KEYWORDS.contains(keyword)) {
				List<Attribute> attributes = new LinkedList<Attribute>();
				attributes.add(new Attribute("String"));

				List<Sequence> sequence = new LinkedList<Sequence>();
				List<Element> elements = new LinkedList<Element>();
				elements.add(new StringLiteral(keyword));
				List<Element> idRest = new LinkedList<Element>();
				idRest.add(new NonTerminal("idrest"));
				elements.add(new NotFollowedBy(new Sequence(idRest)));
				sequence.add(new Sequence(elements));

				OrderedChoice orderedChoice = new OrderedChoice(sequence);
				module.productions.add(new FullProduction(attributes, "", new NonTerminal(keyword), orderedChoice));
			}
		}
		RatsUtil.writeRatsModule(module, tempDir);
	}
}
