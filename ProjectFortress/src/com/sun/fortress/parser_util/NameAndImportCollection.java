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
 * Class containing names and imports for a given component.
 * Used in the Fortress com.sun.fortress.parser.preparser 
 * @see{com.sun.fortress.parser.NameAndImportCollector}.
 */

package com.sun.fortress.parser_util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Import;

public class NameAndImportCollection {

	private APIName componentName;
	private List<Import> imports;
	
	/**
	 * Create a new instance where component name is 
	 * initialized to null, and imports are 
	 * initialized to the empty list.  
	 */
	public NameAndImportCollection() {
		this.componentName = null;
		this.imports = new LinkedList<Import>();
	}

	/**
	 * Create a new instance where component name is 
	 * initialized to name, and imports are imports. 
	 * @param name
	 * @param imports
	 */
	public NameAndImportCollection(APIName name, List<Import> imports) {
		this.componentName = name;
		this.imports = imports;
	}

	public APIName getComponentName() {
		return componentName;
	}

	public void setComponentName(APIName componentName) {
		this.componentName = componentName;
	}

	public List<Import> getImports() {
		return imports;
	}

	public void setImports(List<Import> imports) {
		this.imports = imports;
	}
	
	public String toString() {
		String s = "Component "+this.getComponentName() + " has the following imports\n";
		for (Import i: this.imports) {
			s += "  a)"+i.toString()+"\n";
		}		
		return s;
	}

}
