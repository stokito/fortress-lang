/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Class for collecting names and imports for a given component.
 * Used in the Fortress com.sun.fortress.compiler.Fortress.
 */

package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.*;

import java.util.LinkedList;
import java.util.List;

public class NameAndImportCollector extends NodeDepthFirstVisitor<List<Import>> {

    private NameAndImportCollection namesAndImports;


    @Override
    public List<Import> forComponentOnly(Component that,
                                         List<Import> info,
                                         List<Import> name_result,
                                         List<List<Import>> imports_result,
                                         List<List<Import>> decls_result,
                                         List<List<Import>> comprises_result,
                                         List<List<Import>> exports_result) {
        this.namesAndImports = new NameAndImportCollection();
        this.namesAndImports.setComponentName(that.getName());
        this.namesAndImports.setImports(collapseList(imports_result));

        return super.forComponentOnly(that,
                                      info,
                                      name_result,
                                      imports_result,
                                      decls_result,
                                      comprises_result,
                                      exports_result);
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
        for (List<Import> ls : apis_result) {
            imports.addAll(ls);
        }
        return imports;
    }

    @Override
    public List<Import> forImportApiOnly(ImportApi that, List<Import> info, List<List<Import>> apis_result) {
        List<Import> ls = collapseList(apis_result);
        ls.add(that);
        return ls;
    }


    @Override
    public List<Import> forImportedNamesOnly(ImportedNames that, List<Import> info, List<Import> api_result) {
        List<Import> ls = api_result;
        ls.add(that);
        return ls;
    }

    @Override
    public List<Import> forImportNamesOnly(ImportNames that,
                                           List<Import> info,
                                           List<Import> api_result,
                                           List<List<Import>> aliasedNames_result) {
        List<Import> ls = collapseList(aliasedNames_result);
        ls.add(that);
        return ls;
    }

    @Override
    public List<Import> forImportStarOnly(ImportStar that,
                                          List<Import> info,
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
