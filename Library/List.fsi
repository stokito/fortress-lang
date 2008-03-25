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

api List

(** Array Lists, immutable style (not the mutable Java ArrayList style).

    An ArrayList is an immutable segment of a mutable array.  The rest
    of the mutable array may contain elements of purelists in which
    this list is contained, or may be free for future use.  Every
    PureList includes a pointer to a flag canExtend; if this flag is
    true we are permitted to add additional elements to the PureList
    in place by writing into the mutable array.  At most one instance
    sharing the same backing array will obtain permission to extend
    the array in this way; we atomically check and update the flag to
    guarantee this.  Having obtained permission to extend the list,
    that permission may be extended to future attempts to extend.

    Eventually the backing array fills and we must allocate a new
    backing array to accept new elements.  At the moment we're not
    particularly careful to avoid stealing permission to extend for
    overflowing append operations.

    Note that because of this implementation, an ArrayList can be
    efficiently extended on the right (addRight) and appended to
    (append), but cannot be efficiently extended on the left
    (addLeft).

    Note also that the implementation hasn't yet been carefully
    checked for amortization, so it is quite likely there are a number
    of asymptotic infelicities in the implementation.

    Finally, note that this is an efficient *amortized* structure.  An
    individual operation may be quite slow due to copying work.

    Baking these off vs PureLists, they look very good in practice.
 **)

(** Lists of some item type.  Used to collect elements of unknown type
    into a list whose element type is as specific as possible. **)
trait SomeList excludes { Number, HasRank }
        (* comprises List[\E\] where [\E\] *)
    append(f:SomeList): SomeList
(*
    addLeft(e:Any): SomeList
    addRight(e:Any): SomeList
*)
end

(** Generic list trait.
    We return a Generator for non-List-specific operations for which
    reuse of the Generator won't increase asymptotic complexity, but
    return a List in cases (such as map and filter) where it will.
*)
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
opr BIG <|[\T,U\] g: ( Reduction[\SomeList\], T->SomeList) -> SomeList |>: List[\U\]

(** Convert generator into list; can be used to desugar list
    comprehensions *)
list[\E\](g:Generator[\E\]):List[\E\]

(** Flatten a list of lists *)
concat[\E\](x:List[\List[\E\]\]):List[\E\]

emptyList[\E\](): List[\E\]

(** emptyList[\E\](n) allocates an empty list that can accept n
    addRight operations without reallocating the underlying storage. **)
emptyList[\E\](n:ZZ32): List[\E\]

singleton[\E\](e:E): List[\E\]

(** A reduction object for concatenating lists. *)
object Concat[\E\] extends Reduction[\ List[\E\] \]
  empty(): List[\E\]
  join(a:List[\E\], b:List[\E\]): List[\E\]
end

(** Covariant Singleton function, for use with CVConcat. **)
cvSingleton(e:Any): SomeList

(** A reduction object for concatenating lists covariantly. *)
object CVConcat extends Reduction[\SomeList\]
  empty(): SomeList
  join(a:SomeList, b:SomeList): SomeList
end

end
