/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Class containing names and imports for a given component.
 * Used in the Fortress com.sun.fortress.parser.preparser
 * @see{com.sun.fortress.parser.NameAndImportCollector}.
 */

package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Import;

import java.util.LinkedList;
import java.util.List;

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
     *
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
        StringBuilder buf = new StringBuilder();
        buf.append("Component " + this.getComponentName() + " has the following imports\n");
        for (Import i : this.imports) {
            buf.append("  a)" + i.toString() +"\n");
        }
        return buf.toString();
    }

}
