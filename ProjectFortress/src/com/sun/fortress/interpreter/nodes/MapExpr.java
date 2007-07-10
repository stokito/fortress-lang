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

public class MapExpr extends BaseExpr {
  private final List<Pair<Expr, Expr>> _elements;

  /**
   * Constructs a MapExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public MapExpr(Span in_span, List<Pair<Expr, Expr>> in_elements) {
    super(in_span);

    if (in_elements == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'elements' to the MapExpr constructor was null");
    }
    _elements = in_elements;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forMapExpr(this);
    }

    MapExpr(Span span) {
        super(span);
        _elements = null;
    }

  final public List<Pair<Expr, Expr>> getElements() { return _elements; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forMapExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forMapExpr(this); }

  /**
   * Implementation of toString that uses
   * {@link #output} to generate a nicely tabbed tree.
   */
  public java.lang.String toString() {
    java.io.StringWriter w = new java.io.StringWriter();
    output(w);
    return w.toString();
  }

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  public void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("MapExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Pair<Expr, Expr>> temp_elements = getElements();
    writer.startLine();
    writer.print("elements = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_elements = true;
    for (Pair<Expr, Expr> elt_temp_elements : temp_elements) {
      isempty_temp_elements = false;
      writer.startLine("* ");
      if (elt_temp_elements == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_elements);
          writer.print(" ");
          writer.printEscaped(elt_temp_elements);
        } else { writer.print(elt_temp_elements); }
      }
    }
    writer.unindent();
    if (isempty_temp_elements) writer.print(" }");
    else writer.startLine("}");
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values of the fields of the
   * object. Thus, two objects created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      MapExpr casted = (MapExpr) obj;
      List<Pair<Expr, Expr>> temp_elements = getElements();
      List<Pair<Expr, Expr>> casted_elements = casted.getElements();
      if (!(temp_elements == casted_elements || temp_elements.equals(casted_elements))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  public int generateHashCode() {
    int code = getClass().hashCode();
    List<Pair<Expr, Expr>> temp_elements = getElements();
    code ^= temp_elements.hashCode();
    return code;
  }
}
