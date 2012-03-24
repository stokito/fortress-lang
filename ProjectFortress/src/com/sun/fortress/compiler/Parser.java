/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.io.File;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.sun.fortress.parser.import_collector.ImportCollector;
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

    /** Parse a single file. */
    public static Result macroParse(File f,
                                    final GlobalEnvironment env,
                                    boolean verbose) {
        try {
            if ( Shell.withMacro() ) { // if not bypass the syntactic abstraction
                // parse only the import statements
                CompilationUnit cu = importCollector(f);
                List<GrammarIndex> grammars; // directly imported grammars
                if (cu instanceof Component) {
                    Component c = (Component) cu;
                    List<GrammarIndex> result = getImportedGrammars(c, env);
                    if (!result.isEmpty()) {
                        Debug.debug(Debug.Type.SYNTAX, 2, "Component: ",
                                    c.getName(), " imports grammars...");
                    }
                    grammars = result;
                } else { // empty for an API
                    grammars = new LinkedList<GrammarIndex>();
                }
                if (verbose) System.err.println("Parsing file: "+f.getName());
                if (grammars.isEmpty()) { // without new syntax
                    return new Result(parseFileConvertExn(f),
                                      f.lastModified());
                } else { // with new syntax
                    return parseWithGrammars(f, env, verbose, grammars);
                }
            } else { // if bypass the syntactic abstraction
                return new Result(parseFileConvertExn(f),
                                  f.lastModified());
            }
        } catch (StaticError se) {
            return new Result(se);
        } finally {
            try {
                Files.rm( ProjectProperties.preparserErrorLog(f) );
            } catch (IOException ioe) {}
            try {
                Files.rm( ProjectProperties.macroErrorLog(f) );
            } catch (IOException ioe) {}
        }
    }

    /**
     * Return a partial compilation unit including import statements.
     */
    public static CompilationUnit importCollector(File file) {
        try {
            String filename = file.getCanonicalPath();
            BufferedReader in = Useful.utf8BufferedFileReader(file);
            ImportCollector collector = new ImportCollector(in, filename);
            xtc.parser.Result collectorResult = collector.pFile(0);
            try {
                CompilationUnit cu = checkResultCU(collectorResult, collector, filename);
                return cu;
            } catch (ParserError err) {
                in.close();
                in = Useful.utf8BufferedFileReader(file);
                reportSyntaxErrors( preParser(in, filename) );
                throw err;
            } finally {
                in.close();
            }
        } catch (FileNotFoundException fnfe) {
            throw convertExn(fnfe, file);
        } catch (IOException ioe) {
            throw convertExn(ioe, file);
        }
    }

    /**
     * Checks that a xtc.parser.Result contains a CompilationUnit,
     * and checks the filename for the appropriate suffix.
     * Throws a ParserError (note, subtype of StaticError) if the parse fails.
     * Throws a StaticError if the filename has the wrong suffix.
     */
    private static CompilationUnit checkResultCU(xtc.parser.Result parseResult,
                                                 ParserBase parser,
                                                 String filename) throws IOException {
        if ( parseResult.hasValue() ) {
            CompilationUnit cu = (CompilationUnit)((SemanticValue) parseResult).value;
            if ( cu instanceof Api ) {
                if (filename.endsWith(ProjectProperties.API_SOURCE_SUFFIX))
                    return (Api)cu;
                else throw StaticError.make("API files must have suffix "
                                            + ProjectProperties.API_SOURCE_SUFFIX,
                                            (Api)cu);
            } else if ( cu instanceof Component ) {
                if (filename.endsWith(ProjectProperties.COMP_SOURCE_SUFFIX))
                    return (Component)cu;
                else throw StaticError.make("Component files must have suffix "
                                            + ProjectProperties.COMP_SOURCE_SUFFIX,
                                            (Component)cu);
            } else throw new RuntimeException("Unexpected parse result: " + cu);
        } else throw new ParserError((ParseError) parseResult, parser);
    }

    /* Returns a list of grammars imported by a component.
     * This method scans the component for import statements and
     * collects them in a list -- GrammarIndex.
     */
    private static List<GrammarIndex> getImportedGrammars(Component c,
                                                          final GlobalEnvironment env) {
        final List<GrammarIndex> grammars = new ArrayList<GrammarIndex>();
        c.accept(new TemplateNodeDepthFirstVisitor_void() {
                @Override public void forImportApiOnly(ImportApi that) {
                    bug(that, errorMsg("Not yet implemented: 'import api APIName'; ",
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

    /* Invokes the syntax abstraction system on a component in 3 steps
     * 1. Create a parser from the grammars
     * 2. Create a Java parser by invoking Rats!
     * 3. Parse the original component with that parser and transform
     *    it to a core Fortress AST.
     */
    private static Result parseWithGrammars(File f,
                                            GlobalEnvironment env,
                                            boolean verbose,
                                            List<GrammarIndex> grammars) {
        EnvFactory.initializeGrammarIndexExtensions(env.apis().values(), grammars);

        /* Compile the syntax abstractions and create a temporary parser */
        Class<?> temporaryParserClass = ParserMaker.parserForComponent(grammars);

        Debug.debug( Debug.Type.SYNTAX, 2, "Created a temporary parser." );

        BufferedReader in = null;
        try {
            in = Useful.utf8BufferedFileReader(f);
            /* instantiate the class using reflection */
            ParserBase p = RatsUtil.getParserObject(temporaryParserClass, in, f.toString());
            reportSyntaxErrors(getSyntaxErrors( ProjectProperties.macroErrorLog(f) ));
            /* call the parser on the component and checks the validity,
             * get back a component AST
             */
            CompilationUnit original = checkResultCU(RatsUtil.getParserObject(p), p, f.getName());
            // dump(original, "original-" + f.getName());
            /* Transform the syntax abstraction nodes into core Fortress */
            CompilationUnit cu = (CompilationUnit) Transform.transform(env, original);
            // dump(cu, "dump-" + f.getName());
            return new Result(cu, f.lastModified());
        } catch (Exception e) {
            String desc =
                "Error occurred while instantiating and executing a temporary parser: "
                + temporaryParserClass.getCanonicalName();
            if (Debug.isOnFor(1, Debug.Type.STACKTRACE) ||
                    Debug.isOnFor(Debug.MAX_LEVEL, Debug.Type.SYNTAX) ) {
                e.printStackTrace();
            } else {
                System.err.println(Shell.turnOnDebugMessage);
            }
            if (e.getMessage() != null) { desc += " (" + e.getMessage() + ")"; }
            return new Result(StaticError.make(desc, f));
        } finally {
            try {
                Files.rm( ProjectProperties.preparserErrorLog(f) );
                if (in != null) in.close();
            } catch (IOException ioe) {}
        }
    }

    /* Utilities */
    @SuppressWarnings("unused")
	private static void dump( Node node, String name ){
        try{
            ASTIO.writeJavaAst( (CompilationUnit) node, name );
            Debug.debug( Debug.Type.SYNTAX, 1, "Dumped node to ", name );
        } catch ( IOException e ){
            e.printStackTrace();
        }
    }

    /**
     * Parses a file as a compilation unit.
     * Converts checked exceptions like IOException and FileNotFoundException
     * to StaticError with appropriate error message.
     * Validates the parse by calling
     * parseCU (see also description of exceptions there).
     */
    public static CompilationUnit parseFileConvertExn(File file) {
        try {
            String filename = file.getCanonicalPath();
            return parseCU(Useful.utf8BufferedFileReader(file), filename,
                           preParser(Useful.utf8BufferedFileReader(file), filename));
        } catch (FileNotFoundException fnfe) {
            throw convertExn(fnfe, file);
        } catch (IOException ioe) {
            throw convertExn(ioe, file);
        }
    }

    /**
     * Parses a string as a compilation unit. See parseFileConvertExn above.
     */
    public static CompilationUnit parseString(APIName api_name,
                                              String buffer) throws IOException {
        String filename = api_name.getText();
        return parseCU(Useful.bufferedStringReader(buffer), filename,
                       preParser(Useful.bufferedStringReader(buffer),
                                 filename));
    }

    private static CompilationUnit parseCU(BufferedReader in,
                                           String filename,
                                           List<StaticError> errors) throws IOException {
        String parserLogFile = filename + ".parserError.log";
        String syntaxLogFile = filename + ".syntaxError.log";
        try {
            Fortress parser = new Fortress(in, filename);
            xtc.parser.Result parseResult = parser.pFile(0);
            errors.addAll( getSyntaxErrors( parserLogFile ) );
            if ( parseResult.hasValue() ) {
                CompilationUnit cu = (CompilationUnit)((SemanticValue) parseResult).value;
                cu.accept( new SyntaxChecker( Useful.filenameToBufferedWriter( syntaxLogFile ) ) );
                errors.addAll(getSyntaxErrors(syntaxLogFile));
            }
            reportSyntaxErrors( errors );
            return checkResultCU(parseResult, parser, filename);
        } finally {
            try {
                Files.rm( parserLogFile );
            } catch (IOException ioe) {}
            try {
                Files.rm( syntaxLogFile );
                in.close();
            } catch (IOException ioe) {}
        }
    }

    private static List<StaticError> getSyntaxErrors(String parserLogFile)
        throws IOException {
        List<StaticError> errors = new ArrayList<StaticError>();
        File parserLog = new File( parserLogFile );
        if ( parserLog.length() != 0 ) {
            BufferedReader reader = Useful.filenameToBufferedReader( parserLogFile );
            String line = reader.readLine();
            String message = "";
            StringBuilder buf = new StringBuilder();
            while ( line != null ) {
                if ( beginError(line) && !message.equals("") ) {
                    errors.add(StaticError.make(message.substring(0,message.length()-1)));
                    buf = new StringBuilder();
                    buf.append(line + "\n");
                    message = buf.toString();
                } else {
                    buf.append(line + "\n");
                    message = buf.toString();
                }
                line = reader.readLine();
            }
            if ( ! message.equals("") )
                errors.add(StaticError.make(message.substring(0,message.length()-1)));
        }
        return errors;
    }

    private static boolean beginError(String line) {
        return (! line.startsWith(" "));
    }

    private static void reportSyntaxErrors(List<StaticError> errors) {
        if ( ! errors.isEmpty() )
            throw new MultipleStaticError( errors );
    }

    /**
     * Preparses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see also description of exceptions there).
     * Converts checked exceptions like IOException and FileNotFoundException
     * to StaticError with appropriate error message.
     */
    private static List<StaticError> preParser(BufferedReader in,
                                               String filename) throws IOException {
        String preparserLogFile = ProjectProperties.preparserErrorLog(filename);
        try {
            PreFortress preparser = new PreFortress(in, filename);
            preparser.pFile(0);
            List<StaticError> errors = getSyntaxErrors( preparserLogFile );
            return errors;
        } finally {
            try {
                Files.rm( preparserLogFile );
                in.close();
            } catch (IOException e) {}
        }
    }

    private static StaticError convertExn(IOException ioe, File f) {
        String desc = "Unable to read file";
        if (ioe.getMessage() != null) { desc += " (" + ioe.getMessage() + ")"; }
        return StaticError.make(desc, f);
    }

    private static StaticError convertExn(FileNotFoundException fnfe, File f) {
        return StaticError.make("Cannot find file " + f.getAbsolutePath(), f);
    }

}
