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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public abstract class LetExpr extends Expr implements DefOrDecl {
  private final List<Expr> _body;

  /**
   * Constructs a LetExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public LetExpr(Span in_span, List<Expr> in_body) {
    super(in_span);

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the LetExpr constructor was null");
    }
    _body = in_body;
  }

    protected LetExpr(Span span) {
        super(span);
        _body = null;
    }

  public List<Expr> getBody() { return _body; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  public abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  public abstract int generateHashCode();
}
