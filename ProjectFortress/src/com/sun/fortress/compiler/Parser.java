/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import java.io.File;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.io.IOUtil;

import xtc.parser.ParserBase;
import xtc.parser.SemanticValue;
import xtc.parser.ParseError;
//import xtc.parser.Result; // Not imported to prevent name clash.
import com.sun.fortress.parser.Fortress; // Shadows Fortress in this package
import com.sun.fortress.parser.preparser.PreFortress;
import com.sun.fortress.parser_util.SyntaxChecker;

import com.sun.fortress.Shell;
import com.sun.fortress.useful.Files;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.TemplateNodeDepthFirstVisitor_void;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.syntax_abstractions.ParserMaker;
import com.sun.fortress.syntax_abstractions.phases.Transform;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

/**
 * Methods for parsing files using base Fortress syntax.
 * For a parser that respects abstractions, use
 * com.sun.fortress.syntax_abstractions.parser.FortressParser.
 */
public class Parser {

    public static class Result extends StaticPhaseResult {
        private final Iterable<Api> _apis;
        private final Iterable<Component> _components;
        private long _lastModified;

        public Result(CompilationUnit cu, long lastModified) {
            if (cu instanceof Api) {
                _apis = IterUtil.singleton((Api)cu);
                _components = IterUtil.empty();
            } else if (cu instanceof Component) {
                _apis = IterUtil.empty();
                _components = IterUtil.singleton((Component)cu);
            } else {
                throw new RuntimeException("Unexpected parse result: " + cu);
            }
            _lastModified = lastModified;
        }

        public Result(StaticError error) {
            super(IterUtil.singleton(error));
            _apis = IterUtil.empty();
            _components = IterUtil.empty();
        }

        public Result(Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = IterUtil.empty();
            _components = IterUtil.empty();
        }

        public Iterable<Api> apis() { return _apis; }
        public Iterable<Component> components() { return _components; }
        public long lastModified() { return _lastModified; }
    }

    /** Parses a single file. */
    public static Result macroParse(File f,
                                    final GlobalEnvironment env,
                                    boolean verbose) {
        try {
            if ( Shell.getPreparse() ) { // if not bypass the syntactic abstraction
                CompilationUnit cu = Parser.preparseFileConvertExn(f); // preparse
                List<GrammarIndex> grammars; // directly imported grammars
                if (cu instanceof Component) {
                    Component c = (Component) cu;
                    List<GrammarIndex> result = getImportedGrammars(c, env);
                    if (!result.isEmpty()) {
                        Debug.debug(Debug.Type.SYNTAX, "Component: ",
                                    c.getName(), " imports grammars...");
                    }
                    grammars = result;
                } else { // empty for an API
                    grammars = new LinkedList<GrammarIndex>();
                }
                if (verbose) System.err.println("Parsing file: "+f.getName());
                if (grammars.isEmpty()) { // without new syntax
                    return new Result(Parser.parseFileConvertExn(f),
                                      f.lastModified());
                } else { // with new syntax
                    return parseWithGrammars(f, env, verbose, grammars);
                }
            } else { // if bypass the syntactic abstraction
                return new Result(Parser.parseFileConvertExn(f),
                                  f.lastModified());
            }
        } catch (StaticError se) {
            return new Result(se);
        }
    }

    private static List<GrammarIndex> getImportedGrammars(Component c,
                                                          final GlobalEnvironment env) {
        final List<GrammarIndex> grammars = new ArrayList<GrammarIndex>();
        c.accept(new TemplateNodeDepthFirstVisitor_void() {
                @Override public void forImportApiOnly(ImportApi that) {
                    bug(that, errorMsg("NYI 'Import api APIName'; ",
                                       "try 'import APIName.{...}' instead."));
                }

                @Override public void forImportStarOnly(ImportStar that) {
                    APIName api = that.getApiName();
                    if (env.definesApi(api)) {
                        for (GrammarIndex g: env.api(api).grammars().values()) {
                            if (!that.getExceptNames().contains(g.getName())) {
                                grammars.add(g);
                            }
                        }
                    } else {
                        StaticError.make("Undefined API: "+api, that);
                    }
                }

                @Override public void forImportNamesOnly(ImportNames that) {
                    APIName api = that.getApiName();
                    if (env.definesApi(api)) {
                        ApiIndex apiIndex = env.api(api);
                        for (AliasedSimpleName aliasedName: that.getAliasedNames()) {
                            if (aliasedName.getName() instanceof Id) {
                                Id importedName = (Id) aliasedName.getName();
                                if (apiIndex.grammars().containsKey(importedName.getText())) {
                                    grammars.add(apiIndex.grammars().get(importedName.getText()));
                                }
                            }
                        }
                    } else {
                        StaticError.make("Undefined API: "+api, that);
                    }
                }
            });
        return grammars;
    }

    private static Result parseWithGrammars(File f,
                                            GlobalEnvironment env,
                                            boolean verbose,
                                            List<GrammarIndex> grammars) {
        EnvFactory.initializeGrammarIndexExtensions(env.apis().values(), grammars);

        // Compile the syntax abstractions and create a temporary parser
        Class<?> temporaryParserClass = ParserMaker.parserForComponent(grammars);

        Debug.debug( Debug.Type.SYNTAX, 2, "Created a temporary parser." );

        BufferedReader in = null;
        try {
            in = Useful.utf8BufferedFileReader(f);
            ParserBase p =
                RatsUtil.getParser(temporaryParserClass, in, f.toString());
            CompilationUnit original = Parser.checkResultCU(RatsUtil.parse(p), p, f.getName());
            // dump(original, "original-" + f.getName());
            CompilationUnit cu = (CompilationUnit) Transform.transform(env, original);
            // dump(cu, "dump-" + f.getName());
            return new Result(cu, f.lastModified());
        } catch (Exception e) {
            String desc =
                "Error occurred while instantiating and executing a temporary parser: "
                + temporaryParserClass.getCanonicalName();
            if (Debug.isOnMax()) {
                e.printStackTrace();
            } else {
                System.err.println(Shell.turnOnDebugMessage);
            }
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

    /**
     * Parses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see also description of exceptions there).
     * Converts checked exceptions like IOException and FileNotFoundException
     * to StaticError with appropriate error message.
     */
    public static CompilationUnit parseFileConvertExn(File file) {
        try {
            preparseFileConvertExn(file);
            BufferedReader in = Useful.utf8BufferedFileReader(file);
            String filename = file.getCanonicalPath();
            try {
                Fortress parser = new Fortress(in, filename);
                xtc.parser.Result parseResult = parser.pFile(0);
                return checkResultCU(parseResult, parser, filename);
            } finally {
                Files.rm( filename + ".parserError.log" );
                in.close();
            }
        } catch (FileNotFoundException fnfe) {
            throw convertExn(fnfe, file);
        } catch (IOException ioe) {
            throw convertExn(ioe, file);
        }
    }

    /**
     * Parses a string as a compilation unit. See parseFile above.
     */
    public static CompilationUnit parseString(APIName api_name, String buffer) throws IOException {
        // Also throws StaticError, ParserError
        BufferedReader in = Useful.bufferedStringReader(buffer);
        try {
            Fortress parser = new Fortress(in, api_name.getText());
            xtc.parser.Result parseResult = parser.pFile(0);
            return checkResultCU(parseResult, parser, api_name.getText());
        } finally {
            in.close();
        }
    }

    private static boolean beginError(String line) {
        return (! line.startsWith(" "));
    }

    /**
     * Checks that a xtc.parser.Result is contains a CompilationUnit,
     * and checks the filename for the appropriate suffix.
     * Throws a ParserError (note, subtype of StaticError) if the parse fails.
     * Throws a StaticError if the filename has the wrong suffix.
     */
    public static CompilationUnit checkResultCU(xtc.parser.Result parseResult,
                                                ParserBase parser,
                                                String filename) throws IOException {
        String parserLogFile;
        boolean isParse = false;
        if ( parser instanceof Fortress ) {
            parserLogFile = filename + ".parserError.log";
            isParse = true;
        } else if ( parser instanceof PreFortress ) {
            parserLogFile = filename + ".preparserError.log";
        } else {
            parserLogFile = filename + ".macroError.log";
        }
        File parserLog = new File( parserLogFile );

        if (parseResult.hasValue()) {
            CompilationUnit cu = (CompilationUnit)((SemanticValue) parseResult).value;
            if ( isParse ) {
                String syntaxLogFile = filename + ".syntaxError.log";
                cu.accept( new SyntaxChecker( Useful.filenameToBufferedWriter( syntaxLogFile ) ) );
                File syntaxLog = new File( syntaxLogFile );
                if ( parserLog.length() + syntaxLog.length() != 0 ) {
                    BufferedReader reader = Useful.filenameToBufferedReader( parserLogFile );
                    String line = reader.readLine();
                    ArrayList<StaticError> errors = new ArrayList<StaticError>();
                    String message = "";
                    while ( line != null ) {
                        if ( beginError(line) && !message.equals("") ) {
                            errors.add(StaticError.make(message.substring(0,message.length()-1)));
                            message = line + "\n";
                        } else
                            message += line + "\n";
                        line = reader.readLine();
                    }
                    if ( !message.equals("") )
                        errors.add(StaticError.make(message.substring(0,message.length()-1)));
                    reader = Useful.filenameToBufferedReader( syntaxLogFile );
                    line = reader.readLine();
                    while ( line != null ) {
                        if ( beginError(line) && !message.equals("") ) {
                            errors.add(StaticError.make(message.substring(0,message.length()-1)));
                            message = line + "\n";
                        } else
                            message += line + "\n";
                        line = reader.readLine();
                    }
                    if ( !message.equals("") )
                        errors.add(StaticError.make(message.substring(0,message.length()-1)));
                    Files.rm( parserLogFile );
                    Files.rm( syntaxLogFile );
                    throw new MultipleStaticError(errors);
                } else {
                    Files.rm( parserLogFile );
                    Files.rm( syntaxLogFile );
                }
            }
            else {
                if ( parserLog.length() != 0 ) {
                    BufferedReader reader = Useful.filenameToBufferedReader( parserLogFile );
                    String line = reader.readLine();
                    ArrayList<StaticError> errors = new ArrayList<StaticError>();
                    String message = "";
                    while ( line != null ) {
                        if ( beginError(line) && !message.equals("") ) {
                            errors.add(StaticError.make(message.substring(0,message.length()-1)));
                            message = line + "\n";
                        } else
                            message += line + "\n";
                        line = reader.readLine();
                    }
                    if ( !message.equals("") )
                        errors.add(StaticError.make(message.substring(0,message.length()-1)));
                    Files.rm( parserLogFile );
                    throw new MultipleStaticError(errors);
                } else {
                    Files.rm( parserLogFile );
                }
            }

            if (cu instanceof Api) {
                if (filename.endsWith(ProjectProperties.API_SOURCE_SUFFIX)) {
                    return (Api)cu;
                } else {
                    throw StaticError.make("Api files must have suffix "
                                           + ProjectProperties.API_SOURCE_SUFFIX,
                                           (Api)cu);
                }
            } else if (cu instanceof Component) {
                if (filename.endsWith(ProjectProperties.COMP_SOURCE_SUFFIX)) {
                    return (Component)cu;
                } else {
                    throw StaticError.make("Component files must have suffix "
                                           + ProjectProperties.COMP_SOURCE_SUFFIX,
                                           (Component)cu);
                }
            } else {
                throw new RuntimeException("Unexpected parse result: " + cu);
            }
        } else {
            if ( parserLog.length() != 0 ) {
                System.err.println("Syntax error(s):");
                BufferedReader reader = Useful.filenameToBufferedReader( parserLogFile );
                String line = reader.readLine();
                while ( line != null ) {
                    System.err.println( line );
                    line = reader.readLine();
                }
                Files.rm( parserLogFile );
                throw new ParserError(new xtc.parser.ParseError("", 0), parser);
            } else {
                Files.rm( parserLogFile );
            }
            throw new ParserError((ParseError) parseResult, parser);
        }
    }

    // Pre-parser

    /**
     * Preparses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see also description of exceptions there).
     * Converts checked exceptions like IOException and FileNotFoundException
     * to StaticError with appropriate error message.
     */
    public static CompilationUnit preparseFileConvertExn(File file) {
        try {
            BufferedReader in = Useful.utf8BufferedFileReader(file);
            String filename = file.getCanonicalPath();
            try {
                PreFortress parser = new PreFortress(in, filename);
                xtc.parser.Result parseResult = parser.pFile(0);
                return checkResultCU(parseResult, parser, filename);
            } finally {
                Files.rm( filename + ".preparserError.log" );
                in.close();
            }
        } catch (FileNotFoundException fnfe) {
            throw convertExn(fnfe, file);
        } catch (IOException ioe) {
            throw convertExn(ioe, file);
        }
    }

    private static StaticError convertExn(IOException ioe, File f) {
        String desc = "Unable to read file";
        if (ioe.getMessage() != null) { desc += " (" + ioe.getMessage() + ")"; }
        return StaticError.make(desc, f.toString());
    }

    private static StaticError convertExn(FileNotFoundException fnfe, File f) {
        return StaticError.make("Cannot find file " + f.getName(), f.toString());
    }

}
