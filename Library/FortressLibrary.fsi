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
    abstract opr =(self, other:Self): Boolean
end

(** Total ordering *)
object LexicographicPartialReduction
        extends { MonoidReduction[\Comparison\],
                  ReductionWithZeroes[\Comparison, Comparison\] }
    empty(): Comparison
    join(a:Comparison, b:Comparison):Comparison
    isZero(_:Unordered): Boolean
    isZero(_:Comparison): Boolean
end

object LexicographicReduction
        extends { MonoidReduction[\TotalComparison\],
                  ReductionWithZeroes[\TotalComparison, TotalComparison\] }
    empty(): TotalComparison
    join(a:TotalComparison, b:TotalComparison):TotalComparison
    isLeftZero(_:EqualTo): Boolean
    isLeftZero(_:Comparison): Boolean
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
    opr CMP(self, other:Unordered): Comparison
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
        extends { Equality[\Self\] }
    opr CMP(self, other:Self): Comparison
    opr <(self, other:Self): Boolean
    opr >(self, other:Self): Boolean
    opr =(self, other:Self): Boolean
    opr <=(self, other:Self): Boolean
    opr >=(self, other:Self): Boolean
end

(** %StandardMin% is the %MIN% operator; most types that implement %MIN%
    will implement a corresponding total order.  It's a separate type
    to account for the existence of floating point numbers, for which
    NaN counts as a bottom that is less than anything else but doesn't
    actually participate in the standard total ordering.  It is
    otherwise the case that %a MIN b = a% when %a <= b% and that
    %a MIN b = b MIN a%. **)
trait StandardMin[\T extends StandardMin[\T\]\]
    opr MIN(self, other:T): T
end

(** %StandardMax% is the %MAX% operator; most types that implement %MAX%
    will implement a corresponding total order.  It's a separate type
    to account for the existence of floating point numbers, for which
    NaN counts as a bottom that is less than anything else but doesn't
    actually participate in the standard total ordering.  It is
    otherwise the case that %a MAX b = a% when %a <= b% and that
    %a MAX b = b MAX a%. **)
trait StandardMax[\T extends StandardMax[\T\]\]
    opr MAX(self, other:T): T
end

(** StandardTotalOrder is the usual total order using %<%,%>%,%<=%,%>=%,%=%, and %CMP%.
    Most values that define a comparison should do so using this.
    Minimal complete definition: either %CMP% or %<% (it is advisable to
    define %=% in the latter case).  As noted above, %MIN%
    and %MAX% respect the total order and are defined in the obvious
    way. **)
trait StandardTotalOrder[\Self extends StandardTotalOrder[\Self\]\]
        extends { StandardPartialOrder[\Self\], StandardMin[\Self\], StandardMax[\Self\] }
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
* \subsection*{Numeric hierarchy}
************************************************************)

(** Additive group making use of %+%.  Must define %+% and
    either unary or binary %-%. **)
trait AdditiveGroup[\Self extends AdditiveGroup[\Self\]\]
    getter zero(): Self
    abstract opr +(self, other: Self): Self
    opr -(self, other: Self): Self
    opr -(self) : Self
end

(** Place holder for exclusions of MultiplicativeRing **)
trait AnyMultiplicativeRing end

(** Multiplicative ring using TIMES and juxtaposition.
    Define opr TIMES; juxtaposition is defined in terms of TIMES. **)
trait MultiplicativeRing[\Self extends MultiplicativeRing[\Self\]\]
        extends { AdditiveGroup[\Self\], AnyMultiplicativeRing }
    abstract getter one(): Self
    abstract opr TIMES(self, other:Self): Self
    opr juxtaposition(self, other:Self): Self
    (** Exponentiation need only deal with natural exponents. **)
    opr ^(self, other:ZZ64): Self
end

trait Number
      extends { StandardPartialOrder[\Number\], StandardMin[\Number\], StandardMax[\Number\] }
      comprises { RR64 }
    opr =(self, b:Number):Boolean
    opr =/=(self, b:Number):Boolean
    opr <(self, b:Number):Boolean
    opr <=(self, b:Number):Boolean
    opr >(self, b:Number):Boolean
    opr >=(self, b:Number):Boolean
    opr CMP(self, b:Number):Comparison
    (** In case of NaN, %MIN% and %MAX% return a NaN, otherwise it respects the
        total order. **)
    opr MIN(self, b:Number):Number
    opr MAX(self, b:Number):Number

    opr -(self):RR64
    opr +(self,b:Number):RR64
    opr -(self,b:Number):RR64
    opr DOT(self,b:Number):RR64
    opr TIMES(self,b:Number):RR64
    opr juxtaposition
         (self,b:Number):RR64
    opr /(self,b:Number):RR64
    opr SQRT(self):RR64
    opr PLUS_UP(self,b:Number):RR64
    opr MINUS_UP(self,b:Number):RR64
    opr DOT_UP(self,b:Number):RR64
    opr SLASH_UP(self,b:Number):RR64
    opr SQRT_UP(self):RR64
    opr PLUS_DOWN(self,b:Number):RR64
    opr MINUS_DOWN(self,b:Number):RR64
    opr DOT_DOWN(self,b:Number):RR64
    opr SLASH_DOWN(self,b:Number):RR64
    opr SQRT_DOWN(self):RR64
    opr |a:RR64| : RR64
    opr ^(self, b:RR64):RR64
    sin(self):RR64
    cos(self):RR64
    tan(self):RR64
    asin(self):RR64
    acos(self):RR64
    atan(self):RR64
    atan2(self,x:Number):RR64
    log(self):RR64
    exp(self):RR64
    floor(self):RR64
    opr |\self/| : ZZ64
    ceiling(self):RR64
    opr |/self\| : ZZ64
    truncate(self):ZZ64
end

trait RR64 extends Number comprises { RR32, Float, AnyIntegral, FloatLiteral }
    (** returns true if the value is an IEEE NaN **)
    getter isNaN(): Boolean
    (** returns true if the value is an IEEE infinity **)
    getter isInfinite(): Boolean
    (** returns true if the value is a valid number (not NaN) **)
    getter isNumber(): Boolean
    (** returns true if the value is finite **)
    getter isFinite(): Boolean
    (** %check% returns %Just(its argument)% if it is finite, otherwise %Nothing%. **)
    getter check(): Maybe[\RR64\]
    (** %check_star% returns %Just(its argument)% if it is non-NaN, otherwise %Nothing%. **)
    getter check_star(): Maybe[\RR64\]
    (** obtain the raw bits of the IEEE floating-point representation of this value. **)
    getter rawBits():ZZ64
    (** next higher IEEE float **)
    getter nextUp():RR64
    (** next lower IEEE float **)
    getter nextDown():RR64
    (** %MINNUM% and %MAXNUM% return a numeric result where possible (avoiding NaN).
        Note that %MINNUM% and %MAX% form a lattice with NaN at the top, and
        that %MAXNUM% and %MIN% form a lattice with NaN at the bottom.  **)
    opr MINNUM(self, b:RR64):RR64
    opr MAXNUM(self, b:RR64):RR64
    (** returns a value of type RR32 **)
    narrow(self): RR32
end

trait AnyIntegral extends { RR64 } end

trait Integral[\I extends Integral[\I\]\] extends { StandardTotalOrder[\I\], AnyIntegral }
        comprises { ZZ, IntLiteral }
    getter zero(): I
    getter one(): I

    opr -(self):I
    opr +(self,b:I):I
    opr -(self,b:I):I
    opr DOT(self,b:I):I
    opr TIMES(self,b:I):I
    opr juxtaposition(self,b:I):I
    opr DIV(self,b:I):I
    opr REM(self,b:I):I
    opr MOD(self,b:I):I
    opr GCD(self,b:I):I
    opr LCM(self,b:I):I
    opr CHOOSE(self,b:I):I
    opr BITAND(self,b:I):I
    opr BITOR(self,b:I):I
    opr BITXOR(self,b:I):I
    opr LSHIFT(self,b:ZZ64):I
    opr RSHIFT(self,b:ZZ64):I
    opr BITNOT(self):I
    opr ^(self, b:ZZ64):RR64
end

trait ZZ64 extends { Integral[\ZZ64\], ZZ } comprises { Long, ZZ32 }
    narrow(self):ZZ32
    big(self):ZZ
end

trait ZZ extends { Integral[\ZZ\] } comprises { BigNum, ZZ64}
end

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

    (** Filtering data from a generator.  Only elements that satisfy
        the predicate p are retained.  Natural order and cross product
        properties are otherwise preserved. **)
    filter(f: E -> Condition[\()\]): Generator[\E\]

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

__filter[\E\](g:Generator[\E\], p:E->Condition[\()\]): Generator[\E\]

__bigOperator[\I,O,R,L\](o:BigOperator[\I,O,R,L\],desugaredClauses:(Reduction[\L\],I->L)->L): O

(** Application of two nested BIG operators, possibly with fusion.  This only covers
    a comprehension of the form:
       BIG outer [ xs <- expro ] (BIG inner [x <- xs] expri)
    The desugarer extracts comprehensions of this form from more complex nests of
    comprehensions, using a combination of splitting:
        BIG OP [ gs1, gs2 ] expr = BIG OP [ gs1 ] (BIG OP [ gs2 ] expr)
    and filter squeezing:
        BIG OP [ xs <- g, p(xs) ] expr = BIG OP [ xs <- g.filter(p) ] expr
    There are some big caveats to this explanation in practice.  Most important is that
    we don't unlift and lift or do input/output conversion except where neccessary, so
    splitting skips these operations in between the inner and outer comprehension.
    **)
__bigOperator2[\I,M,O,R1,L1,R2,L2,E\](outer:BigOperator[\M,O,R1,L1\],
                                    inner:BigOperator[\I,M,R2,L2\],
                                    gg: Generator[\Generator[\E\]\],
                                    innerBody:E->L2): L1

(* Not currently used for desugaring, but will be used in future.
__nest[\E1,E2\](g:Generator[\E1\], f:E1->Generator[\E2\]):Generator[\E2\]

__map[\E,R\](g:Generator[\E\], f:E->R): Generator[\R\]
*)

(** Used in desugaring binding %if% **)
__cond[\E,R\](c:Condition[\E\], t:E->R, e:()->R): R
__cond[\E\](c:Condition[\E\], t:E->()): ()

(** Used in desugaring binding %while% **)
__whileCond[\E\](c:Condition[\E\], b:E->()): ()

trait SequentialGenerator[\E\] extends { Generator[\E\] }
    seq(self)
    map[\G\](f: E->G): SequentialGenerator[\G\]
    nest[\G\](f: E -> Generator[\G\]): Generator[\G\]
    cross[\F\](g:Generator[\F\]): Generator[\(E,F)\]
end

(** A Condition is a Generator that generates 0 or 1 element.
    Conditions can be used as nullary comprehension generators or
    as predicates in an if expression. **)
trait Condition[\E\] extends SequentialGenerator[\E\]
    getter isEmpty(): Boolean
    getter holds(): Boolean
    getter size(): ZZ32
    getter get(): E throws NotFound
    getter bounds(): FullRange[\ZZ32,true\]
    getter indices(): FullRange[\ZZ32,true\]
    getter indexValuePairs(): Generator[\(ZZ32,E)\]
    opr |self|: ZZ32
    opr [i:ZZ32]:E throws NotFound

    (** %cond% is the core of a %Condition%, in terms of which all other
        methods are written.  When %holds()%, %t% is invoked with the
        value of %get()%; otherwise %e% is called. **)
    abstract cond[\G\](t: E -> G, e: () -> G): G

    (** For a %Condition%, these methods run eagerly. **)
    generate[\G\](r:Reduction[\G\], body: E -> G): G
    map[\G\](f: E->G): Generator[\G\]
    ivmap[\G\](f: (ZZ32,E)->G): Generator[\G\]
    nest[\G\](f: E -> Generator[\G\]): Generator[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\(E,G)\]
    mapReduce[\R\](body: E->R, _:(R,R)->R, zero:R): R
    reduce(_:(E,E)->E, z:E):E
    reduce(r: Reduction[\E\]):E
    loop(f:E->()): ()
    opr IN(x:E, self):Boolean
end

(** Conjunction and disjunction of condition functions **)
opr ANDCOND[\I\](p1:I->Condition[\()\], p2:I->Condition[\()\]): I->Condition[\()\]

(** Conjunction and disjunction of condition functions **)
opr ORCOND[\I\](p1:I->Condition[\()\], p2:I->Condition[\()\]): I->Condition[\()\]


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
value trait AnyMaybe extends Equality[\AnyMaybe\] excludes Number
        (** not yet: ``%comprises Maybe[\T\] where [\T\]%'' *)
    abstract getter holds() : Boolean
    opr =(self, other:AnyMaybe): Boolean
end

just(t:Any):AnyMaybe

(** %Maybe% represents either %Nothing% or a single element of type
    %T% (%Just[\T\]%), which may be retrieved by calling %get%.  An
    object of type %Maybe[\T\]% can be used as a generator; it is either
    empty (%Nothing%) or generates the single element yielded by
    %get%, so there is no issue of canonical order or parallelism.

    Thus, %Just[\T\]% can be used as a single-element generator, and
    %Nothing% can be used as an empty generator. *)
value trait Maybe[\T\]
        extends { AnyMaybe, Condition[\T\], ZeroIndexed[\T\] }
        comprises { Nothing[\T\], Just[\T\] }
end

value object Just[\T\](x:T) extends Maybe[\T\]
    getter size(): ZZ32
    getter toString():String
    getter holds(): Boolean
    getter get(): T
    opr |self| : ZZ32
    getDefault(_:T): T
    cond[\R\](t:T->R, _:()->R): R
    generate[\R\](_:Reduction[\R\],m:T->R): R
    opr[i:ZZ32]:T
    opr[r:Range[\ZZ32\]]:Maybe[\T\]
    map[\G\](f: T->G): Just[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\(T,G)\]
    mapReduce[\R\](m: T->R, _:(R,R)->R, _:R): R
    reduce(_:(T,T)->T, _:T):T
    reduce(_: Reduction[\T\]):T
    loop(f:T->()): ()
    opr =(self,o:Just[\T\]): Boolean
end

(** %Nothing% will become a non-parametric singleton when we get where
    clauses working. *)
value object Nothing[\T\] extends Maybe[\T\]
    getter size(): ZZ32
    getter holds(): Boolean
    getter get(): T
    getter toString():String
    opr |self| : ZZ32
    getDefault(t:T):T
    cond[\R\](t:T->R, _:()->R): R
    generate[\R\](r:Reduction[\R\],_:T->R): R
    opr[_:ZZ32]: T
    opr[r:Range[\ZZ32\]]: Nothing[\T\]
    map[\G\](f: T->G): Nothing[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\(T,G)\]

    mapReduce[\R\](_: T->R, _:(R,R)->R, z:R): R
    reduce(_:(T,T)->T, z:T):T
    reduce(r: Reduction[\T\]):T
    loop(f:T->()): ()
    opr =(self,_:Nothing[\T\]): Boolean
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

object IndexOutOfBounds(min:ZZ32,max:ZZ32,index:ZZ32) extends UncheckedException
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

trait HasRank extends Equality[\HasRank\] excludes { Number, AnyMaybe }
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
    abstract getter bounds(): FullRange[\I,true\]
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
    bounds(): FullRange[\ZZ32,true\]
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
        extends { HasRank, Indexed[\E,I\], DelegatedIndexed[\E,I\] }
        comprises { Array[\E,I\], ImmutableArray[\E,I\] }
    (** CONCRETE GETTERS
        Default implementations of getters based on abstract methods
        below. **)
    getter indices(): Indexed[\I,I\]
    getter indexValuePairs(): Indexed[\(I,E),I\]
    getter generator(): Indexed[\E,I\]

    (** CONCRETE METHODS
        Default implementations of most array stuff based on the above.
        The things we can't provide are anything involving replica. **)

    opr[i:I]:E

    (** Initialize element at index i with value v.  This should occur
        once, before any other access or assignment occurs to element
        i.  An error will be signaled if an uninitialized element is
        read or an initialized element is re-initialized. **)
    init(i:I, v:E)

    generate[\R\](r: Reduction[\R\], body: E->R): R
    seq(self): SequentialGenerator[\E\]

    (** 0-based non-bounds-checked indexing. **)
    abstract get(i:I): E
    abstract init0(i:I, e:E): ()
    abstract zeroIndices(): FullRange[\I,true\]
    (** Convert from %base%-based indexing to 0-based indexing,
        performing bounds checking. **)
    abstract offset(i:I): I
    (** Convert from 0-based indexing to %base%-based indexing. **)
    abstract toIndex(i:I): I
    (** Indexed functionality with more specific type information. **)
    abstract opr[r:Range[\I\]] : ReadableArray[\E,I\]
    abstract opr[_:OpenRange[\Any\]] : ReadableArray[\E,I\]
    abstract ivmap[\R\](f:(I,E)->R): ReadableArray[\R, I\]
    abstract map[\R\](f:E->R): ReadableArray[\R, I\]

    (** Shift the origin of an array.  This should yield a new view of
        the same array; that is, initialization and/or update to either will
        be reflected in the other. **)
    abstract shift(newOrigin:I):ReadableArray[\E,I\]

    (** Bulk initialization of an array using a given function or
        value.  These are defined with more specific self types in
        StandardImmutableArrayType. **)
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
    abstract fill(f:I->E):ImmutableArray[\E,I\]
    abstract fill(v:E):ImmutableArray[\E,I\]
    abstract copy():ImmutableArray[\E,I\]
    abstract replica[\U\]():ImmutableArray[\U,I\]

    (** Thaw array (return mutable copy). **)
    abstract thaw():Array[\E,I\]
end

trait Array[\E,I\] extends { ReadableArray[\E,I\], MutableIndexed[\E,I\] }
    abstract put(i:I, e:E): ()
    opr[i:I]:=(v:E):()

    opr[r:Range[\I\]]:=(a:Indexed[\E,I\]):()

    abstract opr[r:Range[\I\]] : Array[\E,I\]
    abstract opr[_:OpenRange[\Any\]] : Array[\E,I\]
    abstract ivmap[\R\](f:(I,E)->R): Array[\R, I\]
    abstract map[\R\](f:E->R): Array[\R, I\]
    abstract shift(newOrigin:I):Array[\E,I\]
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

(** NOTE: %StandardImmutableArrayType% is a parent of
    %StandardMutableArrayType%.  It therefore does not extend
    %ImmutableArray% as you might expect.  Other types that extend
    it should also extend %ImmutableArray% explicitly. **)
trait StandardImmutableArrayType[\T extends StandardImmutableArrayType[\T,E,I\],E,I\]
        extends { ReadableArray[\E,I\] }
    fill(f:I->E):T
    fill(v:E):T
    abstract copy():T
end


trait StandardMutableArrayType[\T extends StandardMutableArrayType[\T,E,I\],E,I\]
        extends { StandardImmutableArrayType[\T,E,I\], Array[\E,I\] }
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
        extends { Indexed1[\s0\], Rank1, ReadableArray[\T,ZZ32\] }
        comprises { ImmutableArray1[\T,b0,s0\], Array1[\T,b0,s0\] }
    getter size():ZZ32
    getter bounds():FullRange[\ZZ32,true\]
    abstract getter mutability():String
    getter toString()
    opr |self| : ZZ32

    subarray[\nat b, nat s, nat o\]():ReadableArray1[\T, b, s\]

    (** Offset converts from %b0%-indexing to 0-indexing,
        bounds checking en route. *)
    offset(i:ZZ32):ZZ32
    toIndex(i:ZZ32):ZZ32

    zeroIndices(): FullRange[\ZZ32,true\]
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

trait Vector[\T extends Number, nat s0\]
        extends { Array1[\T,0,s0\], AdditiveGroup[\Vector[\T,s0\]\] }
        excludes { AnyMultiplicativeRing }
    opr +(self, v:Vector[\T,s0\]): Vector[\T,s0\]
    opr -(self, v:Vector[\T,s0\]): Vector[\T,s0\]
    opr -(self): Vector[\T,s0\]
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
  getter bounds():FullRange[\(ZZ32,ZZ32),true\]
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

  zeroIndices():FullRange[\(ZZ32,ZZ32),true\]

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

trait Matrix[\T extends Number, nat s0, nat s1\]
        extends { Array2[\T, 0, s0, 0, s1\], AdditiveGroup[\Matrix[\T,s0,s1\]\] }
        excludes { AnyMultiplicativeRing }
    opr +(self, v:Matrix[\T,s0,s1\]): Matrix[\T,s0,s1\]
    opr -(self, v:Matrix[\T,s0,s1\]): Matrix[\T,s0,s1\]
    opr -(self): Matrix[\T,s0,s1\]
    scale(t: T): Matrix[\T,s0,s1\]
    mul[\ nat s2 \](other: Matrix[\T,s1,s2\]): Matrix[\T,s0,s2\]
    rmul(v: Vector[\T,s1\]): Vector[\T,s0\]
    lmul(v: Vector[\T,s0\]): Vector[\T,s1\]
    t(): Matrix[\T,s1,s0\]
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
    getter bounds():FullRange[\(ZZ32,ZZ32,ZZ32),true\]

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

    zeroIndices():FullRange[\(ZZ32,ZZ32,ZZ32),true\]

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

trait Reduction[\R\] end

(** Invariants:
    join must be associative with identity empty
    unlift(lift(x)) = x
 **)
trait ActualReduction[\R,L\] extends Reduction[\R\]
    abstract getter toString()
    abstract empty(): L
    abstract join(a: L, b: L): L
    abstract lift(r:R): L
    abstract unlift(l:L): R
    (** If this reduction left-distributes over r, return a pair of
        reductions with the same lift and unlift **)
    leftDistribute(r: Reduction[\R\]): Maybe[\(Reduction[\R\],Reduction[\R\])\]
    (** If this reduction right-distributes over r, return a pair of
        reductions with the same lift and unlift **)
    rightDistribute(r: Reduction[\R\]): Maybe[\(Reduction[\R\],Reduction[\R\])\]
    (** If this reduction distributes over r, return a pair of
        reductions with the same lift and unlift **)
    distribute(r: Reduction[\R\]): Maybe[\(Reduction[\R\],Reduction[\R\])\]
end

trait PossibleReductionPair[\R\] extends Condition[\SomeReductionPair[\R\]\]
    comprises { NoReductionPair[\R\], SomeReductionPair[\R\] }
end

object NoReductionPair[\R\] extends PossibleReductionPair[\R\]
    getter holds(): Boolean
    getter cond[\G\](t:PossibleReductionPair[\R\]->G, e:()->G): G
end

trait SomeReductionPair[\R\] extends PossibleReductionPair[\R\]
    getter holds(): Boolean
    getter cond[\G\](t:PossibleReductionPair[\R\]->G, e:()->G): G
    abstract getter outer(): Reduction[\R\]
    abstract getter inner(): Reduction[\R\]
end

trait ReductionPair[\R,L\] extends SomeReductionPair[\R\]
    abstract getter outer(): ActualReduction[\R,L\]
    abstract getter inner(): ActualReduction[\R,L\]
end

(** The usual lifting to Maybe for identity-less operators **)
trait AssociativeReduction[\R\] extends ActualReduction[\R,AnyMaybe\]
    empty(): Nothing[\R\]
    join(a: AnyMaybe, b: AnyMaybe): AnyMaybe
    abstract simpleJoin(a:Any, b:Any): Any
    lift(r:Any): AnyMaybe
    unlift(r:AnyMaybe): R
end

trait CommutativeReduction[\R\] extends AssociativeReduction[\R\] end

(** Monoids don't require a special lift and unlift operation. **)
trait MonoidReduction[\R\] extends ActualReduction[\R,R\]
    lift(r:R): R
    unlift(r:R): R
end

trait CommutativeMonoidReduction[\R\] extends MonoidReduction[\R\] end

trait ReductionWithZeroes[\R,L\] extends ActualReduction[\R,L\]
    isLeftZero(l:L): Boolean
    isRightZero(l:L): Boolean
    isZero(l:L): Boolean
end

trait BigOperator[\I,O,R,L\]
    abstract getter reduction(): ActualReduction[\R,L\]
    abstract getter body(): I->R
    abstract getter unwrap(): R->O
end

object BigReduction[\R,L\](r:ActualReduction[\R,L\]) extends BigOperator[\R,R,R,L\]
    getter reduction(): ActualReduction[\R,L\]
    getter body(): R->R
    getter unwrap(): R->R
end

object Comprehension[\I,O,R,L\](u: R->O, r: ActualReduction[\R,L\], singleton:I->R)
        extends BigOperator[\I,O,R,L\]
    getter reduction(): ActualReduction[\R,L\]
    getter body(): I->R
    getter unwrap(): R->O
end

(** VoidReduction is usually done for effect, so we pretend that
    the completion performs the effects.  This rules out things
    distributing over void (that would change the number of effects in
    our program) but not void distributing over other things. **)
object VoidReduction extends { CommutativeMonoidReduction[\()\] }
    getter toString()
    empty(): ()
    join(a: (), b: ()): ()
end

(* Hack to permit any Number to work non-parametrically. *)
object SumReduction extends CommutativeMonoidReduction[\Number\]
    getter toString()
    empty(): Number
    join(a: Number, b: Number): Number
end

opr SUM[\T extends Number\](): Comprehension[\T,Number,Number,Number\]

object ProdReduction extends CommutativeMonoidReduction[\Number\]
    getter toString()
    empty(): Number
    join(a:Number, b:Number): Number
end

opr PROD[\T extends Number\](): Comprehension[\T,Number,Number,Number\]

object MinReduction[\T extends StandardMin[\T\]\] extends CommutativeReduction[\T\]
    getter toString()
    simpleJoin(a:T, b:T): T
end

opr BIG MIN[\T extends StandardMin[\T\]\](): BigReduction[\T,AnyMaybe\]

object MaxReduction[\T extends StandardMax[\T\]\] extends CommutativeReduction[\T\]
    getter toString()
    simpleJoin(a:T, b:T): T
end

opr BIG MAX[\T extends StandardMax[\T\]\](): BigReduction[\T,AnyMaybe\]

opr BIG MINNUM(): BigReduction[\RR64,RR64\]

opr BIG MAXNUM(): BigReduction[\RR64,RR64\]

(** AndReduction and OrReduction take advantage of natural zeroes for early exit. **)
object AndReduction
        extends { CommutativeMonoidReduction[\Boolean\],
                  ReductionWithZeroes[\Boolean,Boolean\] }
    getter toString()
    empty(): Boolean
    join(a: Boolean, b: Boolean): Boolean
    isZero(a:Boolean): Boolean
end

opr BIG AND[\T\](): BigReduction[\Boolean,Boolean\]

object OrReduction
        extends { CommutativeMonoidReduction[\Boolean\],
                  ReductionWithZeroes[\Boolean,Boolean\] }
    getter toString()
    empty(): Boolean
    join(a: Boolean, b: Boolean): Boolean
    isZero(a:Boolean): Boolean
end

opr BIG OR[\T\]():BigReduction[\Boolean, Boolean\]

(** A reduction performing String concatenation **)
object StringReduction extends MonoidReduction[\String\]
    getter toString()
    empty(): String
    join(a:String, b:String): String
end

(** A reduction performing String concatenation with a space **)
object SpaceReduction extends MonoidReduction[\String\]
    getter toString()
    empty(): String
    join(a:String, b:String): String
end

(** A reduction performing String concatenation with newline separation **)
object NewlineReduction extends AssociativeReduction[\String\]
    getter toString()
    simpleJoin(a:String, b:String): String
end

(** This operator performs string concatenation, first converting
    its inputs (of type Any) to String if necessary. **)
opr BIG ||(): Comprehension[\Any,String,String,String\]

(** This operator performs string concatenation, first converting
    its inputs (of type Any) to String if necessary, and separating
    non-empty components by a space. **)
opr BIG |||(): Comprehension[\Any,String,String,String\]

(** This operator performs string concatenation with newline
    separation, first converting its inputs (of type Any) to String if
    necessary. **)
opr BIG //(): Comprehension[\Any,String,AnyMaybe,AnyMaybe\]

(** A %MapReduceReduction% takes an associative binary function %j% on
    arguments of type %R%, and the identity of that function %z%, and
    returns the corresponding reduction. **)
object MapReduceReduction[\R\](j:(R,R)->R, z:R) extends MonoidReduction[\R\]
    getter toString()
    empty(): R
    join(a:R, b:R): R
end

(** A %MIMapReduceReduction% takes an associative binary function %j% on
    arguments of type %R%, and the identity of that function %z%, and
    returns the corresponding reduction. **)
object MIMapReduceReduction[\R\](j:(Any,Any)->R, z:Any) extends MonoidReduction[\Any\]
    getter toString()
    empty(): R
    join(a:Any, b:Any): R
end

(** %embiggen% takes a type %T% and an operation (either an operator
    %OP% or binary function %f% on type %T%), along with the identity
    %z% of the operation, and returns a function suitable as the right-hand
    side of the definition of the corresponding BIG operator. **)
(*
embiggen[\T,opr OP\](z:T): ((Reduction[\T\],T->T) -> T) -> T
*)
embiggen[\T\](j:(Any,Any)->T, z:T) : Comprehension[\T,T,Any,Any\]

trait FilterGenerator[\E\] extends Generator[\E\]
    getter g(): Generator[\E\]
    getter p(): E -> Condition[\()\]
    getter toString(): String
    generate[\R\](r:Reduction[\R\], m: E->R): R
    reduce(r: Reduction[\E\]): E
    filter(p': E -> Condition[\()\]): FilterGenerator[\E\]
    seq(self)
end

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
    excludes { CompleteRange[\T\] }
end

object OpenRange[\T\] extends { Range[\T\], PartialRange[\T\] }
    toString():String
    opr =(self,_:OpenRange[\T\]): Boolean
    opr CMP(self,other:Range[\T\]): Comparison
end

opr PARTIAL_LEXICO(a:Comparison, b:Comparison)
opr PARTIAL_LEXICO(a:Comparison, b:()->Comparison)

trait RangeWithLower[\T\] extends Range[\T\]
        comprises { LowerRange[\T\], CompleteRange[\T\] }
    abstract getter lower():T
end

object LowerRange[\T\](lo:T) extends { RangeWithLower[\T\], PartialRange[\T\] }
    getter lower():T
    toString():String
    opr =(self, x:LowerRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

trait RangeWithUpper[\T\] extends Range[\T\]
        comprises { UpperRange[\T\], CompleteRange[\T\] }
    abstract getter upper():T
end

object UpperRange[\T\](up:T) extends { RangeWithUpper[\T\], PartialRange[\T\] }
    getter upper():T
    toString():String
    opr =(self,x:UpperRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

trait RangeWithExtent[\T\] extends Range[\T\]
        comprises { ExtentRange[\T\], CompleteRange[\T\] }
    abstract getter extent():T
    toString():String
end

object ExtentRange[\T\](ex:T) extends { RangeWithExtent[\T\], PartialRange[\T\] }
    getter extent():T
    opr =(self,x:ExtentRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

(** The type %CompleteRange[\T\]% is really just %FullRange[\T,flag\]% for
    either setting of %flag%.  We need it for stuff like %typecase%
    and %extends% clauses since we can't bind universally quantified
    variables in those settings.  Since essentially all the methods are
    independent of the setting of flag we include them here. **)
trait CompleteRange[\T\]
        extends { RangeWithLower[\T\], RangeWithUpper[\T\],
                  RangeWithExtent[\T\], Indexed[\T,T\] }
        comprises { FullRange[\T,true\], FullRange[\T,false\] }
    getter indices(): FullRange[\T,true\]
    abstract getter isBounded(): Boolean
    abstract getter isSized(): Boolean
    abstract getter increment(): T

    (** The following two methods are used in bounds checking code to
        permit ranges that are just outside the edge of the given
        range.   Because they're intended for internal use we ignore
        the setting of flag and return a CompleteRange. **)
    abstract widenUpper(): CompleteRange[\T\]
    abstract widenLower(): CompleteRange[\T\]

    opr[r:Range[\T\]]: FullRange[\T,true\]
    opr[_:OpenRange[\T\]]: FullRange[\T,true\]
    (** Square-bracket indexing on a FullRange restricts that range to
         the range provided.  Restriction should behave as follows:
            - Restriction to an OpenRange is the identity.
            - An UpperRange or ExtentRange restrict the upper bound and
              extent of the range.
            - A LowerRange restricts the lower bound and extent of the range.
        Note that this makes it compatible with the square-bracket
        indexing of the Indexed trait. **)
    opr[r:LowerRange[\T\]]: FullRange[\T,true\]
    opr[r:UpperRange[\T\]]: FullRange[\T,true\]
    opr[r:ExtentRange[\T\]]: FullRange[\T,true\]
    opr[r:CompleteRange[\T\]]: FullRange[\T,true\]
    toString():String
    opr =(self, other:CompleteRange[\T\]): Boolean
    opr CMP(self, other:Range[\T\]): Comparison
end

(** A %FullRange% is either bounded (generated from a lower and upper
    bound, as in %l:u%) or sized (generated from a lower bound and a
    size, as in %l#s%).  These two kinds of range behave identically
    in most settings; the only time they behave differently is when we
    construct a strided range: both %l:u:i% and %l#s:i% generate
    elements %l, l+i, l+2i, ...%, but the former is bounded and
    generates no element larger than %i% and the latter is sized and
    generates exactly %s% elements.

    Data structure bounds are always bounded. **)
trait FullRange[\T, bool bounded\] extends CompleteRange[\T\]
        comprises { ... }
    getter isBounded(): Boolean
    getter isSized(): Boolean
end

(** The %#% and %:% operators serve as factories for parallel ranges. **)
opr #[\I extends AnyIntegral\](lo:I, ex:I): FullRange[\I,false\]
(*
opr #(lo:IntLiteral, ex:IntLiteral): FullRange[\ZZ32\]
*)
opr #[\I extends AnyIntegral, J extends AnyIntegral\]
     (lo:(I,J), ex:(I,J)): FullRange[\(I,J),false\]
opr #[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\]
     (lo:(I,J,K), ex:(I,J,K)): FullRange[\(I,J,K),false\]

opr :[\I extends AnyIntegral\](lo:I, hi:I): FullRange[\I,true\]
(*
opr :(lo:IntLiteral, ex:IntLiteral): FullRange[\ZZ32\]
*)
opr :[\I extends AnyIntegral, J extends AnyIntegral\]
     (lo:(I,J), hi:(I,J)): FullRange[\(I,J),true\]
opr :[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\]
     (lo:(I,J,K), hi:(I,J,K)): FullRange[\(I,J,K),true\]

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

(** %nanoTime% returns the current time in nanoseconds.  Currently,
    this only supports taking differences of results of %nanoTime%
    to produce an elapsed time interval. **)
nanoTime():ZZ64

(** %printTaskTrace% dumps some internal error state. *)
printTaskTrace():()

recordTime(dummy: Any): ()
printTime(dummy: Any): ()

fromRawBits(a:ZZ64):RR64

random(a:Number):RR64

match(regex:String,some:String):Boolean

(** %char% converts an integer unicode code point into the
    corresponding 16-bit %Char%.  Note that we don't presently deal
    gracefully with %Char%s outside the 16-bit plane. **)
char(a:ZZ32):Char

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

forDigit(x:ZZ32, radix:ZZ32): Maybe[\Char\]
forDigit(x:ZZ32, radixString:String): Maybe[\Char\]

(** opr // appends a single newline separator. **)
opr (x:Any)// : String

(** opr /// appends a double newline separator **)
opr (x:Any)/// : String

(* A way to get environment information from inside of fortress *)
getEnvironment(name:String, defaultValue:String):String

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
opr CMP[\A,B\](t1:(A,B), t2:(A,B)): Comparison
opr =[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr <[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr <=[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr >[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr >=[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Boolean
opr CMP[\A,B,C\](t1:(A,B,C), t2:(A,B,C)): Comparison

end
