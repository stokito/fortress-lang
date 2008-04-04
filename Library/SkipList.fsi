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

api SkipList
import PureList.{...}

(** A %SkipList% type consists of a root node and %pInverse = 1/p%,
    where the fraction %p% is used in the negative binomial distribution
    to select random levels for insertion.
**)
trait SkipList[\Key,Val,nat pInverse\]
    comprises { ... }

  getter toString():String

  (** Depracated.  Use %|self|% instead. *)
  getter size():ZZ32

  (** The number of values stored in this tree. *)
  opr |self| : ZZ32

  (** Given a search key, try to return a value that matches that key. *)
  search(k:Key):Maybe[\Val\]

  (** Add a (key, value) pair to this tree. *)
  add(k:Key, v:Val):SkipList[\Key,Val,pInverse\]

  (** Remove one (key, value) pair from this tree. *)
  remove(k:Key):SkipList[\Key,Val,pInverse\]

  (** Merge two skip trees. *)
  merge(other:SkipList[\Key,Val,pInverse\]):SkipList[\Key,Val,pInverse\]
end

(* Construct an empty skip list. *)
NewList[\Key,Val,nat pInverse\]():SkipList[\Key,Val,pInverse\]

end
