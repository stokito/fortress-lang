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
 * Fortress functional header fronts.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;
import java.util.List;

import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.useful.None;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.useful.MagicNumbers;

public class FnHeaderFront {
    private Option<Id> receiver;
    private FnName name;
    private List<StaticParam> staticParams;
    private List<Param> params;

    public FnHeaderFront(Option<Id> receiver, FnName name,
                         List<StaticParam> staticParams,
                         List<Param> params, Option<Param> param) {
        this.receiver = receiver;
        this.name = name;
        this.staticParams = staticParams;
        this.params = params;
        if (param.isPresent())
            this.params.add(0, (Param)param.getVal());
    }

    public FnHeaderFront(Option<Id> receiver, FnName name,
                         List<StaticParam> staticParams, List<Param> params1) {
        this(receiver, name, staticParams, params1, None.<Param>make());
    }

    public FnHeaderFront(FnName name, List<StaticParam> staticParams,
                         List<Param> params1, Option<Param> param2) {
        this(None.<Id>make(), name, staticParams, params1, param2);
    }

    public FnHeaderFront(FnName name, List<StaticParam> staticParams,
                         List<Param> params1) {
        this(None.<Id>make(), name, staticParams, params1, None.<Param>make());
    }

    public FnHeaderFront(FnName name, List<Param> params1) {
        this(None.<Id>make(), name, FortressUtil.emptyStaticParams(), params1,
             None.<Param>make());
    }

    public Option<Id> getReceiver() {
        return receiver;
    }

    public FnName getName() {
        return name;
    }

    public List<StaticParam> getStaticParams() {
        return staticParams;
    }

    public List<Param> getParams() {
        return params;
    }

    public int hashCode() {
        return receiver.hashCode() * MagicNumbers.w
            + name.hashCode() * MagicNumbers.r
            + MagicNumbers.hashList(staticParams, MagicNumbers.m)
            + MagicNumbers.hashList(params, MagicNumbers.l);
    }

    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            FnHeaderFront fhf = (FnHeaderFront) o;
            return receiver.equals(fhf.getReceiver())
                && name.equals(fhf.getName())
                && staticParams.equals(fhf.getStaticParams())
                && params.equals(fhf.getParams());
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (receiver.isPresent()) {
            sb.append(receiver.getVal());
            sb.append(".");
        }
        sb.append(String.valueOf(name));
        return sb.toString();
    }
}
