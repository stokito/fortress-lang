(*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

api CompilerLibrary

(************************************************************
 * Value bindings
 ************************************************************)

(* Placed up front to work around bug #357 *)

(*
true : Boolean
false : Boolean
*)

(************************************************************
 * Simple Combinators
 ************************************************************)

ignore(_:Any):()

opr XOR(a:Boolean, b:Boolean):Boolean
opr OR(a:Boolean, b:Boolean):Boolean
opr AND(a:Boolean, b:Boolean):Boolean
(*
opr OR(a:Boolean, b:()->Boolean):Boolean
opr AND(a:Boolean, b:()->Boolean):Boolean
*)
opr NOT(a:Boolean):Boolean
opr ->(a: Boolean, b:Boolean):Boolean
(*
opr ->(a: Boolean, b:()->Boolean):Boolean
*)
opr <->(a: Boolean, b:Boolean):Boolean


(************************************************************
 * Simple Range support
 ************************************************************)

(* Just enough for counted loops for now. *)

__loop(g: GeneratorZZ32, body: ZZ32->()): ()

trait GeneratorZZ32
    seq(self): GeneratorZZ32
    loop(body:ZZ32->()): ()
end

(*
opr :(lo:ZZ32, hi:ZZ32): GeneratorZZ32
*)
opr #(lo:ZZ32, sz:ZZ32): GeneratorZZ32


end
