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
 * Class holding information about modules, like their parameters and attributes.
 * 
 */

package com.sun.fortress.macro;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleImport;
import xtc.parser.ModuleList;
import xtc.parser.ModuleModification;
import xtc.parser.ModuleName;
import xtc.parser.Sequence;
import xtc.tree.Attribute;

public class ModuleInfo {

	private static final String LITERAL_EXPR = "Literal";
	private static final String DELIMITED_EXPR = "DelimitedExpr";
	private static final String NO_SPACE_EXPR = "NoSpaceExpr";
	private static final String SYMBOL = "Symbol";
	private static final String SPACING = "Spacing";
	private static final String KEYWORD = "Keyword";
	private final static ModuleName Literal = new ModuleName(LITERAL_EXPR);
	private final static ModuleName DelimitedExpr = new ModuleName(DELIMITED_EXPR);
	private final static ModuleName NoSpaceExpr = new ModuleName(NO_SPACE_EXPR);
	private final static ModuleName Symbol = new ModuleName(SYMBOL);
	private final static ModuleName Spacing = new ModuleName(SPACING);
	private final static ModuleName Keyword = new ModuleName(KEYWORD);
	private static final ModuleDependency DelimitedExprAttr = new ModuleImport(DelimitedExpr);;
	private static final ModuleDependency NoSpaceExprAttr = new ModuleImport(NoSpaceExpr);
	private static final ModuleDependency SymbolAttr = new ModuleImport(Symbol);
	private static final ModuleDependency SpacingAttr = new ModuleImport(Spacing);
	private static final ModuleDependency KeywordAttr = new ModuleImport(Keyword);
	
	public static List<ModuleName> getParameters(ModuleEnum e) {
		List<ModuleName> result = new LinkedList<ModuleName>();
		switch (e) {
		case LITERAL:
			result.add(DelimitedExpr);
			result.add(NoSpaceExpr);
			result.add(Symbol);
			result.add(Spacing);
			result.add(Keyword);
			return result;
			// TODO
		default:
			return result;
			
		}
	}

	public static List<ModuleDependency> getModuleModification(ModuleEnum e) {
		List<ModuleDependency> result = new LinkedList<ModuleDependency>();
		switch (e) {
		case LITERAL:
			result.add(DelimitedExprAttr);
			result.add(NoSpaceExprAttr);
			result.add(SymbolAttr);
			result.add(SpacingAttr);
			result.add(KeywordAttr);
			result.add(new ModuleModification(ModuleInfo.Literal, 
										      new ModuleList(ModuleInfo.getParameters(e)), 
										      null));
			return result;
			// TODO
		default:
			throw new RuntimeException("NYI: "+e);
			
		}
	}
	
	public static String getASTName(ProductionEnum e) {
		switch (e) {
		case LITERAL:
			return "Expr";
			// TODO
		default:
			return "NYI";
			
		}
	}

}
