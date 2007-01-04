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

package com.sun.fortress.interpreter.nodes;


import java.util.List;

import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;
import com.sun.fortress.interpreter.useful.Useful;


public abstract class FnDefOrDecl extends Tree implements Generic, Applicable,
        DefOrDecl {

    List<Modifier> mods;

    FnName name;

    Option<List<StaticParam>> staticParams;

    List<Param> params;

    Option<TypeRef> returnType;

    List<TypeRef> throwss;

    List<WhereClause> where;

    Contract contract;

    public FnDefOrDecl(Span s, List<Modifier> mods, FnName name,
            Option<List<StaticParam>> staticParams, List<Param> params,
            Option<TypeRef> returnType, List<TypeRef> throwss,
            List<WhereClause> where, Contract contract) {
        super(s);
        this.mods = mods;
        this.name = name;
        this.staticParams = staticParams;
        this.params = params;
        this.returnType = returnType;
        this.throwss = throwss;
        this.where = where;
        this.contract = contract;
    }

    public String getSelfName() {
        return WellKnownNames.defaultSelfName;
    }

    @Override
    public String toString() {

        return name.name()
                + (staticParams.isPresent() ?
                        Useful.listInOxfords(staticParams.getVal()) : "")
                + Useful.listInParens(params)
                + (returnType.isPresent() ? (":" + returnType.getVal()) : "")
                + "\n\t@" + name.at();
    }

    public FnDefOrDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the contract.
     */
    public Contract getContract() {
        return contract;
    }

    /**
     * @return Returns the mods.
     */
    public List<Modifier> getMods() {
        return mods;
    }

    /**
     * @return Returns the name.
     */
    public FnName getFnName() {
        return name;
    }

    @Override
    public String stringName() {
        return name.name();
    }

    /**
     * @return Returns the params.
     */
    public List<Param> getParams() {
        return params;
    }

    /**
     * @return Returns the returnType.
     */
    public Option<TypeRef> getReturnType() {
        return returnType;
    }

    /**
     * @return Returns the throwss.
     */
    public List<TypeRef> getThrowss() {
        return throwss;
    }

    /**
     * @return Returns the staticParams.
     */
    public Option<List<StaticParam>> getStaticParams() {
        return staticParams;
    }

    /**
     * @return Returns the where.
     */
    public List<WhereClause> getWhere() {
        return where;
    }

    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(stringName());
    }

}

// / and fn_def_or_decl =
// / [
// / | `FnDecl of fn_def
// / | `AbsFnDecl of fn_decl
// / ] node
// /
