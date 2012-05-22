(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api RecA
import RecB.{...}

trait Odd
  getter anEven(): Even
end

odd(x:ZZ32): Boolean
(* even(x:ZZ32): Boolean *)

end
