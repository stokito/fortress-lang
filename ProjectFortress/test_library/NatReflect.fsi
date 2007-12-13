(*******************************************************************************
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
 ******************************************************************************)

api NatReflect

(** Reflection for run-time integers into compile-time nat parameters.
 *
 *  Basically you can call the function reflect(n) and it will return
 *  an instance of NatParam.  But every instance of NatParam is an
 *  object N[\n\], so we can write a function which takes N[\n\] and
 *  pass it a NatParam; within that function n becomes a static nat
 *  parameter.
 *)
trait NatParam
  (* comprises { N[\n\] } where [\ nat n \] *)
  abstract getter toZZ() : ZZ32
  abstract getter toString(): String
end

value object N[\nat n\]() extends { NatParam }
end

(* Actually convert a ZZ32 into a NatParam. *)
reflect(z:ZZ32):NatParam

end
