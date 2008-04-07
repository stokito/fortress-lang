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

api FortressLibrary

(** The %builtinPrimitive% function is actually recognized as a special piece
    of built-in magic by the Fortress interpreter.  The %javaClass%
    argument names a Java Class which is a subclass of
    \texttt{com.sun.fortress.interpreter.glue.NativeApp}, which provides code
    for the closure which is used in place of the call to
    %builtinPrimitive%.  Meanwhile all the necessary type information,
    argument names, etc. must be declared here in Fortress-land.  For
    examples, see the end of this file.

    In practice, if you are extending the interpreter you will probably
    want to extend \texttt{com.sun.fortress.interpreter.glue.NativeFn0,1,2,3,4}
    or one of their subclasses defined in
    \texttt{com.sun.fortress.interpreter.glue.primitive}.  These types are
    generally easier to work with, and the boilerplate packing and
    unpacking of values is done for you.
**)
builtinPrimitive[\T\](javaClass:String):T

(************************************************************
* \subsection*{Simple Combinators}
************************************************************

Casting *)

cast[\T extends Any\](x:Any):T

instanceOf[\T extends Any\](x:Any):Boolean

(** Useful functions *)

ignore(_:Any):()

identity[\T extends Any\](x:T):T

(* Should we depracate tuple and use identity instead?  Decision: no. *)
tuple[\T\](x:T):T

(* Function composition *)
opr COMPOSE[\A,B,C\](f: B->C, g: A->B): A->C

fail(s:String)

(************************************************************
* \subsection*{Control over locality and location}
************************************************************

At the moment, all Fortress objects are immediately shared by default. *)

shared[\T extends Any\](x:T): T

isShared(x:Any): Boolean

localize[\T extends Any\](x:T): T

(* copy is presently unimplemented.
copy[\T extends Any\](x:T): T
*)

trait Region extends Equality[\Region\]
    getter toString(): String
    isLocalTo(r: Region): Boolean
end

object Global extends Region end

region(a:Any): Region

here(): Region

(************************************************************
* \subsection*{Equality and ordering}
************************************************************)

opr =(a:Any, b:Any):Boolean

opr =/=(a:Any, b:Any):Boolean

trait Equality[\Self extends Equality[\Self\]\]
        excludes { Number } (* Until Number is an actual type. *)
    abstract opr =(self, other:Self): Boolean
end

(** Total ordering *)

object LexicographicPartialReduction extends Reduction[\Comparison\]
    empty(): Comparison
    join(a:Comparison, b:Comparison):Comparison
end

object LexicographicReduction extends Reduction[\TotalComparison\]
    empty(): TotalComparison
    join(a:TotalComparison, b:TotalComparison):TotalComparison
end

trait Comparison
        extends { StandardPartialOrder[\Comparison\] }
        comprises { Unordered, TotalComparison }
    abstract getter toString(): String
    opr =(self, other:Comparison): Boolean
    opr LEXICO(self, other:Comparison): Comparison
    abstract opr INVERSE(self): Comparison
end

(** Unordered is the outcome of %a CMP b% when %a% and %b% are partially
    ordered and no ordering relationship exists between them. **)
object Unordered extends Comparison
    getter toString(): String
    opr =(self, other:Unordered): Boolean
    opr <(self, other:Comparison): Boolean
    opr INVERSE(self): Comparison
end

(** %TotalComparison% is both a partial order (including %Unordered%)
    and a total order (%TotalComparison% alone).  Its method
    definitions avoid ambiguities between these orderings. *)
trait TotalComparison
        extends { Comparison, StandardTotalOrder[\TotalComparison\] }
        comprises { LessThan, EqualTo, GreaterThan }
    opr =(self, other:Comparison): Boolean
    opr CMP(self, other:Unordered): Boolean
    opr >=(self, other:Unordered): Boolean
    opr >=(self, other:Comparison): Boolean
    opr LEXICO(self, other:TotalComparison): TotalComparison
    opr LEXICO(self, other:()->TotalComparison): TotalComparison
    abstract opr INVERSE(self): TotalComparison
end

object LessThan extends TotalComparison
    getter toString(): String
    opr =(self, other:LessThan): Boolean
    opr CMP(self, other:LessThan): Comparison
    opr CMP(self, other:TotalComparison): TotalComparison
    opr <(self, other:LessThan): Boolean
    opr <(self, other:TotalComparison): Boolean
    opr INVERSE(self): TotalComparison
end

object GreaterThan extends TotalComparison
    getter toString(): String
    opr =(self, other:GreaterThan): Boolean
    opr CMP(self, other:GreaterThan): Comparison
    opr CMP(self, other:TotalComparison): TotalComparison
    opr <(self, other:TotalComparison): Boolean
    opr INVERSE(self): TotalComparison
end

object EqualTo extends TotalComparison
    getter toString(): String
    opr =(self, other:EqualTo): Boolean
    opr CMP(self, other:TotalComparison): TotalComparison
    opr <(self, other:LessThan): Boolean
    opr <(self, other:TotalComparison): Boolean
    opr LEXICO(self, other:TotalComparison): TotalComparison
    opr LEXICO(self, other:()->TotalComparison): TotalComparison
    opr INVERSE(self): TotalComparison
end

(** StandardPartialOrder is partial ordering using %<%,%>%,%<=%,%>=%,%=%, and %CMP%.
    This is primarily for floating-point values.  Minimal complete
    definition: %CMP% or %{ <, = }%. **)
trait StandardPartialOrder[\Self extends StandardPartialOrder[\Self\]\]
        excludes { Number } (* Until Number is an actual type. *)
    opr CMP(self, other:Self): Comparison
    opr <(self, other:Self): Boolean
    opr >(self, other:Self): Boolean
    opr =(self, other:Self): Boolean
    opr <=(self, other:Self): Boolean
    opr >=(self, other:Self): Boolean
end

(** StandardTotalOrder is the usual total order using %<%,%>%,%<=%,%>=%,%=%, and %CMP%.
    Most values that define a comparison should do so using this.
    Minimal complete definition: either %CMP% or %<% (it is advisable to
    define %=% in the latter case). **)
trait StandardTotalOrder[\Self extends StandardTotalOrder[\Self\]\]
        extends StandardPartialOrder[\Self\]
        excludes { Number } (* Until Number is an actual type. *)
    opr CMP(self, other:Self): Comparison
    opr >=(self, other:Self): Boolean
end

(************************************************************
 * \subsection*{Assertions}
 *)
assert(flag:Boolean): ()

assert(flag: Boolean, failMsg: String): ()

(** This version of %assert% checks the equality of its first two arguments;
    if unequal it includes the remaining arguments in its error indication. *)
assert(x:Any, y:Any, failMsg: Any...): ()

(************************************************************
* \subsection*{Generator support}
************************************************************

**
 * We say an object which extends %Generator[\T\]% ``generates objects of
 * type %T%.''
 *
 * Generators are used to express iteration in Fortress.  Every generated
 * expression in Fortress (eg. for loop and comprehension) is desugared into
 * calls to methods of Generator, chiefly the %generate% method.
 *
 * Every generator has a notion of a ``natural order'' (which by default is
 * unspecified), which describes the ordering of reduction operations,
 * and also describes the order in which elements are produced by the
 * sequential version of the same generator (given by the
 * %seq(self)% method).  The default implementation of
 * %seq(self)% guarantees that these orders will match.
 *
 * Note in particular that the natural order of a Generator must be
 * consistent; that is, if %a SEQV b% then %a% and %b%
 * must yield %SEQV% elements in the same natural order.  However,
 * note that unless a type specifically documents otherwise, no
 * particular element ordering is guaranteed, nor is it necessary to
 * guarantee that %a=b% have the same natural order when equality is
 * defined.
 *
 * Note that more complex derived generators are specified further down
 * in the definition of Generator.  These have the same notions of
 * natural order and by default are defined in terms of the
 * %generate% method.
 *
 * Minimal complete definition of a %Generator% is the %generate%
 * method.  *)
trait Generator[\E\]
    excludes { Number }
    (** %generate% is the core of %Generator%.  It generates elements of
        type %E% and passes them to the %body% function.  This generation
        can occur using any mixture of serial and parallel execution
        desired by the author of the generator; by default uses of a
        generator must assume every call to %body% occurs in
        parallel.

        The results of generation are combined using the reduction
        object %R%, which specifies a monoidal operation (associative
        and with an identity).  Body results must be combined together
        following the natural order of the generator.  The author of
        the generator is free to use the identity element anywhere
        desired in this computation, and to group reductions in any
        way desired; if no elements are generated, the identity must be
        returned. *)
    abstract generate[\R\](r: Reduction[\R\], body: E->R): R

    (** \textbf{Transforming generators into new generators} *)

    (** %map% applies a function %f% to each element generated and yields
        the result.  The resulting generator must have the same
        ordering and cross product properties as the generator from
        which it is derived. *)
    map[\G\](f: E->G): Generator[\G\]
    (** %seq% produces a sequential version of the same generator, in
        which elements are produced in the generator's natural order. *)
    seq(self): SequentialGenerator[\E\]

    (** Nesting of two generators; the innermost is data-dependent
        upon the outer one.  This is specifically designed to be
        overloaded so that the combined generators have properties
        appropriate to the pairing.  Because of the data dependency,
        the natural order of the nesting is the natural order of the
        inner generators, in the natural order the outer nesting
        produces them.  So, for example, if we write:
          %(0#3).nest[\ZZ32\](fn (n:ZZ32):Generator[\ZZ32\] => ((n 100)#4))
        then the natural order is 0,1,2,3,100,101,102,103,200,201,202,203.
     **)
    nest[\G\](f: E -> Generator[\G\]): Generator[\G\]

    (** Cross product of two generators.  This is specifically
        designed to be overloaded, such that pairs of independent
        generators can be combined to produce a generator which
        possibly interleaves the iteration spaces of the two
        generators.  For example, we might combine
        % (0#16).cross(0#32)
        such that it first splits the second range in half, then the
        first range in half, then the second, and so forth.

        Consider a grid for which the rows are labeled from top to
        bottom with the elements of %a% in natural order, and the
        columns are labeled from left to right with the elements of %g%
        in natural order.  Each point in the grid corresponds to a
        pair %(a,b)% that must be generated by
        %self.cross(g)%.  In the natural
        order of the cross product, an element must occur after those
        that lie above and to the left of it in the grid.  By default
        the natural order of the cross product is left-to-right, top
        to bottom.  Programmers must not rely on the default order,
        except that cross products involving one or more sequential
        generators are always performed in the default order.  Note
        that this means that the following have the same natural
        order:
          %seq(a).cross(b)
          %a.cross(seq(b))
          %seq(a).cross(seq(b))
        But %seq(a.cross(b))% may have a different natural order. *)
    cross[\G\](g: Generator[\G\]): Generator[\(E,G)\]

    (** \textbf{Derived generation operations} *)

    (** %mapReduce% is equivalent to %generate%, but takes an explicit %join%
        and %zero% which can have any type.  It still assumes %join% is
        associative and that %zero% is the identity of %join%. **)
    mapReduce[\R\](body: E->R, join:(R,R)->R, zero:R): R
    (** %reduce% works much like %generate% or %mapReduce%,
        but has no body expression. **)
    reduce(j:(E,E)->E, z:E):E
    reduce(r: Reduction[\E\]):E
    (** %loop% is a version of %generate% which discards the %()% results
        of the body computation.  It can be used to translate
        reduction-variable-free %for% loops. **)
    loop(f:E->()): ()

    (** %x IN self% holds if $x$ is generated by this generator.  By
        default this is implemented using the naive $O(n)$ algorithm. **)
    opr IN(x:E, self): Boolean
end

(** The following stubs exist as a temporary workaround to shortcomings
   in interpreter type inference, and are intended for use by
   reduction desugaring. *)
__generate[\E,R\](g:Generator[\E\], r: Reduction[\R\], b:E->R): R

__nest[\E1,E2\](g:Generator[\E1\], f:E1->Generator[\E2\]):Generator[\E2\]

__map[\E,R\](g:Generator[\E\], f:E->R): Generator[\R\]

trait SequentialGenerator[\E\] extends { Generator[\E\] }
    seq(self)
    map[\G\](f: E->G): SequentialGenerator[\G\]
    nest[\G\](f: E -> Generator[\G\]): Generator[\G\]
    cross[\F\](g:Generator[\F\]): Generator[\(E,F)\]
end

(** %opr IN% returns true if any element generated by its second argument is
    %=% to its first argument.  %x NOTIN g% is simply %NOT (x IN g)%. **)
opr NOTIN[\E\](x: E, this: Generator[\E\]): Boolean

sequential[\T\](g:Generator[\T\]):SequentialGenerator[\T\]


(************************************************************
* \subsection{The Maybe type}
* \seclabel{maybe-type}
************************************************************

** This trait makes excludes work without where clauses, and allows opr =
   to remain non-parametric. *)
value trait MaybeType extends Equality[\MaybeType\] excludes Number
        (** not yet: ``%comprises Maybe[\T\] where [\T\]%'' *)
    abstract getter isJust() : Boolean
    opr =(self, other:MaybeType): Boolean
end

(** %Maybe% represents either %Nothing% or a single element of
    type %T% (%Just[\T\]%), which may
    be retrieved by calling %unJust%.  An object of type
    %Maybe[\T\]% can be used as a
    generator; it is either empty (%Nothing%) or generates the single
    element yielded by %unJust%, so there is no issue of canonical
    order or parallelism.

    Thus, %Just[\T\]% can be used as a
    single-element generator, and %Nothing% can be used as an
    empty generator. *)
value trait Maybe[\T\]
        extends { MaybeType, SequentialGenerator[\T\], ZeroIndexed[\T\] }
        comprises { Nothing[\T\], Just[\T\] }
    abstract getter unJust() : T throws NotFound
    abstract unJust(t:T): T
    abstract maybe[\R\](nothingAction: ()->R, justAction: T->R): R
end

value object Just[\T\](x:T) extends Maybe[\T\]
    getter size(): ZZ32
    getter toString():String
    getter isJust(): Boolean
    getter unJust(): T
    opr |self| : ZZ32
    unJust(_:T): T
    generate[\R\](_:Reduction[\R\],m:T->R): R
    opr[i:ZZ32]:T
    opr[r:Range[\ZZ32\]]:Maybe[\T\]
    map[\G\](f: T->G): Just[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\(T,G)\]
    mapReduce[\R\](m: T->R, _:(R,R)->R, _:R): R
    reduce(_:(T,T)->T, _:T):T
    reduce(_: Reduction[\T\]):T
    loop(f:T->()): ()
    maybe[\R\](_: ()->R, justAction: T->R): R
    opr =(self,o:Just[\T\]): Boolean
end

(** %Nothing% will become a non-parametric singleton when we get where
    clauses working. *)
value object Nothing[\T\] extends Maybe[\T\]
    getter size(): ZZ32
    getter isJust(): Boolean
    getter unJust(): T
    getter toString():String
    opr |self| : ZZ32
    unJust(t:T):T
    generate[\R\](r:Reduction[\R\],_:T->R): R
    opr[_:ZZ32]: T
    opr[r:Range[\ZZ32\]]: Nothing[\T\]
    map[\G\](f: T->G): Nothing[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\(T,G)\]

    mapReduce[\R\](_: T->R, _:(R,R)->R, z:R): R
    reduce(_:(T,T)->T, z:T):T
    reduce(r: Reduction[\T\]):T
    loop(f:T->()): ()
    maybe[\R\](nothingAction: ()->R, _: T->R): R
    opr =(self,_:Nothing[\T\])
end

(************************************************************
* \subsection*{Exception hierarchy}
************************************************************)

trait Exception comprises { UncheckedException, CheckedException }
end

(* Exceptions which are not checked *)

trait UncheckedException extends Exception excludes CheckedException
end

object FailCalled(s:String) extends UncheckedException
  toString(): String
end

object DivisionByZero extends UncheckedException
end

object UnpastingError extends UncheckedException
end

object CallerViolation extends UncheckedException
end

object CalleeViolation extends UncheckedException
end

object TestFailure extends UncheckedException
end

object ContractHierarchyViolation extends UncheckedException
end

object NoEqualityOnFunctions extends UncheckedException
end

object InvalidRange extends UncheckedException
end

object ForbiddenException(chain : Exception) extends UncheckedException
end

(* Should this be called ``IndexNotFound'' instead? *)
object NotFound extends UncheckedException
end

object IndexOutOfBounds extends UncheckedException
end

object NegativeLength extends UncheckedException
end

object IntegerOverflow extends UncheckedException
end

object RationalComparisonError extends UncheckedException
end

object FloatingComparisonError extends UncheckedException
end

(* Checked Exceptions *)

trait CheckedException extends Exception excludes UncheckedException
end

object CastError extends CheckedException
end

object IOFailure extends CheckedException
end

object MatchFailure extends CheckedException
end

(* SetsNotDisjoint? *)
object DisjointUnionError extends CheckedException
end

object APIMissing extends CheckedException
end

object APINameCollision extends CheckedException
end

object ExportedAPIMissing extends CheckedException
end

object HiddenAPIMissing extends CheckedException
end

object TryAtomicFailure extends CheckedException
end

(* Should take a spawned thread as an argument *)
object AtomicSpawnSynchronization extends {UncheckedException}
end

(************************************************************
* \subsection*{Array support}
************************************************************)

trait HasRank extends Equality[\HasRank\] excludes { Number, MaybeType }
  (** not yet: ``% comprises Array[\T,E,I\] where [\T,E,I\]{ T extends Array[\T,E,I\] }%'' *)
  abstract rank():ZZ32
  opr =(self, other:HasRank): Boolean
end

(* Declared Rank-n-ness *)
trait Rank[\ nat n \] extends HasRank
  rank():ZZ32
end

(** Potemkin exclusion traits.  Really we just want to say that
    ``%Rank[\n\] excludes Rank[\m\] where { m =/= n }%'', but we cannot yet. *)

trait Rank1 extends { Rank[\1\]} excludes { Rank2, Rank3, Number, String }
end

trait Rank2 extends { Rank[\2\]} excludes { Rank3, Number, String }
end

trait Rank3 extends { Rank[\3\]} excludes { Number, String }
end

(** The trait %Indexed_i[\n\]%
   indicates that something has an $i^{th}$ dimension of size $n$.  In
   general, anything which extends %Indexed_i% must also
   extend %Indexed_j% for $j < i$. *)

trait Indexed1[\ nat n \] end

trait Indexed2[\ nat n \] end

trait Indexed3[\ nat n \] end

(** The indexed trait indicates that an object of type %T% can be
indexed using type %I% to obtain elements with type %E%.

An object %i% that is an instance of %Indexed% defines three basic things:

  The indexing operator %opr []%, which must be defined for every instance of
    the type.

  A suite of generators: %i.indices% generates the index space of the
    array.  %i% itself generates the values contained at those indices.
    %i.indexValuePairs% yields pairs of %(index,val)%.  All of these
    share the same natural order.  It is necessary to define one of
    %indices()% and %indexValuePairs()%, in addition to %generate()% (but
    the latter requirement can be dispensed by instead extending
    %DelegatedIndexed%).

  A set of utility functions, %assign%, %fill%, and %copy%.  Only %fill% and
    %copy% need to be defined.
**)
trait Indexed[\E, I\] extends Generator[\E\]
    (** %isEmpty()% indicates whether there are any valid indices.  It is
        defined as %|self| = 0%. *)
    getter isEmpty(): Boolean
    (** %size()% is depracted; use %|self|% instead. *)
    abstract getter size(): ZZ32
    (** %bounds()% yields a range of indices that are valid for the
        indexed generator. *)
    abstract getter bounds(): FullRange[\I\]
    (** %indexValuePairs()% generates the elements of the indexed object
        (exactly those elements that are generated by the object itself),
        but each element is paired with its index.  When we obtain
        %(i,v)% from %indexValuePairs()% we know that:
        \begin{itemize}
           \item %self[i] = v%
           \item the %i% are distinct and %i IN bounds()%
           \item stripping away the %i% yields exactly the results of %v <- self%
        \end{itemize}
        This generator attempts to follow the structure of the
        underlying object as closely as possible.  *)
    getter indexValuePairs(): Indexed[\(I,E),I\]
    (** %indices()% yields the indices corresponding to the elements of
        the indexed object---it corresponds to the index component of
        %indexValuePairs()%.  This may in general be a subset of all the
        valid indices represented by %bounds()%.  This generator
        attempts to follow the structure of the underlying object as
        closely as possible. *)
    getter indices(): Indexed[\I,I\]
    (** %|self|% indicates the number of distinct valid indices that may
        be passed to indexing operations. *)
    abstract opr |self| : ZZ32
    (** Indexing.  %i IN bounds()% must hold. *)
    abstract opr[i:I] : E

    (** Indexing by ranges.  The results are 0-based when the
        underlying index type has a notion of 0.  This ensures
        consistency of behavior between types such as vectors that
        ``only'' support 0-indexing and types such as arrays that permit
        other choices of lower bounds.  The easiest way to write the
        index by ranges operation for an instance of %Indexed% is to
        take advantage of indexing on the ranges themselves by writing
        %(bounds())[r]% in order to narrow and bounds check the range %r%
        and obtain a closed range of indices on the underlying
        data. **)
    abstract opr[r:Range[\I\]] : Indexed[\E,I\]
    opr[_:OpenRange[\Any\]] : Indexed[\E,I\]

    (** Roughly speaking, %ivmap(f)% is equivalent to
        %indexValuePairs.map(f)%.  However %ivmap% is not
        merely a convenient shortcut.  It is actually intended to
        create a copy of the underlying indexed structure when that is
        appropriate.

        The usual %map% function in %Generator% should do the same (and
        does for the instances in this library).  Copying can be bad
        for space, but is complexity-preserving if the mapped
        generator is used more than once. **)
    ivmap[\R\](f:(I,E)->R): Indexed[\R, I\]
    map[\R\](f:E->R): Indexed[\R, I\]
end

trait ZeroIndexed[\E\] extends Indexed[\E,ZZ32\]
    bounds(): FullRange[\ZZ32\]
    zip[\F\](g:ZeroIndexed[\F\]):ZeroIndexed[\(E,F)\]
end

object DefaultZip[\E,F\](e:ZeroIndexed[\E\],f:ZeroIndexed[\F\])
        extends { ZeroIndexed[\(E,F)\], DelegatedIndexed[\(E,F),ZZ32\] }
    getter size(): ZZ32
    getter indices(): Generator[\ZZ32\]
    opr |self| : ZZ32
    opr[i:ZZ32]:(E,F)
    opr[r:Range[\ZZ32\]] : ZeroIndexed[\(E,F)\]
end

trait LexicographicOrder[\T extends LexicographicOrder[\T,E\],E\]
        extends { StandardTotalOrder[\T\], ZeroIndexed[\E\] }
    opr CMP(self, other:T): TotalComparison
    (** We give a specialized version of %=% because it can fail faster
        than %CMP% by checking sizes early. **)
    opr =(self,other:T): Boolean
end

toArray[\E\](g:Indexed[\E,ZZ32\]): Array[\E,ZZ32\]

(** %DelegatedIndexed% is an indexed generator that has recourse to
    another indexed generator internally.  By default, this in turn
    is defined in terms of %indexValuePairs()%.  Thus, it is only
    necessary to define either %indexValuePairs()% or %indices()%.

    This class is designed for convenience; it should not be used as a
    type in running code, but only as a supertype in lieu of %Indexed%.
**)
trait DelegatedIndexed[\E,I\] extends Indexed[\E,I\]
    getter generator(): Generator[\E\]
    getter size(): ZZ32
    opr |self| : ZZ32
    generate[\R\](r: Reduction[\R\], body: E->R): R
    seq(self): SequentialGenerator[\E\]
    cross[\G\](g: Generator[\G\]): Generator[\(E,G)\]
    mapReduce[\R\](body: E->R, join:(R,R)->R, zero:R): R
    reduce(j:(E,E)->E, z:E):E
    reduce(r: Reduction[\E\]):E
    loop(f:E->()): ()
end

(** The %MutableIndexed% trait is an indexed trait whose elements can be
    mutated using indexed assignment.  Right now, we are using this type
    in a somewhat dangerous way, since, for example,
    %Array1[\E,b0,s0\]% extends both
    %Indexed[\Array1[\E,b0,s0\],E,ZZ32\]%
    and
    %Indexed[\Array[\E,ZZ32\],E,ZZ32\]%.
    We will need to find a solution to this at some point.  **)
trait MutableIndexed[\E, I\]
        extends { Indexed[\E,I\] }
    abstract opr[i:I]:=(v:E) : ()

    (** For %Ranged% assignment, the extents of %r% and %v.bounds()% must
        match, but the lower bounds need not. **)
    abstract opr[r:Range[\I\]]:=(v:Indexed[\E,I\]) : ()
    opr[_:OpenRange[\Any\]]:=(v:Indexed[\E,I\]) : ()
end

(** Array whose bounds are implicit rather than static, and which may
    be either mutable or immutable. *)
trait ReadableArray[\E,I\]
        extends { HasRank, Indexed[\E,I\] }
        comprises { Array[\E,I\], ImmutableArray[\E,I\] }

    (** Indexed functionality with more specific type information. **)
    abstract opr[r:Range[\I\]] : ReadableArray[\E,I\]
    abstract opr[_:OpenRange[\Any\]] : ReadableArray[\E,I\]
    abstract ivmap[\R\](f:(I,E)->R): ReadableArray[\R, I\]
    abstract map[\R\](f:E->R): ReadableArray[\R, I\]

    (** Shift the origin of an array.  This should yield a new view of
        the same array; that is, initialization and/or update to either will
        be reflected in the other. **)
    abstract shift(newOrigin:I):ReadableArray[\E,I\]

    (** Initialize element at index %i% with value %v%.  This should occur
        once, before any other access or assignment occurs to element
        %i%.  An error will be signaled if an uninitialized element is
        read or an initialized element is re-initialized. **)
    abstract init(i:I, v:E): ()

    (** Bulk initialization of an array using a given function or value. **)
    abstract fill(f:I->E):ReadableArray[\E,I\]
    abstract fill(v:E):ReadableArray[\E,I\]

    abstract copy():ReadableArray[\E,I\]

    (** Create a fresh array structurally identical to the present
        one, but holding elements of type %U%. **)
    abstract replica[\U\]():ReadableArray[\U,I\]

    opr =(self, other:HasRank): Boolean
end

trait ImmutableArray[\E,I\] extends { ReadableArray[\E,I\] }
        excludes { Array[\E,I\] }
    abstract opr[r:Range[\I\]] : ImmutableArray[\E,I\]
    abstract opr[_:OpenRange[\Any\]] : ImmutableArray[\E,I\]
    abstract ivmap[\R\](f:(I,E)->R): ImmutableArray[\R, I\]
    abstract map[\R\](f:E->R): ImmutableArray[\R, I\]
    abstract shift(newOrigin:I):ImmutableArray[\E,I\]
    abstract init(i:I, v:E): ()
    abstract fill(f:I->E):ImmutableArray[\E,I\]
    abstract fill(v:E):ImmutableArray[\E,I\]
    abstract copy():ImmutableArray[\E,I\]
    abstract replica[\U\]():ImmutableArray[\U,I\]

    (** Thaw array (return mutable copy). **)
    abstract thaw():Array[\E,I\]
end

trait Array[\E,I\] extends { ReadableArray[\E,I\], MutableIndexed[\E,I\] }
    abstract opr[r:Range[\I\]] : Array[\E,I\]
    abstract opr[_:OpenRange[\Any\]] : Array[\E,I\]
    abstract ivmap[\R\](f:(I,E)->R): Array[\R, I\]
    abstract map[\R\](f:E->R): Array[\R, I\]
    abstract shift(newOrigin:I):Array[\E,I\]
    abstract init(i:I, v:E): ()
    abstract fill(f:I->E):Array[\E,I\]
    abstract fill(v:E):Array[\E,I\]
    abstract assign(f:I->E):Array[\E,I\]
    abstract copy():Array[\E,I\]
    abstract replica[\U\]():Array[\U,I\]

    (** Freeze array (return mutable copy). **)
    abstract freeze(): ImmutableArray[\E,I\]
end

(** Factory for arrays that returns an empty 0-indexed array of a given
    run-time-determined size. **)
array[\E\](x:ZZ32):Array[\E,ZZ32\]
array[\E\](x:ZZ32,y:ZZ32):Array[\E,(ZZ32,ZZ32)\]
array[\E\](x:ZZ32,y:ZZ32,z:ZZ32):Array[\E,(ZZ32,ZZ32,ZZ32)\]

(** Factory for immutable arrays that returns an empty 0-indexed array
    of a given run-time-determined size. **)
immutableArray[\E\](x:ZZ32):ImmutableArray[\E,ZZ32\]
immutableArray[\E\](x:ZZ32,y:ZZ32):ImmutableArray[\E,(ZZ32,ZZ32)\]
immutableArray[\E\](x:ZZ32,y:ZZ32,z:ZZ32):ImmutableArray[\E,(ZZ32,ZZ32,ZZ32)\]

primitiveArray[\E\](x:ZZ32):Array[\E,ZZ32\]

primitiveImmutableArray[\E\](x:ZZ32):ImmutableArray[\E,ZZ32\]

(** Array type supporting un-bounds-checked 0-based indexing.
    Useful for the internals of all the array functionality. **)
trait ArrayTypeWith0[\E,I\]
      extends { ReadableArray[\E,I\], DelegatedIndexed[\E,I\] }
    (** 0-based non-bounds-checked indexing. **)
    abstract get(i:I): E
    abstract init0(i:I, e:E): ()
    abstract zeroIndices(): FullRange[\I\]
    (** Convert from %base%-based indexing to 0-based indexing,
        performing bounds checking. **)
    abstract offset(i:I): I
    (** Convert from 0-based indexing to %base%-based indexing. **)
    abstract toIndex(i:I): I
end

(** NOTE: %StandardImmutableArrayType% is a parent of
    %StandardMutableArrayType%.  It therefore does not extend
    %ImmutableArrayType% as you might expect.  Other types that extend
    it should also extend %ImmutableArrayType% explicitly. **)
trait StandardImmutableArrayType[\T extends StandardImmutableArrayType[\T,E,I\],E,I\]
        extends { ArrayTypeWith0[\E,I\] }
    (** CONCRETE GETTERS:
        Default implementations of getters based on abstract methods
        in %StandardArrayType%. **)
    getter indices(): Indexed[\I,I\]
    getter indexValuePairs(): Indexed[\(I,E),I\]
    getter generator(): Indexed[\E,I\]

    (** CONCRETE METHODS:
        Default implementations of most array stuff based on the above.
        The things we cannot provide are anything involving replica. **)
    opr[i:I]:E
    init(i:I, v:E)

    generate[\R\](r: Reduction[\R\], body: E->R): R
    seq(self): SequentialGenerator[\E\]

    fill(f:I->E):T
    fill(v:E):T
    abstract copy():T
end


trait StandardMutableArrayType[\T extends StandardMutableArrayType[\T,E,I\],E,I\]
    extends { StandardImmutableArrayType[\T,E,I\], Array[\E,I\] }
    (** 0-based non-bounds-checked indexing. **)
    abstract put(i:I, e:E): ()
    opr[i:I]:=(v:E):()

    opr[r:Range[\I\]]:=(a:Indexed[\E,I\]):()

    assign(v:T):T
    assign(f:I->E):T
end

(** Canonical partitioning of a positive number %x% into two pieces.  If
   %(a,b) = partition(n)%
   and $n > 0$ then $0 < a <= b$, $n = a + b$.
   As it turns out we choose $a$ to be the largest power of $2 < n$.
*)
partition(x:ZZ32):(ZZ32,ZZ32)

(** A %ReadableArray1[\T,b0,s0\]% is
    an arbitrary 1-dimensional array whose %s0% elements are of
    type %T%, and whose lowest index is %b0%.

    The natural order of all generators is from %b0% to
    %b0+s0-1%. **)
trait ReadableArray1[\T, nat b0, nat s0\]
        extends { Indexed1[\s0\], Rank1, ArrayTypeWith0[\T,ZZ32\] }
        comprises { ImmutableArray1[\T,b0,s0\], Array1[\T,b0,s0\] }
    getter size():ZZ32
    getter bounds():FullRange[\ZZ32\]
    abstract getter mutability():String
    getter toString()
    opr |self| : ZZ32

    subarray[\nat b, nat s, nat o\]():ReadableArray1[\T, b, s\]

    (** Offset converts from %b0%-indexing to 0-indexing,
        bounds checking en route. *)
    offset(i:ZZ32):ZZ32
    toIndex(i:ZZ32):ZZ32

    zeroIndices(): FullRange[\ZZ32\]
end

trait ImmutableArray1[\T, nat b0, nat s0\]
    extends { StandardImmutableArrayType[\ImmutableArray1[\T,b0,s0\],T,ZZ32\],
              ImmutableArray[\T,ZZ32\], ReadableArray1[\T,b0,s0\] }
    getter mutability():String
    shift(o:ZZ32): ImmutableArray[\T,ZZ32\]
    opr[r: Range[\ZZ32\]] : ImmutableArray[\T,ZZ32\]
    opr[_:OpenRange[\ZZ32\]] : ImmutableArray1[\T,0,s0\]
    opr[_:OpenRange[\Any\]] : ImmutableArray1[\T,0,s0\]

    (** %subarray% selects a subarray of this array based on static parameters.
        %b#s% are the new bounds of the array; %o% is
        the index of the subarray within the current array. **)
    subarray[\nat b, nat s, nat o\]():ImmutableArray1[\T, b, s\]

    (** The %replica% method returns a replica of the array (similar layout
        etc.) but with a different element type. *)
    replica[\U\]():ImmutableArray1[\U,b0,s0\]

    copy():ImmutableArray1[\T,b0,s0\]

    thaw():Array1[\T,b0,s0\]
    map[\R\](f:T->R): ImmutableArray1[\R,b0,s0\]
    ivmap[\R\](f:(ZZ32,T)->R): ImmutableArray1[\R,b0,s0\]
end

(** %Array1[\T,b0,s0\]% is a 1-dimension array whose %s0% elements are of
    type %T%, and whose lowest index is %b0%. **)
trait Array1[\T, nat b0, nat s0\]
    extends { ReadableArray1[\T,b0,s0\],
              StandardMutableArrayType[\Array1[\T,b0,s0\],T,ZZ32\] }
    excludes {Number, String}

    getter mutability():String

    shift(o:ZZ32): Array[\T,ZZ32\]

    opr[r: Range[\ZZ32\]] : Array[\T,ZZ32\]
    opr[_:OpenRange[\ZZ32\]] : Array1[\T,0,s0\]
    opr[_:OpenRange[\Any\]] : Array1[\T,0,s0\]

    subarray[\nat b, nat s, nat o\]():Array1[\T, b, s\]

    replica[\U\]():Array1[\U,b0,s0\]

    copy():Array1[\T,b0,s0\]
    freeze():ImmutableArray1[\T,b0,s0\]
    map[\R\](f:T->R): Array1[\R,b0,s0\]
    ivmap[\R\](f:(ZZ32,T)->R): Array1[\R,b0,s0\]
end

trait Vector[\T extends Number, nat s0\] extends Array1[\T,0,s0\]
    add(v:Vector[\T,s0\]): Vector[\T,s0\]
    subtract(v:Vector[\T,s0\]): Vector[\T,s0\]
    negate(): Vector[\T,s0\]
    scale(t: T): Vector[\T,s0\]
    pmul(v: Vector[\T,s0\]): Vector[\T,s0\]
    dot(v: Vector[\T,s0\]): T
end

(** %__builtinFactory1% must be a non-overloaded 0-parameter factory for
   1-D arrays.  The type parameters are enshrined in \texttt{LHSEvaluator.java}
   and \texttt{NonPrimitive.java}; the factory name is enshrined in
   \texttt{WellKnownNames.java}.  There must be some factory, named in this
   file, with this type signature.  A similar thing is true for
   $k$-dimensional array types. *)
__builtinFactory1[\T, nat b0, nat s0\]():Array1[\T,b0,s0\]

(** %__immutableFactory1% is a non-overloaded 0-parameter factory for
   0-indexed 1-D write-once arrays.  It is also mentioned in \texttt{WellKnownNames} as it
   is used to allocate storage for varargs. *)
__immutableFactory1[\T, nat b0, nat s0\]():Array1[\T,b0,s0\]

array1[\T, nat s0\]():Array1[\T,0,s0\]
array1[\T, nat s0\](v:T):Array1[\T,0,s0\]
array1[\T, nat s0\](f:ZZ32->T):Array1[\T,0,s0\]

immutableArray1[\T, nat s0\](): ImmutableArray1[\T,0,s0\]

(** %vector% is the same as %array1%, but specialized to numeric type arguments. *)
vector[\T extends Number, nat s0\]():Vector[\T,s0\]
vector[\T extends Number, nat s0\](v:T):Vector[\T,s0\]
vector[\T extends Number, nat s0\](f:ZZ32->T):Vector[\T,s0\]


opr +[\ T extends Number, nat n, nat m \]
     (me : Vector[\T,n\], other : Vector[\T,n\]):Vector[\T,n\]

opr -[\ T extends Number, nat n, nat m \]
     (me : Vector[\T,n\], other : Vector[\T,n\]):Vector[\T,n\]

opr -[\ T extends Number, nat n, nat m \]
     (me : Vector[\T,n\]):Vector[\T,n\]

pmul[\ T extends Number, nat k \]
    (a : Vector[\T,k\], b : Vector[\T,k\]):Vector[\T,k\]

opr DOT[\ T extends Number, nat n, nat m, nat p \]
       (me : Vector[\T,n\], other : Vector[\T,n\]):T

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (me : Vector[\T,n\], other : Vector[\T,n\]):T

opr DOT[\ T extends Number, nat n, nat m, nat p \]
       (me : Vector[\T,n\], other : T) : Vector[\T,n\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (me : Vector[\T,n\], other : T) : Vector[\T,n\]

opr DOT[\ T extends Number, nat n, nat m, nat p \]
        (other : T, me : Vector[\T,n\]) : Vector[\T,n\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (other : T, me : Vector[\T,n\]) : Vector[\T,n\]

squaredNorm[\T extends Number, nat s0\](a:Vector[\T,s0\]):T

opr ||[\ T extends Number, nat k \]me : Vector[\T,k\]|| : RR64

(** %Array2[\T,b0,s0,b1,s1\]%
    is the type of 2-dimensional arrays of element type %T%, with
    size %s0% in the first dimension and %s1% in the second
    dimension and lowest index %(b0,b1)%.  Natural order for
    all generators in each dimension is from $b$ to $b+s-1$; the
    overall order of elements need only be consistent with the cross
    product of these orderings (see %Generator.cross()%). **)
trait Array2[\T, nat b0, nat s0, nat b1, nat s1\]
    extends { Indexed1[\s0\], Indexed2[\s1\], Rank2,
              StandardMutableArrayType[\Array2[\T,b0,s0,b1,s1\],T,(ZZ32,ZZ32)\] }
    excludes { Number, String }
  getter size():ZZ32
  getter bounds():FullRange[\(ZZ32,ZZ32)\]
  getter toString()
  opr |self| : ZZ32
  (** Translate from %b0%, %b1%-indexing to 0-indexing, checking bounds. **)
  offset(t:(ZZ32,ZZ32)):(ZZ32,ZZ32)
  toIndex(t:(ZZ32,ZZ32)):(ZZ32,ZZ32)
  opr[x:ZZ32,y:ZZ32]:=(v:T):()
  opr[r:Range[\(ZZ32,ZZ32)\]]: Array[\T,(ZZ32,ZZ32)\]
  opr[_:OpenRange[\ZZ32\]] : Array2[\T,0,s0,0,s1\]
  opr[_:OpenRange[\Any\]] : Array2[\T,0,s0,0,s1\]
  shift(t:(ZZ32,ZZ32)): Array[\T,(ZZ32,ZZ32)\]

  (** 2-D subarray given static subarray parameters.
      %(bo1,bo2)#(so1,so2)% are output bounds.
      The result is the subarray starting at %(o0,o1)% in the original array.
   **)
  subarray[\nat bo0, nat so0, nat bo1, nat so1, nat o0, nat o1\]
          (): Array2[\T,bo0,so0,bo1,so1\]

  zeroIndices():FullRange[\(ZZ32,ZZ32)\]

  replica[\U\]():Array2[\U,b0,s0,b1,s1\]
  copy():Array2[\T,b0,s0,b1,s1\]
  put(t:(ZZ32, ZZ32), v:T) : ()
  get(t:(ZZ32, ZZ32)):T
  t():Array2[\T,b1,s1,b0,s0\]
  (* Copied here for better return type information. *)
  map[\R\](f:T->R): Array2[\R,b0,s0,b1,s1\]
  ivmap[\R\](f:((ZZ32,ZZ32),T)->R): Array2[\R,b0,s0,b1,s1\]

  freeze():ImmutableArray[\T,(ZZ32,ZZ32)\]
end

trait Matrix[\T extends Number, nat s0, nat s1\] extends Array2[\T, 0, s0, 0, s1\]
    abstract add(v:Matrix[\T,s0,s1\]): Matrix[\T,s0,s1\]
    abstract subtract(v:Matrix[\T,s0,s1\]): Matrix[\T,s0,s1\]
    abstract negate(): Matrix[\T,s0,s1\]
    abstract scale(t: T): Matrix[\T,s0,s1\]
    abstract mul[\ nat s2 \](other: Matrix[\T,s1,s2\]): Matrix[\T,s0,s2\]
    abstract rmul(v: Vector[\T,s1\]): Vector[\T,s0\]
    abstract lmul(v: Vector[\T,s0\]): Vector[\T,s1\]
    abstract t(): Matrix[\T,s1,s0\]
end

__builtinFactory2[\T,nat b0,nat s0,nat b1,nat s1\]():Array2[\T,b0,s0,b1,s1\]

(** %array2% is a factory for 0-based 2-D arrays. **)
array2[\T, nat s0, nat s1\]():Array2[\T,0,s0,0,s1\]
array2[\T, nat s0, nat s1\](v:T):Array2[\T,0,s0,0,s1\]
array2[\T, nat s0, nat s1\](f:(ZZ32,ZZ32)->T):Array2[\T,0,s0,0,s1\]

(** %matrix% is the same as %array2%, but specialized to numeric type
   arguments, except that the default value (if given) is used to
   construct a multiple of the identity matrix. **)
matrix[\T extends Number, nat s0, nat s1\]():Matrix[\T,s0,s1\]
matrix[\T extends Number, nat s0, nat s1\](v:T):Matrix[\T,s0,s1\]

opr +[\ T extends Number, nat n, nat m \]
     (me:Matrix[\T,n,m\], other:Matrix[\T,n,m\]): Matrix[\T,n,m\]

opr -[\ T extends Number, nat n, nat m \]
     (me:Matrix[\T,n,m\], other:Matrix[\T,n,m\]) : Matrix[\T,n,m\]

opr -[\ T extends Number, nat n, nat m \]
     (me:Matrix[\T,n,m\]): Matrix[\T,n,m\]

(** Matrix multiplication. *)
opr DOT[\ T extends Number, nat n, nat m, nat p\]
       (me:Matrix[\T,n,m\], other:Matrix[\T,m,p\]): Matrix[\T,n,p\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p\]
     (me:Matrix[\T,n,m\], other:Matrix[\T,m,p\]): Matrix[\T,n,p\]

(** Matrix-vector multiplication. *)
opr DOT[\ T extends Number, nat n, nat m, nat p \]
       (me:Matrix[\T,n,m\], v:Vector[\T,m\]):Vector[\T,n\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (me:Matrix[\T,n,m\], v:Vector[\T,m\]):Vector[\T,n\]

(** Vector-matrix multiplication. *)
opr DOT[\ T extends Number, nat n, nat m, nat p \]
       (v:Vector[\T,n\], me:Matrix[\T,n,m\]):Vector[\T,m\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (v:Vector[\T,n\], me:Matrix[\T,n,m\]):Vector[\T,m\]

opr DOT[\ T extends Number, nat n, nat m, nat p \]
       (me : Matrix[\T,n,m\], other : T) : Matrix[\T,n,m\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (me : Matrix[\T,n,m\], other : T) : Matrix[\T,n,m\]

opr DOT[\ T extends Number, nat n, nat m, nat p \]
       (other : T, me : Matrix[\T,n,m\]) : Matrix[\T,n,m\]

opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (other : T, me : Matrix[\T,n,m\]) : Matrix[\T,n,m\]

(** %Array3[\T,b0,s0,b1,s1,b2,s2\]% is the type of 3-dimensional arrays
    of element type %T%, with size %s_i% in the $i^{th}$ dimension and lowest
    index %(b0,b1,b2)%.  Natural order for all generators in each
    dimension is from %b% to %b+s-1%; the overall order of elements need
    only be consistent with the cross product of these orderings (see
    %Generator.cross()%). **)
trait Array3[\T, nat b0, nat s0, nat b1, nat s1, nat b2, nat s2\]
    extends { Indexed1[\s0\], Indexed2[\s1\], Indexed3[\s2\], Rank3,
              StandardMutableArrayType[\Array3[\T,b0,s0,b1,s1,b2,s2\],T,
                                        (ZZ32,ZZ32,ZZ32)\] }
    excludes { Number, String }

    getter size():ZZ32
    getter bounds():FullRange[\(ZZ32,ZZ32,ZZ32)\]

    getter toString():String

    opr |self| : ZZ32

    (** Again, %offset% performs bounds checking and shifts to 0-indexing. *)
    offset(t:(ZZ32,ZZ32,ZZ32)):(ZZ32,ZZ32,ZZ32)
    toIndex(t:(ZZ32,ZZ32,ZZ32)):(ZZ32,ZZ32,ZZ32)

    (** And %get% and %put% are 0-indexed without bounds checks. *)
    abstract put(t:(ZZ32,ZZ32,ZZ32), v:T) : ()
    abstract get(t:(ZZ32,ZZ32,ZZ32)):T

    opr[i:ZZ32, j:ZZ32, k:ZZ32] := (v:T)
    opr[r:Range[\(ZZ32,ZZ32,ZZ32)\]]: Array[\T,(ZZ32,ZZ32,ZZ32)\]
    opr[_:OpenRange[\ZZ32\]] : Array3[\T,0,s0,0,s1,0,s2\]
    opr[_:OpenRange[\Any\]] : Array3[\T,0,s0,0,s1,0,s2\]
    shift(t:(ZZ32,ZZ32,ZZ32)): Array[\T,(ZZ32,ZZ32)\]

    (** 2-D subarray given static subarray parameters.
        %(bo1,bo2)#(so1,so2)% are output bounds.
        The result is the subarray starting at %(o0,o1)% in the original array.
     **)
    subarray[\nat bo0, nat so0, nat bo1, nat so1, nat bo2, nat so2,
              nat o0, nat o1, nat o2\]
            (): Array3[\T,bo0,so0,bo1,so1,bo2,so2\]

    zeroIndices():FullRange[\(ZZ32,ZZ32,ZZ32)\]

    replica[\U\]():Array3[\U,b0,s0,b1,s1,b2,s2\]
    copy():Array3[\T,b0,s0,b1,s1,b2,s2\]
    map[\R\](f:T->R): Array3[\R,b0,s0,b1,s1,b2,s2\]
    ivmap[\R\](f:((ZZ32,ZZ32,ZZ32),T)->R): Array3[\R,b0,s0,b1,s1,b2,s2\]

    freeze():ImmutableArray[\T,(ZZ32,ZZ32,ZZ32)\]
end

__builtinFactory3[\T, nat b0, nat s0, nat b1, nat s1, nat b2, nat s2\]():
        Array3[\T,b0,s0,b1,s1,b2,s2\]

array3[\T,nat s0, nat s1, nat s2\]():Array3[\T,0,s0,0,s1,0,s2\]

(************************************************************
* \subsection*{Reductions}
************************************************************)

trait Reduction[\ R \]
    abstract getter toString():String
    abstract empty(): R
    abstract join(a: R, b: R): R
end

object VoidReduction extends Reduction[\()\]
    getter toString()
    empty(): ()
    join(a: (), b: ()): ()
end

(** Hack to permit any %Number% to work non-parametrically. **)
object SumReduction extends Reduction[\Number\]
    getter toString()
    empty(): Number
    join(a: Number, b: Number): Number
end

opr SUM[\T\](g:(Reduction[\Number\],T->Number)->Number): Number

object ProdReduction extends Reduction[\Number\]
    getter toString()
    empty(): Number
    join(a:Number, b:Number): Number
end

opr PROD[\T\](g:(Reduction[\Number\],T->Number)->Number): Number

(** Hack to permit both %Number% and %TotalOrder% to work. **)
object MinReduction extends Reduction[\Any\]
    getter toString()
    empty(): Any
    join(a: Any, b: Any): Any
end

(** Again, type information is notoriously non-specific to permit
   either %TotalOrder% or %Number% types. **)
opr BIG MIN[\T\](g:(Reduction[\Any\],T->Any)->Any): Any

object NoMax extends UncheckedException end

(** Hack to permit both %Number% and %TotalOrder% to work. **)
object MaxReduction extends Reduction[\Any\]
    getter toString()
    empty(): Any
    join(a: Any, b: Any): Any
end

opr BIG MAX[\T\](g:(Reduction[\Any\],T->Any)->Any): Any

(** %AndReduction% and %OrReduction% take advantage of natural zeroes for early exit. **)
object AndReduction extends Reduction[\Boolean\]
    getter toString()
    empty(): Boolean
    join(a: Boolean, b: Boolean): Boolean
end

opr BIG AND[\T\](g:(Reduction[\Boolean\],T->Boolean)->Boolean):Boolean

object OrReduction extends Reduction[\Boolean\]
    getter toString()
    empty(): Boolean
    join(a: Boolean, b: Boolean): Boolean
end

opr BIG OR[\T\](g:(Reduction[\Boolean\],T->Boolean)->Boolean):Boolean

object StringReduction extends Reduction[\String\]
    getter toString()
    empty(): Boolean
    join(a:String, b:String): String
end

opr BIG STRING(g:(Reduction[\String\],Any->String)->String): String

(************************************************************
* \subsection*{Ranges}
************************************************************

** Ranges in general represent uses of the %#% and %:% operators.
    It is mostly subtypes of %Range% that are interesting.

    The partial order on ranges describes containment:
      %a < b% if and only if all points in %a% are strictly contained in %b%.
 **)
trait Range[\T\]
    extends StandardPartialOrder[\Range[\T\]\]
    comprises { RangeWithLower[\T\], RangeWithUpper[\T\],
                RangeWithExtent[\T\], PartialRange[\T\] }
    excludes { Number }
    opr =(self,_:Range[\T\]): Boolean
end

trait PartialRange[\T\] extends Range[\T\]
    comprises { OpenRange[\T\],
                LowerRange[\T\], UpperRange[\T\], ExtentRange[\T\] }
    excludes { FullRange[\T\] }
end

object OpenRange[\T\] extends { Range[\T\], PartialRange[\T\] }
    toString():String
    opr =(self,_:OpenRange[\T\]): Boolean
    opr CMP(self,other:Range[\T\]): Comparison
end

opr PARTIAL_LEXICO(a:Comparison, b:Comparison)
opr PARTIAL_LEXICO(a:Comparison, b:()->Comparison)

trait RangeWithLower[\T\] extends Range[\T\]
        comprises { LowerRange[\T\], FullRange[\T\] }
    abstract getter lower():T
end

object LowerRange[\T\](lo:T) extends { RangeWithLower[\T\], PartialRange[\T\] }
    getter lower():T
    toString():String
    opr =(self, x:LowerRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

trait RangeWithUpper[\T\] extends Range[\T\]
        comprises { UpperRange[\T\], FullRange[\T\] }
    abstract getter upper():T
end

object UpperRange[\T\](up:T) extends { RangeWithUpper[\T\], PartialRange[\T\] }
    getter upper():T
    toString():String
    opr =(self,x:UpperRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

trait RangeWithExtent[\T\] extends Range[\T\]
        comprises { ExtentRange[\T\], FullRange[\T\] }
    abstract getter extent():T
    toString():String
end

object ExtentRange[\T\](ex:T) extends { RangeWithExtent[\T\], PartialRange[\T\] }
    getter extent():T
    opr =(self,x:ExtentRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

trait FullRange[\T\]
        extends { RangeWithLower[\T\], RangeWithUpper[\T\],
                  RangeWithExtent[\T\], Indexed[\T,T\] }
        comprises { ... }
    getter indices(): FullRange[\T\]

    opr[r:Range[\T\]]: FullRange[\T\]
    opr[_:OpenRange[\T\]]: FullRange[\T\]
    (** Square-bracket indexing on a %FullRange% restricts that range to
         the range provided.  Restriction should behave as follows:
        \begin{itemize}
            \item Restriction to an %OpenRange% is the identity.
            \item An %UpperRange% or %ExtentRange% restrict the upper bound and
                  extent of the range.
            \item A %LowerRange% restricts the lower bound and extent of the range.
        \end{itemize}
        Note that this makes it compatible with the square-bracket
        indexing of the %Indexed% trait. **)
    opr[r:LowerRange[\T\]]: FullRange[\T\]
    opr[r:UpperRange[\T\]]: FullRange[\T\]
    opr[r:ExtentRange[\T\]]: FullRange[\T\]
    opr[r:FullRange[\T\]]: FullRange[\T\]

    toString():String

    opr =(self, other:FullRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

(** The %#% and %:% operators serve as factories for parallel ranges. **)
opr #[\I extends Integral\](lo:I, ex:I): Range[\I\]
opr #(lo:IntLiteral, ex:IntLiteral): Range[\ZZ32\]
opr #[\I extends Integral, J extends Integral\]
     (lo:(I,J), ex:(I,J)): Range[\(I,J)\]
opr #[\I extends Integral, J extends Integral, K extends Integral\]
     (lo:(I,J,K), ex:(I,J,K)): Range[\(I,J,K)\]
opr :[\I extends Integral\](lo:I, hi:I): FullRange[\I\]
opr :(lo:IntLiteral, ex:IntLiteral): FullRange[\ZZ32\]
opr :[\I extends Integral, J extends Integral\]
     (lo:(I,J), hi:(I,J)): Range[\(I,J)\]
opr :[\I extends Integral, J extends Integral, K extends Integral\]
     (lo:(I,J,K), hi:(I,J,K)): Range[\(I,J,K)\]

(** Factories for incomplete ranges. **)
opr (x:T)#[\T\] : LowerRange[\T\]
opr (x:T):[\T\] : LowerRange[\T\]
opr #[\T\](x:T) : ExtentRange[\T\]
opr :[\T\](x:T) : UpperRange[\T\]

opr #(): OpenRange[\Any\]
opr :(): OpenRange[\Any\]

(***********************************************************
* \subsection{Top-level primitives}
************************************************************)

opr |[\ N extends Integral \]x:N|

opr -(a:ZZ32):ZZ32
opr +(a:ZZ32,b:ZZ32):ZZ32
opr -(a:ZZ32,b:ZZ32):ZZ32
opr DOT(a:ZZ32,b:ZZ32):ZZ32
opr juxtaposition
     (a:ZZ32,b:ZZ32):ZZ32
opr DIV(a:ZZ32,b:ZZ32):ZZ32
opr REM(a:ZZ32,b:ZZ32):ZZ32
opr MOD(a:ZZ32,b:ZZ32):ZZ32
opr GCD(a:ZZ32,b:ZZ32):ZZ32
opr LCM(a:ZZ32,b:ZZ32):ZZ32
opr CHOOSE(a:ZZ32,b:ZZ32):ZZ32
opr BITAND(a:ZZ32,b:ZZ32):ZZ32
opr BITOR(a:ZZ32,b:ZZ32):ZZ32
opr BITXOR(a:ZZ32,b:ZZ32):ZZ32
opr LSHIFT(a:ZZ32,b:Integral):ZZ32
opr RSHIFT(a:ZZ32,b:Integral):ZZ32
opr BITNOT(a:ZZ32):ZZ32
opr =(a:ZZ32, b:ZZ32):Boolean
opr <=(a:ZZ32, b:ZZ32):Boolean
opr ^(a:ZZ32, b:Integral):Number
(** %widen% converts a %ZZ32% into a valid %ZZ64% quantity. **)
widen(a:ZZ32):ZZ64
(** %partitionL% returns the highest power of %2 < a%, used to
    partition iteration spaces for arrays and ranges. **)
partitionL(a:ZZ32):ZZ32

(** %nanoTime% returns the current time in nanoseconds.  Currently,
    this only supports taking differences of results of %nanoTime%
    to produce an elapsed time interval. **)
nanoTime():ZZ64

(** %printTaskTrace% dumps some internal error state. *)
printTaskTrace():()

recordTime(dummy: Any): ()
printTime(dummy: Any): ()

opr -(a: IntLiteral): IntLiteral
opr +(a: IntLiteral,b: IntLiteral): IntLiteral
opr -(a: IntLiteral,b: IntLiteral): IntLiteral
opr DOT(a: IntLiteral,b: IntLiteral): IntLiteral
opr juxtaposition
     (a: IntLiteral,b: IntLiteral): IntLiteral
opr DIV(a: IntLiteral,b: IntLiteral): IntLiteral
opr REM(a: IntLiteral,b: IntLiteral): IntLiteral
opr MOD(a: IntLiteral,b: IntLiteral): IntLiteral
opr GCD(a: IntLiteral,b: IntLiteral): IntLiteral
opr LCM(a: IntLiteral,b: IntLiteral): IntLiteral
opr CHOOSE(a: IntLiteral,b: IntLiteral): IntLiteral
opr BITAND(a: IntLiteral,b: IntLiteral): IntLiteral
opr BITOR(a: IntLiteral,b: IntLiteral): IntLiteral
opr BITXOR(a: IntLiteral,b: IntLiteral): IntLiteral
opr LSHIFT(a: IntLiteral,b:Integral): IntLiteral
opr RSHIFT(a: IntLiteral,b:Integral): IntLiteral
opr BITNOT(a: IntLiteral): IntLiteral
opr =(a: IntLiteral, b: IntLiteral):Boolean
opr <=(a: IntLiteral, b: IntLiteral):Boolean
opr ^(a: IntLiteral, b:Integral):Number

opr -[\ T extends Number, nat n, nat m \]
     (a:Integral):ZZ64
opr +[\ T extends Number, nat n, nat m \]
     (a:Integral,b:Integral):ZZ64
opr -[\ T extends Number, nat n, nat m \]
     (a:Integral,b:Integral):ZZ64
opr DOT[\ T extends Number, nat n, nat m, nat p \]
     (a:Integral,b:Integral):ZZ64
opr juxtaposition[\ T extends Number, nat n, nat m, nat p \]
     (a:Integral,b:Integral):ZZ64
opr DIV(a:Integral,b:Integral):ZZ64
opr REM(a:Integral,b:Integral):ZZ64
opr MOD(a:Integral,b:Integral):ZZ64
opr GCD(a:Integral,b:Integral):ZZ64
opr LCM(a:Integral,b:Integral):ZZ64
opr CHOOSE(a:Integral,b:Integral):ZZ64
opr BITAND(a:Integral,b:Integral):ZZ64
opr BITOR(a:Integral,b:Integral):ZZ64
opr BITXOR(a:Integral,b:Integral):ZZ64
opr LSHIFT(a:Integral,b:Integral):ZZ64
opr RSHIFT(a:Integral,b:Integral):ZZ64
opr BITNOT(a:Integral):ZZ64
opr =(a:Integral, b:Integral):Boolean
opr <=(a:Integral, b:Integral):Boolean
opr ^(a:ZZ64, b:Integral):Number
narrow(a:ZZ64):ZZ32

opr   <(a:Integral, b:Integral):Boolean
opr   >(a:Integral, b:Integral):Boolean
opr  >=(a:Integral, b:Integral):Boolean
opr CMP(a:Integral, b:Integral):TotalComparison
opr MIN[\I extends Integral\](a:I, b:I):I
opr MAX[\I extends Integral\](a:I, b:I):I

opr -(a:RR64):RR64
opr +(a:Number,b:Number):RR64
opr -(a:Number,b:Number):RR64
opr DOT(a:Number,b:Number):RR64
opr juxtaposition
     (a:Number,b:Number):RR64
opr /(a:Number,b:Number):RR64
opr =(a:Number, b:Number):Boolean
opr =/=(a:Number, b:Number):Boolean
opr <(a:Number, b:Number):Boolean
opr <=(a:Number, b:Number):Boolean
opr >(a:Number, b:Number):Boolean
opr >=(a:Number, b:Number):Boolean
opr CMP(a:Number, b:Number):Comparison
opr MIN(a:Number, b:Number):Boolean
opr MAX(a:Number, b:Number):Boolean
opr |a:RR64| : RR64
opr ^(a:Number, b:Number):RR64
opr SQRT(a:Number):RR64
sin(a:Number):RR64
cos(a:Number):RR64
tan(a:Number):RR64
asin(a:Number):RR64
acos(a:Number):RR64
atan(a:Number):RR64
atan2(y:Number,x:Number):RR64
log(a:Number):RR64
exp(a:Number):RR64
floor(a:Number):RR64
opr |\a:Number/| : ZZ64
ceiling(a:Number):RR64
opr |/a:Number\| : ZZ64
truncate(a:Number):ZZ64
random(a:Number):RR64

opr =(a:Char, b:Char):Boolean

opr DOT(a:String, b:String):String
opr juxtaposition
     (a:String, b:String):String
opr DOT(a:Number, b:String):String
opr juxtaposition
     (a:Number, b:String):String
opr DOT(a:String, b:Number):String
opr juxtaposition
     (a:String, b:Number):String
opr DOT(a:Boolean, b:String):String
opr juxtaposition
     (a:Boolean, b:String):String
opr DOT(a:String, b:Boolean):String
opr juxtaposition
     (a:String, b:Boolean):String
opr DOT(a:String, c:Char):String
opr juxtaposition
     (a:String, c:Char):String
opr DOT(c:Char, a:String):String
opr juxtaposition
     (c:Char, a:String):String
opr DOT(a:String, b:()):String
opr juxtaposition(a:String, b:()):String
opr DOT(a:String, b:(Any,Any)):String
opr juxtaposition(a:String, b:(Any,Any)):String
opr juxtaposition(a:String, b:(Any,Any,Any)):String
opr DOT(a:(), b:String):String
opr juxtaposition(a:(), b:String):String
opr DOT(a:Any, b:String):String
opr juxtaposition(a:Any, b:String):String
opr DOT(a:String, b:Any):String
opr juxtaposition(a:String, b:Any):String

opr =(a:String, b:String):Boolean
opr <(a:String, b:String):Boolean
opr <=(a:String, b:String):Boolean
opr >(a:String, b:String):Boolean
opr >=(a:String, b:String):Boolean
opr CMP(a:String, b:String):TotalComparison

outFileOpen(name:String):BufferedWriter
outFileWrite(file: BufferedWriter, str: String):()
outFileClose(file: BufferedWriter):()

substring(str: String, beginIndex: ZZ32, endIndex: ZZ32):String
length(str: String):ZZ32

print(a:String):()
println(a:String):()
print(a:Number):()
println(a:Number):()
print(a:Boolean):()
println(a:Boolean):()
println(a:Char):()
print(a:Any):()
println(a:Any):()
(** 0-argument versions handle passing of () to single-argument versions. **)
print():()
println():()

(** The following three functions are useful temporary hacks for
    debugging multi-threaded programs. **)
printThreadInfo(a:String):()
printThreadInfo(a:Number):()
throwError(a:String):()

opr SEQV(a:Any, b:Any):Boolean

opr  OR(a:Boolean, b:Boolean):Boolean
opr AND(a:Boolean, b:Boolean):Boolean
opr  OR(a:Boolean, b:()->Boolean):Boolean
opr AND(a:Boolean, b:()->Boolean):Boolean
opr NOT(a:Boolean):Boolean
opr ->(a: Boolean, b:Boolean):Boolean
opr ->(a: Boolean, b:()->Boolean):Boolean
opr <->(a: Boolean, b:Boolean):Boolean

true : Boolean
false : Boolean

opr +[\T extends Number\](x:T):T

opr =[\A,B\](t1:(A,B), t2:(A,B)): Boolean
opr <[\A,B\](t1:(A,B), t2:(A,B)): Boolean
opr <=[\A,B\](t1:(A,B), t2:(A,B)): Boolean
opr >[\A,B\](t1:(A,B), t2:(A,B)): Boolean
opr >=[\A,B\](t1:(A,B), t2:(A,B)): Boolean
opr CMP[\A,B\](t1:(A,B), t2:(A,B)): Boolean
opr =[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr <[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr <=[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr >[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr >=[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr CMP[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean

end
