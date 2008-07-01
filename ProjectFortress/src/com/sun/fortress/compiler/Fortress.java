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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sun.fortress.Shell;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.CacheBasedRepository;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.phases.GrammarRewriter;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;

public class Fortress {

    private final FortressRepository _repository;
    public static FortressRepository CURRENT_INTERPRETER_REPOSITORY = null;

    public FortressRepository getRepository() {
        return _repository;
    }

    /**
     * This is used to communicate, clumsily, with parsers generated by syntax expansion.
     * The interface should be improved.
     */
    public static void setCurrentInterpreterRepository( FortressRepository g ){
        CURRENT_INTERPRETER_REPOSITORY = g;
    }

    public static FortressRepository specificRepository(Path p, FortressRepository cache ){
        FortressRepository fr = new GraphRepository( p, cache );
        CURRENT_INTERPRETER_REPOSITORY = fr;
        return fr;
    }

    public static FortressRepository specificRepository(Path p) {
        return specificRepository( p, new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR) );
    }

    public Fortress() {
        _repository = new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR);
    }

    public Fortress(FortressRepository repository) { _repository = repository; }

    /**
     * Compile all definitions in the given files, and any additional sources that
     * they depend on, and add them to the fortress.
     */
    public Iterable<? extends StaticError> compile(Path path, String file) {
        FortressRepository bcr = specificRepository( path, _repository );

        Debug.debug( 2, "Compiling file " + file );
        Parser.Result result = new Parser.Result();

        APIName name = Shell.cuName(file);
        try {
            if (Shell.isApi(file)) {
                Api a = (Api) bcr.getApi(name).ast();
                result = new Parser.Result(result, new Parser.Result(a, bcr.getModifiedDateForApi(name)));
            } else if (Shell.isComponent(file)) {
                Component c = (Component) bcr.getComponent(name).ast();
                result = new Parser.Result(result, new Parser.Result(c, bcr.getModifiedDateForComponent(name)));
            } else {
                System.out.println( "Don't know what kind of file " + file +
                                    " is. Append .fsi or .fss." );
            }
        } catch (ProgramError pe) {
            Iterable<? extends StaticError> se = pe.getStaticErrors();
            if (se == null) {
                result = new Parser.Result(result, new Parser.Result(new WrappedException(pe, ProjectProperties.debug)));
            }
            else {
                result = new Parser.Result(result, new Parser.Result(se));
            }
        } catch (RepositoryError ex) {
            throw ex;
        } catch ( FileNotFoundException ex ){
            throw new WrappedException(ex);
        } catch ( IOException e ){
            throw new WrappedException(e);
        } catch (StaticError ex) {
            result = new Parser.Result(result, new Parser.Result(new WrappedException(ex, ProjectProperties.debug)));
        }

        if (!result.isSuccessful()) { return result.errors(); }
        if (bcr.verbose())
            System.err.println("Compiling done.");

        GlobalEnvironment env = new GlobalEnvironment.FromMap(bcr.apis());

        return IterUtil.empty();
    }

    public Iterable<? extends StaticError>  run(Path path, APIName componentName, boolean test, List<String> args) {
        FortressRepository bcr = specificRepository( path, _repository );

        try {
            CompilationUnit cu = bcr.getLinkedComponent(componentName).ast();
            Driver.runProgram(bcr, cu, test, args);
        } catch (Throwable th) {
            // TODO FIXME what is the proper treatment of errors/exceptions etc.?
            if (th instanceof FortressException) {
                FortressException pe = (FortressException) th;
                if (pe.getStaticErrors() != null)
                    return pe.getStaticErrors();
            }
            if (th instanceof RuntimeException)
                throw (RuntimeException) th;
            if (th instanceof Error)
                throw (Error) th;
            throw new WrappedException(th, ProjectProperties.debug);
        }

        return IterUtil.empty();
    }

    public Iterable<? extends StaticError> analyze(GlobalEnvironment env,
                                                    Iterable<Api> apis,
                                                    Iterable<Component> components,
                                                    long lastModified) {
        String phase = "";

        // Build ApiIndices before disambiguating to allow circular references.
        // An IndexBuilder.ApiResult contains a map of strings (names) to
        // ApiIndices.
        IndexBuilder.ApiResult rawApiIR = IndexBuilder.buildApis(apis, lastModified);
        if (!rawApiIR.isSuccessful()) { return rawApiIR.errors(); }

        // Build ComponentIndices before disambiguating to allow circular references.
        // An IndexBuilder.ComponentResult contains a map of strings (names) to
        // ComponentIndices.
        IndexBuilder.ComponentResult rawComponentIR =
            IndexBuilder.buildComponents(components, lastModified);
        if (!rawComponentIR.isSuccessful()) { return rawComponentIR.errors(); }

        // Build a new GlobalEnvironment consisting of all APIs in a global
        // repository combined with all APIs that have been processed in the previous
        // step.  For now, we are implementing pure static linking, so there is
        // no global repository.
        GlobalEnvironment rawApiEnv =
            new GlobalEnvironment.FromMap(CollectUtil.union(_repository.apis(),
                                                            rawApiIR.apis()));

        // Rewrite all API ASTs so they include only fully qualified names, relying
        // on the rawApiEnv constructed in the previous step. Note that, after this
        // step, the rawApiEnv is stale and needs to be rebuilt with the new API ASTs.
        Disambiguator.ApiResult apiDR =
            Disambiguator.disambiguateApis(apis, rawApiEnv);
        if (!apiDR.isSuccessful()) { return apiDR.errors(); }

        Disambiguator.ComponentResult componentDR =
            Disambiguator.disambiguateComponents(components, env,
                                                 rawComponentIR.components());
        if (!componentDR.isSuccessful()) { return componentDR.errors(); }

        if (phase.equals("disambiguate"))
            return IterUtil.empty();

        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR =
            IndexBuilder.buildApis(apiDR.apis(), System.currentTimeMillis());
        if (!apiIR.isSuccessful()) { return apiIR.errors(); }

        // Rebuild ComponentIndices.
        IndexBuilder.ComponentResult componentIR =
            IndexBuilder.buildComponents(componentDR.components(),
                                         System.currentTimeMillis());
        if (!componentIR.isSuccessful()) { return componentIR.errors(); }

        // Rebuild GlobalEnvironment.
        GlobalEnvironment apiEnv =
            new GlobalEnvironment.FromMap(CollectUtil.union(_repository.apis(),
                                                            apiIR.apis()));

        // Rewrite grammars, see GrammarRewriter for more details.
        GrammarRewriter.ApiResult apiID = GrammarRewriter.rewriteApis(apiIR.apis(), apiEnv);
        if (!apiID.isSuccessful()) { return apiID.errors(); }

        // Rebuild ApiIndices.
        apiIR = IndexBuilder.buildApis(apiID.apis(), System.currentTimeMillis());
        if (!apiIR.isSuccessful()) { return apiIR.errors(); }

        // Rebuild GlobalEnvironment.
        apiEnv =
            new GlobalEnvironment.FromMap(CollectUtil.union(_repository.apis(),
                                                            apiIR.apis()));

        // Do all type checking and other static checks on APIs.
        StaticChecker.ApiResult apiSR =
            StaticChecker.checkApis(apiIR.apis(), apiEnv);
        if (!apiSR.isSuccessful()) { return apiSR.errors(); }

        StaticChecker.ComponentResult componentSR =
            StaticChecker.checkComponents(componentIR.components(), env);
        if (!componentSR.isSuccessful()) { return componentSR.errors(); }

        if (phase.equals("compile"))
            return IterUtil.empty();

        Desugarer.ApiResult apiDSR =
            Desugarer.desugarApis(apiSR.apis(), apiEnv);

        // Generate top-level byte code environments
        TopLevelEnvGen.ComponentResult componentGR =
            TopLevelEnvGen.generate(componentSR.components(), env);
        if(!componentGR.isSuccessful()) { return componentGR.errors(); }

        // Generate code.  Code is stored in the _repository object.
        // In an implementation with pure static linking, we would have to write
        // this code back out to a file.
        // In an implementation with fortresses,
        // we would write this code into the resident fortress.
        for (Map.Entry<APIName, ApiIndex> newApi : apiDSR.apis().entrySet()) {
            if ( compiledApi( newApi.getKey(), apis ) ){
                Debug.debug( 2, "Analyzed api " + newApi.getKey() );
                _repository.addApi(newApi.getKey(), newApi.getValue());
            }
        }

        // Additional optimization phases can be inserted here

        for (Map.Entry<APIName, ComponentIndex> newComponent :componentSR.components().entrySet()) {
            _repository.addComponent(newComponent.getKey(), newComponent.getValue());
        }

        Debug.debug( 2, "Done with analyzing apis " + apis + ", components " + components );

        return IterUtil.empty();
    }

    private boolean compiledApi( APIName name, Iterable<Api> apis ){
        for ( Api api : apis ){
            if ( api.getName().equals(name) ){
                return true;
            }
        }
        return false;
    }
}
