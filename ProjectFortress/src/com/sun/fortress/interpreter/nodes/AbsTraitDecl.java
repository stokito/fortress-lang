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
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public class AbsTraitDecl extends TraitDefOrDecl implements GenericDefOrDecl, AbsDecl {

  /**
   * Constructs a AbsTraitDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public AbsTraitDecl(Span in_span, List<Modifier> in_mods, Id in_name, Option<List<StaticParam>> in_staticParams, Option<List<TypeRef>> in_extendsClause, List<TypeRef> in_excludes, Option<List<TypeRef>> in_bounds, List<WhereClause> in_where, List<? extends DefOrDecl> in_fns) {
    super(in_span, in_mods, in_name, in_staticParams, in_extendsClause, in_excludes, in_bounds, in_where, in_fns);
  }

  /**
   * Empty constructor, for reflective access.  Clients are
   * responsible for manually instantiating each field.
   */
  protected AbsTraitDecl() {
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forAbsTraitDecl(this);
    }

    AbsTraitDecl(Span span) {
        super(span);
    }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forAbsTraitDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forAbsTraitDecl(this); }

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
    writer.print("AbsTraitDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Modifier> temp_mods = getMods();
    writer.startLine();
    writer.print("mods = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_mods = true;
    for (Modifier elt_temp_mods : temp_mods) {
      isempty_temp_mods = false;
      writer.startLine("* ");
      if (elt_temp_mods == null) {
        writer.print("null");
      } else {
        elt_temp_mods.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_mods) writer.print(" }");
    else writer.startLine("}");

    Id temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    temp_name.outputHelp(writer, lossless);

    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    writer.startLine();
    writer.print("staticParams = ");
    if (lossless) {
      writer.printSerialized(temp_staticParams);
      writer.print(" ");
      writer.printEscaped(temp_staticParams);
    } else { writer.print(temp_staticParams); }

    Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
    writer.startLine();
    writer.print("extendsClause = ");
    if (lossless) {
      writer.printSerialized(temp_extendsClause);
      writer.print(" ");
      writer.printEscaped(temp_extendsClause);
    } else { writer.print(temp_extendsClause); }

    List<TypeRef> temp_excludes = getExcludes();
    writer.startLine();
    writer.print("excludes = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_excludes = true;
    for (TypeRef elt_temp_excludes : temp_excludes) {
      isempty_temp_excludes = false;
      writer.startLine("* ");
      if (elt_temp_excludes == null) {
        writer.print("null");
      } else {
        elt_temp_excludes.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_excludes) writer.print(" }");
    else writer.startLine("}");

    Option<List<TypeRef>> temp_bounds = getBounds();
    writer.startLine();
    writer.print("bounds = ");
    if (lossless) {
      writer.printSerialized(temp_bounds);
      writer.print(" ");
      writer.printEscaped(temp_bounds);
    } else { writer.print(temp_bounds); }

    List<WhereClause> temp_where = getWhere();
    writer.startLine();
    writer.print("where = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_where = true;
    for (WhereClause elt_temp_where : temp_where) {
      isempty_temp_where = false;
      writer.startLine("* ");
      if (elt_temp_where == null) {
        writer.print("null");
      } else {
        elt_temp_where.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_where) writer.print(" }");
    else writer.startLine("}");

    List<? extends DefOrDecl> temp_fns = getFns();
    writer.startLine();
    writer.print("fns = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_fns = true;
    for (DefOrDecl elt_temp_fns : temp_fns) {
      isempty_temp_fns = false;
      writer.startLine("* ");
      if (elt_temp_fns == null) {
        writer.print("null");
      } else {
        elt_temp_fns.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_fns) writer.print(" }");
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
      AbsTraitDecl casted = (AbsTraitDecl) obj;
      List<Modifier> temp_mods = getMods();
      List<Modifier> casted_mods = casted.getMods();
      if (!(temp_mods == casted_mods || temp_mods.equals(casted_mods))) return false;
      Id temp_name = getName();
      Id casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      Option<List<StaticParam>> temp_staticParams = getStaticParams();
      Option<List<StaticParam>> casted_staticParams = casted.getStaticParams();
      if (!(temp_staticParams == casted_staticParams || temp_staticParams.equals(casted_staticParams))) return false;
      Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
      Option<List<TypeRef>> casted_extendsClause = casted.getExtendsClause();
      if (!(temp_extendsClause == casted_extendsClause || temp_extendsClause.equals(casted_extendsClause))) return false;
      List<TypeRef> temp_excludes = getExcludes();
      List<TypeRef> casted_excludes = casted.getExcludes();
      if (!(temp_excludes == casted_excludes || temp_excludes.equals(casted_excludes))) return false;
      Option<List<TypeRef>> temp_bounds = getBounds();
      Option<List<TypeRef>> casted_bounds = casted.getBounds();
      if (!(temp_bounds == casted_bounds || temp_bounds.equals(casted_bounds))) return false;
      List<WhereClause> temp_where = getWhere();
      List<WhereClause> casted_where = casted.getWhere();
      if (!(temp_where == casted_where || temp_where.equals(casted_where))) return false;
      List<? extends DefOrDecl> temp_fns = getFns();
      List<? extends DefOrDecl> casted_fns = casted.getFns();
      if (!(temp_fns == casted_fns || temp_fns.equals(casted_fns))) return false;
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
    List<Modifier> temp_mods = getMods();
    code ^= temp_mods.hashCode();
    Id temp_name = getName();
    code ^= temp_name.hashCode();
    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    code ^= temp_staticParams.hashCode();
    Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
    code ^= temp_extendsClause.hashCode();
    List<TypeRef> temp_excludes = getExcludes();
    code ^= temp_excludes.hashCode();
    Option<List<TypeRef>> temp_bounds = getBounds();
    code ^= temp_bounds.hashCode();
    List<WhereClause> temp_where = getWhere();
    code ^= temp_where.hashCode();
    List<? extends DefOrDecl> temp_fns = getFns();
    code ^= temp_fns.hashCode();
    return code;
  }
}
