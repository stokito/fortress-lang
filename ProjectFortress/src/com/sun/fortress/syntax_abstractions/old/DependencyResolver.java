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
 * Class extracting which other Rats! modules from the Fortress grammar
 * a given macro declaration depends on.
 * 
 */

package com.sun.fortress.syntax_abstractions.old;

import java.util.LinkedList;
import java.util.List;

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleName;

import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo.*;

public class DependencyResolver {

	private List<ModuleDependency> dependencies;
	private List<ModuleName> parameters;

//	public void resolveDependencies(SyntaxHeaderFront syntaxHeaderFront) {
//		dependencies = new LinkedList<ModuleDependency>();
//		parameters = new LinkedList<ModuleName>();
//		boolean addKeyword = false;
//		for (Pair<IdName, Option<QualifiedName>> p: syntaxHeaderFront.getParameters()) {
//			if (p.getB().isNone() ||
//					(p.getB().isSome() && Option.unwrap(p.getB()).getName().stringName().equals("Keyword"))) {
//				addKeyword  = true;
//			}	
//		}
//		if (addKeyword) {
//			dependencies.add(KeywordImport);
//			parameters.add(Keyword);
//		}
//	}

	/**
	 * Returns the list of additional dependencies
	 * @param syntaxHeaderFront
	 * @return
	 */
	public List<ModuleDependency> getDependencies() {	
		return dependencies;
	}

	/**
	 * 
	 * @param syntaxHeaderFront
	 * @return
	 */
	public List<ModuleName> getParameters() {	
		return parameters;
	}
}
