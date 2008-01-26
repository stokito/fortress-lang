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

package com.sun.fortress.parser_util.precedence_opexpr;

import java.util.List;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.PureList;
import edu.rice.cs.plt.tuple.Option;

/**
 * Class TypeInfixFrame, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class TypeInfixFrame extends Object implements InfixFrame {
   private final Op _op;
   private final Option<List<TraitType>> _throws;
   private final Type _arg;
   private int _hashCode;
   private boolean _hasHashCode = false;

   /**
    * Constructs a TypeInfixFrame.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public TypeInfixFrame(Op in_op, Option<List<TraitType>> in_throws,
                         Type in_arg) {
      super();
      _op = in_op;
      _throws = in_throws;
      _arg = in_arg;
   }

   public Op getOp() { return _op; }
   public Option<List<TraitType>> getThrows() { return _throws; }
    public Type getArg() { return _arg; }

   public abstract <RetType> RetType accept(InfixFrameVisitor<RetType> visitor);
   public abstract void accept(InfixFrameVisitor_void visitor);
   public abstract void outputHelp(TabPrintWriter writer);
   protected abstract int generateHashCode();
   public final int hashCode() {
      if (! _hasHashCode) { _hashCode = generateHashCode(); _hasHashCode = true; }
      return _hashCode;
   }
}
