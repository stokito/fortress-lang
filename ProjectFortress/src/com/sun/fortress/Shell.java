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

package com.sun.fortress;

import com.sun.fortress.repository.CacheBasedRepository;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;
import java.io.*;
import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.compiler.*;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.syntax_abstractions.phases.GrammarRewriter;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Debug;

public final class Shell {
    static boolean test;

    /* These numbers have no importance, so don't worry about the order or even
     * their value ( but they must be unique ).
     * If you feel like changing this to an enum at any point that is fine.
     */
    private static final int PHASE_TOPLEVEL = 0;
    private static final int PHASE_DESUGAR = 1;
    private static final int PHASE_TYPECHECK = 2;
    private static final int PHASE_GRAMMAR = 3;
    private static final int PHASE_DISAMBIGUATE = 4;
    private static final int PHASE_EMPTY = 5;

    /* set this statically if you only want to run upto a certain phase */
    private static int currentPhase = PHASE_TOPLEVEL;

    private final FortressRepository _repository;
    private static final CacheBasedRepository defaultRepository =
        new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR);
    public static FortressRepository CURRENT_INTERPRETER_REPOSITORY = null;

    public Shell(FortressRepository repository) { _repository = repository; }

    public FortressRepository getRepository() {
        return _repository;
    }

    private static void setPhase( int phase ){
        currentPhase = phase;
    }

    /**
     * This is used to communicate, clumsily, with parsers generated by syntax expansion.
     * The interface should be improved.
     */
    public static void setCurrentInterpreterRepository( FortressRepository g ){
        CURRENT_INTERPRETER_REPOSITORY = g;
    }

    private static GraphRepository specificRepository(Path p, CacheBasedRepository cache ){
        GraphRepository fr = new GraphRepository( p, cache );
        CURRENT_INTERPRETER_REPOSITORY = fr;
        return fr;
    }

    public static GraphRepository specificRepository(Path p) {
        return specificRepository( p, defaultRepository );
    }

    /* Helper method to print usage message.*/
    private static void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println(" compile [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" [run] [-test] [-debug [type]* [#]] somefile.fss arg...");
        System.err.println(" parse [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" disambiguate [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" desugar [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" grammar [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" typecheck [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" help");
    }

    private static void printHelpMessage() {
        System.err.println
        ("Invoked as script: fortress args\n"+
         "Invoked by java: java ... com.sun.fortress.Shell args\n"+
         "\n"+
         "fortress compile [-out file] [-debug [type]* [#]] somefile.fs{s,i}\n"+
         "  Compile somefile. If compilation succeeds no message will be printed.\n"+
         "\n"+
         "fortress [run] [-test] [-debug [type]* [#]] somefile.fss arg ...\n"+
         "  Runs somefile.fss through the Fortress interpreter, passing arg ... to the\n"+
         "  run method of somefile.fss.\n"+
         "\n"+
         "fortress parse [-out file] [-debug [type]* [#]] somefile.fs{i,s}\n"+
         "  Parses a file. If parsing succeeds the message \"Ok\" will be printed.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress disambiguate [-out file] [-debug [type]* [#]] somefile.fs{i,s}\n"+
         "  Disambiguates a file.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress desugar [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Desugars a file.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress grammar [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Rewrites syntax grammars in a file.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress typecheck [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Typechecks a file. If type checking succeeds no message will be printed.\n"+
         "\n"+
         "More details on each flag:\n"+
         "   -out file : dumps the processed abstract syntax tree to a file.\n"+
         "   -test : first runs test functions associated with the program.\n"+
         "   -debug : enables debugging to the maximum level (99) for all \n"+
         "            debugging types and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number, \n"+
         "            and sets all debugging types on.\n"+
         "   -debug types : sets debugging types to the specified types with \n"+
         "            the maximum debugging level. \n" +
         "   -debug types # : sets debugging to the specified level, where # is a number, \n"+
         "            and the debugging types to the specified types. \n" +
         "   The acceptable debugging types are:\n"+
         "            " + Debug.typeStrings() + "\n"+
         "\n"
        );
    }

    private static void turnOnTypeChecking(){
        com.sun.fortress.compiler.StaticChecker.typecheck = true;
    }

    /* Main entry point for the fortress shell.*/
    public static void main(String[] tokens) throws InterruptedException, Throwable {
        if (tokens.length == 0) {
            printUsageMessage();
            System.exit(-1);
        }

        // Now match the assembled string.
        try {
            String what = tokens[0];
            List<String> args = Arrays.asList(tokens).subList(1, tokens.length);
            if (what.equals("compile")) {
                compile(args, Option.<String>none());
            } else if (what.equals("run")) {
                run(args);
            } else if ( what.equals("parse" ) ){
                parse(args, Option.<String>none());
            } else if ( what.equals( "disambiguate" ) ){
                setPhase( PHASE_DISAMBIGUATE );
                compile(args, Option.<String>none());
            } else if ( what.equals( "desugar" ) ){
                setPhase( PHASE_DESUGAR );
                compile(args, Option.<String>none());
            } else if ( what.equals( "grammar" ) ){
                setPhase( PHASE_GRAMMAR );
                compile(args, Option.<String>none());
            } else if (what.equals("typecheck")) {
                /* TODO: remove the next line once type checking is permanently turned on */
                turnOnTypeChecking();
                setPhase( PHASE_TYPECHECK );
                compile(args, Option.<String>none());
            } else if (what.contains(ProjectProperties.COMP_SOURCE_SUFFIX)
                       || (what.startsWith("-") && tokens.length > 1)) {
                // no "run" command.
                run(Arrays.asList(tokens));
            } else if (what.equals("help")) {
                printHelpMessage();

            } else { printUsageMessage(); }
        }
        catch (UserError error) {
            System.err.println(error.getMessage());
        }
        catch (IOException error) {
            System.err.println(error.getMessage());
        }
    }

    /**
     * Parse a file. If the file parses ok it will say "Ok".
     * If you want a dump then give -out somefile.
     */
    private static void parse(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to compile");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
                rest = Debug.parseOptions(rest);
            }
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;

            parse( rest, out );
        } else {
            parse( s, out );
        }
    }

    private static void parse( String file, Option<String> out){
        try{
            CompilationUnit unit = Parser.parseFile(cuName(file), new File(file));
            System.out.println( "Ok" );
            if ( out.isSome() ){
                try{
                    ASTIO.writeJavaAst(unit, out.unwrap());
                    System.out.println( "Dumped parse tree to " + out.unwrap() );
                } catch ( IOException e ){
                    System.err.println( "Error while writing " + out.unwrap() );
                }
            }
        } catch ( FileNotFoundException f ){
            System.err.println( file + " not found" );
        } catch ( IOException ie ){
            System.err.println( "Error while reading " + file );
        } catch ( StaticError s ){
            System.err.println(s);
        }
    }

    private static boolean isApi(String file){
        return file.endsWith(ProjectProperties.API_SOURCE_SUFFIX);
    }

    private static boolean isComponent(String file){
        return file.endsWith(ProjectProperties.COMP_SOURCE_SUFFIX);
    }

    private static APIName cuName( String file ){
        if ( file.endsWith( ProjectProperties.COMP_SOURCE_SUFFIX ) ||
             file.endsWith( ProjectProperties.API_SOURCE_SUFFIX ) ){
            return NodeFactory.makeAPIName(file.substring( 0, file.lastIndexOf(".") ));
        }
        return NodeFactory.makeAPIName(file);
    }

    public static boolean checkCompilationUnitName(String filename,
                                                   String cuname) {
        String file = filename.substring( 0, filename.lastIndexOf(".") );
        file = file.replace('/','.');
        file = file.replace('\\','.');
        return file.endsWith(cuname);
    }

    /**
     * Compile a file.
     * If you want a dump then give -out somefile.
     */
    private static void compile(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to compile");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
            	rest = Debug.parseOptions(rest);
            }
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;
            compile(rest, out);
        } else {
            try {
                Path path = ProjectProperties.SOURCE_PATH;
                if (s.contains("/")) {
                    String head = s.substring(0, s.lastIndexOf("/"));
                    s = s.substring(s.lastIndexOf("/")+1, s.length());
                    path = path.prepend(head);
                }
                Iterable<? extends StaticError> errors = compile(path, s, out );
                if ( errors.iterator().hasNext() ){
                    for (StaticError error: errors) {
                        System.err.println(error);
                    }
                }
            } catch (RepositoryError error) {
                System.err.println(error);
            }
        }
    }

    /**
     * Compile a file.
     */
    public static Iterable<? extends StaticError> compile(Path path, String file) {
        return compile(path, file, Option.<String>none());
    }

    private static Iterable<? extends StaticError> compile(Path path, String file, Option<String> out) {
        GraphRepository bcr = specificRepository( path, defaultRepository );

        Debug.debug( Debug.Type.FORTRESS, 2, "Compiling file ", file );
        APIName name = cuName(file);
        try {
            if ( isApi(file) ) {
                Api a = (Api) bcr.getApi(name).ast();
                if ( out.isSome() )
                    ASTIO.writeJavaAst(defaultRepository.getApi(name).ast(), out.unwrap());
            } else if (isComponent(file)) {
                Component c = (Component) bcr.getComponent(name).ast();
                if ( out.isSome() )
                    ASTIO.writeJavaAst(defaultRepository.getComponent(name).ast(), out.unwrap());
            } else {
                System.out.println( "Don't know what kind of file " + file +
                                    " is. Append .fsi or .fss." );
            }
        } catch (ProgramError pe) {
            Iterable<? extends StaticError> se = pe.getStaticErrors();
            if (se == null) {
                return IterUtil.singleton(new WrappedException(pe, Debug.isOnMax()));
            }
            else {
                return se;
            }
        } catch (RepositoryError ex) {
            throw ex;
        } catch ( FileNotFoundException ex ){
            throw new WrappedException(ex);
        } catch ( IOException e ){
            throw new WrappedException(e);
        } catch (StaticError ex) {
             return IterUtil.singleton(new WrappedException(ex, Debug.isOnMax()));
        }

        if (bcr.verbose())
            System.err.println("Compiling done.");

        return IterUtil.empty();
    }

    /**
     * Run a file.
     */
    private static void run(List<String> args)
        throws UserError, IOException, Throwable {
        if (args.size() == 0) {
            throw new UserError("Need a file to run");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
            	rest = Debug.parseOptions(rest);
            }
            if (s.equals("-test")) {
            	test = true;
            }
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;

            run(rest);
        } else {
            run(s, rest);
        }
    }

    private static void run(String fileName, List<String> args)
        throws UserError, Throwable {
        try {
            Path path = ProjectProperties.SOURCE_PATH;
            if (fileName.contains("/")) {
                String head = fileName.substring(0, fileName.lastIndexOf("/"));
                fileName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length());
                path = path.prepend(head);
            }
            APIName componentName = cuName(fileName);
            GraphRepository bcr = specificRepository( path, defaultRepository );
            Iterable<? extends StaticError> errors = IterUtil.empty();

            try {
                CompilationUnit cu = bcr.getLinkedComponent(componentName).ast();
                Driver.runProgram(bcr, cu, test, args);
            } catch (Throwable th) {
                // TODO FIXME what is the proper treatment of errors/exceptions etc.?
                if (th instanceof FortressException) {
                    FortressException pe = (FortressException) th;
                    if (pe.getStaticErrors() != null)
                        errors = pe.getStaticErrors();
                }
                if (th instanceof RuntimeException)
                    throw (RuntimeException) th;
                if (th instanceof Error)
                    throw (Error) th;
                throw new WrappedException(th, Debug.isOnMax());
            }

            for (StaticError error: errors) {
                System.err.println(error);
            }
            // If there are no errors, all components will have been written to disk by the CacheBasedRepository.
        } catch ( StaticError e ){
            System.err.println(e);
            if ( Debug.isOnMax() ){
                e.printStackTrace();
            }
        } catch (RepositoryError e) {
            System.err.println(e.getMessage());
        } catch (FortressException e) {
            System.err.println(e.getMessage());
            e.printInterpreterStackTrace(System.err);
            if (Debug.isOnMax()) {
                e.printStackTrace();
            } else {
                System.err.println("Turn on -debug for Java-level error dump.");
            }
            System.exit(1);
        }
    }

    public static class AnalyzeResult extends StaticPhaseResult {
        private final Map<APIName, ApiIndex> _apis;
        private final Map<APIName, ComponentIndex> _components;

        public AnalyzeResult(Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = new HashMap<APIName, ApiIndex>();
            _components = new HashMap<APIName, ComponentIndex>();
        }

        public AnalyzeResult(Map<APIName, ApiIndex> apis,
                             Map<APIName, ComponentIndex> components,
                             Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
            _components = components;
        }

        public Map<APIName, ApiIndex> apis() { return _apis; }
        public Map<APIName, ComponentIndex> components() { return _components; }
    }

    public static AnalyzeResult disambiguate(FortressRepository repository,
                                             GlobalEnvironment env,
                                             Iterable<Api> apis,
                                             Iterable<Component> components,
                                             long lastModified) throws StaticError {
        // Build ApiIndices before disambiguating to allow circular references.
        // An IndexBuilder.ApiResult contains a map of strings (names) to
        // ApiIndices.
        IndexBuilder.ApiResult rawApiIR = IndexBuilder.buildApis(apis, lastModified);
        if (!rawApiIR.isSuccessful()) {
            throw new MultipleStaticError(rawApiIR.errors());
        }

        // Build ComponentIndices before disambiguating to allow circular references.
        // An IndexBuilder.ComponentResult contains a map of strings (names) to
        // ComponentIndices.
        IndexBuilder.ComponentResult rawComponentIR =
            IndexBuilder.buildComponents(components, lastModified);
        if (!rawComponentIR.isSuccessful()) {
            throw new MultipleStaticError(rawComponentIR.errors());
        }

        // Build a new GlobalEnvironment consisting of all APIs in a global
        // repository combined with all APIs that have been processed in the previous
        // step.  For now, we are implementing pure static linking, so there is
        // no global repository.
        GlobalEnvironment rawApiEnv =
            new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
                                                            rawApiIR.apis()));

        // Rewrite all API ASTs so they include only fully qualified names, relying
        // on the rawApiEnv constructed in the previous step. Note that, after this
        // step, the rawApiEnv is stale and needs to be rebuilt with the new API ASTs.
        Disambiguator.ApiResult apiDR =
            Disambiguator.disambiguateApis(apis, rawApiEnv, repository.apis());
        if (!apiDR.isSuccessful()) {
            throw new MultipleStaticError(apiDR.errors());
        }

        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR =
            IndexBuilder.buildApis(apiDR.apis(), lastModified);
        if (!apiIR.isSuccessful()) {
            throw new MultipleStaticError(apiIR.errors());
        }

        // Rebuild GlobalEnvironment.
        GlobalEnvironment apiEnv =
            new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
                                                            apiIR.apis()));

        Disambiguator.ComponentResult componentDR =
            Disambiguator.disambiguateComponents(components, apiEnv,
                                                 rawComponentIR.components());
        if (!componentDR.isSuccessful()) {
            throw new MultipleStaticError(componentDR.errors());
        }

        // Rebuild ComponentIndices.
        IndexBuilder.ComponentResult componentsDone =
            IndexBuilder.buildComponents(componentDR.components(), lastModified);
        if (!componentsDone.isSuccessful()) {
            throw new MultipleStaticError(componentsDone.errors());
        }

        return new AnalyzeResult(apiIR.apis(), componentsDone.components(), IterUtil.<StaticError>empty());
    }

    public static AnalyzeResult rewriteGrammar(FortressRepository repository,
                                               GlobalEnvironment env,
                                               Iterable<Api> apis,
                                               Iterable<Component> components,
                                               long lastModified) throws StaticError {
        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, lastModified);
        IndexBuilder.ComponentResult componentsDone = IndexBuilder.buildComponents(components, lastModified);
        GlobalEnvironment apiEnv =
            new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
                                                            apiIndex.apis()));
        GrammarRewriter.ApiResult apiID = GrammarRewriter.rewriteApis(apiIndex.apis(), apiEnv);
        if (!apiID.isSuccessful()) {
            throw new MultipleStaticError(apiID.errors());
        }

        IndexBuilder.ApiResult apiDone = IndexBuilder.buildApis(apiID.apis(), lastModified);
        if (!apiDone.isSuccessful()) {
            throw new MultipleStaticError(apiDone.errors());
        }

        return new AnalyzeResult(apiDone.apis(), componentsDone.components(), IterUtil.<StaticError>empty());
    }

    public static AnalyzeResult typecheck(FortressRepository _repository,
                                          GlobalEnvironment env,
                                          Iterable<Api> apis,
                                          Iterable<Component> components,
                                          long lastModified) throws StaticError {
        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, lastModified);
        IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(components, lastModified);
        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(_repository.apis(),
                                                                                   apiIndex.apis()));

        StaticChecker.ApiResult apiSR = StaticChecker.checkApis( apiIndex.apis(), apiEnv );

        if ( !apiSR.isSuccessful() ){
            throw new MultipleStaticError(apiSR.errors());
        }

        StaticChecker.ComponentResult componentSR =
            StaticChecker.checkComponents( componentIndex.components(), env);

        if ( !componentSR.isSuccessful() ){
            throw new MultipleStaticError(componentSR.errors());
        }

        return new AnalyzeResult(apiSR.apis(), componentSR.components(), IterUtil.<StaticError>empty());

    }

    public static AnalyzeResult desugar(FortressRepository _repository,
                                        GlobalEnvironment env,
                                        Iterable<Api> apis,
                                        Iterable<Component> components,
                                        long lastModified) throws StaticError {
        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, lastModified);
        IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(components, lastModified);
        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(_repository.apis(),
                                                                                   apiIndex.apis()));

        Desugarer.ApiResult apiDSR = Desugarer.desugarApis(apiIndex.apis(), apiEnv);

        if ( ! apiDSR.isSuccessful() ){
            throw new MultipleStaticError(apiDSR.errors());
        }

        return new AnalyzeResult(apiDSR.apis(), componentIndex.components(), IterUtil.<StaticError>empty());
    }

    public static AnalyzeResult topLevelEnvironment(FortressRepository _repository,
                                                    GlobalEnvironment env,
                                                    Iterable<Api> apis,
                                                    Iterable<Component> components,
                                                    long lastModified) throws StaticError {
        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, lastModified);
        IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(components, lastModified);
        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(_repository.apis(),
                                                                                   apiIndex.apis()));

        TopLevelEnvGen.CompilationUnitResult apiGR =
            TopLevelEnvGen.generateApiEnvs(apiIndex.apis(), apiEnv);

        if ( !apiGR.isSuccessful() ){
            throw new MultipleStaticError(apiGR.errors());
        }

        // Generate top-level byte code environments for components
        TopLevelEnvGen.CompilationUnitResult componentGR =
            TopLevelEnvGen.generateComponentEnvs(componentIndex.components(), env);

        if ( !componentGR.isSuccessful() ){
            throw new MultipleStaticError(componentGR.errors());
        }

        return new AnalyzeResult(apiIndex.apis(), componentIndex.components(), IterUtil.<StaticError>empty());
    }

    private static abstract class Phase{
        Phase parent;
        public Phase( Phase parent ){
            this.parent = parent;
        }

        public abstract AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError ;

        public AnalyzeResult run() throws StaticError {
            AnalyzeResult result = parent.run();
            List<Api> apis = new ArrayList<Api>();
            List<Component> components = new ArrayList<Component>();
            for ( Map.Entry<APIName,ApiIndex> entry : result.apis().entrySet() ){
                apis.add( (Api) entry.getValue().ast() );
            }
            for ( Map.Entry<APIName,ComponentIndex> entry : result.components().entrySet() ){
                components.add( (Component) entry.getValue().ast() );
            }
            return execute( apis, components );
        }
    }

    private static Phase getPhase(final FortressRepository repository,
                                  final GlobalEnvironment env,
                                  final Iterable<Api> apis,
                                  final Iterable<Component> components,
                                  final long lastModified,
                                  final int phase){
        class EmptyPhase extends Phase{
            public EmptyPhase(){
                super(null);
            }

            public AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError {
                return null;
            }

            public AnalyzeResult run() throws StaticError {
                Debug.debug( Debug.Type.FORTRESS, 1, "Start phase Empty" );
                IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, lastModified);
                IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(components, lastModified);
                return new AnalyzeResult(apiIndex.apis(), componentIndex.components(), IterUtil.<StaticError>empty());
            }
        }

        class DisambiguatePhase extends Phase {
            public DisambiguatePhase( Phase parent ){
                super(parent);
            }

            public AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError {
                Debug.debug( Debug.Type.FORTRESS, 1, "Start phase Disambiguate" );
                return disambiguate(repository, env, apis, components, lastModified);
            }
        }

        class TypecheckPhase extends Phase {
            public TypecheckPhase( Phase parent ){
                super(parent);
            }

            public AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError {
                Debug.debug( Debug.Type.FORTRESS, 1, "Start phase TypeCheck" );
                return typecheck(repository, env, apis, components, lastModified);
            }
        }

        class DesugarPhase extends Phase {
            public DesugarPhase( Phase parent ){
                super(parent);
            }

            public AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError {
                Debug.debug( Debug.Type.FORTRESS, 1, "Start phase Desugar" );
                return desugar(repository, env, apis, components, lastModified);
            }
        }

        class TopLevelPhase extends Phase {
            public TopLevelPhase( Phase parent ){
                super(parent);
            }

            public AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError {
                Debug.debug( Debug.Type.FORTRESS, 1, "Start phase TopLevelEnvironment" );
                return topLevelEnvironment(repository, env, apis, components, lastModified);
            }
        }

        class GrammarPhase extends Phase {
            public GrammarPhase( Phase parent ){
                super(parent);
            }

            public AnalyzeResult execute( Iterable<Api> apis, Iterable<Component> components ) throws StaticError {
                Debug.debug( Debug.Type.FORTRESS, 1, "Start phase GrammarPhase" );
                return rewriteGrammar(repository, env, apis, components, lastModified);
            }
        }

        Lambda<Integer,Phase> nextPhase = new Lambda<Integer,Phase>(){
            public Phase value(Integer phase){
                return getPhase(repository, env, apis, components, lastModified, phase );
            }
        };

        switch (phase){
            case PHASE_TOPLEVEL : return new TopLevelPhase(nextPhase.value(PHASE_DESUGAR));
            case PHASE_DESUGAR : return new DesugarPhase(nextPhase.value(PHASE_TYPECHECK));
            case PHASE_TYPECHECK : return new TypecheckPhase(nextPhase.value(PHASE_GRAMMAR));
            case PHASE_GRAMMAR : return new GrammarPhase(nextPhase.value(PHASE_DISAMBIGUATE));
            case PHASE_DISAMBIGUATE : return new DisambiguatePhase(nextPhase.value(PHASE_EMPTY));
            case PHASE_EMPTY : return new EmptyPhase();
            default : throw new RuntimeException( "Invalid phase number: " + phase );
        }
    }

    /* run all the analysis available */
    public static AnalyzeResult analyze(final FortressRepository repository,
                                        final GlobalEnvironment env,
                                        final Iterable<Api> apis,
                                        final Iterable<Component> components,
                                        final long lastModified) throws StaticError {
        return getPhase(repository, env, apis, components, lastModified, currentPhase).run();
    }


}
