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
package com.sun.fortress.interpreter.parser;
import java.util.List;

import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.Modifier;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.WhereClause;
import com.sun.fortress.interpreter.useful.MagicNumbers;

public class TraitHeader {

    private List<Modifier> mods;
    private Id name;
    private List<StaticParam> staticParams;
    private List<TypeRef> extendsClause;
    private List<TypeRef> excludesClause;
    private Option<List<TypeRef>> comprisesClause;
    private Option<List<WhereClause>> whereClause;

    public TraitHeader(List<Modifier> mods, Id name,
                       List<StaticParam> staticParams,
                       List<TypeRef> extendsClause, List<TypeRef> excludesClause,
                       Option<List<TypeRef>> comprisesClause,
                       Option<List<WhereClause>> whereClause) {
        this.mods = mods;
        this.name = name;
        this.staticParams = staticParams;
        this.extendsClause = extendsClause;
        this.excludesClause = excludesClause;
        this.comprisesClause = comprisesClause;
        this.whereClause = whereClause;
    }

    public List<Modifier> getMods() {
        return mods;
    }

    public Id getName() {
        return name;
    }

    public List<StaticParam> getStaticParams() {
        return staticParams;
    }

    public List<TypeRef> getExtendsClause() {
        return extendsClause;
    }

    public List<TypeRef> getExcludesClause() {
        return excludesClause;
    }

    public Option<List<TypeRef>> getComprisesClause() {
        return comprisesClause;
    }

    public Option<List<WhereClause>> getWhereClause() {
        return whereClause;
    }

    public int hashCode() {
        return MagicNumbers.hashList(mods, MagicNumbers.m)
            + name.hashCode() * MagicNumbers.n
            + MagicNumbers.hashList(staticParams, MagicNumbers.e)
            + MagicNumbers.hashList(extendsClause, MagicNumbers.l)
            + MagicNumbers.hashList(excludesClause, MagicNumbers.y)
            + comprisesClause.hashCode() * MagicNumbers.a
            + whereClause.hashCode() * MagicNumbers.t;
    }

    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            TraitHeader th = (TraitHeader) o;
            return mods.equals(th.getMods())
                && name.equals(th.getName())
                && staticParams.equals(th.getStaticParams())
                && extendsClause.equals(th.getExtendsClause())
                && excludesClause.equals(th.getExcludesClause())
                && comprisesClause.equals(th.getComprisesClause())
                && whereClause.equals(th.getWhereClause());
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("trait ");
        sb.append(String.valueOf(name));
        return sb.toString();
    }
}
