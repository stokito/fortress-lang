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
 * Class which builds a table of pieces of Rats! AST which corresponds the macro 
 * declarations given as input.
 * The Rats! ASTs are combined to Rats! modules which are written to files on the
 * file system.
 * 
 */

package com.sun.fortress.syntax_abstractions;

import java.util.Collection;
import java.util.Map;

import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.phases.GrammarTranslator;
import com.sun.fortress.syntax_abstractions.phases.ItemDisambiguator;
import com.sun.fortress.syntax_abstractions.phases.ModuleResolver;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;

public class FileBasedMacroCompiler implements MacroCompiler {

	public Result compile(Map<GrammarDef, Boolean> grammars) {
		
		/*
		 * Compute a grammar environment
		 */
		GrammarEnvironment env = new GrammarEnvironment(grammars);
		if (!env.errors().isEmpty()) { return new Result(null, env.errors()); }
	
		/* 
		 * Resolve grammar extension and production extension, but leave the 
		 * the content of the productions untouched
		 */
		ModuleResolver.Result mrr = ModuleResolver.resolve(env);
		if (!mrr.isSuccessful()) { return new Result(null, mrr.errors()); }

//		for (Module m: mrr.modules()) {
//			System.err.print(m);
//		}
		
		/*
		 *  Disambiguation of item symbols
		 */
		ItemDisambiguator.Result er = ItemDisambiguator.disambiguateEnv(mrr.modules());
		if (!er.isSuccessful()) { return new Result(null, er.errors()); }			
	
		/*
		 * Translate each grammar to a corresponding Rats! module
		 */
		GrammarTranslator.Result gtr = GrammarTranslator.translate(er.modules());
		if (!gtr.isSuccessful()) { return new Result(null, gtr.errors()); }
		
		/*
		 * For each changed module write it to a file and run Rats! to 
		 * generate a temporary parser.
		 */
		RatsParserGenerator.Result rpgr = RatsParserGenerator.generateParser(gtr.modules(), gtr.keywords());
		if (!rpgr.isSuccessful()) { return new Result(null, rpgr.errors()); }

		/*
		 * Return the temporary parser
		 */
		return new Result(rpgr.parserClass());

//		/*
//		 * Translate each macro declaration and add it to a macro table
//		 */					
//		RatsMacroTable syntaxTable = new RatsMacroTable();
//		RatsMacroTranslator ratsMacroTranslator = new RatsMacroTranslator();
////		for (SyntaxDef syntaxDef: syntaxDefs ) {
////				syntaxDef.accept(ratsMacroTranslator);
////				syntaxTable.add(ratsMacroTranslator.getMacroDecl());				
////		}
//
//		RuleTranslator ruleTranslator = new RuleTranslator();
//		for (RatsMacroDecl ratsMacroDecl: syntaxTable.getAllMacroDecls()) {
//			ruleTranslator.applyRules(ratsMacroDecl.getRules());			
//		}
//		ruleTranslator.saveModules();
//		
//		
//		/* Run through all the Rats! modules from the Fortress grammar
//		 * which have been extended and generate the extending Rats! module.
//		 * For all the modules which are extended we need to change the
//		 * name used in Fortress.rats.
//		 */
//		List<Module> modules = new LinkedList<Module>();
//		for (ModuleEnum e: syntaxTable.getModules()) { 
//			modules.add(createRatsModule(e, syntaxTable.getMacroDecls(e)));
//		}

	}

}
