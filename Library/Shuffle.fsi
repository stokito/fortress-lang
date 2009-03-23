(*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
