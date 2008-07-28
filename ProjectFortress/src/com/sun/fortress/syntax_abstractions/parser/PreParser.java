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

package com.sun.fortress.syntax_abstractions.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import xtc.parser.ParseError;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Methods to parse a file to a collection of API ASTs which define
 * the syntax used in the file.
 */
public class PreParser {

	   public static class Result extends StaticPhaseResult {
	        private Collection<GrammarIndex> grammars;

	        public Result(Collection<GrammarIndex> grammars) {
	        	this.grammars = grammars;
	        }

	        public Result(StaticError error) {
	            super(IterUtil.singleton(error));
	            this.grammars = new LinkedList<GrammarIndex>();
	        }

	        public Result(Iterable<? extends StaticError> errors) {
	            super(errors);
	            grammars = new LinkedList<GrammarIndex>();
	        }

	        public Result(Result r1, Result r2) {
	            super(r1, r2);
	            this.grammars = new LinkedList<GrammarIndex>();
	            grammars.addAll(r1.grammars);
	            grammars.addAll(r2.grammars);
	        }

	        public Collection<GrammarIndex> getGrammars() {
	        	return this.grammars;
	        }
	    }

           private static List<APIName> removeExecutableApi(List<APIName> all){
               APIName executable = NodeFactory.makeAPIName("Executable");
               List<APIName> fixed = new ArrayList<APIName>();
               for ( APIName name : all ){
                   if ( ! name.equals( executable ) ){
                       fixed.add( name );
                   }
               }
               return fixed;
           }

           public static List<APIName> collectComponentImports(Component comp){
               final List<APIName> all = new ArrayList<APIName>();

               comp.accept( new NodeDepthFirstVisitor_void(){
                   @Override
                   public void forImportedNamesDoFirst(ImportedNames that) {
                       Debug.debug( Debug.Type.SYNTAX, 2, "Add import api ", that.getApi() );
                       all.add( that.getApi() );
                   }

                   @Override
                   public void forExport(Export that){
                       Debug.debug( Debug.Type.SYNTAX, 2, "Add export api ", that.getApis() );
                       all.addAll( that.getApis() );
                   }

                   @Override
                   public void forImportApi(ImportApi that){
                       for ( AliasedAPIName api : that.getApis() ){
                           Debug.debug( Debug.Type.SYNTAX, 2, "Add aliased api ", api.getApi() );
                           all.add( api.getApi() );
                       }
                   }
               });

               /*
               for ( Import i : comp.getImports() ){
                   ImportedNames names = (ImportedNames) i;
                   all.add( names.getApi() );
               }
               for ( Export export : comp.getExports() ){
                   all.addAll( export.getApis() );
               }
               */

               return removeExecutableApi(all);
           }

           public static List<APIName> collectApiImports(Api api){
               List<APIName> all = new ArrayList<APIName>();
               for ( Import i : api.getImports() ){
                   if (i instanceof ImportedNames) {
                       ImportedNames names = (ImportedNames) i;
                       all.add( names.getApi() );
                   } else { // i instanceof ImportApi
                       ImportApi apis = (ImportApi) i;
                       for ( AliasedAPIName a : apis.getApis() ) {
                           all.add( a.getApi() );
                       }
                   }
               }

               return removeExecutableApi(all);
           }

           /* get a list of imported apis from a component/api */
	public static List<APIName> getImportedApis(APIName name, File f) throws StaticError {
		Parser.Result pr = parseFile(name, f);
		if ( ! pr.isSuccessful() ){
                    for ( StaticError e : pr.errors() ){
                        throw e;
                    }
		}
		List<APIName> all = new ArrayList<APIName>();
                for ( Component comp : pr.components() ){
                    all.addAll( collectComponentImports(comp) );
                }
                for ( Api api : pr.apis() ){
                    all.addAll( collectApiImports(api) );
                }
                return all;
	}

	/** Parses a single file. */
	public static Result parse(APIName api_name, File f, GlobalEnvironment env) {

            Parser.Result pr = parseFile(api_name, f);
            if (!pr.isSuccessful()) { return new Result(pr.errors()); }
            
            // TODO: Check that result only contains at most one component
            
            Collection<GrammarIndex> result = new LinkedList<GrammarIndex>();
            for (Component c: pr.components()) {
                ImportedApiCollector collector = new ImportedApiCollector(env);
                collector.collectApis(c);
                if (collector.importsTopLevelGrammars()) {
                    result.addAll(collector.getGrammars());
                }
		
                if (!result.isEmpty()) {
                    Debug.debug(Debug.Type.SYNTAX, "Component: ", c.getName(), " imports grammars...");
                }
            }
            
            return new Result(result);
	}
    
    private static Parser.Result parseFile(APIName api_name, File f) {
        try {
            return new Parser.Result(Parser.preparseFileConvertExn(api_name, f),
                                     f.lastModified());
        } catch (StaticError se) {
            return new Parser.Result(se);
        }
    }
}
