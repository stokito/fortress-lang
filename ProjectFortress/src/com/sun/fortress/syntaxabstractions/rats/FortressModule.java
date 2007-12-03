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

package com.sun.fortress.syntaxabstractions.rats;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleList;
import xtc.parser.ModuleName;
import xtc.tree.Attribute;

public class FortressModule {
	
	private Map<String, Module> moduleNamesToExt;
	private String freshName;

	public FortressModule(String freshName) {
		super();
		this.freshName = freshName;
	}

	public void addModuleNames(Collection<Module> modules, String tempDir) {
		String filename = tempDir+RatsUtil.getModulePath()+"Fortress.rats";
		Module fortressModule = RatsUtil.getRatsModule(filename);

		moduleNamesToExt = new HashMap<String, Module>();
		for (Module module: modules) {
			String moduleName = module.name.name;
			if (moduleName.endsWith("Ext")) {
				String originalName = moduleName.substring(0, moduleName.length()-3);
				moduleNamesToExt.put(originalName, module);
			}
		}
		
		/* Rename all module name instantiations which have been extended and
		 * add all new dependencies
		 */
		for (ModuleDependency moduleDependency: fortressModule.dependencies) {
		
			if (moduleDependency instanceof ModuleInstantiation) {
				ModuleInstantiation moduleInstantiation =  (ModuleInstantiation) moduleDependency;

				// Rename all the arguments to the module instantiation
				List<ModuleName> newModuleNames = new LinkedList<ModuleName>();
				newModuleNames.addAll(moduleInstantiation.arguments.names);
				
				if (moduleNamesToExt.keySet().contains(moduleInstantiation.module.name)) {
					// Add additional arguments
					String originalModuleName = moduleInstantiation.module.name;
					int numberOfOriginalArguments = moduleInstantiation.arguments.size();
					int numberOfNewArguments = moduleNamesToExt.get(originalModuleName).parameters.size();
					if (numberOfOriginalArguments  < numberOfNewArguments) {
						for (int inx=numberOfOriginalArguments; inx < numberOfNewArguments; inx++) {
							String name = moduleNamesToExt.get(originalModuleName).parameters.names.get(inx).name;
							newModuleNames.add(new ModuleName(name));	
						}
					}
					
					// Rename the module
					moduleInstantiation.module = getModuleName(moduleNamesToExt.get(moduleInstantiation.module.name));
				}
				moduleInstantiation.arguments = new ModuleList(newModuleNames);
			}
		}
		
		List<ModuleDependency> ls = fortressModule.dependencies;
		ModuleName name = new ModuleName("com.sun.fortress.parser.A");
		ModuleName name2 = new ModuleName("A");
		List<ModuleName> dependencies = new LinkedList<ModuleName>();
		dependencies.add(new ModuleName("Keyword"));
		dependencies.add(new ModuleName("Spacing"));
		//ls.add(new ModuleInstantiation(name , new ModuleList(dependencies), name2));
		name = new ModuleName("com.sun.fortress.parser.B");
		name2 = new ModuleName("B");
		//ls.add(new ModuleInstantiation(name , new ModuleList(dependencies), name2));
		
		
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

	private ModuleName getModuleName(Module module) {
		return new ModuleName(module.name.name);
	}
	

}
