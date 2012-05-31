(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Sparse

(** Trim array %v% to length %l%. **)
trim[\T\](v:Array[\T,ZZ32\],l:ZZ32):Array[\T,ZZ32\]

(** Sparse vector, represented as ordered sequence of index/value pairs.
    Absent entries are 0. *)
object SparseVector[\T, nat n\](mem:Array[\(ZZ32,T),ZZ32\])
  extends Vector[\T,n\]
end

(** %sparse% constructs a sparse vector from dense vector. *)
sparse[\T extends Number,nat n\](me:Array1[\RR64,0,n\]):SparseVector[\RR64,n\]

(** Compressed sparse row representation.
    A dense array sparse row vectors. *)
object Csr[\N extends Number, nat n, nat m\]
                 (rows:Array1[\SparseVector[\N,m\],0,n\])
  extends Matrix[\N,n,m\]
end

(** A compressed sparse column matrix is just the transpose of a Csr matrix. *)
object Csc[\N extends Number, nat n, nat m\]
                 (cols:Array1[\SparseVector[\N,n\],0,m\])
  extends Matrix[\N,n,m\]
end

end
