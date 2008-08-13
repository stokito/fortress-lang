/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.nodes_util;

import java.util.List;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.HasAt;

/**
 * functional declaration or function expression
 * nodes_util.NodeUtil declares the following static methods:
 *  - String nameAsMethod(Applicable)
 *  - Option<Expr> getBody(Applicable)
 */
public interface Applicable extends HasAt {
    public IdOrOpOrAnonymousName getName();
    public List<StaticParam> getStaticParams();
    public List<Param> getParams();
    public Option<Type> getReturnType();
    public Option<WhereClause> getWhere();
}
