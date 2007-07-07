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

public abstract class VarDefOrDecl extends AbstractNode implements DefOrDecl {
  private final List<LValue> _lhs;

  /**
   * Constructs a VarDefOrDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public VarDefOrDecl(Span in_span, List<LValue> in_lhs) {
    super(in_span);

    if (in_lhs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'lhs' to the VarDefOrDecl constructor was null");
    }
    _lhs = in_lhs;
  }

    VarDefOrDecl(Span s) {
        super(s);
        _lhs = null;
    }

  public List<LValue> getLhs() { return _lhs; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  protected abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  protected abstract int generateHashCode();
}
