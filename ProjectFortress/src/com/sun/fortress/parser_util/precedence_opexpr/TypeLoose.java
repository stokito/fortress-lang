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
 * Class TypeLoose, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class TypeLoose extends TypeInfixFrame {

   /**
    * Constructs a TypeLoose.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public TypeLoose(Op in_op, Option<List<TraitType>> in_throws,
                    Type in_arg) {
      super(in_op, in_throws, in_arg);
   }


   public <RetType> RetType accept(InfixFrameVisitor<RetType> visitor) { return visitor.forTypeLoose(this); }
   public void accept(InfixFrameVisitor_void visitor) { visitor.forTypeLoose(this); }

   /**
    * Implementation of toString that uses
    * {@link #output} to generated nicely tabbed tree.
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
      outputHelp(new TabPrintWriter(writer, 2));
   }

   public void outputHelp(TabPrintWriter writer) {
      writer.print("TypeLoose" + ":");
   }

   /**
    * Implementation of equals that is based on the values
    * of the fields of the object. Thus, two objects
    * created with identical parameters will be equal.
    */
   public boolean equals(java.lang.Object obj) {
      if (obj == null) return false;
      if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
         return false;
      } else {
         TypeLoose casted = (TypeLoose) obj;
         if (! (getOp().equals(casted.getOp()))) return false;
         if (! (getThrows().equals(casted.getThrows()))) return false;
         return true;
      }
   }

   /**
    * Implementation of hashCode that is consistent with
    * equals. The value of the hashCode is formed by
    * XORing the hashcode of the class object with
    * the hashcodes of all the fields of the object.
    */
   protected int generateHashCode() {
      int code = getClass().hashCode();
      code ^= getOp().hashCode();
      code ^= getThrows().hashCode();
      return code;
   }
}
