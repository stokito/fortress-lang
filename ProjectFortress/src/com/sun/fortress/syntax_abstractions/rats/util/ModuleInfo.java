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

/*
 * Class holding information about modules, like their parameters and attributes.
 * 
 */

package com.sun.fortress.syntax_abstractions.rats.util;

import java.util.HashSet;
import java.util.Set;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;

import edu.rice.cs.plt.tuple.Option;

public class ModuleInfo {
	
	public static final String MODULE_NAME_PREFIX = "com.sun.fortress.parser.";
	
	/**
	 * Given the name of a grammar decl member. Return whether it is a 
	 * defined in a fortress core module
	 * @param memberName
	 * @return
	 */
	public static boolean isFortressModule(QualifiedIdName memberName) {
		if (memberName.getApi().isNone()) {
			return false;
		}
		APIName api = Option.unwrap(memberName.getApi());
		if (!api.getIds().get(0).getText().equals("FortressSyntax")) {
			return false;
		}

		Set<String> fortressModules = getFortressModuleNames();
		return fortressModules.contains(api.getIds().get(1).getText());
	}

	public static Set<String> getFortressModuleNames() {
		Set<String> fortressModules = new HashSet<String>();
		fortressModules.add("AbsField");
		fortressModules.add("Compilation");
		fortressModules.add("Declaration");
		fortressModules.add("DelimitedExpr");
		fortressModules.add("Expression");
		fortressModules.add("Field");
		fortressModules.add("Fortress");
		fortressModules.add("Function");
		fortressModules.add("Header");
		fortressModules.add("Identifier");
		fortressModules.add("Keyword");
		fortressModules.add("Literal");
		fortressModules.add("LocalDecl");
		fortressModules.add("Method");
		fortressModules.add("MethodParam");
		fortressModules.add("NoNewlineExpr");
		fortressModules.add("NoSpaceExpr");
		fortressModules.add("OtherDecl");
		fortressModules.add("Parameter");		
		fortressModules.add("Spacing");
		fortressModules.add("Symbol");
		fortressModules.add("Syntax");
		fortressModules.add("TraitObject");
		fortressModules.add("Type");
		fortressModules.add("Unicode");
		fortressModules.add("Variable");
		return fortressModules;
	}
	
}
