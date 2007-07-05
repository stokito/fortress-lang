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
import com.sun.fortress.interpreter.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.useful.Option;
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.IterableOnceTranslatingList;


// / and unit_var = unit_var_rec node
// / and unit_var_rec =
// / {
// / unit_var_name : id;
// / unit_var_type : dim_type;
// / }
// /
public class UnitVar extends AbstractNode implements Decl {

    List<Id> names;

    Option<TypeRef> type;

    Option<Expr> def;

    boolean si;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forUnitVar(this);
    }

    UnitVar(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public List<Id> getNames() {
        return names;
    }

    /**
     * @return Returns the type.
     */
    public Option<TypeRef> getType() {
        return type;
    }

    public Option<Expr> getDef() {
        return def;
    }

    public boolean getSI() {
        return si;
    }

}
