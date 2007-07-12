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
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public abstract class AbsObjectExpr extends DelimitedExpr {
  private final Option<List<TypeRef>> _traits;
  private final List<? extends DefOrDecl> _defOrDecls;

  /**
   * Constructs a AbsObjectExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public AbsObjectExpr(Span in_span, Option<List<TypeRef>> in_traits, List<? extends DefOrDecl> in_defOrDecls) {
    super(in_span);

    if (in_traits == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'traits' to the AbsObjectExpr constructor was null");
    }
    _traits = in_traits;

    if (in_defOrDecls == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'defOrDecls' to the AbsObjectExpr constructor was null");
    }
    _defOrDecls = in_defOrDecls;
  }

    AbsObjectExpr(Span span) {
        super(span);
        _traits = null;
        _defOrDecls = null;
    }

  public Option<List<TypeRef>> getTraits() { return _traits; }
  public List<? extends DefOrDecl> getDefOrDecls() { return _defOrDecls; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  public abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  public abstract int generateHashCode();
}
