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

api ArrayList

(** Finger trees, based on Ralf Hinze and Ross Paterson's article, JFP
    16:2 2006.

    Why finger trees?  They're balanced and support nearly any
    operation we care to think of in optimal asymptotic time and
    space.  The code is niggly due to lots of cases, but fast in
    practice.

    It's also a trial for encoding type-based invariants in Fortress.
    Can we represent "array of size at most n"?  Not yet, but we ought
    to be able to do so.  This involves questions about the encoding
    of existentials, especially constrained existentials.

  *)

(** Generic list trait.
    We return a Generator for non-List-specific operations for which
    reuse of the Generator won't increase asymptotic complexity, but
    return a List in cases (such as map and filter) where it will.
*)
trait List[\E\] extends { Indexed[\E,ZZ32\], Equality }
        excludes { Number, HasRank }
  getter left():Maybe[\E\]
  getter right():Maybe[\E\]
  getter extractLeft(): Maybe[\(E,List[\E\])\]
  getter extractRight(): Maybe[\(List[\E\],E)\]
  getter indices(): ParRange[\ZZ32\]
  getter indexValuePairs(): Generator[\(ZZ32,E)\]
  map[\G\](f: E->G): List[\G\]
  append(f:List[\E\]): List[\E\]
  addLeft(e:E):List[\E\]
  addRight(e:E):List[\E\]
  take(n:ZZ32): List[\E\]
  drop(n:ZZ32): List[\E\]
  opr [n:ZZ32]: E
  opr [n:Range[\ZZ32\]]: List[\E\]
  split(n:ZZ32): (List[\E\], List[\E\])
  split(): (List[\E\], List[\E\])
  reverse(): List[\E\]
  zip[\F\](other: List[\F\]): Generator[\(E,F)\]
  filter(p: E -> Boolean): List[\E\]
  toString():String
  concatMap[\G\](f: E->List[\G\]): List[\G\]
end

(** Vararg factory for lists; provides aggregate list constants *)
opr [\E\]<| xs: E... |>: List[\E\]

(** Convert generator into list; can be used to desugar list
    comprehensions *)
list[\E\](g:Generator[\E\]):List[\E\]

(** Flatten a list of lists *)
concat[\E\](x:List[\List[\E\]\]):List[\E\]

emptyList[\E\](): List[\E\]
singleton[\E\](e:E): List[\E\]

object Concat[\E\]() extends Reduction[\ List[\E\] \]
  empty(): List[\E\]
  join(a:List[\E\], b:List[\E\]): List[\E\]
end

end
