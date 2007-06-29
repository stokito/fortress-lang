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

public class Try extends DelimitedExpr {
  private final Expr _body;
  private final Option<Catch> _catchClause;
  private final List<TypeRef> _forbid;
  private final Option<Expr> _finallyClause;

  /**
   * Constructs a Try.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public Try(Span in_span, Expr in_body, Option<Catch> in_catchClause, List<TypeRef> in_forbid, Option<Expr> in_finallyClause) {
    super(in_span);

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the Try constructor was null. This class may not have null field values.");
    }
    _body = in_body;

    if (in_catchClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'catchClause' to the Try constructor was null. This class may not have null field values.");
    }
    _catchClause = in_catchClause;

    if (in_forbid == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'forbid' to the Try constructor was null. This class may not have null field values.");
    }
    _forbid = in_forbid;

    if (in_finallyClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'finallyClause' to the Try constructor was null. This class may not have null field values.");
    }
    _finallyClause = in_finallyClause;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTry(this);
    }

    Try(Span span) {
        super(span);
        _body = null;
        _catchClause = null;
        _forbid = null;
        _finallyClause = null;
    }

  final public Expr getBody() { return _body; }
  final public Option<Catch> getCatchClause() { return _catchClause; }
  final public List<TypeRef> getForbid() { return _forbid; }
  final public Option<Expr> getFinallyClause() { return _finallyClause; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forTry(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forTry(this); }

  /**
   * Implementation of toString that uses
   * {@see #output} to generated nicely tabbed tree.
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
    outputHelp(new TabPrintWriter(writer, 2));
  }

  public void outputHelp(TabPrintWriter writer) {
    writer.print("Try" + ":");
    writer.indent();

    writer.startLine("");
    writer.print("span = ");
    Span temp_span = getSpan();
    if (temp_span == null) {
      writer.print("null");
    } else {
      writer.print(temp_span);
    }

    writer.startLine("");
    writer.print("body = ");
    Expr temp_body = getBody();
    if (temp_body == null) {
      writer.print("null");
    } else {
      temp_body.outputHelp(writer);
    }

    writer.startLine("");
    writer.print("catchClause = ");
    Option<Catch> temp_catchClause = getCatchClause();
    if (temp_catchClause == null) {
      writer.print("null");
    } else {
      writer.print(temp_catchClause);
    }

    writer.startLine("");
    writer.print("forbid = ");
    List<TypeRef> temp_forbid = getForbid();
    if (temp_forbid == null) {
      writer.print("null");
    } else {
      writer.print(temp_forbid);
    }

    writer.startLine("");
    writer.print("finallyClause = ");
    Option<Expr> temp_finallyClause = getFinallyClause();
    if (temp_finallyClause == null) {
      writer.print("null");
    } else {
      writer.print(temp_finallyClause);
    }
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values
   * of the fields of the object. Thus, two objects
   * created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      Try casted = (Try) obj;
      if (! (getBody().equals(casted.getBody()))) return false;
      if (! (getCatchClause().equals(casted.getCatchClause()))) return false;
      if (! (getForbid().equals(casted.getForbid()))) return false;
      if (! (getFinallyClause().equals(casted.getFinallyClause()))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with
   * equals. The value of the hashCode is formed by
   * XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    code ^= getBody().hashCode();
    code ^= getCatchClause().hashCode();
    code ^= getForbid().hashCode();
    code ^= getFinallyClause().hashCode();
    return code;
  }
}
