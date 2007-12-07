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

package com.sun.fortress.syntax_abstractions.rats.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.syntax_abstractions.intermediate.CoreModule;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.old.RatsMacroDecl;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;


import xtc.parser.ModuleDependency;
import xtc.parser.ModuleImport;
import xtc.parser.ModuleList;
import xtc.parser.ModuleModification;
import xtc.parser.ModuleName;
import xtc.parser.Sequence;
import xtc.tree.Attribute;

public class ModuleInfo {

	public static final String MODULE_NAME_EXTENSION = "Ext";
	public static final String MODULE_NAME_PREFIX = "com.sun.fortress.parser.";
	
	private static final String LITERAL_EXPR = "Literal";
	private static final String DELIMITED_EXPR = "DelimitedExpr";
	private static final String NO_SPACE_EXPR = "NoSpaceExpr";
	private static final String SYMBOL = "Symbol";
	private static final String SPACING = "Spacing";
	private static final String KEYWORD = "Keyword";
	private static final String IDENTIFIER = "Identifier";
	private static final String LOCALDECL = "LocalDecl";
	private static final String EXPRESSION = "Expression";
	private static final String TYPE = "Type";
	private static final String HEADER = "Header";
	private static final String TRAITOBJECT = "TraitObject";
	public final static ModuleName Literal = new ModuleName(LITERAL_EXPR);
	public final static ModuleName DelimitedExpr = new ModuleName(DELIMITED_EXPR);
	public final static ModuleName NoSpaceExpr = new ModuleName(NO_SPACE_EXPR);
	public final static ModuleName Symbol = new ModuleName(SYMBOL);
	public final static ModuleName Spacing = new ModuleName(SPACING);
	public final static ModuleName Keyword = new ModuleName(KEYWORD);
	public static final ModuleName Identifier = new ModuleName(IDENTIFIER);
	public static final ModuleName LocalDecl = new ModuleName(LOCALDECL);
	public static final ModuleName Expression = new ModuleName(EXPRESSION);
	public static final ModuleName Type = new ModuleName(TYPE);
	public static final ModuleName Header = new ModuleName(HEADER);
	public static final ModuleName TraitObject = new ModuleName(TRAITOBJECT);
	public static final ModuleDependency DelimitedExprImport = new ModuleImport(DelimitedExpr);;
	public static final ModuleDependency NoSpaceExprImport = new ModuleImport(NoSpaceExpr);
	public static final ModuleDependency SymbolImport = new ModuleImport(Symbol);
	public static final ModuleDependency SpacingImport = new ModuleImport(Spacing);
	public static final ModuleDependency KeywordImport = new ModuleImport(Keyword);
	public static final ModuleDependency IdentifierImport = new ModuleImport(Identifier);
	public static final ModuleDependency LiteralImport = new ModuleImport(Literal);
	public static final ModuleDependency LocalDeclImport = new ModuleImport(LocalDecl);
	public static final ModuleDependency ExprImport = new ModuleImport(Expression);
	public static final ModuleDependency TypeImport = new ModuleImport(Type);
	public static final ModuleDependency HeaderImport = new ModuleImport(Header);
	public static final ModuleDependency TraitObjectImport = new ModuleImport(TraitObject);
	
	/**
	 * Returns the module parameter from the Fortress parser.
	 * @param e The module which parameter should be returned.
	 * @return The set of module names.
	 */
	public static List<ModuleName> getParameters(ModuleEnum e) {
		List<ModuleName> result = new LinkedList<ModuleName>();
		switch (e) {
		case LITERAL:
			result.add(DelimitedExpr);result.add(NoSpaceExpr);result.add(Symbol);
			result.add(Spacing);
			return result;
		case DELIMITEDEXPR:
			result.add(TraitObject);result.add(Header);result.add(Type);
			result.add(Expression);result.add(LocalDecl);result.add(Literal);
			result.add(Identifier);result.add(Keyword);result.add(Symbol);
			result.add(Spacing);
			return result;
			// TODO: Add more
		default:
			throw new RuntimeException("NYI: "+e);
		}
	}

	/**
	 * Returns a set of module dependencies for the give module.
	 * @param e the given module.
	 * @return The set of module dependencies.
	 */
	public static List<ModuleDependency> getModuleModification(ModuleEnum e) {
		List<ModuleDependency> result = new LinkedList<ModuleDependency>();
		List<ModuleName> ls = new LinkedList<ModuleName>();
		ls.addAll(ModuleInfo.getParameters(e));
		switch (e) {
		case LITERAL:
			result.add(DelimitedExprImport);result.add(NoSpaceExprImport);
			result.add(SymbolImport);result.add(SpacingImport);
			result.add(new ModuleModification(new ModuleName(ModuleInfo.MODULE_NAME_PREFIX+"Literal"), 
										      new ModuleList(ls), 
										      null));
			return result;
		case DELIMITEDEXPR:
			result.add(TraitObjectImport);result.add(HeaderImport);result.add(TypeImport);
			result.add(ExprImport);result.add(LocalDeclImport);result.add(LiteralImport);
			result.add(IdentifierImport);result.add(KeywordImport);result.add(SymbolImport);
			result.add(SpacingImport);
			result.add(new ModuleModification(new ModuleName(ModuleInfo.MODULE_NAME_PREFIX+"DelimitedExpr"), 
										      new ModuleList(ls), 
										      null));
			return result;
			// TODO
		default:
			throw new RuntimeException("NYI: "+e);
		}
	}
	
	public static String getProductionReturnType(ProductionEnum e) {
		Map<ProductionEnum, String> productionReturnTypeFromProductionEnum = new HashMap<ProductionEnum, String>();  
		productionReturnTypeFromProductionEnum.put(ProductionEnum.LITERAL, "Expr");
		productionReturnTypeFromProductionEnum.put(ProductionEnum.DELIMITEDEXPR, "Expr");
		productionReturnTypeFromProductionEnum.put(ProductionEnum.EXPRESSION, "Expr");
		// TODO: Add more
		String result = productionReturnTypeFromProductionEnum.get(e);
		if (result == null) {
			throw new RuntimeException("NYI: "+e);
		}
		return result;
	}

	public static ProductionEnum getProductionEnum(String name) {
		Map<String, ProductionEnum> productionFromSyntaxParamType = new HashMap<String, ProductionEnum>();  
		productionFromSyntaxParamType.put("Literal",         ProductionEnum.LITERAL);
		productionFromSyntaxParamType.put("DelimitedExpr",   ProductionEnum.DELIMITEDEXPR);
		productionFromSyntaxParamType.put("Expr",            ProductionEnum.EXPRESSION);
		// TODO: Add more
		ProductionEnum result = productionFromSyntaxParamType.get(name); 
		if (result == null) {
			throw new RuntimeException("NYI: "+name);
		}
		return result;
	}

	public static ModuleEnum getModuleFromProduction(ProductionEnum production) {
		Map<ProductionEnum, ModuleEnum> productionFromSyntaxParamType = new HashMap<ProductionEnum, ModuleEnum>();  
		productionFromSyntaxParamType.put(ProductionEnum.LITERAL,         ModuleEnum.LITERAL);
		productionFromSyntaxParamType.put(ProductionEnum.DELIMITEDEXPR,   ModuleEnum.DELIMITEDEXPR);
		productionFromSyntaxParamType.put(ProductionEnum.EXPRESSION,            ModuleEnum.EXPRESSION);
		// TODO: Add more
		ModuleEnum result = productionFromSyntaxParamType.get(production);
		if (result == null) {
			throw new RuntimeException("NYI: "+production);
		}
		return result;
	}

	public static ModuleName getModuleName(ModuleEnum e) {
		Map<ModuleEnum, String> moduleToModuleName = new HashMap<ModuleEnum, String>();
		moduleToModuleName.put(ModuleEnum.FORTRESS, 	 "Fortress");
		moduleToModuleName.put(ModuleEnum.COMPILATION, 	 "Compilation");
		moduleToModuleName.put(ModuleEnum.DECLARATION ,  "Declaration");
		moduleToModuleName.put(ModuleEnum.SYNTAX, 	     "Syntax");
		moduleToModuleName.put(ModuleEnum.TRAITOBJECT, 	 "TraitObject");
		moduleToModuleName.put(ModuleEnum.FUNCTION, 	 "Function");
		moduleToModuleName.put(ModuleEnum.PARAMETER, 	 "Parameter");
		moduleToModuleName.put(ModuleEnum.METHOD, 	     "Method");
		moduleToModuleName.put(ModuleEnum.METHODPARAM, 	 "MethodParam");
		moduleToModuleName.put(ModuleEnum.VARIABLE, 	 "Variable");
		moduleToModuleName.put(ModuleEnum.FIELD, 	     "Field");
		moduleToModuleName.put(ModuleEnum.ABSFIELD, 	 "AbsField");
		moduleToModuleName.put(ModuleEnum.HEADER, 	     "Header");
		moduleToModuleName.put(ModuleEnum.OTHERDECL, 	 "OtherDecl");
		moduleToModuleName.put(ModuleEnum.TYPE, 	     "Type");
		moduleToModuleName.put(ModuleEnum.EXPRESSION, 	 "Expression");
		moduleToModuleName.put(ModuleEnum.DELIMITEDEXPR, "DelimitedExpr");
		moduleToModuleName.put(ModuleEnum.NONEWLINEEXPR, "NoNewlineExpr");
		moduleToModuleName.put(ModuleEnum.NOSPACEEXPR, 	 "NoSpaceExpr");
		moduleToModuleName.put(ModuleEnum.LOCALDECL, 	 "LocalDecl");
		moduleToModuleName.put(ModuleEnum.SYMBOL, 	     "Symbol");
		moduleToModuleName.put(ModuleEnum.SPACING, 	     "Spacing");
		moduleToModuleName.put(ModuleEnum.UNICODE, 	     "Unicode");
		moduleToModuleName.put(ModuleEnum.LITERAL, 	     "Literal");
		moduleToModuleName.put(ModuleEnum.KEYWORD, 	     "Keyword");
		moduleToModuleName.put(ModuleEnum.IDENTIFIER,    "Identifier");

		ModuleName result =  new ModuleName(ModuleInfo.MODULE_NAME_PREFIX+moduleToModuleName.get(e));
		if (result == null) {
			throw new RuntimeException("NYI: "+e);
		}
		return result;
	}
	
	public static boolean isFortressModule(String name) {
		Set<String> fortressModules = new HashSet<String>();
		fortressModules.add("Fortress");
		fortressModules.add("Compilation");
		fortressModules.add("Declaration");
		fortressModules.add("Syntax");
		fortressModules.add("TraitObject");
		fortressModules.add("Function");
		fortressModules.add("Parameter");
		fortressModules.add("Method");
		fortressModules.add("MethodParam");
		fortressModules.add("Variable");
		fortressModules.add("Field");
		fortressModules.add("AbsField");
		fortressModules.add("Header");
		fortressModules.add("OtherDecl");
		fortressModules.add("Type");
		fortressModules.add("Expression");
		fortressModules.add("DelimitedExpr");
		fortressModules.add("NoNewlineExpr");
		fortressModules.add("NoSpaceExpr");
		fortressModules.add("LocalDecl");
		fortressModules.add("Symbol");
		fortressModules.add("Spacing");
		fortressModules.add("Unicode");
		fortressModules.add("Literal");
		fortressModules.add("Keyword");
		fortressModules.add("Identifier");
		return fortressModules.contains(name);
	}
	
	public static boolean isCoreProduction(String s) {
		Map<String, Module> coreProductionAndModules = new HashMap<String, Module>();
		Module literal = new CoreModule();
		literal.setName(s);
		coreProductionAndModules.put("Literal", literal);
		// TODO: create the map on the fly from Rats! files
		return coreProductionAndModules.containsKey(s);
	}
	
	public static Module getCoreModule(String s) {
		Map<String, Module> coreProductionAndModules = new HashMap<String, Module>();
		Module literal = new CoreModule();
		literal.setName(s);
		coreProductionAndModules.put("Literal", literal);
		// TODO: create the map on the fly from Rats! files
		Module result = coreProductionAndModules.get(s);
		if (result == null) {
			throw new RuntimeException("NYI: "+s);
		}
		return result;
	}
	
	public static ModuleName getExtendedModuleName(ModuleEnum e) {
		return new ModuleName(getExtendedModuleName(getModuleName(e).name));
	}
	
	public static String getExtendedModuleName(String name) {
		return name+ModuleInfo.MODULE_NAME_EXTENSION;
	}
	
	public static String getProductionName(ProductionEnum e) {
		Map<ProductionEnum, String> produtionEnumToProductionName = new HashMap<ProductionEnum, String>();
		produtionEnumToProductionName.put(ProductionEnum.LITERAL,  	      "Literal");
		produtionEnumToProductionName.put(ProductionEnum.DELIMITEDEXPR,   "DelimitedExpr");
		produtionEnumToProductionName.put(ProductionEnum.EXPRESSION, 	  "Expr");
		// TODO: Add more
		String result =  produtionEnumToProductionName.get(e);
		if (result == null) {
			throw new RuntimeException("NYI: "+e);
		}
		return result;
	}

	public static String getExtensionPoint(ProductionEnum production) {
		Map<ProductionEnum, String> produtionEnumToExtensionPoint = new HashMap<ProductionEnum, String>();
		produtionEnumToExtensionPoint.put(ProductionEnum.LITERAL,  	      "STRING");
		produtionEnumToExtensionPoint.put(ProductionEnum.DELIMITEDEXPR,   "TRY");
		// TODO: Add more
		String result =  produtionEnumToExtensionPoint.get(production);
		if (result == null) {
			throw new RuntimeException("NYI: "+production);
		}
		return result;
	}
	
//	public static ModuleDependency getModuleImport(String moduleName) {
//		Map<String, ModuleDependency> moduleNameToImportDependency = new HashMap<String, ModuleDependency>();
//		moduleNameToImportDependency.put("DelimitedExpr", DelimitedExprAttr);
//		moduleNameToImportDependency.put("NoSpaceExpr", NoSpaceExprAttr);
//		moduleNameToImportDependency.put("Symbol", SymbolAttr);
//		moduleNameToImportDependency.put("Spacing", SpacingAttr);
//		moduleNameToImportDependency.put("Keyword", KeywordAttr);
//		moduleNameToImportDependency.put("Identifier", IdentifierAttr);		
//		moduleNameToImportDependency.put("Literal", LiteralAttr);
//		moduleNameToImportDependency.put("LocalDecl", LocalDeclAttr);		
//		moduleNameToImportDependency.put("Expression", ExprAttr);
//		moduleNameToImportDependency.put("Type", TypeAttr);
//		moduleNameToImportDependency.put("Header", HeaderAttr);
//		moduleNameToImportDependency.put("TraitObject", TraitObjectAttr);	
//		
//		return moduleNameToImportDependency.get(moduleName);
//	}

}
