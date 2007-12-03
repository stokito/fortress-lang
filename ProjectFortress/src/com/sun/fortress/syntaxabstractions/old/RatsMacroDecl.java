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

/*
 * Class containing a collection of Rats! AST nodes which corresponds to a 
 * given macro declaration.
 * 
 */


package com.sun.fortress.syntaxabstractions.old;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleName;
import xtc.parser.Sequence;

import com.sun.fortress.syntaxabstractions.rats.util.ModuleEnum;
import com.sun.fortress.syntaxabstractions.rats.util.ProductionEnum;
import com.sun.fortress.syntaxabstractions.rules.Rule;

public class RatsMacroDecl {

	private ModuleEnum module;
	private ProductionEnum production;
	private List<Sequence> sequence;
	private List<ModuleDependency> dependencies;
	private List<ModuleName> parameters;
	private Collection<Rule> rules;


	public RatsMacroDecl(ModuleEnum module, ProductionEnum production) {
		this.module = module;
		this.production = production;
		this.dependencies = new LinkedList<ModuleDependency>();
		this.rules = new LinkedList<Rule>();
//		for (ModuleName moduleName: ModuleInfo.getParameters(module)) {
//			dependencies.add(ModuleInfo.getModuleImport(moduleName.name));
//		}
	}
	
	public RatsMacroDecl(ModuleEnum module, ProductionEnum production,
			List<Sequence> sequence) {
		this(module, production);
		this.sequence = sequence;
	}

	public ModuleEnum getModule() {
		return this.module;
	}
	
	public void setModule(ModuleEnum moduleEnum) {
		this.module = moduleEnum;
	}

	public ProductionEnum getProduction() {
		return this.production;
	}
	
	public void setProduction(ProductionEnum productionEnum) {
		this.production = productionEnum;
	}

	public List<Sequence> getSequence() {
		return this.sequence;
	}
	
	public void setSequence(List<Sequence> sequence) {
		this.sequence = sequence;
	}

	public void addDependencies(List<ModuleDependency> dependencies) {
		this.dependencies = dependencies;		
	}
	
	public void addParameters(List<ModuleName> parameters) {
		this.parameters = parameters;		
	}

	public List<ModuleDependency> getDependencies() {
		return dependencies;
	}

	public List<ModuleName> getParameters() {
		return parameters;
	}

	public void addRule(Rule rule) {
		this.rules.add(rule);
	}
	
	public Collection<Rule> getRules() {
		return this.rules;
	}

	public void addRules(Collection<Rule> rules) {
		this.rules.addAll(rules);		
	}

}
