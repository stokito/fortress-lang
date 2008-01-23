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
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.GrammarIndex;
import com.sun.fortress.syntax_abstractions.phases.ModuleTranslator;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;

import edu.rice.cs.plt.tuple.Option;

public abstract class Module {

	protected String name;
	private boolean isTopLevel;
	
	protected Collection<Module> imports;
	protected Module modify;
	protected Collection<Module> extendedModules;
	private Map<String, Set<SyntaxSymbol>> tokenMap;
	
	protected Collection<ProductionIndex> productions;
	protected ProductionEnv productionEnv;
	protected List<ModuleName> parameters;
	protected List<ModuleDependency> dependencies;
	
	public Module() {
		this.imports = new LinkedList<Module>();
		this.extendedModules = new LinkedList<Module>();
		this.tokenMap = new HashMap<String, Set<SyntaxSymbol>>();
		
		this.productions = new LinkedList<ProductionIndex>();
		this.parameters = new LinkedList<ModuleName>();
		this.dependencies = new LinkedList<ModuleDependency>();
	}

	public Module(String name, Collection<ProductionIndex> productions) {
		this();
		this.name = name;
		this.productions.addAll(productions);
	}

	public Collection<ProductionIndex> getDefinedProductions() {
		return this.productions;
	}
	
	public Collection<ProductionIndex> getProductions() {
		List<ProductionIndex> productions = new LinkedList<ProductionIndex>();
		for (ProductionIndex p: this.productions) {
			// Test for privacy of production here
			productions.add(p);
		}

		for (Module m : this.getExtendedModules()) {
			productions.addAll(m.getProductions());
		}
		return productions;
	}

	public boolean containsProduction(ProductionIndex p, String name) {
		// System.err.println("***");
		for (ProductionIndex production: this.getProductions()) {
			// System.err.println("Matching: "+production.getName().toString()+" - "+name);
			if ((production.getName().toString().equals(name)) &&
				(!p.equals(production))) {
				return true;
			}
		}
		return false;
	}
	
	public void setProductions(Collection<ProductionIndex> productions) {
		this.productions = productions;
	}
	
	public void addProductions(Collection<? extends ProductionIndex> productions) {
		for (ProductionIndex p: productions) {
			this.addProduction(p.getName().toString(), p);
		}
	}
	
	public void addProductions(GrammarIndex grammar, Collection<? extends ProductionIndex> productions) {
		for (ProductionIndex p: productions) {
			this.addProduction(p.getName().toString(), p);
		}		
	}
	
	private void addProduction(String name, ProductionIndex p) {
//		if (p.getExtends().isSome() &&
//			this.productionsExtendsMap.keySet().contains(Option.unwrap(p.getExtends()).getName())) {
//			NonterminalDef pd = this.productionsExtendsMap.get(Option.unwrap(p.getExtends()).getName());
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
	public Map<Module, Set<ProductionIndex>> getExtendedCoreModules() {
		Map<Module, Set<ProductionIndex>> extendedCoreModules = 
			                            new HashMap<Module, Set<ProductionIndex>>();
		List<ProductionIndex> lsp = new LinkedList<ProductionIndex>(); 
		for (ProductionIndex production: this.productions) {
			String name = "";
			// TODO: fix extends
//			if (production.getExtends().isSome() &&
//				ModuleInfo.isCoreProduction((name = Option.unwrap(production.getExtends()).getName().toString()))) {
//				Module module = ModuleInfo.getCoreModule(name);
//				Set<ProductionIndex> productions = new HashSet<ProductionIndex>();
//				if (extendedCoreModules.containsKey(module)) {
//					productions = extendedCoreModules.get(module);
//				}
//				// Rename production if it extends a core production
//				//productions.add(ModuleTranslator.renameProduction(production, name));
//				module.setImports(this.getExtendedModules());
//				extendedCoreModules.put(module, productions);
//			}
//			else {
//				lsp.add(production);
//			}
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
		Iterator<ProductionIndex> pit = this.productions.iterator();
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

	public Set<String> getKeywords() {
		final Set<String> keywords = new HashSet<String>();
		for (ProductionIndex p: this.getProductions()) {
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
	
	public Set<String> getTokens() {
		final Set<String> tokens = new HashSet<String>();
		for (ProductionIndex p: this.getProductions()) {
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

	public void setExtendedModules(Collection<Module> extendedModules) {
		this.extendedModules = extendedModules;		
	}

	public void setToplevel(boolean b) {
		this.isTopLevel = b;		
	}

	public List<ModuleName> getParameters() {
		return this.parameters;
	}

	public Collection<? extends ModuleDependency> getDependencies() {
		return this.dependencies;
	}
}
