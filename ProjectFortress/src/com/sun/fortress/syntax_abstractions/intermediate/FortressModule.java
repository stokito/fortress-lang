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

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleName;

import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;

import edu.rice.cs.plt.tuple.Option;

/*
 * This module corresponds to a grammar that is part of the Fortress grammars. 
 */
public class FortressModule extends Module {

	public FortressModule() {}
	
	public FortressModule(String name, Collection<ProductionIndex<? extends NonterminalDecl>> productions) {
		super(name, productions);
		initialize();
	}

	private void initialize() {
		Option<xtc.parser.Module> om = RatsUtil.parseRatsModule(RatsUtil.getFortressSrcDir()+RatsUtil.getModulePath()+name+".rats");
		if (om.isSome()) {
			xtc.parser.Module m = Option.unwrap(om);
			for (ModuleName name: m.parameters.names) {
				this.parameters.add(name);
			}
			
			for (ModuleDependency dep: m.dependencies) {
				this.dependencies.add(dep);
			}
		}
	}

}
