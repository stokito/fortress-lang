(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api PureList

import Maybe.{...}
import Util.{...}

(** Finger trees, based on Ralf Hinze and Ross Paterson's article,
    Journal of Funtional Programming 16:2 2006 \cite{fingerTree}.

    These are API-compatible with the %List% library, except that they
    do not support covariant construction.  In most cases, you should be
    able to replace an import of %List% by %PureList% or vice versa
    and see only performance differences between the two.

    Why finger trees?  They are balanced and support nearly any
    operation we care to think of in optimal asymptotic time and
    space.  The code is niggly due to lots of cases, but fast in
    practice.

    It is also a trial for encoding type-based invariants in Fortress.
    Can we represent ``array of size at most %n%''?  Not yet, but we ought
    to be able to do so.  This involves questions about the encoding
    of existentials, especially constrained existentials.  If you are
    curious about the details of type-based invariants, the source
    code may prove instructive.
  *)
(******************** *)
(** %List%.  We return a %Generator% for non-list-specific operations
    for which reuse of the %Generator% will not increase asymptotic
    complexity, but return a %List% in cases (such as %map% and
    %filter%) where it will.  Indexing on lists operates from the
    left, and ordering is lexicographic reading from the left. *)
trait List[\E\] 
  (** %left% and %extractLeft% return the leftmost element in the list
      (and in the latter case, the remainder of the list without its
      leftmost element).  They return %Nothing% if the list is empty.
      %right% and %extractRight% do the same with the rightmost
      element. *)
  getter left():Maybe[\E\]
  getter right():Maybe[\E\]
  getter extractLeft(): Maybe[\(E,List[\E\])\]
  getter extractRight(): Maybe[\(List[\E\],E)\]
  getter reverse(): List[\E\]
  (** %append% returns a list containing the elements of %self% followed
      by the elements of %f% *)
  append(f:List[\E\]): List[\E\]
  (** the operator %||% performs the %append% operation *)
  opr ||(self, other:List[\E\]): List[\E\]
  (** %addLeft% and %addRight% add an element to the left or right of
      the list, respectively *)
  addLeft(e:E):List[\E\]
  addRight(e:E):List[\E\]
  (** %take% returns the leftmost %n% elements of the list; if the
      list is shorter than this, the entire list is returned. *)
  take(n:ZZ32): List[\E\]
  (** %drop% drops the leftmost %n% elements of the list; if the list
      is shorter than this, an empty list is returned. *)
  drop(n:ZZ32): List[\E\]
  (** %l.split(n)% is equivalent to %(l.take(n),l.drop(n))%.  Note in
      particular that appending its results yields the original
      list. *)
  split(n:ZZ32): (List[\E\], List[\E\])
  (** %split% splits the list into two smaller lists.  If %|l| > 1%
      both lists will be non-empty. *)
  split(): (List[\E\], List[\E\])
  zip[\F\](other: List[\F\]): Generator[\(E,F)\]
  filter(p: E -> Boolean): List[\E\]
  (** %concatMap% is an in-place version of the %nest% method from
      %Generator%; it flattens the result into an actual list, rather than
      returning an abstract %Generator%. *)
  concatMap[\G\](f: E->List[\G\]): List[\G\]
end

(*

(** Vararg factory for lists; provides aggregate list constants: *)
opr <|[\E\] xs: E... |>: List[\E\]
(** List comprehensions: *)
opr BIG <|[\T\]|>: Comprehension[\T,List[\T\],AnyCovColl,AnyCovColl\]
opr BIG <|[\T\] g:Generator[\T\]|>: List[\T\]

opr BIG CONCAT[\T\](): BigReduction[\List[\T\],List[\T\]\]
opr BIG CONCAT[\T\](g: Generator[\List[\T\]\]):List[\T\]

(** Convert generator into list (simpler type than comprehension above): *)
list[\E\](g:Generator[\E\]):List[\E\]

(** Flatten a list of lists *)
concat[\E\](x:List[\List[\E\]\]):List[\E\]

emptyList[\E\](): List[\E\]
singleton[\E\](e:E): List[\E\]

(** A reduction object for concatenating lists. *)
object Concat[\E\] extends MonoidReduction[\ List[\E\] \]
  empty(): List[\E\]
  join(a:List[\E\], b:List[\E\]): List[\E\]
end

*)

end
