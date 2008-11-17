/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.astgen;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.CodeGenerator;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.NodeInterface;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types.*;

/**
 * Produce a single argument constructor which takes a Span, and instantiates
 * each field to null, leaving clients to manually instantiate each field correctly.
 */
public class SingleSpanConstructorGenerator extends CodeGenerator {

  public SingleSpanConstructorGenerator(ASTModel ast) { super(ast); }

  public Iterable<Class<? extends CodeGenerator>> dependencies() { return IterUtil.empty(); }

  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {}

  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    // boolean hasSingleSpanConstructor = true;
    // boolean allDefaults = true;
    int nonDefault = 0;
    // Guaranteed to iterate at least once, all fields have Span.
    for (Field f : c.allFields(ast)) {
        //hasSingleSpanConstructor = false;
        //allDefaults &= f.defaultValue().isSome();
        if (f.defaultValue().isNone()) nonDefault++;
    }

    // hasSingleSpanConstructor |= allDefaults;

    // if (!hasSingleSpanConstructor) {
    // 1 is the Span field, which is known not to be defaulted
    if (nonDefault > 1) {
      writer.startLine("/**");
      writer.startLine(" * Single Span constructor, for template gap access.  Clients are ");
      writer.startLine(" * responsible for never accessing other fields than the gapId and ");
      writer.startLine(" * templateParams.");
      writer.startLine(" */");
      writer.startLine("protected " + c.name() + "(Span span) {");
      writer.indent();
      writer.startLine("super(span);");
      for (Field f : c.declaredFields(ast)) {
          String init;
          if (f.type() instanceof PrimitiveName) {
            if (f.type().name().equals("boolean")) { init = "false"; }
            else { init = "0"; }
          }
          else { init = "null"; }
          writer.startLine("_" + f.name() + " = " + init + ";");
        }
      writer.unindent();
      writer.startLine("}");
      writer.println();
    }
  }

  public void generateAdditionalCode() {}

}
