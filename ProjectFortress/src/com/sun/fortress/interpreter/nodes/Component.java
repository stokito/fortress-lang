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

public class Component extends CompilationUnit {
  private final List<Export> _exports;
  private final List<? extends DefOrDecl> _defs;

  /**
   * Constructs a Component.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Component(Span in_span, DottedId in_name, List<Import> in_imports, List<Export> in_exports, List<? extends DefOrDecl> in_defs) {
    super(in_span, in_name, in_imports);

    if (in_exports == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'exports' to the Component constructor was null");
    }
    _exports = in_exports;

    if (in_defs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'defs' to the Component constructor was null");
    }
    _defs = in_defs;
  }

    // Necessary for vistor pattern
    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forComponent(this);
    }

    // Necessary for reflective creation.
    public Component(Span span) {
        super(span);
        _exports = null;
        _defs = null;
    }

  final public List<Export> getExports() { return _exports; }
  final public List<? extends DefOrDecl> getDefs() { return _defs; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forComponent(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forComponent(this); }

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
    writer.print("Component:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    DottedId temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    if (lossless) {
      writer.printSerialized(temp_name);
      writer.print(" ");
      writer.printEscaped(temp_name);
    } else { writer.print(temp_name); }

    List<Import> temp_imports = getImports();
    writer.startLine();
    writer.print("imports = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_imports = true;
    for (Import elt_temp_imports : temp_imports) {
      isempty_temp_imports = false;
      writer.startLine("* ");
      if (elt_temp_imports == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_imports);
          writer.print(" ");
          writer.printEscaped(elt_temp_imports);
        } else { writer.print(elt_temp_imports); }
      }
    }
    writer.unindent();
    if (isempty_temp_imports) writer.print(" }");
    else writer.startLine("}");

    List<Export> temp_exports = getExports();
    writer.startLine();
    writer.print("exports = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_exports = true;
    for (Export elt_temp_exports : temp_exports) {
      isempty_temp_exports = false;
      writer.startLine("* ");
      if (elt_temp_exports == null) {
        writer.print("null");
      } else {
        elt_temp_exports.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_exports) writer.print(" }");
    else writer.startLine("}");

    List<? extends DefOrDecl> temp_defs = getDefs();
    writer.startLine();
    writer.print("defs = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_defs = true;
    for (DefOrDecl elt_temp_defs : temp_defs) {
      isempty_temp_defs = false;
      writer.startLine("* ");
      if (elt_temp_defs == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_defs);
          writer.print(" ");
          writer.printEscaped(elt_temp_defs);
        } else { writer.print(elt_temp_defs); }
      }
    }
    writer.unindent();
    if (isempty_temp_defs) writer.print(" }");
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
      Component casted = (Component) obj;
      DottedId temp_name = getName();
      DottedId casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      List<Import> temp_imports = getImports();
      List<Import> casted_imports = casted.getImports();
      if (!(temp_imports == casted_imports || temp_imports.equals(casted_imports))) return false;
      List<Export> temp_exports = getExports();
      List<Export> casted_exports = casted.getExports();
      if (!(temp_exports == casted_exports || temp_exports.equals(casted_exports))) return false;
      List<? extends DefOrDecl> temp_defs = getDefs();
      List<? extends DefOrDecl> casted_defs = casted.getDefs();
      if (!(temp_defs == casted_defs || temp_defs.equals(casted_defs))) return false;
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
    DottedId temp_name = getName();
    code ^= temp_name.hashCode();
    List<Import> temp_imports = getImports();
    code ^= temp_imports.hashCode();
    List<Export> temp_exports = getExports();
    code ^= temp_exports.hashCode();
    List<? extends DefOrDecl> temp_defs = getDefs();
    code ^= temp_defs.hashCode();
    return code;
  }
}
