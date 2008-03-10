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

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.phases.GrammarTranslator;
import com.sun.fortress.syntax_abstractions.phases.ModuleTranslator;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;

public class FileBasedMacroCompiler implements MacroCompiler {

	public Result compile(Collection<GrammarEnv> envs, GlobalEnvironment env) {
		
		/*
		 * Initialize GrammarIndex
		 */
		GrammarIndexInitializer.Result geir = GrammarIndexInitializer.init(envs); 
		if (!geir.isSuccessful()) { return new Result(null, geir.errors()); }
		
		/* 
		 * Resolve grammar extensions and extensions of nonterminal definitions.
		 */
		ModuleTranslator.Result mrr = ModuleTranslator.translate(envs);
		if (!mrr.isSuccessful()) { return new Result(null, mrr.errors()); }
		
//		for (Module m: mrr.modules()) {
			// System.err.println(m);
//		}
		
		/*
		 * Translate each grammar to a corresponding Rats! module
		 */
		GrammarTranslator.Result gtr = GrammarTranslator.translate(mrr.modules(), env);
		if (!gtr.isSuccessful()) { return new Result(null, gtr.errors()); }
		
		/*
		 * For each changed module write it to a file and run Rats! to 
		 * generate a temporary parser.
		 */
		RatsParserGenerator.Result rpgr = RatsParserGenerator.generateParser(gtr.modules());
		if (!rpgr.isSuccessful()) { return new Result(null, rpgr.errors()); }

		/*
		 * Return the temporary parser
		 */
		return new Result(rpgr.parserClass());
	}

}
