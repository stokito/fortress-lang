(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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

object Val(v: Value) extends RuntimeValue end

object FnValue(fun: FnExpr, sigma: Map[\String, RuntimeValue\]) extends RuntimeValue end

(* Runtime value factories *****************************************************)
val(expr: Value): RuntimeValue
val(expr: FnExpr, sigma: Map[\String, RuntimeValue\]): RuntimeValue

(* Initialize the helper table *)
initDynamic(): ()

(* Evaluate a given program. *)
opr VDASH(p: Program): RuntimeValue

end
