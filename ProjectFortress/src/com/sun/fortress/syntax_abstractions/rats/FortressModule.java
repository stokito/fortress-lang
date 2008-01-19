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
 * 
 */

package com.sun.fortress.syntax_abstractions.rats;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;

import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleList;
import xtc.parser.ModuleName;
import xtc.tree.Attribute;

public class FortressModule {
	
	/**
	 * A set which preserves the order of the elements
	 * @param <T>
	 */
	public class LinkedSet<T> {

		private LinkedList<T> elms;

		public LinkedSet() {
			this.elms = new LinkedList<T>();
		}
		
		public void addAll(List<T> elms) {
			for (T elm: elms) {
				this.add(elm);
			}			
		}

		public void add(int i, T elm) {
			if (!this.elms.contains(elm)) {
				this.elms.add(i, elm);
			}			
		}

		public void add(T elm) {
			if (!this.elms.contains(elm)) {
				this.elms.add(elm);
			}			
		}

		public List<T> asList() {
			return this.elms;
		}

		public String toString() {
			return this.elms.toString();
		}
	}

	private Map<String, Module> moduleNamesToExt;
	private String freshName;

	public FortressModule(String freshName) {
		super();
		this.freshName = freshName;
	}

	public void addModuleNames(String tempDir, 
							   Collection<Module> modules,
							   Collection<Module> keywordModules,
							   Map<String, String> modulesReplacingFortressModules) {
		String filename = tempDir+RatsUtil.getModulePath()+"Fortress.rats";
		Module fortressModule = RatsUtil.getRatsModule(filename);

		moduleNamesToExt = new HashMap<String, Module>();
		for (Module module: modules) {
			String qualifiedName = module.name.name;
			String moduleName = qualifiedName.substring(qualifiedName.lastIndexOf('.')+1);
			if (modulesReplacingFortressModules.containsKey(moduleName)) {
				String modifiedModule = modulesReplacingFortressModules.get(moduleName);
				modifiedModule = qualifiedName.substring(0, qualifiedName.lastIndexOf('.')+1)+ modifiedModule;
				moduleNamesToExt.put(modifiedModule, module);
			}
		}
 
		/* 
		 * Add new arguments and dependencies
		 */
		Collection<ModuleDependency> newDependencies = new LinkedList<ModuleDependency>();
		for (ModuleDependency moduleDependency: fortressModule.dependencies) {
		
			if (moduleDependency instanceof ModuleInstantiation) {
				ModuleInstantiation moduleInstantiation = (ModuleInstantiation) moduleDependency;
				
				LinkedSet<ModuleName> newModuleNames = new LinkedSet<ModuleName>();
				newModuleNames.addAll(moduleInstantiation.arguments.names);

				if (moduleNamesToExt.containsKey(moduleInstantiation.module.name)) {
					String originalModuleName = moduleInstantiation.module.name;
					ModuleName extendedName = new ModuleName(originalModuleName.substring(originalModuleName.lastIndexOf('.')+1)+ModuleInfo.MODULE_NAME_EXTENSION);
					
					List<ModuleName> ls = new LinkedList<ModuleName>();
					ls.addAll(moduleInstantiation.arguments.names);
					newDependencies.add(new ModuleInstantiation(moduleInstantiation.module, new ModuleList(ls), extendedName));
					
					// Add additional arguments
					Module otherModule = moduleNamesToExt.get(originalModuleName);
//					newModuleNames.add(0, extendedName);
					newModuleNames.addAll(otherModule.parameters.names);
//					int numberOfOriginalArguments = moduleInstantiation.arguments.size();
//					int numberOfNewArguments = otherModule.parameters.size()-1; // We have already handled the extended one
//					if (numberOfOriginalArguments  < numberOfNewArguments) {
//						for (int inx=numberOfOriginalArguments; inx < numberOfNewArguments; inx++) {
//							System.err.println(originalModuleName);
//							String name = otherModule.parameters.names.get(inx).name;
//							newModuleNames.add(new ModuleName(name));	
//						}
//					}
					
					Module module = moduleNamesToExt.get(moduleInstantiation.module.name);
					// Add any introduced dependency on Keywords.rats
					if (keywordModules.contains(module)) {
						newModuleNames.add(new ModuleName("Keyword"));
					}
					
					// Rename the module
					moduleInstantiation.module = new ModuleName(module.name.name);
					
				}
				moduleInstantiation.arguments = new ModuleList(newModuleNames.asList());
			}
		}
		
		fortressModule.dependencies.addAll(newDependencies);	
		
		/*
		 * Rename the name of the generated java file
		 */
		Attribute attr = null;
		for (Attribute attribute: fortressModule.attributes) {
			if (attribute.getName().equals("parser")) {
				attr = attribute;
			}
		}
		if (attr != null) {
			fortressModule.attributes.remove(attr);
			fortressModule.attributes.add(new Attribute("parser", "com.sun.fortress.parser."+freshName));
		}
		
		fortressModule.name = new ModuleName(this.freshName);
		
		RatsUtil.writeRatsModule(fortressModule, tempDir);
	}

}
