(*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
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