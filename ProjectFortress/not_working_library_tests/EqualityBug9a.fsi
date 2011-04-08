(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api EqualityBug9a

trait Equality[\Self\] comprises Self
    opr =(self, other:Self): Boolean
end

opr =/=[\T extends Equality[\T\]\](a: T, b: T): Boolean

end
