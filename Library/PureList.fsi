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

api PureList
import List.{SomeList}

(** Finger trees, based on Ralf Hinze and Ross Paterson's article,
    Journal of Funtional Programming 16:2 2006.

    These are api-compatible with the %List% library, except that they
    don't support covariant construction.  In most cases you should be
    able to replace an import of %List% by %PureList% or vice versa
    and see only performance differences between the two.

    Why finger trees?  They're balanced and support nearly any
    operation we care to think of in optimal asymptotic time and
    space.  The code is niggly due to lots of cases, but fast in
    practice.

    It's also a trial for encoding type-based invariants in Fortress.
    Can we represent "array of size at most n"?  Not yet, but we ought
    to be able to do so.  This involves questions about the encoding
    of existentials, especially constrained existentials.  If you're
    curious about the details of type-based invariants, the source
    code may prove instructive.
  *)
(******************** *)
(** %List%.  We return a %Generator% for non-list-specific operations
    for which reuse of the Generator won't increase asymptotic
    complexity, but return a List in cases (such as %map% and
    %filter%) where it will.  *)
trait List[\E\] extends { Equality[\E\], ZeroIndexed[\E\] }
        excludes { Number, HasRank }
  getter left():Maybe[\E\]
  getter right():Maybe[\E\]
  getter extractLeft(): Maybe[\(E,List[\E\])\]
  getter extractRight(): Maybe[\(List[\E\],E)\]
  append(f:List[\E\]): List[\E\]
  addLeft(e:E):List[\E\]
  addRight(e:E):List[\E\]
  take(n:ZZ32): List[\E\]
  drop(n:ZZ32): List[\E\]
  split(n:ZZ32): (List[\E\], List[\E\])
  split(): (List[\E\], List[\E\])
  reverse(): List[\E\]
  zip[\F\](other: List[\F\]): Generator[\(E,F)\]
  filter(p: E -> Boolean): List[\E\]
  toString():String
  concatMap[\G\](f: E->List[\G\]): List[\G\]
end

(** Vararg factory for lists; provides aggregate list constants *)
opr <|[\E\] xs: E... |>: List[\E\]
(** List comprehensions: *)
opr BIG <|[\T,U\] g: ( Reduction[\SomeList\], T->SomeList) -> SomeList|>: List[\U\]

(** Convert generator into list (simpler type than comprehension above): *)
list[\E\](g:Generator[\E\]):List[\E\]

(** Flatten a list of lists *)
concat[\E\](x:List[\List[\E\]\]):List[\E\]

emptyList[\E\](): List[\E\]
singleton[\E\](e:E): List[\E\]

object Concat[\E\] extends Reduction[\ List[\E\] \]
  empty(): List[\E\]
  join(a:List[\E\], b:List[\E\]): List[\E\]
end

end
