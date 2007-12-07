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

package com.sun.fortress.syntax_abstractions.intermediate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.GrammarIndex;
import com.sun.fortress.syntax_abstractions.phases.ModuleResolver;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;

import edu.rice.cs.plt.tuple.Option;

public abstract class Module {

	Collection<ProductionDef> productions;
	Collection<Module> imports;
	Module modify;
	private String name;
	// private Map<String, ProductionDef> names;
	// private Map<String, ProductionDef> productionsExtendsMap;
	private boolean isTopLevel;
	private Collection<Module> extendedModules;
	private Map<QualifiedName, Set<TokenSymbol>> tokenMap;
	
	public Module() {
		super();
		this.productions = new LinkedList<ProductionDef>();
		this.imports = new LinkedList<Module>();
		//this.names = new HashMap<String, ProductionDef>();
		//this.productionsExtendsMap = new HashMap<String, ProductionDef>();
		this.extendedModules = new LinkedList<Module>();
		this.tokenMap = new HashMap<QualifiedName, Set<TokenSymbol>>();
	}

	public Collection<ProductionDef> getDefinedProductions() {
		return this.productions;
	}
	
	public Collection<ProductionDef> getProductions() {
		List<ProductionDef> productions = new LinkedList<ProductionDef>();
		for (ProductionDef p: this.productions) {
			// Test for privacy of production here
			productions.add(p);
		}

		for (Module m : this.getExtendedModules()) {
			productions.addAll(m.getProductions());
		}
		return productions;
	}

	public boolean containsProduction(ProductionDef p, String name) {
		// System.err.println("***");
		for (ProductionDef production: this.getProductions()) {
			// System.err.println("Matching: "+production.getName().toString()+" - "+name);
			if ((production.getName().toString().equals(name)) &&
				(!p.equals(production))) {
				return true;
			}
		}
		return false;
	}
	
	public void setProductions(Collection<ProductionDef> productions) {
		this.productions = productions;
	}
	
	public void addProductions(Collection<? extends ProductionDef> productions) {
		for (ProductionDef p: productions) {
			this.addProduction(p.getName().toString(), p);
		}
	}
	
	public void addProductions(GrammarIndex grammar, Collection<? extends ProductionDef> productions) {
		for (ProductionDef p: productions) {
			this.addProduction(p.getName().toString(), p);
		}		
	}
	
	private void addProduction(String name, ProductionDef p) {
//		if (p.getExtends().isSome() &&
//			this.productionsExtendsMap.keySet().contains(Option.unwrap(p.getExtends()).getName())) {
//			ProductionDef pd = this.productionsExtendsMap.get(Option.unwrap(p.getExtends()).getName());
//			List<SyntaxDef> syntaxDefs = p.getSyntaxDefs();
//			syntaxDefs.addAll(pd.getSyntaxDefs());
//			pd.getSyntaxDefs().clear();
//			pd.getSyntaxDefs().addAll(syntaxDefs);
//		}
//		else {
			this.productions.add(p);
//			this.names.put(name, p);
//		}
	}

	/**
	 * Returns a map from names of fortress core modules to 
	 * sets of productions which extends the modules
	 * TODO: If more than one production extends the same core production, then what???
	 * TODO: It is a hack to use the core modules to transport imports
	 */
	public Map<Module, Set<ProductionDef>> getExtendedCoreModules() {
		Map<Module, Set<ProductionDef>> extendedCoreModules = 
			                            new HashMap<Module, Set<ProductionDef>>();
		List<ProductionDef> lsp = new LinkedList<ProductionDef>(); 
		for (ProductionDef production: this.productions) {
			String name = "";
			if (production.getExtends().isSome() &&
				ModuleInfo.isCoreProduction((name = Option.unwrap(production.getExtends()).getName().toString()))) {
				Module module = ModuleInfo.getCoreModule(name);
				Set<ProductionDef> productions = new HashSet<ProductionDef>();
				if (extendedCoreModules.containsKey(module)) {
					productions = extendedCoreModules.get(module);
				}
				// Rename production if it extends a core production
				productions.add(ModuleResolver.renameProduction(production, name));
				module.setImports(this.getExtendedModules());
				extendedCoreModules.put(module, productions);
			}
			else {
				lsp.add(production);
			}
		}
		this.setProductions(lsp);
		return extendedCoreModules;
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

	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name.substring(0, 1).toUpperCase()+name.substring(1);
	}

	public void isTopLevel(boolean b) {
		this.isTopLevel = b;		
	}
	
	public boolean isTopLevel() {
		return this.isTopLevel;
	}

	public void addExtendedModule(Module module) {
		this.extendedModules.add(module);
	}
	
	public Collection<Module> getExtendedModules() {
		return this.extendedModules;
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
	    
		s+= indent+"* Locally defined productions\n";
		tmpIndent = indent;
		indent += indentation;
		Iterator<ProductionDef> pit = this.productions.iterator();
		while (pit.hasNext()) {
			s+= indent+"- "+pit.next().getName()+"\n";
		}
		indent = tmpIndent;
		
		s+= indent+"* Productions\n";
		indent += indentation;
		pit = this.getProductions().iterator();
		while (pit.hasNext()) {
			s+= indent+"- "+pit.next().getName()+"\n";
		}
		indent = tmpIndent;
		return s;
	}

	public void addTokens(QualifiedName name, Set<TokenSymbol> tokens) {
		this.tokenMap.put(name, tokens);		
	}

	public Collection<Set<TokenSymbol>> getTokens() {
		return this.tokenMap.values();
	}

	public void setExtendedModules(Collection<Module> extendedModules) {
		this.extendedModules = extendedModules;		
	}
}
