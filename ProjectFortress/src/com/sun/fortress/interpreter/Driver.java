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

package com.sun.fortress.interpreter;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.env.ComponentWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.RedefinitionError;
import com.sun.fortress.interpreter.evaluator.BuildTestEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.tasks.EvaluatorTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbsDecl;
import com.sun.fortress.nodes.AbsExternalSyntax;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.AbsVarDecl;
import com.sun.fortress.nodes.AbstractArrowType;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;
import com.sun.fortress.nodes.NodeVisitor_void;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TestDecl;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.VarAbsDeclOrDecl;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.CheckedNullPointerException;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Visitor1;
import com.sun.fortress.useful.Visitor2;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.interpreter.glue.WellKnownNames.*;

public class Driver {

    private static boolean _libraryTest = false;

    static String libraryName = fortressLibrary;
    static String nativesName = fortressBuiltin;
    static String builtinsName = "AnyType";

    private Driver() {};

    static public void runTests() {
    }

    /**
     * Native code sometimes needs access to the library component wrapper.
     */
    static ComponentWrapper libraryComponentWrapper = null;
    public static Environment getFortressLibrary() {
        return libraryComponentWrapper.getEnvironment();
    }

    public static ArrayList<ComponentWrapper> components;

    public static Environment evalComponent(Component p,
                                            FortressRepository fr)
        throws IOException {

        Init.initializeEverything();

        /*
         * Begin "linker" -- to be replaced with the real thing, when more
         * infrastructure is present.
         */

        HashMap<String, ComponentWrapper> linker = new HashMap<String, ComponentWrapper>();

        Stack<ComponentWrapper> pile = new Stack<ComponentWrapper>();
        // ArrayList<ComponentWrapper>
        components = new ArrayList<ComponentWrapper>();

        /* 
         * This looks like gratuitous and useless error checking that
         * interferes with the "test" flag.
         * 
        for(String defaultLib : WellKnownNames.defaultLibrary) {
            APIName libAPI = NodeFactory.makeAPIName(defaultLib);
            if (p.getName().equals(libAPI)) {
                error("Fortress built-in library " + defaultLib + " does not export executable.");
            }
        }
        */
       
        ComponentWrapper comp = commandLineComponent(fr, linker, pile, p);

        /*
         * This "linker" implements a one-to-one, same-name correspondence
         * between APIs and components.
         *
         * phase 0.5: starting with the main component, identify other
         * components that will be needed (the fortress will contain the map
         * from component C's imported APIs to their implementing components).
         * Each component will be paired with its top level environment (TLE).
         *
         * phase 1: for each component, prepare its TLE by creating all name
         * slots (nothing in the slots will is initialized AT ALL -- it's all
         * thunks, empty mutables, and uninitialized types).
         *
         * phase 1.5: for each component C, add to C's TLE all the names that C
         * imports (using the API->component map from the fortress to figure out
         * the values/types associated with each of those names).
         *
         */
        comp.touchExports(false);
        // linker.put("Executable", comp);
        //pile.push(comp);


        /*
         * This is a patch; eventually, it will all be done explicitly.
         */

        /*
         * Notice that builtins is used ONLY to satisfy the interface of the
         * importer for purposes of injecting primitives &c into other components.
         */
        ComponentWrapper builtins = new ComponentWrapper(readTreeOrSourceApi(builtinsName, builtinsName, fr), linker, WellKnownNames.defaultLibrary);
        builtins.getEnvironment().installPrimitives();
        linker.put(builtinsName, builtins);
        
        ComponentWrapper lib = null;

        libraryComponentWrapper = ensureApiImplemented(fr, linker, pile, NodeFactory.makeAPIName(libraryName));
        lib = libraryComponentWrapper.getExportedCW(libraryName);

        ComponentWrapper nativescomp =
            ensureApiImplemented(fr, linker, pile,
                                 NodeFactory.makeAPIName(nativesName));
        ComponentWrapper natives = nativescomp.getExportedCW(nativesName);

        /*
         * This performs closure over APIs and components, ensuring that all are
         * initialized through phase one of building environments.
         */
        while (!pile.isEmpty()) {

            ComponentWrapper cw = pile.pop();
            components.add(cw);

            CompilationUnit c = cw.getCompilationUnit();
            List<Import> imports = c.getImports();

            ensureImportsImplemented(fr, linker, pile, imports);
        }

        // Desugarer needs to know about trait members.
        for (ComponentWrapper cw : components) {
            cw.preloadTopLevel();
        }

        // Iterate to a fixed point, pushing trait info as far as necessary.
        // This is not the same as pushing information into environments, which
        // occurs below the call(s) to populateOne.
        boolean change = true;
        while (change) {
            change = false;
            for (ComponentWrapper cw : components) {
                change |= injectTraitMembersForDesugaring(linker, cw);
            }

            change |= injectLibraryTraits(components, lib);
            change |= injectLibraryTraits(components, natives);
        }

        /*
         * After all apis etc have been imported, populate their environments.
         * First the component is populated, then (recursively, inside that
         * call to populate) the exported apis are populated.
         */
        for (int i = components.size() - 1; i >= 0; i--) {
            ComponentWrapper cw = components.get(i);
            // System.err.println("populating " + cw);
            cw.populateOne();
        }
       
        for (ComponentWrapper cw : components) {
            cw.initTypes();
        }
        for (ComponentWrapper cw : components) {
            scanAllFunctionalMethods(cw.getEnvironment());
        }
        for (ComponentWrapper cw : components) {
            cw.initFuncs();
        }

        for (ComponentWrapper cw : components) {
            finishAllFunctionalMethods(cw.getEnvironment());
        }

        for (ComponentWrapper cw : components) {
            cw.reset();
        }

         for (ComponentWrapper cw : components) {
            cw.initVars();
        }

        return comp.getEnvironment();
    }

 
    /**
     * Copies rewriting information (traits, what members they define) from apis into
     * components.  This scaffolding is necessary to get the simplification right
     * for member (field, method) references.
     *
     * @param linker
     * @param cw
     */
    private static boolean injectTraitMembersForDesugaring(
            HashMap<String, ComponentWrapper> linker, ComponentWrapper cw) {
        CompilationUnit c = cw.getCompilationUnit();
        List<Import> imports = c.getImports();
        boolean change = false;

        for (Import i : imports) {
            if (i instanceof ImportApi) {

                    /*
                     * Not-yet-implemented because of issues with selectors.
                     */
                bug(errorMsg("NYI 'Import api APIName'; ",
                             "try 'import APIName.{...}' instead."));
            } else if (i instanceof ImportedNames) {
                ImportedNames ix = (ImportedNames) i;
                APIName source = ix.getApi();
                String from_apiname = NodeUtil.nameString(source);

                ComponentWrapper from_cw = linker.get(from_apiname);
                ComponentWrapper api_cw = from_cw.getExportedCW(from_apiname);

                /* Pull in names, UNqualified */

                if (ix instanceof ImportNames) {
                    /* A set of names */
                    List<AliasedSimpleName> names = ((ImportNames) ix).getAliasedNames();
                    for (AliasedSimpleName an : names) {
                        IdOrOpOrAnonymousName name = an.getName();
                        Option<IdOrOpOrAnonymousName> alias = an.getAlias();
                        /*
                         * If alias exists, associate the binding from
                         * component_wrapper with alias, otherwise associate
                         * it with plain old name.
                         */
                        /* probable bug: need to insert into ownNonFunction names */
                        change |= cw.desugarer.injectAtTopLevel(alias.unwrap(name).stringName(),
                                                                name.stringName(),
                                                                api_cw.desugarer,
                                                                cw.excludedImportNames);
                    }

                } else if (ix instanceof ImportStar) {
                    /* All names BUT excepts, as they are listed. */
                    final List<IdOrOpOrAnonymousName> excepts = ((ImportStar) ix)
                            .getExcept();
                    final Set<String> except_names = Useful.applyToAllInserting(
                            excepts,
                            new Fn<IdOrOpOrAnonymousName, String>() {
                                public String apply(IdOrOpOrAnonymousName n) {
                                    return NodeUtil.nameString(n);
                                }
                            },
                            new HashSet<String>());

                    for (String s : api_cw.desugarer.getTopLevelRewriteNames()) {
                        if (! except_names.contains(s) &&
                                !cw.isOwnName(s) &&
                                !cw.excludedImportNames.contains(s)) {
                            change |= cw.desugarer.injectAtTopLevel(s, s, api_cw.desugarer, cw.excludedImportNames);
                        }
                    }
                    for (String s : api_cw.desugarer.functionals) {
                        if (! except_names.contains(s) &&
                            //    !cw.isOwnName(s) &&
                                !cw.excludedImportNames.contains(s)) {
                            change |= cw.desugarer.injectAtTopLevel(s, s, api_cw.desugarer, cw.excludedImportNames);
                        }
                    }
                }
            } else {
                bug(errorMsg("NYI Import " + i));
            }
        }


        return change;
    }

    private static boolean injectLibraryTraits(List<ComponentWrapper> components,
            ComponentWrapper lib) {
        boolean change = false;
        for (ComponentWrapper cw : components) {
            for (String s : lib.desugarer.getTopLevelRewriteNames()) {
                if (cw.isOwnName(s)) {
                    continue;
                }
                change |= cw.desugarer.injectAtTopLevel(s, s, lib.desugarer, cw.excludedImportNames);
            }
            for (String s : lib.desugarer.functionals) {
//                if (cw.isOwnName(s)) {
//                    continue;
//                }
                change |= cw.desugarer.injectAtTopLevel(s, s, lib.desugarer, cw.excludedImportNames);
            }
        }
        return change;
    }

    private static void scanAllFunctionalMethods(final Environment e) {
        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                if (o instanceof FTraitOrObjectOrGeneric) {
                    FTraitOrObjectOrGeneric tooog = (FTraitOrObjectOrGeneric) o;
                    tooog.initializeFunctionalMethods(e);
                    if (tooog instanceof FTraitOrObject) {
                        for (FType t : ((FTraitOrObject)tooog).getProperTransitiveExtends())
                            if (t instanceof FTraitOrObjectOrGeneric)
                                ((FTraitOrObject)t).initializeFunctionalMethods(e);
                    } else {

                    }
                }
            }
        };
        e.visit(vt, null, null, null, null);
    }

    private static void finishAllFunctionalMethods(final Environment e) {
        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                if (o instanceof FTraitOrObjectOrGeneric) {
                    FTraitOrObjectOrGeneric tooog = (FTraitOrObjectOrGeneric) o;
                    tooog.finishFunctionalMethods(e);
                    if (tooog instanceof FTraitOrObject) {
                        for (FType t : ((FTraitOrObject)tooog).getProperTransitiveExtends())
                            if (t instanceof FTraitOrObjectOrGeneric)
                                ((FTraitOrObject)t).finishFunctionalMethods(e);
                    }
                }
            }
        };
        e.visit(vt, null, null, null, null);
    }

    /**
     * For each imported API, checks to see if it is already "linked". If not,
     * "finds" it (looks for apiname.fss/apiname.tfs) and reads it in and sticks
     * it in the "pile".
     *
     * For each component, also reads in the corresponding APIs so that we know
     * what to export.
     *
     * @param linker
     * @param pile
     * @param imports
     */
    private static void ensureImportsImplemented (
            FortressRepository fr,
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile,
            List<Import> imports
        )
        throws IOException
    {

        for (Import i : imports) {
            if (i instanceof ImportApi) {
                ImportApi ix = (ImportApi) i;
                List<AliasedAPIName> apis = ix.getApis();
                for (AliasedAPIName adi : apis) {
                    APIName id = adi.getApi();
                    ensureApiImplemented(fr, linker, pile, id);
                }
            }
            else if (i instanceof ImportedNames) {
                ImportedNames ix = (ImportedNames) i;
                APIName source = ix.getApi();
                ensureApiImplemented(fr, linker, pile, source);
            }
            else {
                bug(i, "Unrecognized import");
            }
        }
    }

    /**
     * Returns the component wrapper (for the component) that exports this API.
     * Needs to generalize to sets of APIs in the future, perhaps.
     *
     * @param linker
     * @param pile
     * @param id
     */
    private static ComponentWrapper ensureApiImplemented(
            FortressRepository fr,
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile, APIName name) throws IOException {
        String apiname = NodeUtil.nameString(name);
        ComponentWrapper newwrapper = linker.get(apiname);
        if (newwrapper == null) {
            /*
             * Here, the linker prototype takes the extreme shortcut of assuming
             * that Api Foo is implemented by Component Foo, dots and all.
             *
             * These few lines are what needs to be replaced by a real linker.
             */
            Api newapi = readTreeOrSourceApi(apiname, apiname, fr);
            Component newcomp;
            //boolean is_native = false;

            newcomp = readTreeOrSourceComponent(apiname, apiname, fr) ;

            ComponentWrapper apicw = new ComponentWrapper(newapi, linker, WellKnownNames.defaultLibrary);
            newwrapper = new ComponentWrapper(newcomp, apicw, linker, WellKnownNames.defaultLibrary);
            newwrapper.touchExports(true);
            linker.put(apiname, newwrapper);
            pile.push(newwrapper);

            List<Import> imports = newapi.getImports();
            ensureImportsImplemented(fr, linker, pile, imports);
        }
        return newwrapper;
    }
    
    private static ComponentWrapper commandLineComponent(FortressRepository fr,
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile, Component comp) throws IOException {
        
            APIName name = comp.getName();
            String apiname = NodeUtil.nameString(name);
            ComponentWrapper comp_wrapper; 
            
            List<ComponentWrapper> exports_list = new ArrayList<ComponentWrapper>(1);
            for (Export ex : comp.getExports()) 
                for (APIName ex_apiname : ex.getApis()) {
                    String ex_name = NodeUtil.nameString(ex_apiname);
                    Api newapi = readTreeOrSourceApi(ex_name, ex_name, fr);
                    exports_list.add( new ComponentWrapper(newapi, linker, WellKnownNames.defaultLibrary) );
                }
                         
            comp_wrapper = new ComponentWrapper(comp, exports_list, linker, WellKnownNames.defaultLibrary);
            comp_wrapper.touchExports(true);
            linker.put(apiname, comp_wrapper);
            pile.push(comp_wrapper);

            for (ComponentWrapper apicw : exports_list) {
                List<Import> imports  = apicw.getImports();
                linker.put(apicw.name(), comp_wrapper);
                ensureImportsImplemented(fr, linker, pile, imports);
            }
        return comp_wrapper;
    }

    // This runs the program from inside a task.
    public static FValue
        runProgramTask(Component p,
                       List<String> args, String toBeRun,
                       FortressRepository fr)
        throws IOException
    {

        Environment e = evalComponent(p, fr);

        Closure run_fn = e.getClosure(toBeRun);
        Toplevel toplevel = new Toplevel();
        ArrayList<FValue> fvalueArgs = new ArrayList<FValue>();
        for (String s : args) {
            fvalueArgs.add(FString.make(s));
        }
        FValue ret = run_fn.apply(fvalueArgs, toplevel, e);
        // try {
        // e.dump(System.out);
        // } catch (IOException ioe) {
        // System.out.println("io exception" + ioe);
        // }
       return ret;
    }

    static FortressTaskRunnerGroup group;

    static int getNumThreads() {
        int numThreads;

        String numThreadsString = System.getenv("FORTRESS_THREADS");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);
        else {
            int availThreads = Runtime.getRuntime().availableProcessors();
            if (availThreads <= 2)
                numThreads = availThreads;
            else
                numThreads = (int) Math.floor((double) availThreads/2.0);
        }
        return numThreads;
    }


    public static void runTests(FortressRepository fr, Component p) 
        throws Throwable {

        BuildTestEnvironments bte = new BuildTestEnvironments ();
        bte.visit(p);
        List<String> tests = BuildTestEnvironments.getTests();
            
        if (group == null)
            group = new FortressTaskRunnerGroup(getNumThreads());

        for (String s : tests) {
            List<String> args = new ArrayList<String>();
            EvaluatorTask evTask = new EvaluatorTask(fr, p, s, args);
            group.invoke(evTask);
            if (evTask.causedException()) {
                throw evTask.taskException();
            }
        }            
    }

    // This creates the parallel context
    public static FValue runProgram(FortressRepository fr, Component p,
                                    List<String> args)
        throws Throwable {

        if (group == null)
            group = new FortressTaskRunnerGroup(getNumThreads());

        EvaluatorTask evTask = new EvaluatorTask(fr, p, "run", args);
        try {
            group.invoke(evTask);
            if (evTask.causedException()) {
                throw evTask.taskException();
            }
            return evTask.result();
        }
        finally {
            // group.interruptAll();
        }
    }


    private static class Toplevel implements HasAt {
        public String at() {
            return "toplevel";
        }

        public String stringName() {
            return "driver";
        }
    }

    private static Component readTreeOrSourceComponent(String key, String basename, FortressRepository p) throws IOException {

        String name  = key;
        APIName apiname = NodeFactory.makeAPIName(name);

        ComponentIndex ci = p.getComponent(apiname);
        CompilationUnit c = ci.ast();
        return (Component) c;

    }


    private static Api readTreeOrSourceApi(String key, String basename, FortressRepository p) throws IOException {
        String name  = key;
        APIName apiname = NodeFactory.makeAPIName(name);
        ApiIndex ci = p.getApi(apiname);
        CompilationUnit c = ci.ast();
        return (Api) c;

    }

    static Hashtable<String, CompilationUnit> libraryCache = new Hashtable<String, CompilationUnit>();

}
