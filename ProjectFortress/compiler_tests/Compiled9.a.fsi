(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(* Multiple declarations for A, B and C in different namespaces.
   Within the declaration of trait C,
   method declarations shadow top-level value declarations,
   but trait names should still be in scope.
   This should be allowed. *)

trait A end
trait B extends A end
trait C
  A(): ()
  B(): ()
  B(a: A): B
  C(self): C
end

var A: String
B(a: A): B
