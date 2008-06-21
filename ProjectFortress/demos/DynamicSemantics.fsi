(*******************************************************************************
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
 ******************************************************************************)

api DynamicSemantics
import Map.{...}
import Syntax.{...}

(* Runtime value ***************************************************************)
trait RuntimeValue comprises { Val, FnValue }
  getValue(): Value
  toString(): String
  toSource(): String
end

object Val(val: Value) extends RuntimeValue end

object FnValue(fun: FnExpr, sigma: Map[\String, RuntimeValue\]) extends RuntimeValue end

(* Runtime value factories *****************************************************)
val(expr: Value): RuntimeValue
val(expr: FnExpr, sigma: Map[\String, RuntimeValue\]): RuntimeValue

(* Initialize the helper table *)
initDynamic(): ()

(* Evaluate a given program. *)
opr VDASH(p: Program): RuntimeValue

end
