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

public class ExtentRange extends AbstractNode {
  private final Option<TypeRef> _base;
  private final Option<TypeRef> _size;

  /**
   * Constructs a ExtentRange.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ExtentRange(Span in_span, Option<TypeRef> in_base, Option<TypeRef> in_size) {
    super(in_span);

    if (in_base == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'base' to the ExtentRange constructor was null");
    }
    _base = in_base;

    if (in_size == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'size' to the ExtentRange constructor was null");
    }
    _size = in_size;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forExtentRange(this);
    }

    ExtentRange(Span span) {
        super(span);
        _base = null;
        _size = null;
    }

  final public Option<TypeRef> getBase() { return _base; }
  final public Option<TypeRef> getSize() { return _size; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forExtentRange(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forExtentRange(this); }

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

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("ExtentRange:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Option<TypeRef> temp_base = getBase();
    writer.startLine();
    writer.print("base = ");
    if (lossless) {
      writer.printSerialized(temp_base);
      writer.print(" ");
      writer.printEscaped(temp_base);
    } else { writer.print(temp_base); }

    Option<TypeRef> temp_size = getSize();
    writer.startLine();
    writer.print("size = ");
    if (lossless) {
      writer.printSerialized(temp_size);
      writer.print(" ");
      writer.printEscaped(temp_size);
    } else { writer.print(temp_size); }
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
      ExtentRange casted = (ExtentRange) obj;
      Option<TypeRef> temp_base = getBase();
      Option<TypeRef> casted_base = casted.getBase();
      if (!(temp_base == casted_base || temp_base.equals(casted_base))) return false;
      Option<TypeRef> temp_size = getSize();
      Option<TypeRef> casted_size = casted.getSize();
      if (!(temp_size == casted_size || temp_size.equals(casted_size))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    Option<TypeRef> temp_base = getBase();
    code ^= temp_base.hashCode();
    Option<TypeRef> temp_size = getSize();
    code ^= temp_size.hashCode();
    return code;
  }
}
