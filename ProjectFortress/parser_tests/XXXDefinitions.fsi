(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api XXXDefinitions

x : ZZ32 = 3

f():() = ()

trait T
  m(): () = ()
end

object O
  var f: ZZ32 = 3
  m(): () = ()
end

end
