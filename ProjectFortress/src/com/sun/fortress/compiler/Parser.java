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

package com.sun.fortress.compiler;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;

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
//import com.sun.fortress.parser.Fortress; // Not imported to prevent name clash.

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
import com.sun.fortress.interpreter.drivers.ProjectProperties;

import com.sun.fortress.useful.NI;

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

        public Result(Api api, long lastModified) {
            _apis = IterUtil.singleton(api);
            _components = IterUtil.empty();
            _lastModified = lastModified;
        }

        public Result(Component component, long lastModified) {
            _components = IterUtil.singleton(component);
            _apis = IterUtil.empty();
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

        public Iterable<Api> apis() { return _apis; }
        public Iterable<Component> components() { return _components; }
        public long lastModified() { return _lastModified; }
    }

    public static class Error extends StaticError {
        private final ParseError _parseError;
        private final ParserBase _parser;

        public Error(ParseError parseError, ParserBase parser) {
            _parseError = parseError;
            _parser = parser;
        }

        public String typeDescription() { return "Parse Error"; }

        public String description() {
            String result = _parseError.msg;
            // TODO: I don't know for sure whether this is allowed to be null
            if (result == null || result.equals("")) { result = "Unspecified cause"; }
            return result;
        }

        public String at() {
            if (_parseError.index == -1) { return "Unspecified location"; }
            else { return _parser.location(_parseError.index).toString(); }
        }
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
            try {
                com.sun.fortress.parser.Fortress p =
                    new com.sun.fortress.parser.Fortress(in, f.toString());
                xtc.parser.Result parseResult = p.pFile(0);
                if (parseResult.hasValue()) {
                    Object cu = ((SemanticValue) parseResult).value;
                    if (cu instanceof Api) {
                        Api _cu = (Api) cu;
                        
                        if (f.toString().endsWith(ProjectProperties.API_SOURCE_SUFFIX)) {
                            return new Result(_cu, f.lastModified());
                        } else {
                            return new Result(StaticError.make
                                ("Api files must have suffix " + ProjectProperties.API_SOURCE_SUFFIX,
                                 _cu));
                        }
                    } else if (cu instanceof Component) {
                        Component _cu = (Component) cu;
                        
                        if (f.toString().endsWith(ProjectProperties.COMP_SOURCE_SUFFIX)) {
                            return new Result(_cu, f.lastModified());
                        } else {
                            return new Result(StaticError.make
                                ("Component files must have suffix " + ProjectProperties.COMP_SOURCE_SUFFIX,
                                 _cu));
                        }
                    } else {
                        throw new RuntimeException("Unexpected parse result: " + cu);
                    }
                } else {
                    return new Result(new Parser.Error((ParseError) parseResult, p));
                }
            }
            finally { in.close(); }
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

}
