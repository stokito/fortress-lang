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

package com.sun.fortress.interpreter.drivers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import edu.rice.cs.plt.tuple.Option;


import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.RedefinitionError;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.EvaluatorTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.interpreter.rewrite.Desugarer;
import com.sun.fortress.shell.AutocachingRepository;
import com.sun.fortress.shell.CacheBasedRepository;
import com.sun.fortress.shell.FileBasedRepository;
import com.sun.fortress.shell.PathBasedRepository;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.CheckedNullPointerException;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.PureList;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Visitor2;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class Driver {

    private static int numThreads = Runtime.getRuntime().availableProcessors();

    private static boolean _libraryTest = false;

    public static FortressRepository DEFAULT_INTERPRETER_REPOSITORY = new AutocachingRepository(
            new PathBasedRepository(ProjectProperties.SOURCE_PATH, ProjectProperties.SOURCE_PATH_NATIVE),
            new CacheBasedRepository(ProjectProperties.ensureDirectoryExists("./.interpreter_cache"))
            );
    
   // private static String LIB_DIR = ProjectProperties.TEST_LIB_DIR;
   // private static String LIB_NATIVE_DIR = ProjectProperties.TEST_LIB_NATIVE_DIR;

    private Driver() {};

    static public void runTests() {
    }

    public static BetterEnv evalComponent(CompilationUnit p,
            FortressRepository fr) throws IOException {
        return evalComponent(p, false, fr);
    }

    public static ArrayList<ComponentWrapper> components;

    public static BetterEnv evalComponent(CompilationUnit p,
                                          boolean woLibrary,
                                          FortressRepository fr) throws IOException {

        Init.initializeEverything();

        /*
         * Begin "linker" -- to be replaced with the real thing, when more
         * infrastructure is present.
         */

        HashMap<String, ComponentWrapper> linker = new HashMap<String, ComponentWrapper>();

        Stack<ComponentWrapper> pile = new Stack<ComponentWrapper>();
        // ArrayList<ComponentWrapper>
        components = new ArrayList<ComponentWrapper>();

        ComponentWrapper comp = new ComponentWrapper((Component) p);

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
        comp.getExports(false);
        linker.put("main", comp);
        pile.push(comp);


        /*
         * This is a patch; eventually, it will all be done explicitly.
         */
        ComponentWrapper lib = new ComponentWrapper(Libraries.theLibrary(fr));
        lib.getEnvironment().installPrimitives();
        lib.getExports(true);
        pile.push(lib);

        /*
         * This performs closure over APIs and components, ensuring that all are
         * initialized through phase one of building environments.
         */
        while (!pile.isEmpty()) {

            ComponentWrapper cw = pile.pop();
            components.add(cw);

            CompilationUnit c = cw.getComponent();
            List<Import> imports = c.getImports();

            ensureImportsImplemented(fr, linker, pile, imports);
        }

        // Desugarer needs to know about trait members.
        for (ComponentWrapper cw : components) {
            cw.preloadTopLevel();
        }

        // Iterate to a fixed point, pushing trait info as far as necessary.
        boolean change = true;
        while (change) {
            change = false;
            for (ComponentWrapper cw : components) {
                change |= injectTraitMembersForDesugaring(linker, cw);
            }

            for (ComponentWrapper cw : components) {
                for (String s : lib.dis.getTopLevelRewriteNames()) {
                    if (!cw.isOwnName(s))
                        change |= cw.dis.injectAtTopLevel(s, s, lib.dis, cw.excludedImportNames);
                }
            }
        }

        /*
         * After all apis etc have been imported, populate their environments.
         */
        for (ComponentWrapper cw : components) {
            // System.err.println("populating " + cw);
            cw.populateOne();
        }

        /*
         * Inject imported names into environments.
         *
         * TODO traits and objects must have their members filtered in these
         * injections. This is not at all simple in the case of a component
         * exporting multiple APIs that may also have different views of the
         * same trait, when some subset of those APIs is imported in another
         * component. Because this is not simple, because the many-to-many case
         * is not yet handled, it is not yet implemented. Getting this right
         * will probably affect the code that builds object types and deals with
         * overloading there.
         */
        for (ComponentWrapper cw : components) {
            /*
             * Transitional stuff. Import everything from "library" into a
             * Component.
             */

            if (cw != lib && !woLibrary)
                importAllExcept(cw.getEnvironment(), lib.getEnvironment(), lib.getEnvironment(),
                        Collections.<String> emptyList(),
                        "FortressLibrary",
                        "FortressLibrary",
                        cw);

            injectExplicitImports(linker, cw);
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
            cw.initVars();
        }

        // Libraries.link(be, dis);

        return comp.getEnvironment();
    }

    private static void injectExplicitImports(
            HashMap<String, ComponentWrapper> linker, ComponentWrapper cw) {
        CompilationUnit c = cw.getComponent();
        List<Import> imports = c.getImports();

        final BetterEnv e = cw.getEnvironment();

        /* First handle all imports that name the things they introduce. */

        for (Import i : imports){
            if (i instanceof ImportApi) {
                ImportApi ix = (ImportApi) i;
                List<AliasedAPIName> apis = ix.getApis();
                for (AliasedAPIName adi : apis) {
                    APIName id = adi.getApi();
                    String from_apiname = NodeUtil.nameString(id);

                    Option<Id> alias = adi.getAlias();
                    Option<APIName> as;
                    if (alias.isSome())
                        as = Option.some(NodeFactory.makeAPIName(Option.unwrap(alias)));
                    else as = Option.none();
                    String known_as = NodeUtil.nameString(Option.unwrap(as, id));

                    ComponentWrapper from_cw = linker.get(from_apiname);

                    /*
                     * Every name N in api A with optional alias B is added
                     * to e, using the value from the component C
                     * implementing A. If B is present, C.N is referenced as
                     * B.N, else it is referenced as A.N.
                     */

                    /*
                     * Not-yet-implemented because of issues with selectors.
                     */
                    bug(adi, errorMsg("Import of dotted names from API ",
                                      adi, "; try import * from insteand."));
                }

            } else if (i instanceof ImportedNames) {
                ImportedNames ix = (ImportedNames) i;
                APIName source = ix.getApi();
                String from_apiname = NodeUtil.nameString(source);

                ComponentWrapper from_cw = linker.get(from_apiname);
                BetterEnv from_e = from_cw.getEnvironment();
                BetterEnv api_e = from_cw.getExportedCW(from_apiname)
                        .getEnvironment();

                /* Pull in names, UNqualified */

                if (ix instanceof ImportNames) {
                    /* A set of names */
                    List<AliasedSimpleName> names = ((ImportNames) ix).getAliasedNames();
                    for (AliasedSimpleName an : names) {
                        SimpleName name = an.getName();
                        Option<SimpleName> alias = an.getAlias();
                        /*
                         * If alias exists, associate the binding from
                         * component_wrapper with alias, otherwise associate
                         * it with plain old name.
                         */

                        inject(e, api_e, from_e, name, alias, from_apiname,
                               NodeUtil.nameString(from_cw.getComponent().getName()), cw);
                    }

                } else if (ix instanceof ImportStar) { /* Do nothing */}
            } else {

            }


        }

        /* Next handle import-*. When two of these try to introduce the same
         * name (implicitly), the name remains undefined, except for functions.
         *
         * When one of these tries to introduce a name previously defined locally
         * or in a non-* import, the new (import-*) definition is ignored.
         */
        for (Import i : imports) {
            if (i instanceof ImportedNames) {
                ImportedNames ix = (ImportedNames) i;
                APIName source = ix.getApi();
                String from_apiname = NodeUtil.nameString(source);

                ComponentWrapper from_cw = linker.get(from_apiname);
                BetterEnv from_e = from_cw.getEnvironment();
                BetterEnv api_e = from_cw.getExportedCW(from_apiname)
                        .getEnvironment();

                /* Pull in names, UNqualified */

                if (ix instanceof ImportStar) {
                    /* All names BUT excepts, as they are listed.
                     * Include all names defined locally in the "except" list,
                     * because local definitions block import-* imports.
                     */
                    final List<SimpleName> excepts = ((ImportStar) ix)
                            .getExcept();
                    final Collection<String> except_names = Useful.applyToAllInserting(
                            excepts, new Fn<SimpleName, String>() {
                                public String apply(SimpleName n) {
                                    return NodeUtil.nameString(n);
                                }
                            },
                            new BASet<String>(StringComparer.V));// cw.ownNonFunctionNames.copy());

                    importAllExcept(e, api_e, from_e, except_names,
                                    from_apiname,
                                    NodeUtil.nameString(from_cw.getComponent().getName()),
                                    cw);

                }
            } else {

            }
        }
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
        CompilationUnit c = cw.getComponent();
        List<Import> imports = c.getImports();
        boolean change = false;

        for (Import i : imports) {
            if (i instanceof ImportApi) {

                    /*
                     * Not-yet-implemented because of issues with selectors.
                     */
                    bug(errorMsg("NYI Import of dotted names ; try 'import * from' instead."));

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
                        SimpleName name = an.getName();
                        Option<SimpleName> alias = an.getAlias();
                        /*
                         * If alias exists, associate the binding from
                         * component_wrapper with alias, otherwise associate
                         * it with plain old name.
                         */
                        /* probable bug: need to insert into ownNonFunction names */
                        change |= cw.dis.injectAtTopLevel(Option.unwrap(alias, name).stringName(),
                                name.stringName(),
                                api_cw.dis,
                                cw.excludedImportNames);

                    }

                } else if (ix instanceof ImportStar) {
                    /* All names BUT excepts, as they are listed. */
                    final List<SimpleName> excepts = ((ImportStar) ix)
                            .getExcept();
                    final Set<String> except_names = Useful.applyToAllInserting(
                            excepts,
                            new Fn<SimpleName, String>() {
                                public String apply(SimpleName n) {
                                    return NodeUtil.nameString(n);
                                }
                            },
                            new HashSet<String>());

                    for (String s : api_cw.dis.getTopLevelRewriteNames()) {
                        if (! except_names.contains(s) &&
                                !cw.isOwnName(s) &&
                                !cw.excludedImportNames.contains(s)) {
                            change |= cw.dis.injectAtTopLevel(s, s, api_cw.dis, cw.excludedImportNames);
                        }
                    }
                }
            } else {
                bug(errorMsg("NYI Import " + i));
            }
        }


        return change;
    }

    private static void scanAllFunctionalMethods(final BetterEnv e) {
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

    private static void finishAllFunctionalMethods(final BetterEnv e) {
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


    private static void notImport(String s, Object o, String api, String component) {
//        System.err.println("Not importing from " + api + " into " + component + " name " + s + ", value " + o);
    }

    /**
     * @param into_e
     * @param from_e
     * @param except_names
     *
     * The current import process can attempt to import some entities
     * twice into the same component.  This happesn in the case of
     * certain kinds of recursive api import, and also (more
     * importantly) when there are imports in FortressLibrary.fss.
     * Eventually fixes elsewhere in the system should eliminate this
     * problem; in the mean time we simply catch duplicate insertions
     * here and silently ignore them.  This is a stopgap measure that
     * should go away.
     */
    private static boolean importAllExcept(final BetterEnv into_e,
            BetterEnv api_e, final BetterEnv from_e,
            final Collection<String> except_names,
            final String a,
            final String c,
            final ComponentWrapper importer) {

        final boolean[] flag = new boolean[1];

        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                try {
                    if (!except_names.contains(s) &&
                            !importer.excludedImportNames.contains(s) &&
                            !importer.ownNames.contains(s)) {
                        FType old = into_e.getTypeNull(s);
                        if (old == null) {
                            into_e.putType(s, NI.cnnf(from_e.getTypeNull(s)));
                        } else {
                            importer.excludedImportNames.add(s);
                            into_e.removeType(s); // Safe to remove, because explcitly defined names are excluded already.
                        }
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                }
            }
            }
        };
        /*
         * Potential problem here, we have to make overloading work when we put
         * a function.
         */
        Visitor2<String, FValue> vv = new Visitor2<String, FValue>() {
            public void visit(String s, FValue o) {
                try {
                    boolean do_import = false;
                    if (!except_names.contains(s) &&
                            !importer.excludedImportNames.contains(s)) {
                        if (ComponentWrapper.overloadable(o)) {
                            if (!importer.ownNonFunctionNames.contains(s)) {
                                do_import = true;
                            }
                        } else {
                            if (!importer.ownNames.contains(s)) {
                                do_import = true;
                            }
                        }
                    }
                    if (do_import) {
                        into_e.putValue(s, NI.cnnf(from_e.getValueRaw(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                    }
                }
            }
        };
        Visitor2<String, Number> vi = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                try {
                    if (!except_names.contains(s) &&
                            !importer.excludedImportNames.contains(s)) {
                        into_e.putInt(s, NI.cnnf(from_e.getIntNull(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                }
            }
            }
        };
        Visitor2<String, Number> vn = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                try {
                    if (!except_names.contains(s) && !importer.excludedImportNames.contains(s)) {
                        into_e.putNat(s, NI.cnnf(from_e.getNat(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                }
            }
            }
        };
        Visitor2<String, Boolean> vb = new Visitor2<String, Boolean>() {
            public void visit(String s, Boolean o) {
                try {
                    if (!except_names.contains(s) && !importer.excludedImportNames.contains(s)) {
                        into_e.putBool(s, NI.cnnf(from_e.getBool(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                    }
                }
            }
        };

        api_e.visit(vt, vn, vi, vv, vb);
        return flag[0];
    }

    private static void inject(BetterEnv e, BetterEnv api_e, BetterEnv from_e,
            SimpleName name, Option<SimpleName> alias, String a, String c,
            ComponentWrapper importer) {
        String s = NodeUtil.nameString(name);
        String add_as = s;
        if (alias.isSome()) {
            add_as = NodeUtil.nameString(Option.unwrap(alias));
        }

        importer.ownNames.add(add_as);

        try {
            boolean isOverloadable = false;
            if (api_e.getNatNull(s) != null) {
                e.putNat(add_as, NI.cnnf(from_e.getNatNull(s)));
            }
            if (api_e.getIntNull(s) != null) {
                e.putInt(add_as, NI.cnnf(from_e.getIntNull(s)));
            }
            if (api_e.getBoolNull(s) != null) {
                e.putBool(add_as, NI.cnnf(from_e.getBoolNull(s)));
            }
            if (api_e.getTypeNull(s) != null) {
                e.putType(add_as, NI.cnnf(from_e.getTypeNull(s)));
            }
            if (api_e.getValueRaw(s) != null) {
                FValue fv = api_e.getValueRaw(s);
                isOverloadable = ComponentWrapper.overloadable(fv);
                e.putValue(add_as, NI.cnnf(from_e.getValueRaw(s)));
            }
            if (! isOverloadable)
                importer.ownNonFunctionNames.add(add_as);
        } catch (CheckedNullPointerException ex) {
            error(errorMsg("Import of ", name, " from api ", a,
                           " not found in implementing component ", c));
        }

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
     * @param linker
     * @param pile
     * @param id
     */
    private static void ensureApiImplemented(
            FortressRepository fr,
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile, APIName name) throws IOException {
        String apiname = NodeUtil.nameString(name);
        if (linker.get(apiname) == null) {
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
            
            ComponentWrapper apicw = new ComponentWrapper(newapi);
            ComponentWrapper newwrapper = new ComponentWrapper(newcomp, apicw);
            newwrapper.getExports(true);
            linker.put(apiname, newwrapper);
            pile.push(newwrapper);

            List<Import> imports = newcomp.getImports();
            // This is what pile.push is for:
            // ensureImportsImplemented(linker, pile, imports);

            imports = newapi.getImports();
            ensureImportsImplemented(fr, linker, pile, imports);
        }
    }

    // This runs the program from inside a task.
    public static FValue
        runProgramTask(CompilationUnit p, boolean runTests, boolean woLibrary,
                       List<String> args, String toBeRun,
                       FortressRepository fr)
        throws IOException
    {

        FortressTests.reset();
        BetterEnv e = evalComponent(p, woLibrary, fr);

        Closure run_fn = e.getClosure(toBeRun);
        Toplevel toplevel = new Toplevel();
        if (runTests) {
            List<Closure> testClosures = FortressTests.get();
            for (Iterator<Closure> i = testClosures.iterator(); i.hasNext();) {
                Closure testCl = i.next();
                List<FValue> fvalue_args = new ArrayList<FValue>();

                testCl.apply(fvalue_args, toplevel, e);
            }
        }
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

    // This creates the parallel context
    public static void runProgram(FortressRepository fr,
                                  CompilationUnit p,
                                  boolean runTests,
                                  boolean libraryTest,
                                  boolean woLibrary,
                                  List<String> args)
        throws Throwable
    {
        _libraryTest = libraryTest;
        String numThreadsString = System.getenv("FORTRESS_THREADS");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);

        if (group == null)

           group = new FortressTaskRunnerGroup(numThreads);

        EvaluatorTask evTask = new EvaluatorTask(fr, p, runTests, woLibrary, "run", args);
        try {
            group.invoke(evTask);
        }
        finally {
            // group.interruptAll();
        }
        if (evTask.causedException()) {
            throw evTask.taskException();
        }
    }

    public static void runProgram(FortressRepository fr, CompilationUnit p, boolean runTests,
            List<String> args) throws Throwable {
        runProgram(fr, p, runTests, false, false, args);
    }


    public static void runProgram(FortressRepository fr, CompilationUnit p, List<String> args) throws Throwable {
        runProgram(fr, p, false, false, false, args);
    }

    private static class Toplevel implements HasAt {
        public String at() {
            return "toplevel";
        }

        public String stringName() {
            return "driver";
        }
    }

    public static Component readTreeOrSourceComponent(String key, String basename, FortressRepository p) throws IOException {
        
        String name  = key;
        APIName apiname = NodeFactory.makeAPIName(name);

        ComponentIndex ci = p.getComponent(apiname);
        CompilationUnit c = ci.ast();
        return (Component) c;
        
    }


    public static Api readTreeOrSourceApi(String key, String basename, FortressRepository p) throws IOException {
        String name  = key;
        APIName apiname = NodeFactory.makeAPIName(name);
        ApiIndex ci = p.getApi(apiname);
        CompilationUnit c = ci.ast();
        return (Api) c;
       
    }

    static Hashtable<String, CompilationUnit> libraryCache = new Hashtable<String, CompilationUnit>();

}
