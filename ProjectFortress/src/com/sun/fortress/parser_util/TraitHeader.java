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
 * Fortress trait headers.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;
import java.util.List;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.MagicNumbers;

public class TraitHeader {

    private Id name;
    private List<StaticParam> staticParams;
    private List<TraitTypeWhere> extendsClause;

    public TraitHeader(Id name, List<StaticParam> staticParams,
                       List<TraitTypeWhere> extendsClause) {
        this.name = name;
        this.staticParams = staticParams;
        this.extendsClause = extendsClause;
    }

    public Id getName() {
        return name;
    }

    public List<StaticParam> getStaticParams() {
        return staticParams;
    }

    public List<TraitTypeWhere> getExtendsClause() {
        return extendsClause;
    }

    public int hashCode() {
        return name.hashCode() * MagicNumbers.n
            + MagicNumbers.hashList(staticParams, MagicNumbers.e)
            + MagicNumbers.hashList(extendsClause, MagicNumbers.l);
    }

    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            TraitHeader th = (TraitHeader) o;
            return name.equals(th.getName())
                && staticParams.equals(th.getStaticParams())
                && extendsClause.equals(th.getExtendsClause());
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("trait ");
        sb.append(NodeUtil.nameString(name));
        return sb.toString();
    }
}
