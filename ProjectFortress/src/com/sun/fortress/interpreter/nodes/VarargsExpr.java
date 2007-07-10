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

public class VarargsExpr extends DelimitedExpr {
  private final Expr _varargs;

  /**
   * Constructs a VarargsExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public VarargsExpr(Span in_span, Expr in_varargs) {
    super(in_span);

    if (in_varargs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'varargs' to the VarargsExpr constructor was null");
    }
    _varargs = in_varargs;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forVarargsExpr(this);
    }

    VarargsExpr(Span span) {
        super(span);
        _varargs = null;
    }

  final public Expr getVarargs() { return _varargs; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forVarargsExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forVarargsExpr(this); }

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
    writer.print("VarargsExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Expr temp_varargs = getVarargs();
    writer.startLine();
    writer.print("varargs = ");
    temp_varargs.outputHelp(writer, lossless);
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
      VarargsExpr casted = (VarargsExpr) obj;
      Expr temp_varargs = getVarargs();
      Expr casted_varargs = casted.getVarargs();
      if (!(temp_varargs == casted_varargs || temp_varargs.equals(casted_varargs))) return false;
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
    Expr temp_varargs = getVarargs();
    code ^= temp_varargs.hashCode();
    return code;
  }
}
