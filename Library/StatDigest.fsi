(*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api StatDigest

object StatDigest(n:RR64, sum:RR64, sumSq:RR64)
    getter asString(): String
    getter average(): RR64
    (** Population variance (not sample variance!) *)
    getter variance(): RR64
    (** add n0 occurrences of value v0 to digest *)
    add(n0: RR64, v0: RR64): StatDigest
    (** combine two digests for the same statistic *)
    combine(other:StatDigest): StatDigest
end

emptySD : StatDigest

end
