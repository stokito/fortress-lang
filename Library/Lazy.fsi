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

api Lazy

(* A Lazy[\T\] is a lazily evaluated T with memoization.
   It can eventually be started in parallel. *)
value trait Lazy[\T\] comprises { ... }
  getter val(): T
  run(): ()
end

delay[\T\](f: ()->T):Thunk[\T\]
noDelay[\T\](t:T):Thunk[\T\]

end
