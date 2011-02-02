(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Shuffle
import List.{List}

(** Fair (albeit sequential) shuffle.  Do not fiddle with this (eg to
    parallelize it) unless you can provide a proof of fairness for the
    new algorithm! **)

(** In-place array shuffle **)
shuffle[\T\](a: Array[\T,ZZ32\]): ()

(** Copying shuffle of immutable array. **)
shuffle[\T\](a: ImmutableArray[\T,ZZ32\]): ImmutableArray[\T,ZZ32\]

(** Copying shuffle of list. **)
shuffle[\T\](l: List[\T\]): List[\T\]

end Shuffle
