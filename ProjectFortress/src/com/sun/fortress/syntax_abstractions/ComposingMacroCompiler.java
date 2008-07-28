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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import xtc.tree.Attribute;
import xtc.tree.Comment;
import xtc.parser.Action;
import xtc.parser.Element;
import xtc.parser.FullProduction;
import xtc.parser.AlternativeAddition;
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

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.syntax_abstractions.phases.ComposingSyntaxDefTranslator;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.BaseType;

import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.syntax_abstractions.rats.RatsUtil;

public class ComposingMacroCompiler {

    private static final String FORTRESS = "com.sun.fortress.parser.Fortress";
    private static final String TEMPLATEPARSER = "com.sun.fortress.parser.templateparser.TemplateParser";
    private static final String COMPILATION = "com.sun.fortress.parser.Compilation";
    private static final String TEMPLATECOMPILATION = "com.sun.fortress.parser.templateparser.Compilation";
    private static final char SEP = '/'; // File.separatorChar
    private static final String USER_MODULE_NAME = "USER";

    private ComposingMacroCompiler() {}

    public static class PEG implements Iterable<String> {
        Map<String, List<SyntaxDef>> entries = new HashMap<String,List<SyntaxDef>>();
        Map<String, BaseType> typemap = new HashMap<String, BaseType>();
        public PEG() { }

        public List<SyntaxDef> create(String nt) {
            if (entries.containsKey(nt)) {
                throw new RuntimeException("PEG already has an entry for " + nt);
            }
            List<SyntaxDef> defs = new LinkedList<SyntaxDef>();
            entries.put(nt, defs);
            return defs;
        }
        public List<SyntaxDef> get(String nt) {
            if (!entries.containsKey(nt)) {
                throw new RuntimeException("PEG has no entry for " + nt);
            }
            return entries.get(nt);
        }
        public Iterator<String> iterator() {
            return entries.keySet().iterator();
        }

        public void putType(String nt, BaseType type) {
            if (typemap.containsKey(nt)) {
                throw new RuntimeException("PEG already has a type for " + nt);
            }
            typemap.put(nt, type);
        }
        public BaseType getType(String nt) {
            if (!typemap.containsKey(nt)) {
                throw new RuntimeException("PEG has no entry for " + nt);
            }
            return typemap.get(nt);
        }
    }

    public static class Mangler {
        Set<String> nativeNonterminals;
        Mangler(Set<String> nativeNonterminals) {
            this.nativeNonterminals = nativeNonterminals;
        }
        public String forDefinition(String name) {
            return mangle(name);
        }
        public String forReference(String name) {
            if (nativeNonterminals.contains(name)) {
                // (eg: Fortress.Expression.Expr => Expression.Expr)
                return getRatsModuleName(name) + "." + afterLastDot(name);
            } else {
                return mangle(name);
            }
        }
        public String mangle(String name) {
            return "USER_" + name.replaceAll("_", "__").replace('.', '_');
        }
    }


    public static Class<?> compile( GrammarIndex grammar ){
        Debug.debug(Debug.Type.SYNTAX, 2, 
                    "ComposingMacroCompiler: create parser for grammar " + grammar.getName() );

        Collection<GrammarIndex> imports = grammar.getExtended();
        Debug.debug( Debug.Type.SYNTAX, 2, "Imports: " + imports );
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> definitions = 
            computeDefinitions( grammar );
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> extensions = 
            computeExtensions( grammar );
        return pegFor( collectImports( grammar ), imports, definitions, extensions );
    }

    private static Collection<GrammarIndex> collectImports( GrammarIndex grammar ){
        Collection<GrammarIndex> all = new HashSet<GrammarIndex>();

        Collection<GrammarIndex> extended = grammar.getExtended();
        for ( GrammarIndex importedGrammar : extended ){
            all.add( importedGrammar );
            all.addAll( collectImports( importedGrammar ) );
        }

        return all;
    }


    private static Class<?> pegFor( Collection<GrammarIndex> relevant,
                             Collection<GrammarIndex> imports,
                             Collection<NonterminalIndex<? extends GrammarMemberDecl>> definitions,
                             Collection<NonterminalIndex<? extends GrammarMemberDecl>> extensions ){

        PEG peg = new PEG();

        pegForGrammarDefsOnly( peg, relevant );
        pegForDefs( peg, relevant, definitions );
        for ( NonterminalIndex<? extends GrammarMemberDecl> e : extensions ){
            applyExtension( peg, e, relevant );
        }

        Set<String> extendedNonterminals = importsExtensionDomain( imports );
        Set<String> extendedExplicit = computeExplicit(extensions);
        Set<String> extendedImplicit = setDifference(extendedNonterminals, extendedExplicit);
        List<NonterminalIndex<? extends GrammarMemberDecl>> implicitExtensions = 
            computeImplicitExtensions( extendedImplicit, imports );

        for ( NonterminalIndex<? extends GrammarMemberDecl> e : implicitExtensions ){
            applyExtension( peg, e, relevant );
        }

        Debug.debug( Debug.Type.SYNTAX, 2, "Extended nonterminals: " + extendedNonterminals );
        Debug.debug( Debug.Type.SYNTAX, 2, "Extended explicit: " + extendedExplicit );
        Debug.debug( Debug.Type.SYNTAX, 2, "Extended implicit: " + extendedImplicit );

        Set<String> nativeNonterminals = new HashSet<String>();
        for ( GrammarIndex grammar : relevant ) {
            GrammarDef grammarDef = grammar.ast().unwrap();
            Debug.debug(Debug.Type.SYNTAX, 3, "Grammar " + grammarDef + " native?: " + grammarDef.isNative());
            if (grammarDef.isNative()) {
                // A native grammar can only contain nonterminal declarations
                for (GrammarMemberDecl member : grammarDef.getMembers()) {
                    NonterminalHeader header = member.getHeader();
                    nativeNonterminals.add(NodeUtil.nameString(header.getName()));
                }
            }
        }

        Debug.debug( Debug.Type.SYNTAX, 2, "Native nonterminals: " + nativeNonterminals );

        return makeParser(peg, nativeNonterminals);
    }

    private static void pegForGrammarDefsOnly( PEG peg, Collection<GrammarIndex> relevant ){
        for ( GrammarIndex grammar : relevant ){
            pegForDefs( peg, relevant, grammar.getDeclaredNonterminals() );
        }
    }

    private static void pegForDefs( PEG peg, Collection<GrammarIndex> relevant, 
                                    Collection<NonterminalIndex<? extends GrammarMemberDecl>> nonterminals ){

        for ( NonterminalIndex<? extends GrammarMemberDecl> nonterminal : nonterminals ){
            String nonterminalName = nonterminal.getName().toString();
            List<SyntaxDef> defs = peg.create(nonterminalName);
            Debug.debug( Debug.Type.SYNTAX, 2,
                         "Add " + nonterminalName + " to peg definition" );
            pegEntryForDef( defs, relevant, nonterminal );

            Option<BaseType> type = nonterminal.getAst().getAstType();
            if (type.isSome()) {
                peg.putType(nonterminalName, type.unwrap());
            } else {
                throw new RuntimeException("No type for nonterminal " + nonterminal);
            }
        }
        // pegForDefs grammars defs = map (pegEntryForDef grammars) defs
    }

    private static void pegEntryForDef( final List<SyntaxDef> defs, 
                                        final Collection<GrammarIndex> relevant, 
                                        NonterminalIndex<? extends GrammarMemberDecl> nonterminal ){
        nonterminal.getAst().accept( new NodeDepthFirstVisitor_void(){
            @Override public void forNonterminalDef(NonterminalDef that){
                for ( SyntaxDef def : that.getSyntaxDefs() ){
                    resolveChoice( defs, relevant, def );
                }
            }
            @Override public void for_TerminalDef(_TerminalDef that){
                resolveChoice( defs, relevant, that.getSyntaxDef() );
            }
        });
    }

    private static List<NonterminalIndex<? extends GrammarMemberDecl>> computeImplicitExtensions( final Set<String> implicit, Collection<GrammarIndex> imports ){

        final List<NonterminalIndex<? extends GrammarMemberDecl>> all = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
        for ( GrammarIndex import_ : imports ){
            for ( final NonterminalIndex<? extends GrammarMemberDecl> nonterminal : import_.getDeclaredNonterminals() ){
                nonterminal.getAst().accept( new NodeDepthFirstVisitor_void(){
                    @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                        if ( implicit.contains( that.getHeader().getName().toString() ) ){
                            all.add( nonterminal );
                        }
                    }
                });
            }
        }

        return all;
    }

    private static Set<String> importsExtensionDomain( Collection<GrammarIndex> imports ){
        Set<String> all = new HashSet<String>();

        for ( GrammarIndex grammar : imports ){
            extensionDomain( all, grammar );
        }

        // importsExtensionDomain imports = nub (concat (map extensionDomain imports))

        return all;
    }

    private static void extensionDomain( final Set<String> domain, GrammarIndex grammar ){
        for ( NonterminalIndex<? extends GrammarMemberDecl> nonterminal : grammar.getDeclaredNonterminals() ){
            nonterminal.getAst().accept( new NodeDepthFirstVisitor_void(){
                @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                    domain.add( that.getHeader().getName().toString() );
                }
            });
        }

        // GrammarC _ _ _ exts _) = (map nameof exts)
    }

    private static void applyExtension( PEG peg, 
                                        NonterminalIndex<? extends GrammarMemberDecl> extension, 
                                        Collection<GrammarIndex> relevant ){
        Debug.debug( Debug.Type.SYNTAX, 2, "Apply extensions to " + extension.getName() );
        final List<SyntaxDef> defs = peg.get( extension.getName().toString() );
        if ( defs == null ){
            throw new RuntimeException( "Defs is null, this cannot happen" );
        }

        extension.getAst().accept( new NodeDepthFirstVisitor_void(){
            @Override public void forNonterminalExtensionDef(NonterminalExtensionDef that) {
                defs.addAll( 0, that.getSyntaxDefs() );
            }

            @Override public void forNonterminalDef(NonterminalDef that){
                defs.addAll( 0, that.getSyntaxDefs() );
            }

            @Override public void for_TerminalDef(_TerminalDef that){
                defs.add( 0, that.getSyntaxDef() );
            }
        });
    }

    private static void resolveChoice( List<SyntaxDef> defs, Collection<GrammarIndex> relevant, SyntaxDef def ){

        defs.add( def );

        /* TODO: do something with super choices, SuperSyntaxDef..? */

        /*
            resolveChoice :: [GrammarC] -> Choice -> [Choice]
            resolveChoice grammars (Choice pattern action) = [Choice pattern action]
            resolveChoice grammars (SuperChoice gname nt) = resolvedChoices
                where Just (GrammarC _ _ defs exts xfs) = selectByName grammars gname
                          Just (Extension _ choices) = selectByName exts nt
                                    resolvedChoices = resolveChoices grammars choices
        */
    }

    /*
    private void resolveChoices( List<NonterminalIndex<? extends GrammarMemberDecl>> defs, ... relevant, ... nonterminal ){
    }
    */

    private static Class<?> makeParser(PEG peg, Set<String> nativeNonterminals) {
        Mangler mangler = new Mangler(nativeNonterminals);

        // Turn peg into a single new module
        //   (mangle names to avoid conflict)
        //   extensions of native nonterminals become simple definitions
        //   For each reference to native nonterminal,
        //     rewrite to base parser's nonterminal instead (do not mangle)
        Module userModule = createUserModule();
        for (String nt : peg) {
            addEntry(userModule, mangler, peg, nt);
        }

        // For each extended native nonterminal,
        //   add new module's production to top
        Map<String, Module> baseModules = getBaseModules(RatsUtil.getParserPath());
        Map<String, Module> templateModules = getBaseModules(RatsUtil.getTemplateParserPath());
        for (String definedByPeg : peg) {
            Debug.debug( Debug.Type.SYNTAX, 3,
                         "Checking if " + definedByPeg + " is native");
            if (nativeNonterminals.contains(definedByPeg)) {
                String moduleName = getRatsModuleName(definedByPeg);
                String ntName = afterLastDot(definedByPeg);
                String userExtensionsName = mangler.mangle(definedByPeg);
                Module baseModule = baseModules.get(getRatsModuleName(definedByPeg));
                if ( baseModule.modification != null ){
                    Debug.debug( Debug.Type.SYNTAX, 3, baseModule.getClassName() + " is a modification" );
                }
                Debug.debug( Debug.Type.SYNTAX, 3,
                             "Modify " + definedByPeg +
                             "; ntName=" + ntName + 
                             "; baseModule=" + getRatsModuleName(definedByPeg));
                boolean found = false;
                for (Production p : baseModule.productions) {
                    if (ntName.equals(p.name.name)) {
                        found = true;
                        Element indirection = new NonTerminal(userExtensionsName);
                        Sequence indirectionSequence = new Sequence(indirection);
                        p.choice.alternatives.add(0, indirectionSequence);
                    } else {
                        // Debug.debug( Debug.Type.SYNTAX, 4, "Ignoring production " + p.name.name + ". " + p.getClass().getName() );
                    }
                }
                if (!found) {
                    /*
                    String type = "Expr";
                    Production add = new AlternativeAddition( type, new NonTerminal(userExtensionsName), new OrderedChoice(), null, true );
                    Element indirection = new NonTerminal(userExtensionsName);
                    Sequence indirectionSequence = new Sequence(indirection);
                    add.choice.alternatives.add(0, indirectionSequence);
                    */
                    throw new RuntimeException("Failed to modify " + definedByPeg + ". Could not find a production for it." );
                }
            }
        }

        // Add imports from every (ordinary) fortress module
        // Add import from USER to every fortress module
        Module mainModule = null;
        ModuleName uname = new ModuleName("USERvar");
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

        for ( Module baseModule : templateModules.values() ){
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

                for ( ModuleDependency dependancy : baseModule.dependencies ){
                    if ( dependancy instanceof ModuleModification ){
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

    private static void addEntry(Module m, Mangler mangler, PEG peg, String nt) {
        Debug.debug( Debug.Type.SYNTAX, 2, "Create alternative for " + nt );

        String name = mangler.forDefinition(nt);
        String javaType = FortressTypeToJavaType.analyze(peg.getType(nt));

        ComposingSyntaxDefTranslator translator = 
            new ComposingSyntaxDefTranslator(mangler, nt, javaType, peg.typemap);
        List<Sequence> sequences = translator.visitSyntaxDefs(peg.get(nt));

        List<Attribute> attributes = new LinkedList<Attribute>();
        OrderedChoice choice = new OrderedChoice(sequences);
        Production p = new FullProduction(attributes, javaType, new NonTerminal(name), choice);
        m.productions.add(p);
    }

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

    private static Comment createComment() {
        List<String> lines = new LinkedList<String>();
        lines.add("Module generated by the Fortress compiler on " + new Date());
        return new Comment(Comment.Kind.SINGLE_LINE, lines);
    }

    private static String getRatsModuleName(String name) {
        return afterLastDot(beforeLastDot(name));
    }

    private static String beforeLastDot(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(0, lastDot);
        } else {
            throw new RuntimeException("Saw unqualified nonterminal name: " + name);
            // return name;
        }
    }

    private static String afterLastDot(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(lastDot + 1);
        } else {
            throw new RuntimeException("Saw unqualified nonterminal name: " + name);
            // return name;
        }
    }



    // Utilities

    private static <T> Set<T> setDifference( Set<T> s1, Set<T> s2 ){
        Set<T> all = new HashSet<T>( s1 );
        all.removeAll( s2 );
        return all;
    }

    private static Collection<NonterminalIndex<? extends GrammarMemberDecl>> computeDefinitions( GrammarIndex grammar ){
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> all = 
            new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();

        for ( NonterminalIndex<? extends GrammarMemberDecl> index :
                  grammar.getDeclaredNonterminals() ){
            if ( isDefinition( index ) ){
                all.add( index );
            }
        }

        return all;
    }

    private static Collection<NonterminalIndex<? extends GrammarMemberDecl>> computeExtensions( GrammarIndex grammar ){
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> all = 
            new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();

        for ( NonterminalIndex<? extends GrammarMemberDecl> index : 
                  grammar.getDeclaredNonterminals() ){
            if ( isExtension( index ) ){
                all.add( index );
            }
        }

        return all;
    }

    private static boolean isDefinition( NonterminalIndex<? extends GrammarMemberDecl> index ){
        return index.getAst() instanceof NonterminalDef ||
               index.getAst() instanceof _TerminalDef;
    }

    private static boolean isExtension( NonterminalIndex<? extends GrammarMemberDecl> index ){
        return index.getAst() instanceof NonterminalExtensionDef;
    }

    private static Set<String> computeExplicit( Collection<NonterminalIndex<? extends GrammarMemberDecl>> extensions ){
        Set<String> all = new HashSet<String>();

        for ( NonterminalIndex<? extends GrammarMemberDecl> index : extensions ){
            all.add( index.getName().toString() );
        }

        return all;
    }

}
