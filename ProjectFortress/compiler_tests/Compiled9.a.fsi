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
