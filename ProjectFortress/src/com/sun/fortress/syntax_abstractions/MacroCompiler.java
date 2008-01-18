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
 * Interface for macro compilers
 * 
 */

package com.sun.fortress.syntax_abstractions;

import java.util.Collection;
import java.util.Map;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.GrammarDef;

public interface MacroCompiler {

	public static class Result extends StaticPhaseResult {
		private Class<?> parserClass;
		
		public Result(Class<?> parserClass) {
			super();
			this.parserClass = parserClass;
		}
		
		public Result(Class<?> parserClass,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.parserClass = parserClass;
		}
		
		public Class<?> getParserClass() { return parserClass; }
	}
	
	public Result compile(Collection<GrammarEnv> grammars, GlobalEnvironment env);
	
}
