(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SkipList
import PureList.{...}

(** A %SkipList% type consists of a root node and %pInverse = 1/p%,
    where the fraction %p% is used in the negative binomial distribution
    to select random levels for insertion.
**)
trait SkipList[\Key,Val,nat pInverse\]
    comprises { ... }

  (** Depracated.  Use %|self|% instead. *)
  getter size():ZZ32

  (** The number of values stored. *)
  opr |self| : ZZ32

  (** Given a key, try to return a value that matches that key. *)
  search(k:Key):Maybe[\Val\]

  (** Add a (key, value) pair. *)
  add(k:Key, v:Val):SkipList[\Key,Val,pInverse\]

  (** Remove a (key, value) pair. *)
  remove(k:Key):SkipList[\Key,Val,pInverse\]

  (** Merge two Skip Lists. *)
  merge(other:SkipList[\Key,Val,pInverse\]):SkipList[\Key,Val,pInverse\]
end

(* Construct an empty Skip List. *)
NewList[\Key,Val,nat pInverse\]():SkipList[\Key,Val,pInverse\]

end
