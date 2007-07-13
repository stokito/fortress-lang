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

public class FieldSelection extends Primary implements LHS {
  private final Expr _obj;
  private final Id _id;

  /**
   * Constructs a FieldSelection.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public FieldSelection(Span in_span, Expr in_obj, Id in_id) {
    super(in_span);

    if (in_obj == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'obj' to the FieldSelection constructor was null");
    }
    _obj = in_obj;

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the FieldSelection constructor was null");
    }
    _id = in_id;
  }
  
  protected FieldSelection() {
      super();
      _obj = null;
      _id = null;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFieldSelection(this);
    }

//    FieldSelection(Span span) {
//        super(span);
//        _obj = null;
//        _id = null;
//    }

  final public Expr getObj() { return _obj; }
  final public Id getId() { return _id; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forFieldSelection(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forFieldSelection(this); }

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
    writer.print("FieldSelection:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Expr temp_obj = getObj();
    writer.startLine();
    writer.print("obj = ");
    temp_obj.outputHelp(writer, lossless);

    Id temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    temp_id.outputHelp(writer, lossless);
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
      FieldSelection casted = (FieldSelection) obj;
      Expr temp_obj = getObj();
      Expr casted_obj = casted.getObj();
      if (!(temp_obj == casted_obj || temp_obj.equals(casted_obj))) return false;
      Id temp_id = getId();
      Id casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
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
    Expr temp_obj = getObj();
    code ^= temp_obj.hashCode();
    Id temp_id = getId();
    code ^= temp_id.hashCode();
    return code;
  }
}
