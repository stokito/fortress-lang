/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions;

import com.sun.fortress.compiler.index.GrammarIndex;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.syntax_abstractions.phases.ComposingSyntaxDefTranslator;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.useful.Debug;
import xtc.parser.*;
import xtc.tree.Attribute;
import xtc.tree.Comment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/* Creates a Rats! parser from a PEG. */
public class ParserMaker {

    private static final String FORTRESS = "com.sun.fortress.parser.Fortress";
    private static final String TEMPLATEPARSER = "com.sun.fortress.parser.templateparser.TemplateParser";
    private static final String COMPILATION = "com.sun.fortress.parser.Compilation";
    private static final String TEMPLATECOMPILATION = "com.sun.fortress.parser.templateparser.Compilation";

    private static final String USER_MODULE_NAME = "USER";

    private ParserMaker() {
    }

    /* takes a string and converts it to some internal name */
    public static class Mangler {
        Set<Id> nativeNonterminals;

        Mangler(Set<Id> nativeNonterminals) {
            this.nativeNonterminals = nativeNonterminals;
        }

        public String forDefinition(Id id) {
            return forDefinition(NodeUtil.nameString(id));
        }

        public String forDefinition(String name) {
            return mangle(name);
        }

        public String forReference(Id id) {
            String name = NodeUtil.nameString(id);
            // Don't mangle native nonterminals
            if (nativeNonterminals.contains(id)) {
                // (eg: Fortress.Expression.Expr => Expression.Expr)
                return getRatsModuleName(name, id) + "." + afterLastDot(name, id);
            } else {
                return mangle(name);
            }
        }

        private String mangle(String name) {
            return "USER_" + name.replace("_", "__").replace('.', '_');
        }
    }

    /* main entry point for a grammar of an api */
    public static Class<?> parserForGrammar(GrammarIndex grammar) {
        PEG peg = GrammarComposer.pegForGrammar(grammar);
        return makeParser(peg);
    }

    /* main entry point for a component */
    public static Class<?> parserForComponent(List<GrammarIndex> imports) {
        PEG peg = GrammarComposer.pegForComponent(imports);
        return makeParser(peg);
    }

    /* Compile a parser given a set of nonterminals and productions.
     * This gets a little hairy becuase we need the core Fortress parser
     * as well as the template parser.
     */
    private static Class<?> makeParser(PEG peg) {
        Set<Id> nativeNonterminals = peg.nativeNonterminals;
        Mangler mangler = new Mangler(nativeNonterminals);

        Debug.debug(Debug.Type.SYNTAX, 3, "Native nonterminals: ", nativeNonterminals);

        // Remove empty nonterminals (otherwise Rats! complains)
        peg.removeEmptyNonterminals();

        // Turn peg into a single new module
        //   (mangle names to avoid conflict)
        //   extensions of native nonterminals become simple definitions
        //   For each reference to native nonterminal,
        //     rewrite to base parser's nonterminal instead (do not mangle)
        Module userModule = createUserModule();
        for (Id nt : peg.keySet()) {
            addEntry(userModule, mangler, peg, nt);
        }

        // Compute the keywords and add them to the user module
        addKeywords(userModule, peg);

        // For each extended native nonterminal,
        //   add new module's production to top.
        Map<String, Module> baseModules = getBaseModules(RatsUtil.getParserPath());
        Map<String, Module> templateModules = getBaseModules(RatsUtil.getTemplateParserPath());
        for (Id definedByPegId : peg.keySet()) {
            String definedByPeg = NodeUtil.nameString(definedByPegId);
            Debug.debug(Debug.Type.SYNTAX, 3, "Checking if " + definedByPeg + " is native");
            if (nativeNonterminals.contains(definedByPegId)) {
                Debug.debug(Debug.Type.SYNTAX, 3, "... yes!");
                String moduleName = getRatsModuleName(definedByPeg, definedByPegId);
                String ntName = afterLastDot(definedByPeg, definedByPegId);
                String userExtensionsName = mangler.forDefinition(definedByPeg);
                Module baseModule = baseModules.get(moduleName);
                if (baseModule.modification != null) {
                    Debug.debug(Debug.Type.SYNTAX, 3, baseModule.getClassName() + " is a modification");
                }
                Debug.debug(Debug.Type.SYNTAX,
                            3,
                            "Modify " + definedByPeg + "; ntName=" + ntName + "; baseModule=" + moduleName);
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
                        //            ": " + p.getClass().getName());
                    }
                }
                if (!found) {
                    throw new MacroError(definedByPegId,
                                         "Failed to modify " + definedByPeg + ".  Could not find a production for it.");
                }
            }
        }

        // Add imports from every (ordinary) fortress module.
        // Add import from USER to every fortress module.
        Module mainModule = null;
        ModuleName uname = new ModuleName("USERvar");
        /* modify fortress modules */
        for (Module baseModule : baseModules.values()) {
            if (!baseModule.name.name.equals(FORTRESS) && !baseModule.name.name.equals(TEMPLATEPARSER) &&
                !baseModule.name.name.equals(COMPILATION)) {
                ModuleName bname = new ModuleName(afterLastDot(baseModule.name.name));
                List<ModuleName> ups = new LinkedList<ModuleName>(userModule.parameters.names);
                ups.add(bname);
                userModule.parameters = new ModuleList(ups);
                userModule.dependencies.add(new ModuleImport(bname));
                List<ModuleName> bps = new LinkedList<ModuleName>();
                if (baseModule.parameters != null) bps.addAll(baseModule.parameters.names);
                bps.add(uname);
                baseModule.parameters = new ModuleList(bps);
                baseModule.dependencies.add(new ModuleImport(uname));
            }
        }

        /* modify template parser modules */
        for (Module baseModule : templateModules.values()) {
            if (!baseModule.name.name.equals(TEMPLATEPARSER) && !baseModule.name.name.equals(FORTRESS) &&
                !baseModule.name.name.equals(TEMPLATECOMPILATION)) {
                List<ModuleName> bps = new LinkedList<ModuleName>();
                if (baseModule.parameters != null) bps.addAll(baseModule.parameters.names);
                bps.add(uname);
                baseModule.parameters = new ModuleList(bps);
                baseModule.dependencies.add(new ModuleImport(uname));

                for (ModuleDependency dependancy : baseModule.dependencies) {
                    if (dependancy instanceof ModuleModification) {
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
            if (baseModule != mainModule && !baseModule.name.name.equals(FORTRESS) && !baseModule.name.name.equals(
                    COMPILATION)) {
                ModuleName bname = new ModuleName(afterLastDot(baseModule.name.name));
                argslist.add(bname);
            }
        }
        ModuleList args = new ModuleList(argslist);
        ModuleInstantiation inst = new ModuleInstantiation(new ModuleName(USER_MODULE_NAME), args, uname);
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
        for (String s : f.list(new RatsFilenameFilter())) {
            String name = s.substring(0, s.length() - 5);
            String filename = srcDir + File.separatorChar + s;
            map.put(name, RatsUtil.getRatsModule(filename));
        }
        return map;
    }

    /* given a SyntaxDecl node, convert it to a Rats! production and
     * add it to the peg
     */
    private static void addEntry(Module m, Mangler mangler, PEG peg, Id nt) {
        Debug.debug(Debug.Type.SYNTAX, 2, "Create productions for the nonterminal " + nt);

        String name = mangler.forDefinition(nt);
        String javaType = peg.getJavaType(nt);

        ComposingSyntaxDefTranslator translator = new ComposingSyntaxDefTranslator(mangler, nt, javaType, (NTEnv) peg);
        List<Sequence> sequences = translator.visitSyntaxDefs(unique(peg.getAll(nt)));

        /* don't add an empty sequence */
        List<Attribute> attributes = new LinkedList<Attribute>();
        OrderedChoice choice = new OrderedChoice(sequences);
        Production p = new FullProduction(attributes, javaType, new NonTerminal(name), choice);
        m.productions.add(p);
    }

    /* Filter redundant syntax defs.  Rats! will complain if they are left in. */
    private static List<SyntaxDef> unique(List<SyntaxDef> defs) {
        List<SyntaxDef> all = new LinkedList<SyntaxDef>();
        for (SyntaxDef def : defs) {
            if (!all.contains(def)) {
                all.add(def);
            }
        }
        return all;
    }

    private static void addKeywords(Module m, PEG peg) {
        Set<String> keywords = new HashSet<String>();
        for (Id nt : peg.keySet()) {
            for (SyntaxDef def : peg.getDefs(nt)) {
                collectKeywords(keywords, def);
            }
            for (SyntaxDef def : peg.getExts(nt)) {
                collectKeywords(keywords, def);
            }
        }

        List<String> code = new ArrayList<String>();
        List<Integer> indents = new ArrayList<Integer>();

        if (m.body != null) {
            code.addAll(m.body.code);
            indents.addAll(m.body.indent);
        }

        code.add("static {");
        indents.add(0);
        for (String keyword : keywords) {
            code.add(String.format("FORTRESS_KEYWORDS.add(\"%s\");", keyword));
            indents.add(2);
        }
        code.add("}");
        indents.add(0);

        m.body = new Action(code, indents);
    }

    private static void collectKeywords(final Set<String> keywords, SyntaxDef def) {
        for (SyntaxSymbol symbol : def.getSyntaxSymbols()) {
            symbol.accept(new NodeDepthFirstVisitor_void() {
                @Override
                public void forKeywordSymbolOnly(KeywordSymbol that) {
                    keywords.add(that.getToken());
                }
            });
        }
    }

    /* Rats! module containing all the user defined syntax stuff */
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
        indents.add(4);
        imports.add("import java.util.Map;");
        indents.add(4);
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
            if (!(src instanceof ASTNode)) bug(src, "Only ASTNodes are supported.");
            throw new MacroError((ASTNode) src, "Saw unqualified name: " + name);
        }
    }

    private static String afterLastDot(String name, Node src) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(lastDot + 1);
        } else {
            if (!(src instanceof ASTNode)) bug(src, "Only ASTNodes are supported.");
            throw new MacroError((ASTNode) src, "Saw unqualified name: " + name);
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
}
