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

public abstract class CompilationUnit extends AbstractNode {
  private final DottedId _name;
  private final List<Import> _imports;

  /**
   * Constructs a CompilationUnit.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public CompilationUnit(Span in_span, DottedId in_name, List<Import> in_imports) {
    super(in_span);

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the CompilationUnit constructor was null");
    }
    _name = in_name;

    if (in_imports == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'imports' to the CompilationUnit constructor was null");
    }
    _imports = in_imports;
  }

    CompilationUnit(Span span) {
        super(span);
        _name = null;
        _imports = null;
    }

  public DottedId getName() { return _name; }
  public List<Import> getImports() { return _imports; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  protected abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  protected abstract int generateHashCode();
}
