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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.useful.MagicNumbers;

public class SyntaxHeaderFront {
    private Option<Id> receiver;
    private SimpleName name;
    private List<Param> params;

    public SyntaxHeaderFront(Option<Id> receiver, SimpleName name,
                         List<Param> params, Option<Param> param) {
        this.receiver = receiver;
        this.name = name;
        this.params = params;
        if (param.isSome())
            this.params.add(0, Option.unwrap(param));
    }

    public SyntaxHeaderFront(Option<Id> receiver, SimpleName name,
                         List<Param> params1) {
        this(receiver, name, params1, Option.<Param>none());
    }

    public SyntaxHeaderFront(SimpleName name,
                         List<Param> params1, Option<Param> param2) {
        this(Option.<Id>none(), name, params1, param2);
    }
    
    public SyntaxHeaderFront(SimpleName name) {
    	this.name = name;
    }

    public SyntaxHeaderFront(SimpleName name, List<Param> params1) {
        this(Option.<Id>none(), name, params1, Option.<Param>none());
    }

    public Option<Id> getReceiver() {
        return receiver;
    }

    public SimpleName getName() {
        return name;
    }

    public List<Param> getParams() {
        return params;
    }

    public int hashCode() {
        return receiver.hashCode() * MagicNumbers.w
            + name.hashCode() * MagicNumbers.r
            + MagicNumbers.hashList(params, MagicNumbers.l);
    }

    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            SyntaxHeaderFront fhf = (SyntaxHeaderFront) o;
            return receiver.equals(fhf.getReceiver())
                && name.equals(fhf.getName())
                && params.equals(fhf.getParams());
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (receiver.isSome()) {
            sb.append(Option.unwrap(receiver));
            sb.append(".");
        }
        sb.append(String.valueOf(name));
        return sb.toString();
    }
}
