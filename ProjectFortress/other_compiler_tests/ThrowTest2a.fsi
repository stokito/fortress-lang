(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api ThrowTest2a

object TestFailCalled(s:String) extends UncheckedException
end

testFail(s:String): Zilch

end
