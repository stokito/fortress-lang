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

public class CatchClause extends AbstractNode {
  private final TypeRef _match;
  private final List<Expr> _body;

  /**
   * Constructs a CatchClause.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public CatchClause(Span in_span, TypeRef in_match, List<Expr> in_body) {
    super(in_span);

    if (in_match == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'match' to the CatchClause constructor was null");
    }
    _match = in_match;

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the CatchClause constructor was null");
    }
    _body = in_body;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forCatchClause(this);
    }

    CatchClause(Span span) {
        super(span);
        _match = null;
        _body = null;
    }

  final public TypeRef getMatch() { return _match; }
  final public List<Expr> getBody() { return _body; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forCatchClause(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forCatchClause(this); }

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
    writer.print("CatchClause:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    TypeRef temp_match = getMatch();
    writer.startLine();
    writer.print("match = ");
    temp_match.outputHelp(writer, lossless);

    List<Expr> temp_body = getBody();
    writer.startLine();
    writer.print("body = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_body = true;
    for (Expr elt_temp_body : temp_body) {
      isempty_temp_body = false;
      writer.startLine("* ");
      if (elt_temp_body == null) {
        writer.print("null");
      } else {
        elt_temp_body.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_body) writer.print(" }");
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
      CatchClause casted = (CatchClause) obj;
      TypeRef temp_match = getMatch();
      TypeRef casted_match = casted.getMatch();
      if (!(temp_match == casted_match || temp_match.equals(casted_match))) return false;
      List<Expr> temp_body = getBody();
      List<Expr> casted_body = casted.getBody();
      if (!(temp_body == casted_body || temp_body.equals(casted_body))) return false;
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
    TypeRef temp_match = getMatch();
    code ^= temp_match.hashCode();
    List<Expr> temp_body = getBody();
    code ^= temp_body.hashCode();
    return code;
  }
}
