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

public abstract class Literal extends BaseExpr {
  private final String _text;

  /**
   * Constructs a Literal.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public Literal(Span in_span, String in_text) {
    super(in_span);

    if (in_text == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'text' to the Literal constructor was null. This class may not have null field values.");
    }
    _text = ((in_text == null) ? null : in_text.intern());
  }

    public Literal(Span span) {
        super(span);
        _text = null;
    }

  public String getText() { return _text; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  protected abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  protected abstract int generateHashCode();
}
