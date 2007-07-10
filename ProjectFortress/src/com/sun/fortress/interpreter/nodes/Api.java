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

public class Api extends CompilationUnit {
  private final List<? extends DefOrDecl> _decls;

  /**
   * Constructs a Api.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Api(Span in_span, DottedId in_name, List<Import> in_imports, List<? extends DefOrDecl> in_decls) {
    super(in_span, in_name, in_imports);

    if (in_decls == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'decls' to the Api constructor was null");
    }
    _decls = in_decls;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forApi(this);
    }

    Api(Span span) {
        super(span);
        _decls = null;
    }

  final public List<? extends DefOrDecl> getDecls() { return _decls; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forApi(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forApi(this); }

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
    writer.print("Api:");
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

    List<? extends DefOrDecl> temp_decls = getDecls();
    writer.startLine();
    writer.print("decls = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_decls = true;
    for (DefOrDecl elt_temp_decls : temp_decls) {
      isempty_temp_decls = false;
      writer.startLine("* ");
      if (elt_temp_decls == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_decls);
          writer.print(" ");
          writer.printEscaped(elt_temp_decls);
        } else { writer.print(elt_temp_decls); }
      }
    }
    writer.unindent();
    if (isempty_temp_decls) writer.print(" }");
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
      Api casted = (Api) obj;
      DottedId temp_name = getName();
      DottedId casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      List<Import> temp_imports = getImports();
      List<Import> casted_imports = casted.getImports();
      if (!(temp_imports == casted_imports || temp_imports.equals(casted_imports))) return false;
      List<? extends DefOrDecl> temp_decls = getDecls();
      List<? extends DefOrDecl> casted_decls = casted.getDecls();
      if (!(temp_decls == casted_decls || temp_decls.equals(casted_decls))) return false;
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
    DottedId temp_name = getName();
    code ^= temp_name.hashCode();
    List<Import> temp_imports = getImports();
    code ^= temp_imports.hashCode();
    List<? extends DefOrDecl> temp_decls = getDecls();
    code ^= temp_decls.hashCode();
    return code;
  }
}
