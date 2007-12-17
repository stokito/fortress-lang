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
 * Class for collecting names and imports for a given component.
 * Used in the Fortress com.sun.fortress.compiler.Fortress.
 */

package com.sun.fortress.parser_util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;

/**
 * Created by fagidiot on 22/10/2007
 * @author fagidiot
 */
public class NameAndImportCollector extends NodeDepthFirstVisitor<List<Import>> {

	private NameAndImportCollection namesAndImports;



	@Override
	public List<Import> forComponentOnly(Component that,
			List<Import> name_result, List<List<Import>> imports_result,
			List<List<Import>> exports_result, List<List<Import>> decls_result) {
		this.namesAndImports = new NameAndImportCollection();
		this.namesAndImports.setComponentName(that.getName());
		this.namesAndImports.setImports(collapseList(imports_result));

		return super.forComponentOnly(that, name_result, imports_result,
				exports_result, decls_result);
	}

	@Override
	public List<Import> defaultCase(Node that) {
		return new LinkedList<Import>();
	}

	/**
	 * @param apis_result
	 * @return
	 */
	private List<Import> collapseList(List<List<Import>> apis_result) {
		List<Import> imports = new LinkedList<Import>();
		for (List<Import> ls: apis_result) {
			imports.addAll(ls);
		}
		return imports;
	}

	@Override
	public List<Import> forImportApiOnly(ImportApi that,
			List<List<Import>> apis_result) {
		List<Import> ls = collapseList(apis_result);
		ls.add(that);
		return ls;
	}


	@Override
	public List<Import> forImportedNamesOnly(ImportedNames that,
			List<Import> api_result) {
		List<Import> ls = api_result;
		ls.add(that);
		return ls;
	}

	@Override
	public List<Import> forImportNamesOnly(ImportNames that,
			List<Import> api_result,
			List<List<Import>> aliasedNames_result) {
		List<Import> ls = collapseList(aliasedNames_result);
		ls.add(that);
		return ls;
	}

	@Override
	public List<Import> forImportStarOnly(ImportStar that,
			List<Import> api_result,
			List<List<Import>> except_result) {
		List<Import> ls = collapseList(except_result);
		ls.add(that);
		return ls;
	}

	/**
	 *
	 */
	public NameAndImportCollector() {

	}

	public NameAndImportCollection getResult() {
		return this.namesAndImports;
	}

}
