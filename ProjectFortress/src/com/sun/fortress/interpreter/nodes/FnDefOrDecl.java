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


import java.util.Comparator;
import java.util.List;

import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;
import com.sun.fortress.interpreter.useful.Useful;


public abstract class FnDefOrDecl extends Node implements Generic, Applicable,
        DefOrDecl, Comparable<FnDefOrDecl> {

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

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(FnDefOrDecl a1) {
        FnDefOrDecl a0 = this;
        
        FnName fn0 = a0.getFnName();
        FnName fn1 = a1.getFnName();
        int x = fn0.compareTo(fn1);
        if (x != 0)  return x;

        x = Option.<List<StaticParam>>compare(a0.getStaticParams(), a1.getStaticParams(), StaticParam.listComparer);

        if (x != 0)  return x;
        
        x = Param.listComparer.compare(a0.getParams(), a1.getParams());
        return x;

    }
    
    static class Comparer implements Comparator<FnDefOrDecl> {

        public int compare(FnDefOrDecl o1, FnDefOrDecl o2) {
            return o1.compareTo(o2);
        } 
    }
    
    public final static Comparer comparer = new Comparer();
    
    public int applicableCompareTo(Applicable a) {
        int x = Useful.compareClasses(this, a);
        if (x != 0) return x;
        return compareTo((FnDefOrDecl) a);
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
