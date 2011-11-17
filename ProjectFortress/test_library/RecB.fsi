(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api RecB
import RecA.{...}

trait Even
  getter anOdd(): Odd
end

even(x:ZZ32): Boolean
(* odd(x:ZZ32): Boolean *)

end
