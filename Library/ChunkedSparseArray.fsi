(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

api ChunkedSparseArray

object ChunkedSparseArray[\T, nat n\](defaultValue : T) extends Array1[\T,0,n\]
    isSet(i:ZZ32): Boolean
    isSet0(i:ZZ32): Boolean
end

chunkedSparseArray[\T\](n:ZZ32,t:T): Array[\T,ZZ32\]

end
