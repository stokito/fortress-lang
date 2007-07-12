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

public class _RewriteObjectExpr extends AbsObjectExpr implements GenericDefWithParams {
  private final BATree<String, StaticParam> _implicitTypeParameters;
  private final String _genSymName;
  private final Option<List<StaticParam>> _staticParams;
  private final List<StaticArg> _staticArgs;
  private final Option<List<Param>> _params;

  /**
   * Constructs a _RewriteObjectExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public _RewriteObjectExpr(Span in_span, Option<List<TypeRef>> in_traits, List<? extends DefOrDecl> in_defOrDecls, BATree<String, StaticParam> in_implicitTypeParameters, String in_genSymName, Option<List<StaticParam>> in_staticParams, List<StaticArg> in_staticArgs, Option<List<Param>> in_params) {
    super(in_span, in_traits, in_defOrDecls);

    if (in_implicitTypeParameters == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'implicitTypeParameters' to the _RewriteObjectExpr constructor was null");
    }
    _implicitTypeParameters = in_implicitTypeParameters;

    if (in_genSymName == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'genSymName' to the _RewriteObjectExpr constructor was null");
    }
    _genSymName = in_genSymName.intern();

    if (in_staticParams == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticParams' to the _RewriteObjectExpr constructor was null");
    }
    _staticParams = in_staticParams;

    if (in_staticArgs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticArgs' to the _RewriteObjectExpr constructor was null");
    }
    _staticArgs = in_staticArgs;

    if (in_params == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'params' to the _RewriteObjectExpr constructor was null");
    }
    _params = in_params;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.for_RewriteObjectExpr(this);
    }

    _RewriteObjectExpr(Span span) {
        super(span);
        _implicitTypeParameters = null;
        _genSymName = null;
        _staticParams = null;
        _staticArgs = null;
        _params = null;
    }

  final public BATree<String, StaticParam> getImplicitTypeParameters() { return _implicitTypeParameters; }
  final public String getGenSymName() { return _genSymName; }
  final public Option<List<StaticParam>> getStaticParams() { return _staticParams; }
  final public List<StaticArg> getStaticArgs() { return _staticArgs; }
  final public Option<List<Param>> getParams() { return _params; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.for_RewriteObjectExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.for_RewriteObjectExpr(this); }

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
    writer.print("_RewriteObjectExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Option<List<TypeRef>> temp_traits = getTraits();
    writer.startLine();
    writer.print("traits = ");
    if (lossless) {
      writer.printSerialized(temp_traits);
      writer.print(" ");
      writer.printEscaped(temp_traits);
    } else { writer.print(temp_traits); }

    List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
    writer.startLine();
    writer.print("defOrDecls = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_defOrDecls = true;
    for (DefOrDecl elt_temp_defOrDecls : temp_defOrDecls) {
      isempty_temp_defOrDecls = false;
      writer.startLine("* ");
      if (elt_temp_defOrDecls == null) {
        writer.print("null");
      } else {
        elt_temp_defOrDecls.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_defOrDecls) writer.print(" }");
    else writer.startLine("}");

    BATree<String, StaticParam> temp_implicitTypeParameters = getImplicitTypeParameters();
    writer.startLine();
    writer.print("implicitTypeParameters = ");
    if (lossless) {
      writer.printSerialized(temp_implicitTypeParameters);
      writer.print(" ");
      writer.printEscaped(temp_implicitTypeParameters);
    } else { writer.print(temp_implicitTypeParameters); }

    String temp_genSymName = getGenSymName();
    writer.startLine();
    writer.print("genSymName = ");
    if (lossless) {
      writer.print("\"");
      writer.printEscaped(temp_genSymName);
      writer.print("\"");
    } else { writer.print(temp_genSymName); }

    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    writer.startLine();
    writer.print("staticParams = ");
    if (lossless) {
      writer.printSerialized(temp_staticParams);
      writer.print(" ");
      writer.printEscaped(temp_staticParams);
    } else { writer.print(temp_staticParams); }

    List<StaticArg> temp_staticArgs = getStaticArgs();
    writer.startLine();
    writer.print("staticArgs = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_staticArgs = true;
    for (StaticArg elt_temp_staticArgs : temp_staticArgs) {
      isempty_temp_staticArgs = false;
      writer.startLine("* ");
      if (elt_temp_staticArgs == null) {
        writer.print("null");
      } else {
        elt_temp_staticArgs.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_staticArgs) writer.print(" }");
    else writer.startLine("}");

    Option<List<Param>> temp_params = getParams();
    writer.startLine();
    writer.print("params = ");
    if (lossless) {
      writer.printSerialized(temp_params);
      writer.print(" ");
      writer.printEscaped(temp_params);
    } else { writer.print(temp_params); }
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
      _RewriteObjectExpr casted = (_RewriteObjectExpr) obj;
      Option<List<TypeRef>> temp_traits = getTraits();
      Option<List<TypeRef>> casted_traits = casted.getTraits();
      if (!(temp_traits == casted_traits || temp_traits.equals(casted_traits))) return false;
      List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
      List<? extends DefOrDecl> casted_defOrDecls = casted.getDefOrDecls();
      if (!(temp_defOrDecls == casted_defOrDecls || temp_defOrDecls.equals(casted_defOrDecls))) return false;
      BATree<String, StaticParam> temp_implicitTypeParameters = getImplicitTypeParameters();
      BATree<String, StaticParam> casted_implicitTypeParameters = casted.getImplicitTypeParameters();
      if (!(temp_implicitTypeParameters == casted_implicitTypeParameters || temp_implicitTypeParameters.equals(casted_implicitTypeParameters))) return false;
      String temp_genSymName = getGenSymName();
      String casted_genSymName = casted.getGenSymName();
      if (!(temp_genSymName == casted_genSymName)) return false;
      Option<List<StaticParam>> temp_staticParams = getStaticParams();
      Option<List<StaticParam>> casted_staticParams = casted.getStaticParams();
      if (!(temp_staticParams == casted_staticParams || temp_staticParams.equals(casted_staticParams))) return false;
      List<StaticArg> temp_staticArgs = getStaticArgs();
      List<StaticArg> casted_staticArgs = casted.getStaticArgs();
      if (!(temp_staticArgs == casted_staticArgs || temp_staticArgs.equals(casted_staticArgs))) return false;
      Option<List<Param>> temp_params = getParams();
      Option<List<Param>> casted_params = casted.getParams();
      if (!(temp_params == casted_params || temp_params.equals(casted_params))) return false;
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
    Option<List<TypeRef>> temp_traits = getTraits();
    code ^= temp_traits.hashCode();
    List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
    code ^= temp_defOrDecls.hashCode();
    BATree<String, StaticParam> temp_implicitTypeParameters = getImplicitTypeParameters();
    code ^= temp_implicitTypeParameters.hashCode();
    String temp_genSymName = getGenSymName();
    code ^= temp_genSymName.hashCode();
    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    code ^= temp_staticParams.hashCode();
    List<StaticArg> temp_staticArgs = getStaticArgs();
    code ^= temp_staticArgs.hashCode();
    Option<List<Param>> temp_params = getParams();
    code ^= temp_params.hashCode();
    return code;
  }
}
