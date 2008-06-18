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

import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.shell.BatchCachingAnalyzingRepository;
import com.sun.fortress.shell.BatchCachingRepository;
import com.sun.fortress.syntax_abstractions.phases.GrammarRewriter;
import com.sun.fortress.useful.Path;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;

public class Fortress {

	private final FortressRepository _repository;

	public Fortress(FortressRepository repository) { _repository = repository; }

	//    /**
	//     * Compile all definitions in the given files, and any additional sources that
	//     * they depend on, and add them to the fortress.
	//     */
	//    public Iterable<? extends StaticError> compile(Path path, File... files) {
	//        return compile(path, IterUtil.asIterable(files));
	//    }
	//
	//    /**
	//     * Compile all definitions in the given files, and any additional sources that
	//     * they depend on, and add them to the fortress.
	//     */
	//    public Iterable<? extends StaticError> compile(Path path, Iterable<File> files) {
	//        GlobalEnvironment env = new GlobalEnvironment.FromMap(_repository.apis());
	//
	//        FortressParser.Result pr = FortressParser.parse(files, env, path);
	//        // Parser.Result pr = Parser.parse(files, env);
	//        if (!pr.isSuccessful()) { return pr.errors(); }
	//        System.out.println("Parsing done.");
	//
	//        return analyze(env, pr);
	//    }

	/**
	 * Compile all definitions in the given files, and any additional sources that
	 * they depend on, and add them to the fortress.
	 */
	public Iterable<? extends StaticError> compile(Path path, String... files) {

		BatchCachingAnalyzingRepository bcr = Driver.fssRepository(path, _repository);

		Parser.Result result = compileInner(bcr, files);

		// Parser.Result pr = Parser.parse(files, env);
		if (!result.isSuccessful()) { return result.errors(); }
		if (bcr.verbose())
			System.err.println("Parsing done.");

		GlobalEnvironment env = new GlobalEnvironment.FromMap(bcr.apis());

		//return analyze(env, result);
		return IterUtil.empty();
	}

	private Parser.Result compileInner(BatchCachingAnalyzingRepository bcr, 
			String... files) {
		Parser.Result result = new Parser.Result();

		bcr.addRootApis();

		for (String s : files) {
			APIName name  = Driver.fileAsApi(s);

			try {
				if (name != null) {
					result = addApiToResult(bcr, result, name);
				} else {
					name = Driver.fileAsComponent(s);

					if (name != null) {
						result = addComponentToResult(bcr, result, name);
					} else {
						result = addComponentToResult(bcr, result, NodeFactory.makeAPIName(s));
					}
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
			} catch (Exception ex) {
				result = addExceptionToResult(result, ex);
			}
		}

		for (APIName name : bcr.staleApis()) {
			try {
				if (bcr.verbose())
					System.err.println("Adding api " + name);
				result = addApiToResult(bcr, result, name);
			} catch (Exception ex) {
				result = addExceptionToResult(result, ex);
			}
		}

		for (APIName name : bcr.staleComponents()) {
			try {
				if (bcr.verbose())
					System.err.println("Adding component " + name);
				result = addComponentToResult(bcr, result, name);
			} catch (Exception ex) {
				result = addExceptionToResult(result, ex);
			}
		}

		return result;
	}

	private Parser.Result addExceptionToResult(
			Parser.Result result, Exception ex) {
		result = new Parser.Result(result, new Parser.Result(new WrappedException(ex, ProjectProperties.debug)));
		return result;
	}

	private Parser.Result addComponentToResult(
			BatchCachingRepository bcr, Parser.Result result,
			APIName name) throws FileNotFoundException, IOException {
		Component c = (Component) bcr.getComponent(name).ast();
		result = new Parser.Result(result, new Parser.Result(c, bcr.getModifiedDateForComponent(name)));
		return result;
	}

	private Parser.Result addApiToResult(BatchCachingRepository bcr,
			Parser.Result result, APIName name)
	throws FileNotFoundException, IOException {
		Api a = (Api) bcr.getApi(name).ast();
		result = new Parser.Result(result, new Parser.Result(a, bcr.getModifiedDateForApi(name)));
		return result;
	}

	private Iterable<? extends StaticError> analyze(GlobalEnvironment env,
			Parser.Result pr) {
		Iterable<Api> apis = pr.apis();
		Iterable<Component> components = pr.components();
		long lastModified = pr.lastModified();
		return analyze(env, apis, components, lastModified);
	}

	public Iterable<? extends StaticError> analyze(GlobalEnvironment env,
			Iterable<Api> apis, Iterable<Component> components,
			long lastModified) {
		// Handle APIs first

		// Build ApiIndices before disambiguating to allow circular references.
		// An IndexBuilder.ApiResult contains a map of strings (names) to
		// ApiIndices.
		IndexBuilder.ApiResult rawApiIR = IndexBuilder.buildApis(apis, lastModified);
		if (!rawApiIR.isSuccessful()) { return rawApiIR.errors(); }

		// Build a new GlobalEnvironment consisting of all APIs in a global
		// repository combined with all APIs that have been processed in the previous
		// step. For now, we are implementing pure static linking, so there is
		// no global repository.
		GlobalEnvironment rawApiEnv =
			new GlobalEnvironment.FromMap(CollectUtil.compose(_repository.apis(),
					rawApiIR.apis()));

		// Rewrite all API ASTs so they include only fully qualified names, relying
		// on the rawApiEnv constructed in the previous step. Note that, after this
		// step, the rawApiEnv is stale and needs to be rebuilt with the new API ASTs.
		Disambiguator.ApiResult apiDR =
			Disambiguator.disambiguateApis(apis, rawApiEnv);
		if (!apiDR.isSuccessful()) { return apiDR.errors(); }

		// Rebuild ApiIndices.
		IndexBuilder.ApiResult apiIR = IndexBuilder.buildApis(apiDR.apis(), System.currentTimeMillis());
		if (!apiIR.isSuccessful()) { return apiIR.errors(); }

		// Rebuild GlobalEnvironment.
		GlobalEnvironment apiEnv =
			new GlobalEnvironment.FromMap(CollectUtil.compose(_repository.apis(),
					apiIR.apis()));

		// Rewrite grammars, see GrammarRewriter for more details.
		GrammarRewriter.ApiResult apiID = GrammarRewriter.rewriteApis(apiIR.apis(), apiEnv);
		if (!apiID.isSuccessful()) { return apiID.errors(); }

		// Rebuild ApiIndices.
		apiIR = IndexBuilder.buildApis(apiID.apis(), System.currentTimeMillis());
		if (!apiIR.isSuccessful()) { return apiIR.errors(); }

		// Rebuild GlobalEnvironment.
		apiEnv =
			new GlobalEnvironment.FromMap(CollectUtil.compose(_repository.apis(),
					apiIR.apis()));

		// Do all type checking and other static checks on APIs.
		StaticChecker.ApiResult apiSR =
			StaticChecker.checkApis(apiIR.apis(), apiEnv);
		if (!apiSR.isSuccessful()) { return apiSR.errors(); }

		Desugarer.ApiResult apiDSR =
			Desugarer.desugarApis(apiSR.apis(), apiEnv);

		// Generate code. Code is stored in the _repository object. In an implementation
		// with pure static linking, we would have to write this code back out to a file.
		// In an implementation with fortresses, we would write this code into the resident
		// fortress.
		for (Map.Entry<APIName, ApiIndex> newApi : apiDSR.apis().entrySet()) {
			_repository.addApi(newApi.getKey(), newApi.getValue());
		}

		// Handle components

		// Build ApiIndices before disambiguating to allow circular references.
		// An IndexBuilder.ApiResult contains a map of strings (names) to
		// ApiIndices.
		IndexBuilder.ComponentResult rawComponentIR =
			IndexBuilder.buildComponents(components, lastModified);
		if (!rawComponentIR.isSuccessful()) { return rawComponentIR.errors(); }

		Disambiguator.ComponentResult componentDR =
			Disambiguator.disambiguateComponents(components, env,
					rawComponentIR.components());
		if (!componentDR.isSuccessful()) {
			return componentDR.errors();
		}

		IndexBuilder.ComponentResult componentIR =
			IndexBuilder.buildComponents(componentDR.components(), System.currentTimeMillis());
		if (!componentIR.isSuccessful()) { return componentIR.errors(); }

		StaticChecker.ComponentResult componentSR =
			StaticChecker.checkComponents(componentIR.components(), env);
		if (!componentSR.isSuccessful()) { return componentSR.errors(); }
		
		// Generate top-level byte code environments
		TopLevelEnvGen.ComponentResult componentGR = TopLevelEnvGen.generate(componentSR.components(), env);
		if(!componentGR.isSuccessful()) { return componentGR.errors(); }
		
		// Additional optimization phases can be inserted here        
        

		for (Map.Entry<APIName, ComponentIndex> newComponent :componentSR.components().entrySet()) {
			_repository.addComponent(newComponent.getKey(), newComponent.getValue());
		}

		return IterUtil.empty();
	}

	public Iterable<? extends StaticError>  run(Path path, String componentName, boolean test, boolean nolib, List<String> args) {
		BatchCachingRepository _bcr = Driver.specificRepository(path);

		if (! (_bcr instanceof BatchCachingAnalyzingRepository) ) {
			throw new ProgramError("Please set property fortress.static.analysis=1.");
		}

		BatchCachingAnalyzingRepository bcr = (BatchCachingAnalyzingRepository) _bcr;
		bcr.setVerbose(ProjectProperties.debug);
		Parser.Result result = compileInner(bcr, componentName);
		if (!result.isSuccessful()) { return result.errors(); }

		if (bcr.verbose())
			System.err.println("Parsing done.");

		try {
			CompilationUnit cu = bcr.getLinkedComponent(NodeFactory.makeAPIName(componentName)).ast();
			Driver.runProgram(bcr, cu, test, nolib, args);
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

	private void dumpDisambiguated(Iterable<Api> apis, boolean debug) {
		String dir = System.getProperty("java.io.tmpdir")+File.separatorChar;
		try {
			for (Api a: apis) {
				System.err.println("Dump api: "+a.getName());
				ASTIO.writeJavaAst(a, dir+a.getName());
			}
		} catch (IOException e) {
			throw new WrappedException(e, debug);
		}
	}

}
