(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Compiled3.w

trait T end
object O extends T end

f(): ()
f(x: O, y: T): ()
f(x: T, y: O): ()

end
