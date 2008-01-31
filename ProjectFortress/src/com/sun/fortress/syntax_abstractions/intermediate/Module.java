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

package com.sun.fortress.syntax_abstractions.intermediate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleName;

import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.GrammarIndex;

import edu.rice.cs.plt.tuple.Option;

public abstract class Module {

	protected String name;
	private boolean isTopLevel;
	
	protected Module modify;
	protected Collection<Module> imports;
	private Map<String, Set<SyntaxSymbol>> tokenMap;

	protected List<ModuleName> parameters;
	protected List<ModuleDependency> dependencies;
	
	protected ProductionEnv productionEnv;
	protected Collection<ProductionIndex<? extends NonterminalDecl>> declaredProductions;

	protected Collection<? extends Module> extendedModules; 
	
	public Module() {
		this.imports = new LinkedList<Module>();
		this.tokenMap = new HashMap<String, Set<SyntaxSymbol>>();
		
		this.declaredProductions = new LinkedList<ProductionIndex<? extends NonterminalDecl>>();
		this.extendedModules = new java.util.HashSet<Module>();
		this.parameters = new LinkedList<ModuleName>();
		this.dependencies = new LinkedList<ModuleDependency>();
	}

	public Module(String name, 
			      Collection<ProductionIndex<? extends NonterminalDecl>> declaredProductions) {
		this();
		this.name = name;
		this.declaredProductions.addAll(declaredProductions);
	}

	public void isTopLevel(boolean b) {
		this.isTopLevel = b;		
	}
	
	public boolean isTopLevel() {
		return this.isTopLevel;
	}

	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name.substring(0, 1).toUpperCase()+name.substring(1);
	}

	public Collection<Module> getImports() {
		return imports;
	}
	
	public void setImports(Collection<Module> imports) {
		this.imports = imports;
	}
	
	public Module getModify() {
		return modify;
	}
	
	public void setModify(Module modify) {
		this.modify = modify;
	}

	public List<ModuleName> getParameters() {
		return this.parameters;
	}

	public Collection<? extends ModuleDependency> getDependencies() {
		return this.dependencies;
	}
	
	public Collection<ProductionIndex<? extends NonterminalDecl>> getDeclaredProductions() {
		return this.declaredProductions;
	}
	
	/**
	 * Return true if the given production name are among the declared or inherited productions
	 * @param p
	 * @param name
	 * @return
	 */
	public boolean containsProduction(ProductionIndex<? extends NonterminalDecl> p, String name) {
		for (ProductionIndex<? extends NonterminalDecl> production: this.declaredProductions) {
			if ((production.getName().toString().equals(name)) &&
				(!p.equals(production))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add a set of declared production to this module
	 * @param productions
	 */
	public void addProductions(Collection<? extends ProductionIndex<? extends NonterminalDecl>> productions) {
		for (ProductionIndex<? extends NonterminalDecl> p: productions) {
			this.addProduction(p.getName().toString(), p);
		}
	}
	
	/**
	 * Add a declared production to this module 
	 */
	private void addProduction(String name, ProductionIndex<? extends NonterminalDecl> p) {
			this.declaredProductions.add(p);
	}

	/** 
	 * Returns a set of all token symbols from all declared and inherited productions
	 * @return
	 */
	public Set<String> getKeywords() {
		final Set<String> keywords = new HashSet<String>();
		for (ProductionIndex<? extends NonterminalDecl> p: this.declaredProductions) {
			if (p.ast().isSome()) {
				Option.unwrap(p.ast()).accept(new NodeDepthFirstVisitor_void(){
					@Override
					public void forKeywordSymbol(KeywordSymbol that) {
						keywords.add(that.getToken());
					}
				});
				}
			}
		return keywords;
	}
	
	/** 
	 * Returns a set of all token symbols from all declared and inherited productions
	 * @return
	 */
	public Set<String> getTokens() {
		final Set<String> tokens = new HashSet<String>();
		for (ProductionIndex<? extends NonterminalDecl> p: this.declaredProductions) {
			if (p.ast().isSome()) {
				Option.unwrap(p.ast()).accept(new NodeDepthFirstVisitor_void(){
					@Override
					public void forTokenSymbol(TokenSymbol that) {
						tokens.add(that.getToken());
					}
				});
				}
			}
		return tokens;
	}

	public String toString() {
		String indentation = "  ";
		String indent = indentation;
		String tmpIndent = "";
		String s = "*** "+this.getName()+" ***\n";
		
		s+= indent+"* Imports\n";
		tmpIndent = indent;
		indent += indentation;
		Iterator<Module> mit = this.imports.iterator();
		while (mit.hasNext()) {
			s+= indent+"- "+mit.next().getName()+"\n";
		}
		indent = tmpIndent;
		
		s+= indent+"* modify\n";
		tmpIndent = indent;
		indent += indentation;
	    if (this.getModify() != null) {
	    	s+= indent+" "+this.getModify().getName()+"\n";
	    }
	    indent = tmpIndent;
	    
		s+= indent+"* declared productions\n";
		tmpIndent = indent;
		indent += indentation;
		Iterator<ProductionIndex<? extends NonterminalDecl>> pit = this.declaredProductions.iterator();
		while (pit.hasNext()) {
			s+= indent+"- "+pit.next().getName()+"\n";
		}
		indent = tmpIndent;
		
		s+= indent+"* extended modules\n";
		indent += indentation;
		Iterator<? extends Module> amit = this.extendedModules.iterator();
		while (amit.hasNext()) {
			s+= indent+"- "+amit.next().getName()+"\n";
		}
		indent = tmpIndent;
		return s;
	}

}
