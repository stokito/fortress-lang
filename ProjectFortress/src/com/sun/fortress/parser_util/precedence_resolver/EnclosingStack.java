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

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.nodes.Op;
import com.sun.fortress.parser_util.precedence_opexpr.PostfixOpExpr;
import com.sun.fortress.useful.Empty;
import com.sun.fortress.useful.PureList;


public abstract class EnclosingStack {
   private final PureList<PostfixOpExpr> list;
   protected static final PureList<PostfixOpExpr> EMPTY = new Empty<PostfixOpExpr>();

   public EnclosingStack() { list = EMPTY; }

   public EnclosingStack(PureList<PostfixOpExpr> _list) {
      list = _list;
   }

   public PureList<PostfixOpExpr> getList() { return list; }

   public Layer layer(Op _op) {return new Layer(_op, this); }

   public Layer layer(Op _op, PureList<PostfixOpExpr> _list) {
      return new Layer(_op, _list, this);
   }
}
