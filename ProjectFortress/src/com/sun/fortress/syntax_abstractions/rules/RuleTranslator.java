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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleEnum;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;

import xtc.parser.GrammarVisitor;
import xtc.parser.Module;

/**
 * Class which applies a rule to a module
 * 
 */
public class RuleTranslator {
	
	private Map<ModuleEnum, Module> moduleCache = new HashMap<ModuleEnum, Module>();
	
	public void applyRule(Rule rule) {
		Module module = moduleCache.get(rule.getModule());
		if (module == null) {
			module = RatsUtil.getRatsModule(rule.getModule());
			moduleCache.put(rule.getModule(), module);
		}
		rule.getRuleRewriter().dispatch(module);
	}
	
	public void applyRules(Collection<Rule> rules) {
		for (Rule rule: rules) {
			this.applyRule(rule);
		}
	}


//	private void copyFortressGrammarFiles() {
//		String parserPath = RatsUtil.getParserPath();
//		String tmpParserPath = RatsUtil.getTempParserDir();
//		try {
//			copy(parserPath+"Fortress.rats", tmpParserPath+"Fortress.rats");
//			copy(parserPath+"Compilation.rats", tmpParserPath+"Compilation.rats");
//			copy(parserPath+"Declaration.rats", tmpParserPath+"Declaration.rats");
//			copy(parserPath+"Syntax.rats", tmpParserPath+"Syntax.rats");
//			copy(parserPath+"TraitObject.rats", tmpParserPath+"TraitObject.rats");
//			copy(parserPath+"Function.rats", tmpParserPath+"Function.rats");
//			copy(parserPath+"Parameter.rats", tmpParserPath+"Parameter.rats");
//			copy(parserPath+"Method.rats", tmpParserPath+"Method.rats");
//			copy(parserPath+"MethodParam.rats", tmpParserPath+"MethodParam.rats");
//			copy(parserPath+"Variable.rats", tmpParserPath+"Variable.rats");
//			copy(parserPath+"Field.rats", tmpParserPath+"Field.rats");
//			copy(parserPath+"AbsField.rats", tmpParserPath+"AbsField.rats");
//			copy(parserPath+"Header.rats", tmpParserPath+"Header.rats");
//			copy(parserPath+"OtherDecl.rats", tmpParserPath+"OtherDecl.rats");
//			copy(parserPath+"Type.rats", tmpParserPath+"Type.rats");
//			copy(parserPath+"Expression.rats", tmpParserPath+"Expression.rats");
//			copy(parserPath+"DelimitedExpr.rats", tmpParserPath+"DelimitedExpr.rats");
//			copy(parserPath+"NoNewlineExpr.rats", tmpParserPath+"NoNewlineExpr.rats");
//			copy(parserPath+"NoSpaceExpr.rats", tmpParserPath+"NoSpaceExpr.rats");
//			copy(parserPath+"LocalDecl.rats", tmpParserPath+"LocalDecl.rats");
//			copy(parserPath+"Identifier.rats", tmpParserPath+"Identifier.rats");
//			copy(parserPath+"Symbol.rats", tmpParserPath+"Symbol.rats");
//			copy(parserPath+"Spacing.rats", tmpParserPath+"Spacing.rats");
//			copy(parserPath+"Unicode.rats", tmpParserPath+"Unicode.rats");
//			copy(parserPath+"Literal.rats", tmpParserPath+"Literal.rats");
//			copy(parserPath+"Keyword.rats", tmpParserPath+"Keyword.rats");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}


}
