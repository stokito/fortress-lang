package com.sun.fortress.interpreter.nodes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

/**
 * Class LValue, a component of the Node composite hierarchy.
 * Note: null is not allowed as a value for any field.
 * @version  Generated automatically by ASTGen at Sun Jul 08 09:49:14 EDT 2007
 */
public abstract class LValue extends AbstractNode {

  /**
   * Constructs a LValue.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public LValue(Span in_span) {
    super(in_span);
  }


  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  protected abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  protected abstract int generateHashCode();
}
