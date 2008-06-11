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

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.phases.GrammarTranslator;
import com.sun.fortress.syntax_abstractions.phases.ModuleTranslator;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;
import com.sun.fortress.useful.Debug;

public class FileBasedMacroCompiler implements MacroCompiler {

	public Result compile(Collection<GrammarIndex> grammarIndexs, GlobalEnvironment env) {

//	    for(GrammarIndex g: grammarIndexs) {
//	        System.err.println(g.getName() + ", "+ g.isToplevel());
//	    }
    
		/*
		 * Initialize GrammarIndex
		 */
		GrammarIndexInitializer.Result giir = GrammarIndexInitializer.init(grammarIndexs); 
		if (!giir.isSuccessful()) { return new Result(giir.errors()); }
	
		/* 
		 * Resolve grammar extensions and extensions of nonterminal definitions.
		 */
		ModuleTranslator.Result mrr = ModuleTranslator.translate(grammarIndexs);
		if (!mrr.isSuccessful()) { return new Result(mrr.errors()); }

                Debug.debug( 3, GrammarEnv.getDump() );

                for (Module m: mrr.modules()) {
                    Debug.debug( 3, m.toString() );
                }
                /*
		if (ProjectProperties.debug) {
			for (Module m: mrr.modules()) {
				System.err.println(m);
			}
		}
                */
        
		/*
		 * Translate each grammar to a corresponding Rats! module
		 */
		GrammarTranslator.Result gtr = GrammarTranslator.translate(mrr.modules());
		if (!gtr.isSuccessful()) { return new Result(gtr.errors()); }

		/*
		 * For each changed module write it to a file and run Rats! to 
		 * generate a temporary parser.
		 */
		RatsParserGenerator.Result rpgr = RatsParserGenerator.generateParser(gtr.modules());
		if (!rpgr.isSuccessful()) { return new Result(rpgr.errors()); }

		/*
		 * Return the temporary parser
		 */
		return new Result(rpgr.parserClass());
	}

}
