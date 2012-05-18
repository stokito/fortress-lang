(*******************************************************************************
   Copyright 2009, Oracle and/or its affiliates.
   All rights reserved.


   Use is subject to license terms.

   This distribution may include materials developed by third parties.

******************************************************************************)

trait A
  coerce(x: D)
end

trait B
  coerce(x: C)
end

object C extends A end
object D extends B end
