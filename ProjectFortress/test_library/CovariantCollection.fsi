(*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

api CovariantCollection

(** A special covariant indexed generator type.  Usage: create a
covariant indexed collection from an arbitrary Generator, then consume
it.  This type only needs to exist because we don't presently support
simple covariance (and this is problematic for constructing e.g. PureList,
ArrayList, and Arrayn types efficiently using a Reduction).

Basically, by constructing a CovariantCollection[\T\], we are
simultaneously computing the upper bound T of the collection's
constituents.  We need this upper bound before we can construct a
non-covariant collection of element type T.

From an algebraic standpoint, CovariantCollection reifies the Boom
algebra of generators (which can be seen as hylomorphisms over a Boom
algebra). **)

trait SomeCovariantCollection
(*        comprises { CovariantCollection[\T\] } *)
end

trait CovariantCollection[\T\]
        extends { ZeroIndexed[\T\], Indexed[\T,ZZ32\],
                  SomeCovariantCollection }
        comprises { ... }
    getter indices(): Generator[\ZZ32\]

    opr[rng:Range[\ZZ32\]]: CovariantCollection[\T\]
end

(* Try to get BottomTypes in. *)
cvEmpty[\T\](): CovariantCollection[\T\]

cvUnit[\T\](x:T): CovariantCollection[\T\]

scvUnit(x:Any): SomeCovariantCollection

cvJoin[\T,U\](l:CovariantCollection[\T\], r:CovariantCollection[\U\]):
        SomeCovariantCollection

object CVReduction extends Reduction[\SomeCovariantCollection\]
    empty(): SomeCovariantCollection
    join(l:SomeCovariantCollection, r:SomeCovariantCollection):
        SomeCovariantCollection
end

end
