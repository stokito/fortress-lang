(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api ChunkedSparseArray

object ChunkedSparseArray[\T, nat n\](defaultValue : T) extends Array1[\T,0,n\]
    isSet(i:ZZ32): Boolean
    isSet0(i:ZZ32): Boolean
end

chunkedSparseArray[\T\](n:ZZ32,t:T): Array[\T,ZZ32\]

end
