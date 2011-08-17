/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress functional header fronts.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.useful.MagicNumbers;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class FnHeaderFront {
    private Option<Id> receiver;
    private IdOrOpOrAnonymousName name;
    private List<StaticParam> staticParams;
    private List<Param> params;

    public boolean isSubscriptedAssignment = false;

    public FnHeaderFront(Option<Id> receiver,
                         IdOrOpOrAnonymousName name,
                         List<StaticParam> staticParams,
                         List<Param> params,
                         Option<Param> param) {
        this.receiver = receiver;
        this.name = name;
        this.staticParams = staticParams;
        this.params = params;
        if (param.isSome()) {
            isSubscriptedAssignment = true;
            this.params.add(0, param.unwrap());
        }
    }

    public FnHeaderFront(Option<Id> receiver,
                         IdOrOpOrAnonymousName name,
                         List<StaticParam> staticParams,
                         List<Param> params1) {
        this(receiver, name, staticParams, params1, Option.<Param>none());
    }

    public FnHeaderFront(IdOrOpOrAnonymousName name,
                         List<StaticParam> staticParams,
                         List<Param> params1,
                         Option<Param> param2) {
        this(Option.<Id>none(), name, staticParams, params1, param2);
    }

    public FnHeaderFront(IdOrOpOrAnonymousName name, List<StaticParam> staticParams, List<Param> params1) {
        this(Option.<Id>none(), name, staticParams, params1, Option.<Param>none());
    }

    public FnHeaderFront(IdOrOpOrAnonymousName name, List<Param> params1) {
        this(Option.<Id>none(), name, Collections.<StaticParam>emptyList(), params1, Option.<Param>none());
    }

    public Option<Id> getReceiver() {
        return receiver;
    }

    public IdOrOpOrAnonymousName getName() {
        return name;
    }

    public List<StaticParam> getStaticParams() {
        return staticParams;
    }

    public List<Param> getParams() {
        return params;
    }

    public int hashCode() {
        return receiver.hashCode() * MagicNumbers.w + name.hashCode() * MagicNumbers.r + MagicNumbers.hashList(
                staticParams,
                MagicNumbers.m) + MagicNumbers.hashList(params, MagicNumbers.l);
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass().equals(this.getClass())) {
            FnHeaderFront fhf = (FnHeaderFront) o;
            return receiver.equals(fhf.getReceiver()) && name.equals(fhf.getName()) &&
                   staticParams.equals(fhf.getStaticParams()) && params.equals(fhf.getParams());
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (receiver.isSome()) {
            sb.append(receiver.unwrap());
            sb.append(".");
        }
        sb.append(String.valueOf(name));
        return sb.toString();
    }
}
