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

public abstract class ImportFrom extends Import {
  private final DottedId _source;

  /**
   * Constructs a ImportFrom.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ImportFrom(Span in_span, DottedId in_source) {
    super(in_span);

    if (in_source == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'source' to the ImportFrom constructor was null");
    }
    _source = in_source;
  }

    ImportFrom(Span s) {
        super(s);
        _source = null;
    }

  public DottedId getSource() { return _source; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  protected abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  protected abstract int generateHashCode();
}
