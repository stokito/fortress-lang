(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
end

value object N[\nat n\] extends { NatParam }
end

(* Actually convert a ZZ32 into a NatParam. *)
reflect(z:ZZ32):NatParam

end
