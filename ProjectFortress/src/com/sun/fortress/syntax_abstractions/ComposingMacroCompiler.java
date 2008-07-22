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

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.phases.GrammarTranslator;
import com.sun.fortress.syntax_abstractions.phases.ModuleTranslator;
import com.sun.fortress.syntax_abstractions.rats.RatsParserGenerator;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;

public class ComposingMacroCompiler {

    private ComposingMacroCompiler(){
    }

    public static Class<?> compile( GrammarIndex grammar ){

        Debug.debug( Debug.Type.SYNTAX, 2, "ComposingMacroCompiler: create parser for grammar " + grammar.getName() );
    
        Collection<GrammarIndex> imports = grammar.getExtended();
        Debug.debug( Debug.Type.SYNTAX, 2, "Imports: " + imports );
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> definitions = computeDefinitions( grammar );
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> extensions = computeExtensions( grammar );
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

    private static Collection<NonterminalIndex<? extends GrammarMemberDecl>> computeDefinitions( GrammarIndex grammar ){
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> all = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();

        for ( NonterminalIndex<? extends GrammarMemberDecl> index : grammar.getDeclaredNonterminals() ){
            if ( isDefinition( index ) ){
                all.add( index );
            }
        }

        return all;
    }

    private static Collection<NonterminalIndex<? extends GrammarMemberDecl>> computeExtensions( GrammarIndex grammar ){
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> all = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();

        for ( NonterminalIndex<? extends GrammarMemberDecl> index : grammar.getDeclaredNonterminals() ){
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

    private static Class<?> pegFor( Collection<GrammarIndex> relevant,
                             Collection<GrammarIndex> imports,
                             Collection<NonterminalIndex<? extends GrammarMemberDecl>> definitions,
                             Collection<NonterminalIndex<? extends GrammarMemberDecl>> extensions ){

        Map<String, List<SyntaxDef>> peg = new HashMap<String,List<SyntaxDef>>();

        pegForGrammarDefsOnly( peg, relevant );
        pegForDefs( peg, relevant, definitions );
        for ( NonterminalIndex<? extends GrammarMemberDecl> e : extensions ){
            applyExtension( peg, e, relevant );
        }

        Set<String> extendedNonterminals = importsExtensionDomain( imports );
        Set<String> extendedExplicit = computeExplicit(extensions);
        Set<String> extendedImplicit = setDifference(extendedNonterminals, extendedExplicit);
        List<NonterminalIndex<? extends GrammarMemberDecl>> implicitExtensions = computeImplicitExtensions( extendedImplicit, imports );

        for ( NonterminalIndex<? extends GrammarMemberDecl> e : implicitExtensions ){
            applyExtension( peg, e, relevant );
        }

        if ( Debug.isOnFor( 2, Debug.Type.SYNTAX ) ){
            Debug.debug( Debug.Type.SYNTAX, 2, "Extended nonterminals: " + extendedNonterminals );
            Debug.debug( Debug.Type.SYNTAX, 2, "Extended explicit: " + extendedExplicit );
            Debug.debug( Debug.Type.SYNTAX, 2, "Extended implicit: " + extendedImplicit );
        }

        /* create the parser from the peg and return its class */
        return com.sun.fortress.parser.Fortress.class;
        // return null;

        /*
pegFor relevant imports defs exts = peg
where peg0 = pegForGrammarDefsOnly relevant
peg1 = pegForDefs relevant defs
peg = applyExtensions relevant (exts ++ implicitExts) (peg1 ++ peg0)
extNTs = importsExtensionDomain imports
explicitNTs = map nameof exts
implicitNTs = extNTs \\ explicitNTs
implicitExts = [implicitExtension nt imports | nt <- implicitNTs]
*/
    }

    private static <T> Set<T> setDifference( Set<T> s1, Set<T> s2 ){
        Set<T> all = new HashSet<T>( s1 );
        all.removeAll( s2 );
        return all;
    }

    private static Set<String> computeExplicit( Collection<NonterminalIndex<? extends GrammarMemberDecl>> extensions ){
        Set<String> all = new HashSet<String>();

        for ( NonterminalIndex<? extends GrammarMemberDecl> index : extensions ){
            all.add( index.getName().toString() );
        }

        return all;
    }

    private static void applyExtension( Map<String, List<SyntaxDef>> peg, NonterminalIndex<? extends GrammarMemberDecl> extension, Collection<GrammarIndex> relevant ){
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

    private static void pegForGrammarDefsOnly( Map<String, List<SyntaxDef>> peg, Collection<GrammarIndex> relevant ){
        for ( GrammarIndex grammar : relevant ){
            pegForDefs( peg, relevant, grammar.getDeclaredNonterminals() );
        }
    }

    private static void pegForDefs( Map<String, List<SyntaxDef>> peg, Collection<GrammarIndex> relevant, Collection<NonterminalIndex<? extends GrammarMemberDecl>> nonterminals ){

        for ( NonterminalIndex<? extends GrammarMemberDecl> nonterminal : nonterminals ){
            List<SyntaxDef> defs = new LinkedList<SyntaxDef>();

            /*
            nonterminal.getAst().accept( new NodeDepthFirstVisitor_void(){
                @Override public void forNon
            });
            */

            Debug.debug( Debug.Type.SYNTAX, 2, "Add " + nonterminal.getName().toString() + " to peg definition" );
            peg.put( nonterminal.getName().toString(), defs );
            
            pegEntryForDef( defs, relevant, nonterminal );
        }

        // pegForDefs grammars defs = map (pegEntryForDef grammars) defs
    }

    private static void pegEntryForDef( final List<SyntaxDef> defs, final Collection<GrammarIndex> relevant, NonterminalIndex<? extends GrammarMemberDecl> nonterminal ){

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

    /*
    public Result compile(Collection<GrammarIndex> grammarIndexs, GlobalEnvironment env) {

        //	    for(GrammarIndex g: grammarIndexs) {
        //	        System.err.println(g.getName() + ", "+ g.isToplevel());
        //	    }

        / *
         * Initialize GrammarIndex
         * /
        GrammarIndexInitializer.Result giir = GrammarIndexInitializer.init(grammarIndexs); 
        if (!giir.isSuccessful()) { return new Result(giir.errors()); }

        / * 
         * Resolve grammar extensions and extensions of nonterminal definitions.
         * /
        ModuleTranslator.Result mrr = ModuleTranslator.translate(grammarIndexs);
        if (!mrr.isSuccessful()) { return new Result(mrr.errors()); }

        if( Debug.isOnFor(3, Debug.Type.SYNTAX) ) {
            Debug.debug( Debug.Type.SYNTAX, 3, GrammarEnv.getDump() );
            
            for (Module m: mrr.modules()) {
                Debug.debug( Debug.Type.SYNTAX, 3, m );
            }
        }

        / *
         * Translate each grammar to a corresponding Rats! module
         * /
        GrammarTranslator.Result gtr = GrammarTranslator.translate(mrr.modules());
        if (!gtr.isSuccessful()) { return new Result(gtr.errors()); }

        / *
         * For each changed module write it to a file and run Rats! to 
         * generate a temporary parser.
         * /
        RatsParserGenerator.Result rpgr = RatsParserGenerator.generateParser(gtr.modules());
        if (!rpgr.isSuccessful()) { return new Result(rpgr.errors()); }

        / *
         * Return the temporary parser
         * /
        return new Result(rpgr.parserClass());
    }
    */

}
