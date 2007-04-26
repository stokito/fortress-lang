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

import com.sun.fortress.interpreter.useful.HasAt;


public interface Applicable extends HasAt {
    public Expr getBody();

    public List<Param> getParams();

    public Option<TypeRef> getReturnType();

    public Option<List<StaticParam>> getStaticParams();

    public FnName getFnName();

    public List<WhereClause> getWhere();
    
    public int applicableCompareTo(Applicable a);
    
    /**
     * Returns the index of the 'self' parameter in the list,
     * or -1 if it does not appear.
     */
    // TODO this appears in DefOrDecl as well; that seems wrong.
    public int selfParameterIndex();
    public String nameAsMethod();
}
