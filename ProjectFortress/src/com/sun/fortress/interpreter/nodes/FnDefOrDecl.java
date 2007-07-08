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


import com.sun.fortress.interpreter.useful.Option;
import java.util.Comparator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;
import com.sun.fortress.interpreter.useful.Useful;


public abstract class FnDefOrDecl extends AbstractNode implements Generic, Applicable,
        DefOrDecl {

    List<Modifier> mods;

    FnName name;

    Option<List<StaticParam>> staticParams;

    List<Param> params;

    Option<TypeRef> returnType;

    List<TypeRef> throwss;

    List<WhereClause> where;

    Contract contract;

    transient private boolean isAFunctionalMethodKnown;
    transient private int cachedSelfParameterIndex = -1;
    transient private volatile String asMethodName;

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

    static class Comparer implements Comparator<FnDefOrDecl> {

        public int compare(FnDefOrDecl o1, FnDefOrDecl o2) {
            return NodeComparator.compare(o1, o2);
        }
    }

    public final static Comparer comparer = new Comparer();

    @Override
    public String toString() {

        return NodeUtil.getName(name)
                + (staticParams.isPresent() ?
                        Useful.listInOxfords(staticParams.getVal()) : "")
                + Useful.listInParens(params)
                + (returnType.isPresent() ? (":" + returnType.getVal()) : "")
                + "\n\t@" + NodeUtil.getAt(name);
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

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#isAFunctionalMethod()
     */
    public int selfParameterIndex() {
        if (!isAFunctionalMethodKnown) {
            int i = 0;
            for (Param p : params) {
                Id id = p.getName();
                if (WellKnownNames.defaultSelfName.equals(id.getName())) {
                    cachedSelfParameterIndex = i;
                    break;
                }
                i++;

            }
            isAFunctionalMethodKnown = true;
        }
        return cachedSelfParameterIndex;
    }

    public String nameAsFunction() {
        return NodeUtil.getName(name);
    }
}

// / and fn_def_or_decl =
// / [
// / | `FnDecl of fn_def
// / | `AbsFnDecl of fn_decl
// / ] node
// /
