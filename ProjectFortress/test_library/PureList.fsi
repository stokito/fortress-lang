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

api PureList

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

(* Warning: bogus parameters mentioning *two* undefined types!
   What we want are real constructorless object imports. *)
value object PureList[\E\]( it: FingerTree[\SizedBase[\E\]\] )
      extends { Generator[\E\] }
  getter left():Maybe[\E\]
  getter right():Maybe[\E\]
  getter extractLeft(): Maybe[\(E,PureList[\E\])\]
  getter extractRight(): Maybe[\(PureList[\E\],E)\]
  append(f:PureList[\E\]): PureList[\E\]
  addLeft(e:E):PureList[\E\]
  addRight(e:E):PureList[\E\]
  take(n:ZZ32): PureList[\E\]
  drop(n:ZZ32): PureList[\E\]
  opr [n:ZZ32]: E
  opr [n:Range[\ZZ32\]]: PureList[\E\]
  split(n:ZZ32): (PureList[\E\], PureList[\E\])
  split(): (PureList[\E\], PureList[\E\])
  reverse(): PureList[\E\]
  zip[\F\](other: PureList[\F\]): Generator[\(E,F)\]
  filter(p: E -> Boolean): PureList[\E\]
  opr =(self, other: PureList[\E\]): Boolean
  toString():String
end

emptyList[\E\](): PureList[\E\]
singleton[\E\](e:E): PureList[\E\]
opr [\E\]<| xs: E... |>: PureList[\E\]
list[\E\](g:Generator[\E\]):PureList[\E\]

object Concat[\E\]() extends Reduction[\ PureList[\E\] \]
  empty(): PureList[\E\]
  join(a:PureList[\E\], b:PureList[\E\]): PureList[\E\]
end

end
