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
import java.io.IOException;
import java.util.List;

import xtc.parser.ParserBase;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.Parser.Result;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.syntax_abstractions.ParserMaker;
import com.sun.fortress.syntax_abstractions.phases.Transform;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.rats.util.ParserMediator;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Debug;

/**
 * Methods for parsing files, including syntax extensions from imported
 * grammars. For an interface to the basic Fortress parser, see
 * com.sun.fortress.compiler.Parser.
 */
public class FortressParser {

    /** Parses a single file. */
    public static Result parse(APIName api_name, 
                               File f, 
                               GlobalEnvironment env, 
                               boolean verbose) {
        try {
            if (!ProjectProperties.noPreparse) {
                return parseInner(api_name, f, env, verbose);
            } else {
                return new Result(Parser.parseFileConvertExn(api_name, f),
                                  f.lastModified());
            }
        } catch (StaticError se) {
            return new Result(se);
        }
    }

    private static Result parseInner(APIName api_name,
                                     File f,
                                     final GlobalEnvironment env,
                                     boolean verbose) {
        // throws StaticError, ParserError

        List<GrammarIndex> grammars;
        try {
            grammars = PreParser.parse(api_name, f, env);
        } catch (StaticError se) {
            return new Result(se);
        }

        if (verbose) {
            System.err.println("Parsing file: "+f.getName());
        }
        if (grammars.isEmpty()) {
            return new Result(Parser.parseFileConvertExn(api_name, f), 
                              f.lastModified());
        } else {
            return parseWithGrammars(api_name, f, env, verbose, grammars);
        }
    }

    private static Result parseWithGrammars(APIName api_name,
                                            File f,
                                            GlobalEnvironment env,
                                            boolean verbose,
                                            List<GrammarIndex> grammars) {
        // throws StaticError, ParserError
        EnvFactory.initializeGrammarIndexExtensions(env.apis().values(), grammars);

        // Compile the syntax abstractions and create a temporary parser
        Class<?> temporaryParserClass = ParserMaker.parserForComponent(grammars);

        Debug.debug( Debug.Type.SYNTAX, 2, "Created temporary parser" );

        BufferedReader in = null; 
        try {
            in = Useful.utf8BufferedFileReader(f);
            ParserBase p =
                ParserMediator.getParser(api_name, temporaryParserClass, in, f.toString());
            CompilationUnit original = Parser.checkResultCU(ParserMediator.parse(p), p, f.getName());
            // dump(original, "original-" + f.getName());
            CompilationUnit cu = (CompilationUnit) Transform.transform(env, original);
            // dump(cu, "dump-" + f.getName());
            return new Result(cu, f.lastModified());
        } catch (Exception e) {
            String desc =
                "Error occurred while instantiating and executing a temporary parser: "
                + temporaryParserClass.getCanonicalName();
            e.printStackTrace();
            if (e.getMessage() != null) { desc += " (" + e.getMessage() + ")"; }
            return new Result(StaticError.make(desc, f.toString()));
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
        }
    }

    /* Utilities */

    @SuppressWarnings("unused")
	private static void dump( Node node, String name ){
        try{
            ASTIO.writeJavaAst( (CompilationUnit) node, name );
            Debug.debug( Debug.Type.SYNTAX, 1, "Dumped node to " + name );
        } catch ( IOException e ){
            e.printStackTrace();
        }
    }
}
