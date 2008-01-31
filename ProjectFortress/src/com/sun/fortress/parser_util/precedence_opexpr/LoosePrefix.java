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

import com.sun.fortress.nodes.Op;

/**
 * Class LoosePrefix, a component of the OpExpr composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class LoosePrefix extends Prefix implements PrefixOpExpr {

   /**
    * Constructs a LoosePrefix.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public LoosePrefix(Op in_op) {
      super(in_op);
   }
}
