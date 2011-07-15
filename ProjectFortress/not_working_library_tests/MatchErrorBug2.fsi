(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api MatchErrorBug2

trait StandardMinMax[\T\]
        comprises T
    opr MINMAX(self, other:T): (T,T)
end

trait StandardTotalOrder[\Self\] 
        extends { StandardMinMax[\ StandardTotalOrder[\Self\] \] }
        comprises Self
    opr MINMAX(self, other:StandardTotalOrder[\Self\]):
              (StandardTotalOrder[\Self\],StandardTotalOrder[\Self\])
end

end
