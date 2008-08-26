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

/*
 * Class which builds a table of pieces of Rats! AST which corresponds the macro 
 * declarations given as input.
 * The Rats! ASTs are combined to Rats! modules which are written to files on the
 * file system.
 * 
 */

package com.sun.fortress.syntax_abstractions;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import xtc.tree.Attribute;
import xtc.tree.Comment;
import xtc.parser.Action;
import xtc.parser.Element;
import xtc.parser.FullProduction;
import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleImport;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleModification;
import xtc.parser.ModuleList;
import xtc.parser.ModuleName;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.Sequence;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.compiler.index.NonterminalDefIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.syntax_abstractions.phases.ComposingSyntaxDefTranslator;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SuperSyntaxDef;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;

import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.syntax_abstractions.rats.RatsUtil;

/* Creates a parser by invoking Rats! on the productions defined by grammars
 * and their imports.
 */
public class ComposingMacroCompiler {

    private static final String FORTRESS = 
        "com.sun.fortress.parser.Fortress";
    private static final String TEMPLATEPARSER =
        "com.sun.fortress.parser.templateparser.TemplateParser";
    private static final String COMPILATION =
        "com.sun.fortress.parser.Compilation";
    private static final String TEMPLATECOMPILATION =
        "com.sun.fortress.parser.templateparser.Compilation";

    private static final char SEP = '/'; // File.separatorChar
    private static final String USER_MODULE_NAME = "USER";

    private ComposingMacroCompiler() {}

    /* takes a string and converts it to some internal name */
    public static class Mangler {
        Set<String> nativeNonterminals;
        Mangler(Set<String> nativeNonterminals) {
            this.nativeNonterminals = nativeNonterminals;
        }
        public String forDefinition(Id id) {
            return forDefinition(NodeUtil.nameString(id));
        }
        public String forDefinition(String name) {
            return mangle(name);
        }
        public String forReference(Id id) {
            /* don't mangle native nonterminals */
            String name = NodeUtil.nameString(id);
            if (nativeNonterminals.contains(name)) {
                // (eg: Fortress.Expression.Expr => Expression.Expr)
                return getRatsModuleName(name, id) + "." + afterLastDot(name, id);
            } else {
                return mangle(name);
            }
        }
        private String mangle(String name) {
            return "USER_" + name.replaceAll("_", "__").replace('.', '_');
        }
    }

    /* main entry point for a grammar of an api */
    public static Class<?> compile(GrammarIndex grammar){
        Debug.debug(Debug.Type.SYNTAX, 2, 
                    "ComposingMacroCompiler: create parser for grammar " + grammar.getName());

        List<GrammarIndex> imports = grammar.getExtended();
        Debug.debug(Debug.Type.SYNTAX, 2, "Imports: " + imports);
        List<NonterminalDefIndex> definitions = grammar.getDefinitions();
        List<NonterminalExtendIndex> extensions = grammar.getExtensions();
        return pegFor(collectImports(grammar), imports, definitions, extensions);
    }

    /* main entry point for a component */
    public static Class<?> parserForComponent(List<GrammarIndex> imports) {
        Debug.debug(Debug.Type.SYNTAX, 2, 
                    "ComposingMacroCompiler: create parser for component");
        Debug.debug(Debug.Type.SYNTAX, 2, "Imports: " + imports);
        List<NonterminalDefIndex> definitions = new LinkedList<NonterminalDefIndex>();
        List<NonterminalExtendIndex> extensions = new LinkedList<NonterminalExtendIndex>();
        return pegFor(collectImports(imports), imports, definitions, extensions);
    }

    /* get the complete list of imported grammars */
    private static Set<GrammarIndex> collectImports(GrammarIndex grammar){
        return collectImports(grammar.getExtended());
    }

    /* finds imported grammars as well as their imported gramars */
    private static Set<GrammarIndex> collectImports(List<GrammarIndex> extended) {
        Set<GrammarIndex> all = new HashSet<GrammarIndex>();
        for (GrammarIndex importedGrammar : extended){
            all.add(importedGrammar);
            all.addAll(collectImports(importedGrammar));
        }
        return all;
    }

    /* create a peg and populate it with nonterminals according to the
     * grammar composition rules
     */
    private static Class<?> pegFor(Set<GrammarIndex> relevant,
                                   List<GrammarIndex> imports,
                                   Collection<NonterminalDefIndex> definitions,
                                   Collection<NonterminalExtendIndex> extensions){

        Debug.debug(Debug.Type.SYNTAX, 2, "Relevant grammars: " + relevant);

        PEG peg = new PEG();

        addGrammarDefsOnly(peg, relevant);
        addDefs(peg, relevant, definitions);

        Set<String> extendedNonterminals = importsExtensionDomain(imports);
        Set<String> extendedExplicit = computeExplicit(extensions);
        Set<String> extendedImplicit = setDifference(extendedNonterminals, extendedExplicit);

        Debug.debug(Debug.Type.SYNTAX, 2, "Extended nonterminals: " + extendedNonterminals);
        Debug.debug(Debug.Type.SYNTAX, 2, "Extended explicit: " + extendedExplicit);
        Debug.debug(Debug.Type.SYNTAX, 2, "Extended implicit: " + extendedImplicit);

        for (NonterminalExtendIndex e : extensions){
            applyExtension(peg, e, relevant);
        }
        for (String implicitNonterminal : extendedImplicit) {
            ; // FIXME: fill this in, and delete next part
        }
        // FIXME: from here
        Collection<NonterminalExtendIndex> implicitExtensions = 
            computeImplicitExtensions(extendedImplicit, imports);
        for (NonterminalExtendIndex e : implicitExtensions){
            applyExtension(peg, e, relevant);
        }
        // FIXME: to here

        Set<String> nativeNonterminals = computeNativeNonterminals(relevant);

        peg.removeEmptyNonterminals();
        return makeParser(peg, nativeNonterminals);
    }

    private static Set<String> computeNativeNonterminals(Set<GrammarIndex> relevant) {
        Set<String> nativeNonterminals = new HashSet<String>();
        for (GrammarIndex grammar : relevant) {
            GrammarDef grammarDef = grammar.ast();
            Debug.debug(Debug.Type.SYNTAX, 3,
                        "Grammar " + grammarDef + " native?: " + grammarDef.isNative());
            if (grammarDef.isNative()) {
                // A native grammar can only contain nonterminal declarations
                for (GrammarMemberDecl member : grammarDef.getMembers()) {
                    NonterminalHeader header = member.getHeader();
                    nativeNonterminals.add(NodeUtil.nameString(header.getName()));
                }
            }
        }
        Debug.debug(Debug.Type.SYNTAX, 2, "Native nonterminals: " + nativeNonterminals);
        return nativeNonterminals;
    }

    /* populate entries for definition sites only */
    private static void addGrammarDefsOnly(PEG peg, Set<GrammarIndex> relevant){
        for (GrammarIndex grammar : relevant){
            addDefs(peg, relevant, grammar.getDefinitions());
        }
    }

    /* populate peg for a single grammar and its definitions */
    private static void addDefs(PEG peg, Set<GrammarIndex> relevant, 
                                Collection<NonterminalDefIndex> nonterminals) {
        for (NonterminalDefIndex nonterminal : nonterminals){
            Id nonterminalName = nonterminal.getName();
            List<SyntaxDef> defs = peg.create(nonterminalName);
            Debug.debug(Debug.Type.SYNTAX, 2, "Add " + nonterminalName + " to peg definition");
            addEntryForDef(defs, relevant, nonterminal);

            Option<BaseType> type = nonterminal.ast().getAstType();
            if (type.isSome()) {
                peg.putType(nonterminalName, type.unwrap());
            } else {
                throw new MacroError(nonterminalName, "No type for nonterminal " + nonterminal);
            }
        }
    }

    /* add choices for a single definition */
    private static void addEntryForDef(final List<SyntaxDef> defs, 
                                       final Collection<GrammarIndex> relevant, 
                                       NonterminalDefIndex nonterminal){
        nonterminal.ast().accept(new NodeDepthFirstVisitor_void(){
            @Override public void forNonterminalDef(NonterminalDef that){
                for (SyntaxDecl decl : that.getSyntaxDecls()){
                    resolveAndAdd(defs, relevant, decl);
                }
            }

            @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                throw new MacroError(that, "Unexpected extension in definition?!");
            }
        });
    }

    /* return set of nonterminals that are extended by virtue of being
     * in the extends clause of the grammar
     */
    private static List<NonterminalExtendIndex> computeImplicitExtensions(final Set<String> implicit, 
                                                                          List<GrammarIndex> imports){

        final List<NonterminalExtendIndex> all = new ArrayList<NonterminalExtendIndex>();

        for (GrammarIndex imp : imports){
            for (final NonterminalIndex nonterminal : imp.getDeclaredNonterminals()){
                if (nonterminal instanceof NonterminalExtendIndex) {
                    if (implicit.contains(NodeUtil.nameString(nonterminal.getName()))) {
                        all.add((NonterminalExtendIndex) nonterminal);
                    }
                }
            }
        }
        return all;
    }

    /* add choices to nonterminals when the choice is an extension of the nonterminal
     */
    private static Set<String> importsExtensionDomain(Collection<GrammarIndex> imports){
        Set<String> all = new HashSet<String>();
        for (GrammarIndex grammar : imports){
            extensionDomain(all, grammar);
        }
        return all;
    }

    /* only handles nonterminal extensions */
    private static void extensionDomain(final Set<String> domain, GrammarIndex grammar){
        for (NonterminalIndex nonterminal : grammar.getDeclaredNonterminals()){
            nonterminal.ast().accept(new NodeDepthFirstVisitor_void(){
                @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                    domain.add(that.getHeader().getName().toString());
                }
                @Override public void forNonterminalDef(NonterminalDef that) {
                    return;
                }
            });
        }
    }

    /* adds the choices for nonterminal extensions to the definition nonterminals
     */
    private static void applyExtension(final PEG peg, final NonterminalExtendIndex extension, 
                                       final Collection<GrammarIndex> relevant){
        Debug.debug(Debug.Type.SYNTAX, 2, "Apply extensions to " + extension.getName());
        final List<SyntaxDef> exts = peg.getExts(extension.getName());

        final List<SyntaxDef> intermediate = new ArrayList<SyntaxDef>();
        extension.ast().accept(new NodeDepthFirstVisitor_void(){
                @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                    for (SyntaxDecl decl : that.getSyntaxDecls()){
                        resolveAndAdd(exts, relevant, decl);
                        // intermediate.addAll(resolveChoice(relevant, decl));
                    }
                }

                @Override public void forNonterminalDef(NonterminalDef that){
                    for (SyntaxDecl decl : that.getSyntaxDecls()){
                        resolveAndAdd(exts, relevant, decl);
                        // intermediate.addAll(resolveChoice(relevant, decl));
                    }
                }
            });
        // exts.addAll(intermediate);
    }

    /* lookup the choices of a nonterminal in a specific grammar
     */
    private static Collection<SyntaxDecl> findExtension(Id nonterminalName, 
                                                        Id grammarName, 
                                                        Collection<GrammarIndex> grammars){
        Debug.debug(Debug.Type.SYNTAX, 3, 
                    "Searching for extension for nonterminal " + nonterminalName +
                    " and grammar " + grammarName);
        for (GrammarIndex grammar : grammars){
            Debug.debug(Debug.Type.SYNTAX, 3,
                         "Checking if grammar " + grammar.getName() +
                         " matches " + grammarName);
            if (grammar.getName().equals(grammarName)){
                Debug.debug(Debug.Type.SYNTAX, 3, ".. yes!");
                Option<NonterminalIndex> nt = grammar.getNonterminalDecl(nonterminalName);
                if (nt.isSome()){
                    return nt.unwrap().getSyntaxDecls();
                }
            } else {
                Debug.debug(Debug.Type.SYNTAX, 3, ".. no");
            }
        }
        throw new MacroError(nonterminalName,
                             "Could not find nonterminal " + nonterminalName +
                             " in grammar " + grammarName);
    }

    /* add choices either defined by the SyntaxDecl or the choices defined by the
     * super non-terminal
     */
    private static void resolveAndAdd(final List<SyntaxDef> defs,
                                      final Collection<GrammarIndex> relevant, 
                                      SyntaxDecl decl){
        decl.accept(new NodeDepthFirstVisitor_void(){
            @Override public void forSyntaxDef(SyntaxDef that) {
                Debug.debug(Debug.Type.SYNTAX, 3, "Add choice " + that);
                defs.add(that);
            }
            @Override public void forSuperSyntaxDef(SuperSyntaxDef that) {
                for (SyntaxDecl extension 
                         : findExtension(that.getNonterminal(), that.getGrammar(), relevant)){
                    resolveAndAdd(defs, relevant, extension);
                }
            }
        });
    }

    /* Compile a parser given a set of nonterminals and productions.
     * This gets a little hairy becuase we need the core Fortress parser
     * as well as the template parser.
     */
    private static Class<?> makeParser(PEG peg, Set<String> nativeNonterminals) {
        Mangler mangler = new Mangler(nativeNonterminals);

        Debug.debug(Debug.Type.SYNTAX, 3,
                    "Native nonterminals: " + nativeNonterminals);

        // Turn peg into a single new module
        //   (mangle names to avoid conflict)
        //   extensions of native nonterminals become simple definitions
        //   For each reference to native nonterminal,
        //     rewrite to base parser's nonterminal instead (do not mangle)
        Module userModule = createUserModule();
        for (Id nt : peg.keySet()) {
            addEntry(userModule, mangler, peg, nt);
        }

        // For each extended native nonterminal,
        //   add new module's production to top
        Map<String, Module> baseModules = getBaseModules(RatsUtil.getParserPath());
        Map<String, Module> templateModules = getBaseModules(RatsUtil.getTemplateParserPath());
        for (Id definedByPegId : peg.keySet()) {
            String definedByPeg = NodeUtil.nameString(definedByPegId);
            Debug.debug(Debug.Type.SYNTAX, 3,
                        "Checking if " + definedByPeg + " is native");
            if (nativeNonterminals.contains(definedByPeg)) {
                Debug.debug(Debug.Type.SYNTAX, 3, "... yes!");
                String moduleName = getRatsModuleName(definedByPeg, definedByPegId);
                String ntName = afterLastDot(definedByPeg, definedByPegId);
                String userExtensionsName = mangler.forDefinition(definedByPeg);
                Module baseModule = baseModules.get(getRatsModuleName(definedByPeg, definedByPegId));
                if (baseModule.modification != null){
                    Debug.debug(Debug.Type.SYNTAX, 3, 
                                baseModule.getClassName() + " is a modification");
                }
                Debug.debug(Debug.Type.SYNTAX, 3,
                            "Modify " + definedByPeg +
                            "; ntName=" + ntName + 
                            "; baseModule=" + getRatsModuleName(definedByPeg, definedByPegId));
                boolean found = false;
                for (Production p : baseModule.productions) {
                    if (ntName.equals(p.name.name)) {
                        found = true;
                        Element indirection = new NonTerminal(userExtensionsName);
                        Sequence indirectionSequence = new Sequence(indirection);
                        p.choice.alternatives.add(0, indirectionSequence);
                    } else {
                        // Debug.debug(Debug.Type.SYNTAX, 4,
                        //             "Ignoring production " + p.name.name +
                        //            ". " + p.getClass().getName());
                    }
                }
                if (!found) {
                    throw new MacroError(definedByPegId,
                                         "Failed to modify " + definedByPeg +
                                         ". Could not find a production for it.");
                }
            }
        }

        // Add imports from every (ordinary) fortress module
        // Add import from USER to every fortress module
        Module mainModule = null;
        ModuleName uname = new ModuleName("USERvar");
        /* modify fortress modules */
        for (Module baseModule : baseModules.values()) {
            if (!baseModule.name.name.equals(FORTRESS) && 
                !baseModule.name.name.equals(TEMPLATEPARSER) && 
                !baseModule.name.name.equals(COMPILATION)) {
                ModuleName bname = new ModuleName(afterLastDot(baseModule.name.name));
                List<ModuleName> ups = new LinkedList<ModuleName>(userModule.parameters.names);
                ups.add(bname);
                userModule.parameters = new ModuleList(ups);
                userModule.dependencies.add(new ModuleImport(bname));
                List<ModuleName> bps = new LinkedList<ModuleName>();
                if (baseModule.parameters != null)
                    bps.addAll(baseModule.parameters.names);
                bps.add(uname);
                baseModule.parameters = new ModuleList(bps);
                baseModule.dependencies.add(new ModuleImport(uname));
            }
        }

        /* modify template parser modules */
        for (Module baseModule : templateModules.values()){
            if (!baseModule.name.name.equals(TEMPLATEPARSER) && 
                !baseModule.name.name.equals(FORTRESS) && 
                !baseModule.name.name.equals(TEMPLATECOMPILATION)) {
                // ModuleName bname = new ModuleName(afterLastDot(baseModule.name.name));
                // List<ModuleName> ups = new LinkedList<ModuleName>(userModule.parameters.names);
                // ups.add(bname);
                // userModule.parameters = new ModuleList(ups);
                // userModule.dependencies.add(new ModuleImport(bname));
                List<ModuleName> bps = new LinkedList<ModuleName>();
                if (baseModule.parameters != null)
                    bps.addAll(baseModule.parameters.names);
                bps.add(uname);
                baseModule.parameters = new ModuleList(bps);
                baseModule.dependencies.add(new ModuleImport(uname));

                for (ModuleDependency dependancy : baseModule.dependencies){
                    if (dependancy instanceof ModuleModification){
                        dependancy.arguments.names.add(uname);
                    }
                }
            } else if (baseModule.name.name.equals(TEMPLATEPARSER)) {
                mainModule = baseModule;
            }
        }

        // for each existing instantiation, add USER to the arguments
        for (ModuleDependency dep : mainModule.dependencies) {
            if (!dep.module.name.equals(TEMPLATECOMPILATION)) {
                List<ModuleName> args = new LinkedList<ModuleName>();
                if (dep.arguments != null) {
                    args.addAll(dep.arguments.names);
                }
                args.add(uname);
                dep.arguments = new ModuleList(args);
            }
        }
        // and add an instantiation with all the other modules
        List<ModuleName> argslist = new LinkedList<ModuleName>();
        for (Module baseModule : baseModules.values()) {
            if (baseModule != mainModule &&
                !baseModule.name.name.equals(FORTRESS) &&
                !baseModule.name.name.equals(COMPILATION)) {
                ModuleName bname = new ModuleName(afterLastDot(baseModule.name.name));
                argslist.add(bname);
            }
        }
        ModuleList args = new ModuleList(argslist);
        ModuleInstantiation inst = 
            new ModuleInstantiation(new ModuleName(USER_MODULE_NAME), args, uname);
        mainModule.dependencies.add(inst);

        // Generate parser
        Collection<Module> modules = new LinkedList<Module>();
        modules.addAll(baseModules.values());
        modules.addAll(templateModules.values());
        modules.add(userModule);
        return RatsParserGenerator.generateParser(modules);
    }

    // Maps unqualified module names to modules
    private static Map<String, Module> getBaseModules(String srcDir) {
        Map<String, Module> map = new HashMap<String, Module>();
        class RatsFilenameFilter implements FilenameFilter {
            public boolean accept(File dir, String name) {
                return name.endsWith(".rats");
            }		
        }
        File f = new File(srcDir);
        for (String s: f.list(new RatsFilenameFilter())) {
            String name = s.substring(0, s.length() - 5);
            String filename = srcDir + File.separatorChar + s;
            map.put(name, RatsUtil.getRatsModule(filename));
        }
        return map;
    }

    /* Filter redundant syntax defs. Rats! will complain if they are left in */
    private static List<SyntaxDef> unique(List<SyntaxDef> defs){
        List<SyntaxDef> all = new LinkedList<SyntaxDef>();
        for (SyntaxDef def : defs){
            if (! all.contains(def)){
                all.add(def);
            }
        }
        return all;
    }

    /* given a SyntaxDecl node, convert it to a Rats! production and
     * add it to the peg
     */
    private static void addEntry(Module m, Mangler mangler, PEG peg, Id nt) {
        Debug.debug(Debug.Type.SYNTAX, 2, "Create alternative for " + nt);

        String name = mangler.forDefinition(nt);
        String javaType = peg.getJavaType(nt);

        ComposingSyntaxDefTranslator translator = 
            new ComposingSyntaxDefTranslator(mangler, nt, javaType, (NTEnv)peg);
        List<Sequence> sequences = translator.visitSyntaxDefs(unique(peg.getAll(nt)));

        /* dont add an empty sequence */
        List<Attribute> attributes = new LinkedList<Attribute>();
        OrderedChoice choice = new OrderedChoice(sequences);
        Production p = new FullProduction(attributes, javaType, new NonTerminal(name), choice);
        m.productions.add(p);
    }

    /* Rats! module containing all the user defined syntax stuff
     */
    private static Module createUserModule() {
        // based on RatsUtil.makeExtendingRatsModule()
        Module m = new Module();
        m.name = new ModuleName(USER_MODULE_NAME);
        m.productions = new LinkedList<Production>();
        m.parameters = new ModuleList(new LinkedList<ModuleName>());
        m.dependencies = new LinkedList<ModuleDependency>();
        // FIXME: Add dependencies
        m.documentation = createComment();
        m.header = createHeader();
        return m;
    }

    /* top of the user module */
    private static Action createHeader() {
        List<String> imports = new LinkedList<String>();
        List<Integer> indents = new LinkedList<Integer>();
        indents.add(3);
        imports.add("import java.util.Map;");
        indents.add(3);
        imports.add("import java.util.HashMap;");
        Action a = new Action(imports, indents);
        return a;
    }

    /* cruft */
    private static Comment createComment() {
        List<String> lines = new LinkedList<String>();
        lines.add("Module generated by the Fortress compiler on " + new Date());
        return new Comment(Comment.Kind.SINGLE_LINE, lines);
    }

    /* helper functions */
    private static String getRatsModuleName(String name, Node src) {
        return afterLastDot(beforeLastDot(name, src), src);
    }

    private static String beforeLastDot(String name, Node src) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(0, lastDot);
        } else {
            throw new MacroError(src, "Saw unqualified name: " + name);
        }
    }

    private static String afterLastDot(String name, Node src) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(lastDot + 1);
        } else {
            throw new MacroError(src, "Saw unqualified name: " + name);
        }
    }

    private static String afterLastDot(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(lastDot + 1);
        } else {
            throw new MacroError("Saw unqualified name: " + name);
        }
    }

    // Utilities

    private static <T> Set<T> setDifference(Set<T> s1, Set<T> s2){
        Set<T> all = new HashSet<T>(s1);
        all.removeAll(s2);
        return all;
    }

    /* convert extensions to their names */
    private static Set<String> computeExplicit(Collection<NonterminalExtendIndex> extensions){
        Set<String> all = new HashSet<String>();
        for (NonterminalExtendIndex index : extensions) {
            all.add(index.getName().toString());
        }
        return all;
    }
}
