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

package com.sun.fortress.syntax_abstractions.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import xtc.parser.ParseError;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.syntax_abstractions.GrammarEnv;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.iter.IterUtil;

/**
 * Methods to parse a file to a collection of API ASTs which define
 * the syntax used in the file.
 */
public class PreParser {

	   public static class Result extends StaticPhaseResult {
	        private Collection<GrammarEnv> grammars;
	        
	        public Result(Collection<GrammarEnv> grammars) {
	        	this.grammars = grammars;
	        }

	        public Result(StaticError error) {
	            super(IterUtil.singleton(error));
	            this.grammars = new LinkedList<GrammarEnv>();
	        }
	        
	        public Result(Iterable<? extends StaticError> errors) {
	            super(errors);
	            grammars = new LinkedList<GrammarEnv>();
	        }

	        public Result(Result r1, Result r2) {
	            super(r1, r2);
	            this.grammars = new LinkedList<GrammarEnv>();
	            grammars.addAll(r1.grammars);
	            grammars.addAll(r2.grammars);
	        }

	        public Collection<GrammarEnv> getGrammars() { 
	        	return this.grammars;
	        }
	    }
	
	/**
	 * Convert to a filename that is canonical for each (logical) file, preventing
	 * reparsing the same file.
	 */
	private static File canonicalRepresentation(File f) {
		// treat the same absolute path as the same file; different absolute path but
		// the same *canonical* path (a symlink, for example) is treated as two
		// different files; if absolute file can't be determined, assume they are
		// distinct.
		return IOUtil.canonicalCase(IOUtil.attemptAbsoluteFile(f));
	}

	/** Parses a single file. */
	public static Result parse(File f, GlobalEnvironment env) {
		
		Parser.Result pr = parseFile(f);
		if (!pr.isSuccessful()) { return new Result(pr.errors()); }
		
		// TODO: Check that result only contains at most one component

		ImportedApiCollector namesAndImports = new ImportedApiCollector(env);
		ImportedApiCollector.Result result = namesAndImports.new Result(new LinkedList<GrammarEnv>());
		for (Component c: pr.components()) {
			result = result.add(namesAndImports.collectApis(c));
			if (!result.grammars().isEmpty()) {
				System.err.println("Component: "+c.getName()+" imports grammars...");	
			}
		}
		if (!pr.isSuccessful()) { return new Result(result.errors()); }

		return new Result(result.grammars());
	}
	
	private static Parser.Result parseFile(File f) {
		try {
			BufferedReader in = Useful.utf8BufferedFileReader(f);
			try {
				com.sun.fortress.parser.preparser.PreFortress p =
					new com.sun.fortress.parser.preparser.PreFortress(in, f.toString());
				xtc.parser.Result parseResult = p.pFile(0);
				if (parseResult.hasValue()) {
					Object cu = ((SemanticValue) parseResult).value;
					if (cu instanceof Api) {
						return new Parser.Result((Api) cu, f.lastModified());
					}
					else if (cu instanceof Component) {
						return new Parser.Result((Component) cu, f.lastModified());
					}
					else {
						throw new RuntimeException("Unexpected parse result: " + cu);
					}
				}
				else {
					return new Parser.Result(new Parser.Error((ParseError) parseResult, p));
				}
			}
			finally { in.close(); }
		}
		catch (FileNotFoundException e) {
			return new Parser.Result(StaticError.make("Cannot find file " + f.getName(),
					f.toString()));
		}
		catch (IOException e) {
			String desc = "Unable to read file";
			if (e.getMessage() != null) { desc += " (" + e.getMessage() + ")"; }
			return new Parser.Result(StaticError.make(desc, f.toString()));
		}
	}

	/** Get the filename in which the given API should be defined. */
	private static File fileForApiName(APIName api) {
		return new File(NodeUtil.nameString(api) + ".fsi");
	}
}
