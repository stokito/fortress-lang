(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api MatchErrorBug

trait StandardTotalOrder[\Self\] comprises Self
    opr MIN(self, other:Self): Self
    opr MAX(self, other:Self): Self
    opr MINMAX(self, other:Self): (Self,Self)
end

end
