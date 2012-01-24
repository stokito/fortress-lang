(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Lazy

(* A Lazy[\T\] is a lazily evaluated T with memoization.
   It can eventually be started in parallel. *)
value trait Lazy[\T\] comprises { ... }
  getter val(): T
  run(): ()
end

delay[\T\](f: ()->T):Lazy[\T\]
noDelay[\T\](t:T):Lazy[\T\]

end
