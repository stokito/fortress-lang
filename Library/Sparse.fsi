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

api Sparse

trim[\T\](v:Array[\T,ZZ32\],l:ZZ32):Array[\T,ZZ32\]

object SparseVector[\T, nat n\](mem:Array[\(ZZ32,T),ZZ32\])
  extends Vector[\T,n\]
end

sparse[\T extends Number,nat n\](me:Array1[\RR64,0,n\]):SparseVector[\RR64,n\]

object Csr[\N extends Number, nat n, nat m\]
                 (rows:Array1[\SparseVector[\N,m\],0,n\])
  extends Matrix[\N,n,m\]
end

object Csc[\N extends Number, nat n, nat m\]
                 (cols:Array1[\SparseVector[\N,n\],0,m\])
  extends Matrix[\N,n,m\]
end

end
