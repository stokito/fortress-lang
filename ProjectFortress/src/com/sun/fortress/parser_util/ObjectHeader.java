/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress object headers.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.MagicNumbers;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class ObjectHeader {

    private Id name;
    private List<StaticParam> staticParams;
    private Option<List<Param>> params;
    private List<TraitTypeWhere> extendsClause;
    private FnHeaderClause fnHeaderClause;

    public ObjectHeader(Id name,
                        List<StaticParam> staticParams,
                        Option<List<Param>> params,
                        List<TraitTypeWhere> extendsClause,
                        FnHeaderClause fnHeaderClause) {
        this.name = name;
        this.staticParams = staticParams;
        this.params = params;
        this.extendsClause = extendsClause;
        this.fnHeaderClause = fnHeaderClause;
    }

    public Id getName() {
        return name;
    }

    public List<StaticParam> getStaticParams() {
        return staticParams;
    }

    public Option<List<Param>> getParams() {
        return params;
    }

    public List<TraitTypeWhere> getExtendsClause() {
        return extendsClause;
    }

    public FnHeaderClause getFnHeaderClause() {
        return fnHeaderClause;
    }

    public int hashCode() {
        return name.hashCode() * MagicNumbers.n + MagicNumbers.hashList(staticParams, MagicNumbers.e) +
               params.hashCode() * MagicNumbers.a + MagicNumbers.hashList(extendsClause, MagicNumbers.l) +
               fnHeaderClause.hashCode() * MagicNumbers.t;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass().equals(this.getClass())) {
            ObjectHeader oh = (ObjectHeader) o;
            return name.equals(oh.getName()) && staticParams.equals(oh.getStaticParams()) &&
                   params.equals(oh.getParams()) && extendsClause.equals(oh.getExtendsClause()) &&
                   fnHeaderClause.equals(oh.getFnHeaderClause());
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object ");
        sb.append(NodeUtil.nameString(name));
        return sb.toString();
    }
}
