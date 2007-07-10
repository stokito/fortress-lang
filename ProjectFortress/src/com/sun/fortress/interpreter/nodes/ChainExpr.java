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

public class ChainExpr extends Primary {
  private final Expr _first;
  private final List<Pair<Op, Expr>> _links;

  /**
   * Constructs a ChainExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ChainExpr(Span in_span, Expr in_first, List<Pair<Op, Expr>> in_links) {
    super(in_span);

    if (in_first == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'first' to the ChainExpr constructor was null");
    }
    _first = in_first;

    if (in_links == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'links' to the ChainExpr constructor was null");
    }
    _links = in_links;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forChainExpr(this);
    }

    ChainExpr(Span span) {
        super(span);
        _first = null;
        _links = null;
    }

  final public Expr getFirst() { return _first; }
  final public List<Pair<Op, Expr>> getLinks() { return _links; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forChainExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forChainExpr(this); }

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
    writer.print("ChainExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Expr temp_first = getFirst();
    writer.startLine();
    writer.print("first = ");
    temp_first.outputHelp(writer, lossless);

    List<Pair<Op, Expr>> temp_links = getLinks();
    writer.startLine();
    writer.print("links = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_links = true;
    for (Pair<Op, Expr> elt_temp_links : temp_links) {
      isempty_temp_links = false;
      writer.startLine("* ");
      if (elt_temp_links == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_links);
          writer.print(" ");
          writer.printEscaped(elt_temp_links);
        } else { writer.print(elt_temp_links); }
      }
    }
    writer.unindent();
    if (isempty_temp_links) writer.print(" }");
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
      ChainExpr casted = (ChainExpr) obj;
      Expr temp_first = getFirst();
      Expr casted_first = casted.getFirst();
      if (!(temp_first == casted_first || temp_first.equals(casted_first))) return false;
      List<Pair<Op, Expr>> temp_links = getLinks();
      List<Pair<Op, Expr>> casted_links = casted.getLinks();
      if (!(temp_links == casted_links || temp_links.equals(casted_links))) return false;
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
    Expr temp_first = getFirst();
    code ^= temp_first.hashCode();
    List<Pair<Op, Expr>> temp_links = getLinks();
    code ^= temp_links.hashCode();
    return code;
  }
}
