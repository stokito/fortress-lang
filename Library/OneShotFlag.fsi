(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api OneShotFlag

object OneShot( canTryIt : Boolean )
    getter canTry(): Boolean
    tryOnce() : Boolean
end

noShot: OneShot

oneShot(): OneShot

end
