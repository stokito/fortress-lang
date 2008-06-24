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

package com.sun.fortress.compiler;

import java.io.File;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Box;
import edu.rice.cs.plt.lambda.SimpleBox;

import xtc.parser.ParserBase;
import xtc.parser.SemanticValue;
import xtc.parser.ParseError;
//import xtc.parser.Result; // Not imported to prevent name clash.
import com.sun.fortress.parser.Fortress; // Shadows Fortress in this package
import com.sun.fortress.parser.preparser.PreFortress;

import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.drivers.ProjectProperties;


/**
 * Methods to parse a collection of files to a collection of ASTs. Automatically
 * locates and parses any additional API definitions that are needed.
 */
public class Parser {

    public static class Result extends StaticPhaseResult {
        private final Iterable<Api> _apis;
        private final Iterable<Component> _components;
        private long _lastModified;

        public Result() {
            _apis = IterUtil.empty();
            _components = IterUtil.empty();
        }

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

        public Result(Result r1, Result r2) {
            super(r1, r2);
            _apis = IterUtil.compose(r1._apis, r2._apis);
            _components = IterUtil.compose(r1._components, r2._components);
            _lastModified = Math.max(r1.lastModified(), r2.lastModified());
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


    /**
     * Parse the given files and any additional files that are expected to contain
     * referenced APIs.
     */
    public static Result parse(Iterable<? extends File> files,
                               final GlobalEnvironment env) {
        // box allows mutation of a final var
        final Box<Result> result = new SimpleBox<Result>(new Result());

        Set<File> fileSet = new HashSet<File>();
        for (File f : files) { fileSet.add(canonicalRepresentation(f)); }

        Lambda<File, Set<File>> parseAndGetDepends = new Lambda<File, Set<File>>() {
            public Set<File> value(File f) {
                Result r = parse(f);
                result.set(new Result(result.value(), r));
                if (r.isSuccessful()) {
                    Set<File> newFiles = new HashSet<File>();
                    for (CompilationUnit cu :
                             IterUtil.compose(r.apis(), r.components())) {
                        newFiles.addAll(extractNewDependencies(cu, env));
                    }
                    return newFiles;
                }
                else { return Collections.emptySet(); }
            }
        };

        // parses all dependency files
        CollectUtil.graphClosure(fileSet, parseAndGetDepends);
        return result.value();
    }

    /** Parses a single file. */
    public static Result parse(File f) {
        try {
            BufferedReader in = Useful.utf8BufferedFileReader(f);
            return parse(f.toString(), f.lastModified(), in);
        }
        catch (FileNotFoundException e) {
            return new Result(StaticError.make("Cannot find file " + f.getName(),
                                               f.toString()));
        }
        catch (IOException e) {
            String desc = "Unable to read file";
            if (e.getMessage() != null) { desc += " (" + e.getMessage() + ")"; }
            return new Result(StaticError.make(desc, f.toString()));
        }
    }

    /** Parses a BufferedReader object, given some descriptive information. */
    public static Result parse(String filename, long modificationDate, 
                               BufferedReader in) throws IOException {
        try {
            return new Result(parseFile(null, in, filename), modificationDate);
        } catch (StaticError se) {
            return new Result(se);
        } finally {
            in.close();
        }
    }


    /**
     * Get all files potentially containing APIs imported by cu that aren't
     * currently in fortress.
     */
    private static Set<File> extractNewDependencies(CompilationUnit cu,
                                                    GlobalEnvironment env) {
        Set<APIName> importedApis = new LinkedHashSet<APIName>();
        for (Import i : cu.getImports()) {
            if (i instanceof ImportApi) {
                for (AliasedAPIName apiAlias : ((ImportApi) i).getApis()) {
                    importedApis.add(apiAlias.getApi());
                }
            }
            else { // i instanceof ImportedNames
                importedApis.add(((ImportedNames) i).getApi());
            }
        }

        Set<File> result = new HashSet<File>();
        for (APIName n : importedApis) {
            if (!env.definesApi(n)) {
                File f = canonicalRepresentation(fileForApiName(n));
                if (IOUtil.attemptExists(f)) { result.add(f); }
            }
        }
        return result;
    }

    /** Get the filename in which the given API should be defined. */
    private static File fileForApiName(APIName api) {
        return new File(NodeUtil.nameString(api) + ".fsi");
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


    /**
     * Parses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see also description of exceptions there).
     * Converts checked exceptions like IOException and FileNotFoundException
     * to StaticError with appropriate error message.
     */
    public static CompilationUnit parseFileConvertExn(APIName api_name, File file) {
        try {
            return parseFile(api_name, file);
        } catch (FileNotFoundException fnfe) {
            throw convertExn(fnfe, file);
        } catch (IOException ioe) {
            throw convertExn(ioe, file);
        }
    }

    /**
     * Parses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see description of exceptions there).
     */
    public static CompilationUnit parseFile(APIName api_name, File file)
        throws FileNotFoundException, IOException {
        // Also throws StaticError, ParserError
        BufferedReader in = Useful.utf8BufferedFileReader(file);
        try {
            return parseFile(api_name, in, file.getCanonicalPath());
        } finally {
            in.close();
        }
    }

    /**
     * Parses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see description of exceptions there).
     */
    public static CompilationUnit parseFile(APIName api_name, Reader in, String filename)
        throws IOException {
        // Also throws StaticError, ParserError
        Fortress parser = makeParser(api_name, in, filename);
        xtc.parser.Result parseResult = parser.pFile(0);
        return checkResultCU(parseResult, parser, filename);
    }

    /** 
     * Checks that a xtc.parser.Result is contains a CompilationUnit, 
     * and checks the filename for the appropriate suffix.
     * Throws a ParserError (note, subtype of StaticError) if the parse fails.
     * Throws a StaticError if the filename has the wrong suffix. 
     */
    public static CompilationUnit checkResultCU(xtc.parser.Result parseResult, 
                                         ParserBase parser,
                                         String filename) {
        if (parseResult.hasValue()) {
            Object cu = ((SemanticValue) parseResult).value;
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
            throw new ParserError((ParseError) parseResult, parser);
        }
    }

    private static Fortress makeParser(Reader in, String filename) {
        return new Fortress(in, filename);
    }

    private static Fortress makeParser(APIName api_name, Reader in, String filename) {
        Fortress p = makeParser(in, filename);
        p.setExpectedName(api_name);
        return p;
    }

    // Pre-parser

    /**
     * Preparses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see also description of exceptions there).
     * Converts checked exceptions like IOException and FileNotFoundException
     * to StaticError with appropriate error message.
     */
    public static CompilationUnit preparseFileConvertExn(APIName api_name, File file) {
        try {
            return preparseFile(api_name, file);
        } catch (FileNotFoundException fnfe) {
            throw convertExn(fnfe, file);
        } catch (IOException ioe) {
            throw convertExn(ioe, file);
        }
    }

    /**
     * Preparses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see description of exceptions there).
     */
    public static CompilationUnit preparseFile(APIName api_name, File file)
        throws FileNotFoundException, IOException {
        // Also throws StaticError, ParserError
        BufferedReader in = Useful.utf8BufferedFileReader(file);
        try {
            return preparseFile(api_name, in, file.getCanonicalPath());
        } finally {
            in.close();
        }
    }

    /**
     * Preparses a file as a compilation unit. Validates the parse by calling
     * checkResultCU (see description of exceptions there).
     */
    public static CompilationUnit preparseFile(APIName api_name, Reader in, String filename)
        throws IOException {
        // Also throws StaticError, ParserError
        PreFortress parser = makePreparser(api_name, in, filename);
        xtc.parser.Result parseResult = parser.pFile(0);
        return checkResultCU(parseResult, parser, filename);
    }

    private static PreFortress makePreparser(Reader in, String filename) {
        return new PreFortress(in, filename);
    }

    private static PreFortress makePreparser(APIName api_name, Reader in, String filename) {
        PreFortress p = makePreparser(in, filename);
        p.setExpectedName(api_name);
        return p;
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
