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

package com.sun.fortress.interpreter.evaluator.types;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.useful.HasAt;


public class FTypeTrait extends FTraitOrObject {

    /**
     * Trait methods run in an environment that
     * was surrounding the trait, plus the parameters
     * and where-clause-types introduced in the trait
     * definition.  A trait method environment does
     * NOT contain the members (methods) of the trait
     * itself; those are obained by lookup from $self,
     * which is defined as part of method invocation.
     */
    BetterEnv methodEnv;

 public FTypeTrait(String name, BetterEnv interior, HasAt at) {
      super(name, interior, at);

   }

 protected void finishInitializing() {
     BetterEnv interior = getEnv();
     methodEnv = new BetterEnv(interior, interior.getAt());
     methodEnv.bless();
 }

 public BetterEnv getMethodExecutionEnv() {
     if (methodEnv == null) {
         throw new InterpreterError("Internal error, get of unset methodEnv");
     }
     return methodEnv;
 }

}
