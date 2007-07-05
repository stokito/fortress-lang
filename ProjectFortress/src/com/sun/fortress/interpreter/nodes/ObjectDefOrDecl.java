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

import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.useful.Option;
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


public abstract class ObjectDefOrDecl extends AbstractNode implements Generic,
        DefOrDecl {

    List<Modifier> mods;

    Id name;

    Option<List<StaticParam>> staticParams;

    Option<List<Param>> params;

    Option<List<TypeRef>> traits;

    List<TypeRef> throws_;

    List<WhereClause> where;

    Contract contract;

    ObjectDefOrDecl(Span s) {
        super(s);
    }

    public ObjectDefOrDecl(Span span, List<Modifier> mods2, Id name2,
            Option<List<StaticParam>> staticParams2,
            Option<List<Param>> params2, Option<List<TypeRef>> traits2,
            List<TypeRef> throws_2, List<WhereClause> where2, Contract contract2) {
        super(span);
        mods = mods2;
        name = name2;
        staticParams = staticParams2;
        params = params2;
        traits = traits2;
        throws_ = throws_2;
        where = where2;
        contract = contract2;
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
    public Id getName() {
        return name;
    }

    /**
     * @return Returns the params.
     */
    public Option<List<Param>> getParams() {
        return params;
    }

    /**
     * @return Returns the throws_.
     */
    public List<TypeRef> getThrows_() {
        return throws_;
    }

    /**
     * @return Returns the traits.
     */
    public Option<List<TypeRef>> getTraits() {
        return traits;
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

    /**
     * Same result as subtype getDefOrDecls, but the type is generic to remove the need
     * for picky casting in some clients.
     */
    abstract public List<? extends DefOrDecl> getDefOrDecls();
}
