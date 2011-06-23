(*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api List

(** Array lists, immutable style (not the mutable Java \texttt{ArrayList} style).

    A %List% is an immutable segment of an immutable (really
    write-once) array.  The rest of the array may contain elements of
    lists which overlap this list, or may be free for future use.
    Every %List% includes two internal flags %canExtendLeft% and
    %canExtendRight%; if a flag is true we are permitted to add
    additional elements to the %List% in place by initializing
    additional elements of the array.  At most one instance sharing
    the same backing array will obtain permission to extend the array
    in this way; we atomically check and update the flag to guarantee
    this.  Having obtained permission to extend the list, that
    permission may be extended to future attempts to extend.

    Eventually the backing array fills and we must allocate a new
    backing array to accept new elements.  At the moment, we are not
    particularly careful to avoid stealing permission to extend for
    overflowing %||% operations.

    Note that because of this implementation, a %List% can be
    efficiently extended on either side, but only in a non-persistent
    way; if a single list is extended by two different calls to
    %addRight% or %||% then one of them must pay the cost of
    copying the list elements.

    Note also that the implementation has not yet been carefully
    checked for amortization, so it is quite likely there are a number
    of asymptotic infelicities.

    Finally, note that this is an efficient \emph{amortized} structure.  An
    individual operation may be quite slow due to copying work.

    Baking these off vs %PureList%s (which have good persistent behavior
    and non-amortized worst case behavior), they look very good in
    practice.
 **)
(******************** *)
(** Lists of some item type.  Used to collect elements of unknown type
    into a list whose element type is as specific as possible.  This
    should not be necessary in the presence of true type
    inference. **)
trait AnyList excludes { Number, HasRank }
        (** \vspace{-4ex} Not yet: ``%comprises List[\E\] where [\E\]%'' *)
    opr ||(self, f:AnyList): AnyList
    addLeft(e:Any): AnyList
    addRight(e:Any): AnyList
end

(** %List%.  We return a %Generator% for non-list-specific operations
    for which reuse of the %Generator% will not increase asymptotic
    complexity, but return a %List% in cases (such as %map% and
    %filter%) where it will.  Indexing on lists operates from the
    left, and ordering is lexicographic reading from the left. *)
trait List[\E\] extends { AnyList, LexicographicOrder[\List[\E\],E\] }
        excludes { Number, HasRank, String }
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
  (** the operator %||% returns a list containing the elements of %self% followed
      by the elements of %f% *)
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

(** Vararg factory for lists; provides aggregate list constants: *)
opr <|[\E\] xs: E... |>: List[\E\]
(** List comprehensions: *)
opr BIG <|[\T\]|>:Comprehension[\T,List[\T\],List[\T\],List[\T\]\]
opr BIG <|[\T\] g:Generator[\T\]|>:List[\T\]

opr BIG CONCAT[\T\](): BigReduction[\List[\T\],List[\T\]\]
opr BIG CONCAT[\T\](g: Generator[\List[\T\]\]):List[\T\]

(** Convert generator into list (simpler type than comprehension above): *)
list[\E\](g:Generator[\E\]):List[\E\]

(** Flatten a list of lists *)
concat[\E\](x:List[\List[\E\]\]):List[\E\]

emptyList[\E\](): List[\E\]

(** %emptyList[\E\](n)% allocates an empty list that can accept %n%
    %addRight% operations without reallocating the underlying storage. **)
emptyList[\E\](n:ZZ32): List[\E\]

singleton[\E\](e:E): List[\E\]

(** A reduction object for concatenating lists. *)
object Concat[\E\] extends MonoidReduction[\ List[\E\] \]
  empty(): List[\E\]
  join(a:List[\E\], b:List[\E\]): List[\E\]
end

transpose[\E,F\](xs: List[\(E,F)\]): (List[\E\], List[\F\])

end
