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

import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.useful.Fn;

// / type id = string node
public class Id extends AbstractNode {
    String name;

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Id)) {
            return false;
        }
        Id i = (Id) o;
        return getName().equals(i.getName());
    }

    public Id(Span span, String s) {
        super(span);
        name = s;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forId(this);
    }

    Id(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public int compareTo(Id o) {
        return name.compareTo(o.name);
    }
    /*
    */

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("Id:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    String temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    if (lossless) {
      writer.print("\"");
      writer.printEscaped(temp_name);
      writer.print("\"");
    } else { writer.print(temp_name); }
    writer.unindent();
  }
}
