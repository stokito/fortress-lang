(*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api FortressLibrary

import Stream.{WriteStream}
import FlatString.{FlatString}
import String.{StringStats}

(************************************************************
* \subsection*{Simple Combinators}
*************************************************************)

(** Casting *)

cast[\T extends Any\](x:Any):T

instanceOf[\T extends Any\](x:Any):Boolean

(** Useful functions *)

ignore(_:Any):()

identity[\T extends Any\](x:T):T

(* Should we deprecate tuple and use identity instead?  Decision: no. *)
tuple[\T\](x:T):T

first[\T1,T2\](x:(T1,T2)): T1
second[\T1,T2\](x:(T1,T2)): T2
first[\T1,T2,T3\](x:(T1,T2,T3)): T1
second[\T1,T2,T3\](x:(T1,T2,T3)): T2
third[\T1,T2,T3\](x:(T1,T2,T3)): T3
first[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T1
second[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T2
third[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T3
fourth[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T4

(* Function composition *)
opr COMPOSE[\A,B,C\](f: B->C, g: A->B): A->C

fail[\T\](s:String):T

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

opr BIG LEXICO(): BigReduction[\TotalComparison, TotalComparison\]

opr BIG LEXICO(g: Generator[\TotalComparison\]): TotalComparison

trait Comparison
        extends { StandardPartialOrder[\Comparison\] }
        comprises { Unordered, TotalComparison }
    opr =(self, other:Comparison): Boolean
    opr LEXICO(self, other:Comparison): Comparison
    opr SYMMETRIC_PARTIAL(self, other:Comparison): Comparison
    abstract opr INVERSE(self): Comparison
end

(** Unordered is the outcome of %a CMP b% when %a% and %b% are partially
    ordered and no ordering relationship exists between them. **)
object Unordered extends Comparison
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
    opr =(self, other:LessThan): Boolean
    opr CMP(self, other:LessThan): Comparison
    opr CMP(self, other:TotalComparison): TotalComparison
    opr <(self, other:LessThan): Boolean
    opr <(self, other:TotalComparison): Boolean
    opr SYMMETRIC_PARTIAL(self, other:LessThan): LessThan
    opr SYMMETRIC_PARTIAL(self, other:EqualTo): LessThan
    opr INVERSE(self): TotalComparison
end

object GreaterThan extends TotalComparison
    opr =(self, other:GreaterThan): Boolean
    opr CMP(self, other:GreaterThan): Comparison
    opr CMP(self, other:TotalComparison): TotalComparison
    opr <(self, other:TotalComparison): Boolean
    opr SYMMETRIC_PARTIAL(self, other:GreaterThan): GreaterThan
    opr SYMMETRIC_PARTIAL(self, other:EqualTo): GreaterThan
    opr INVERSE(self): TotalComparison
end

object EqualTo extends TotalComparison
    opr =(self, other:EqualTo): Boolean
    opr CMP(self, other:TotalComparison): TotalComparison
    opr <(self, other:LessThan): Boolean
    opr <(self, other:TotalComparison): Boolean
    opr LEXICO(self, other:TotalComparison): TotalComparison
    opr LEXICO(self, other:()->TotalComparison): TotalComparison
    opr SYMMETRIC_PARTIAL(self, other: Comparison): Comparison
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

(** %StandardMinMax% combines MIN and MAX operators, and provides a
    combined MINMAX operator.  This operator returns both its
    arguments; if equality is possible, self should be the leftmost
    result.  This effectively means that %(a MINMAX b)% stably sorts
    %a% and %b%.  In addition, %a MINMAX b = (a MIN b, a MAX b)% must
    always hold. **)

trait StandardMinMax[\T extends StandardMinMax[\T\]\]
        extends { StandardMin[\T\], StandardMax[\T\] }
    abstract opr MINMAX(self, other:T): (T,T)
    opr MIN(self, other:T): (T,T)
    opr MAX(self, other:T): (T,T)
end

(** StandardTotalOrder is the usual total order using %<%,%>%,%<=%,%>=%,%=%, and %CMP%.
    Most values that define a comparison should do so using this.
    Minimal complete definition: either %CMP% or %<% (it is advisable to
    define %=% in the latter case).  As noted above, %MIN%
    and %MAX% respect the total order and are defined in the obvious
    way. **)
trait StandardTotalOrder[\Self extends StandardTotalOrder[\Self\]\]
        extends { StandardPartialOrder[\Self\], StandardMinMax[\Self\] }
    opr MINMAX(self, other:Self): (Self,Self)
    opr CMP(self, other:Self): TotalComparison
end

(************************************************************
 * \subsection*{Assertions}
 *)
assert(flag:Boolean): ()

assert(flag: Boolean, failMsg: String): ()

(** This version of %assert% checks the equality of its first two arguments;
    if unequal it includes the remaining arguments in its error indication. *)
assert(x:Any, y:Any, failMsg: Any...): ()

deny(flag:Boolean): ()

deny(flag: Boolean, failMsg: String): ()

(** This version of %deny% checks the inequality of its first two arguments;
    if equal it includes the remaining arguments in its error indication. *)
deny(x:Any, y:Any, failMsg: Any...): ()

shouldRaise[\Ex extends Exception\] (expr: ()->()): ()

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
        extends { StandardPartialOrder[\Number\], StandardMinMax[\Number\],
                  AdditiveGroup[\Number\], MultiplicativeRing[\Number\] }
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
    opr MINMAX(self, b:Number):(Number,Number)

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
    opr IEEE_PLUS_UP(self,b:Number):RR64
    opr IEEE_MINUS_UP(self,b:Number):RR64
    opr IEEE_DOT_UP(self,b:Number):RR64
    opr IEEE_PLUS_DOWN(self,b:Number):RR64
    opr IEEE_MINUS_DOWN(self,b:Number):RR64
    opr IEEE_DOT_DOWN(self,b:Number):RR64
    opr IEEE_SLASH_DOWN(self,b:Number):RR64
    opr IEEE_SLASH_UP(self,b:Number):RR64
    opr |self| : RR64
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

trait RR64 extends Number comprises { Float, FloatLiteral, RR32, QQ }
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
    (** obtain the sign bit of the IEEE floating-point representation of this value. **)
    getter signBit():ZZ32
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

(** Returns the rational p/q such that a <= p/q <= b such that
    q is minimized, and for this minimal q, p is nearest 0.
*)
simplestRationalBetween(a:QQ, b:QQ): QQ

trait QQ extends { RR64, StandardPartialOrder[\QQ\] } comprises { ... }
    getter isNaN(): Boolean
    getter isInfinite(): Boolean
    getter isNumber(): Boolean
    getter isFinite(): Boolean
    numerator(self): ZZ
    denominator(self): ZZ  (* Ideally would be NN *)
    opr |self| : QQ
    opr =(self, other:QQ):Boolean
    opr =/=(self, other:QQ):Boolean
    opr <(self, other:QQ):Boolean
    opr <=(self, other:QQ):Boolean
    opr >(self, other:QQ):Boolean
    opr >=(self, other:QQ):Boolean
    (** In case of 0/0, %MIN% and %MAX% return 0/0, otherwise it respects the total order. **)
    opr MIN(self, other:QQ):QQ
    opr MAX(self, other:QQ):QQ
    opr MINMAX(self, other:QQ):(QQ,QQ)
    opr -(self):QQ
    opr +(self,other:QQ):QQ
    opr -(self,other:QQ):QQ
    opr DOT(self,other:QQ):QQ
    opr TIMES(self,other:QQ):QQ
    opr juxtaposition (self,other:QQ):QQ
    opr /(self,other:QQ):QQ
    opr ^(self, other:ZZ64):QQ
    floor(self):ZZ
    opr |\self/| : ZZ
    ceiling(self):ZZ
    opr |/self\| : ZZ
    truncate(self): ZZ
    round(self): ZZ
    opr MINNUM(self, other:QQ):QQ
    opr MAXNUM(self, other:QQ):QQ
end

trait AnyIntegral extends { QQ } end

trait Integral[\I extends Integral[\I\]\] extends { StandardTotalOrder[\I\], AnyIntegral }
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
    opr DIVIDES(self,b:I):Boolean
    opr CHOOSE(self,b:I):I
    opr BITAND(self,b:I):I
    opr BITOR(self,b:I):I
    opr BITXOR(self,b:I):I
    opr LSHIFT(self,b:AnyIntegral):I
    opr RSHIFT(self,b:AnyIntegral):I
    opr BITNOT(self):I
    opr ^(self, b:AnyIntegral):RR64
    unsigned(self):NN64
end

trait NN64 extends { ZZ, Integral[\NN64\] } comprises { UnsignedLong, NN32 }
    opr |self| : NN64
    opr =(self, b:NN64):Boolean
    opr <(self, b:NN64):Boolean
    opr -(self):NN64
    opr +(self,b:NN64):NN64
    opr -(self,b:NN64):NN64
    opr DOT(self,b:NN64):NN64
    opr TIMES(self,b:NN64):NN64
    opr juxtaposition(self,b:NN64):NN64
    opr DIV(self,b:NN64):NN64
    opr REM(self,b:NN64):NN64
    opr MOD(self,b:NN64):NN64
    opr GCD(self,b:NN64):NN64
    opr LCM(self,b:NN64):NN64
    opr CHOOSE(self,b:NN64):NN64
    opr BITAND(self,b:NN64):NN64
    opr BITOR(self,b:NN64):NN64
    opr BITXOR(self,b:NN64):NN64
    opr LSHIFT(self,b:AnyIntegral):NN64
    opr RSHIFT(self,b:AnyIntegral):NN64
    opr BITNOT(self):NN64
    opr ^(self, b:AnyIntegral):RR64
    narrow(self):NN32
    signed(self):NN64
end

trait ZZ32 extends { ZZ64, Integral[\ZZ32\] } comprises { Int, IntLiteral }
    getter zero(): ZZ32
    getter one(): ZZ32
    getter minimum(): ZZ32
    getter maximum(): ZZ32



    opr |self| : ZZ32
    opr =(self, b:ZZ32):Boolean
    opr <(self, b:ZZ32):Boolean

    opr -(self):ZZ32
    opr +(self,b:ZZ32):ZZ32
    opr -(self,b:ZZ32):ZZ32
    opr DOT(self,b:ZZ32):ZZ32
    opr juxtaposition(self,b:ZZ32):ZZ32
    opr DIV(self,b:ZZ32):ZZ32
    opr REM(self,b:ZZ32):ZZ32
    opr MOD(self,b:ZZ32):ZZ32
    opr GCD(self,b:ZZ32):ZZ32
    opr LCM(self,b:ZZ32):ZZ32
    opr CHOOSE(self,b:ZZ32):ZZ32
    opr BITAND(self,b:ZZ32):ZZ32
    opr BITOR(self,b:ZZ32):ZZ32
    opr BITXOR(self,b:ZZ32):ZZ32
    opr LSHIFT(self,b:ZZ64):ZZ32
    opr RSHIFT(self,b:ZZ64):ZZ32
    opr BITNOT(self):ZZ32
    widen(self):ZZ64
    partitionL(self):ZZ32
    unsigned(self):NN32
end

trait ZZ64 extends { ZZ, Integral[\ZZ64\] } comprises { Long, ZZ32 }
    getter zero(): ZZ64
    getter one(): ZZ64
    getter minimum(): ZZ64
    getter maximum(): ZZ64


    opr |self| : ZZ64

    opr =(self, b:ZZ64):Boolean
    opr <(self, b:ZZ64):Boolean
    opr >(self, b:ZZ64):Boolean
    opr >=(self, b:ZZ64):Boolean
    opr <=(self, b:ZZ64):Boolean
    opr CMP(self, b:ZZ64): TotalComparison

    opr -(self):ZZ64
    opr +(self,b:ZZ64):ZZ64
    opr -(self,b:ZZ64):ZZ64
    opr DOT(self,b:ZZ64):ZZ64
    opr TIMES(self,b:ZZ64):ZZ64
    opr juxtaposition(self,b:ZZ64):ZZ64
    opr DIV(self,b:ZZ64):ZZ64
    opr REM(self,b:ZZ64):ZZ64
    opr MOD(self,b:ZZ64):ZZ64
    opr GCD(self,b:ZZ64):ZZ64
    opr LCM(self,b:ZZ64):ZZ64
    opr CHOOSE(self,b:ZZ64):ZZ64
    opr BITAND(self,b:ZZ64):ZZ64
    opr BITOR(self,b:ZZ64):ZZ64
    opr BITXOR(self,b:ZZ64):ZZ64
    opr LSHIFT(self,b:ZZ64):ZZ64
    opr RSHIFT(self,b:ZZ64):ZZ64
    opr BITNOT(self):ZZ64
    narrow(self):ZZ32
    big(self):ZZ
end

trait ZZ extends { Integral[\ZZ\] } comprises { BigNum, ZZ64, NN64 }
    opr /(self,other:ZZ):QQ
    numerator(self): ZZ
    narrow(self): ZZ32
    widen(self): ZZ64
    odd(self): Boolean
    even(self): Boolean
end

(************************************************************
* \subsection*{Generator support}
************************************************************

**
 * The type %Contains[\T\]% "contains" objects of type %T%, in the sense
 * that we can check whether a %T% is a member of a %Contains[\T\]% using
 * %opr IN% and its ken, or using a %case% expression. *)
trait Contains[\T\]
    (** %opr IN% indicates whether %self% contains %elt% **)
    abstract opr IN(elt:T, self): Boolean
    (** The %MATCH% operator is used (as a temporary hack) by case when the
        match clause is a %Contains[\T\]%.  We must choose either %=%, %SEQV%, or
        %IN% as appropriate, depending on the type of the LHS. **)
    opr MATCH(x:Any, self): Boolean
end

(** %x NOTIN g% is simply %NOT (x IN g).% **)
opr NOTIN[\E\](x: E, this: Contains[\E\]): Boolean

(** %g NI x% is equivalent to %x IN g% **)
opr NI[\E\](this: Contains[\E\], x:E): Boolean

(** %g NOTNI x% is equivalent to %x NOTIN g% **)
opr NOTNI[\E\](this: Contains[\E\], x:E): Boolean

(**
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
trait Generator[\E\] extends { Contains[\E\] }
        excludes { Number }

    (** reverse returns a generator which generates the same objects
        in reverse order **)
    getter reverse(): Generator[\E\]

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

__bigOperatorSugar[\I,O,R,L\](o:BigOperator[\I,O,R,L\],g:Generator[\I\]): O

__bigOperator[\I,O,R,L\](o:BigOperator[\I,O,R,L\],desugaredClauses:(Reduction[\L\],I->L)->L): O

(** Application of two nested BIG operators, possibly with fusion.  This covers only
    comprehensions of the form:
        %BIG OUTER [ xs <- expro ] (BIG INNER [x <- xs] expri)
    The desugarer extracts comprehensions of this form from more complex nests of
    comprehensions, using a combination of splitting:
        %BIG OP [ gs1, gs2 ] expr = BIG OP [ gs1 ] (BIG OP [ gs2 ] expr)
    and filter squeezing:
        %BIG OP [ xs <- g, p(xs) ] expr = BIG OP [ xs <- g.filter(p) ] expr
    There are some big caveats to this explanation in practice.  Most important is that
    we don't unlift and lift or do input/output conversion except where neccessary, so
    splitting skips these operations in between the inner and outer comprehension.
    **)

(*
__bigOperator2[\I0,O0,R0,L0,I1,O1 extends I0,R1,L1,E,F extends Generator[\E\]\]
              (outer:BigOperator[\I0,O0,R0,L0\],
               inner:BigOperator[\I1,O1,R1,L1\],
               gg: Generator[\F\],
               innerBody: E->I1):O0
*)

(*
__bigOperator2[\I0,O0,R0,L0,I1,O1 extends I0,R1,L1,E,F extends Generator[\E\]\]
              (outer:Comprehension[\I0,O0,R0,L0\],
               inner:Comprehension[\I1,O1,R1,L1\],
               gg: Generator[\F\],
               innerBody: E->I1):O0
*)

__bigOperator2[\I0,O0,R0,L0,I1,O1,R1,L1,E,F extends Generator[\E\]\]
              (outer:BigOperator[\I0,O0,R0,L0\],
               inner:BigOperator[\I1,O1,R1,L1\],
               gg: Generator[\F\],
               innerBody: E->I1):O0

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
    seq(self): SequentialGenerator[\E\]
    map[\G\](f: E->G): SequentialGenerator[\G\]
    nest[\G\](f: E -> Generator[\G\]): Generator[\G\]
    cross[\F\](g:Generator[\F\]): Generator[\(E,F)\]
end

(** A Condition is a Generator that generates 0 or 1 element.
    Conditions can be used as nullary comprehension generators or
    as predicates in an if expression. **)
trait Condition[\E\] extends SequentialGenerator[\E\]
    getter isEmpty(): Boolean
    getter nonEmpty(): Boolean
    getter holds(): Boolean
    getter size(): ZZ32
    getter get(): E throws NotFound
    getter bounds(): CompactFullRange[\ZZ32\]
    getter indices(): CompactFullRange[\ZZ32\]
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

sequential[\T\](g:Generator[\T\]):SequentialGenerator[\T\]


(************************************************************
* \subsection{The Maybe type}
* \seclabel{maybe-type}
************************************************************

** This trait makes excludes work without where clauses, and allows opr =
   to remain non-parametric. *)
value trait AnyMaybe extends { Equality[\AnyMaybe\], AnyUniqueItem } excludes Number
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
        extends { AnyMaybe, Condition[\T\], ZeroIndexed[\T\], UniqueItem[\T\] }
        comprises { Nothing[\T\], Just[\T\] }
    opr SQCAP(self, o: Maybe[\T\]): Maybe[\T\]
end

value object Just[\T\](x:T) extends Maybe[\T\]
    getter size(): ZZ32
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
    opr SQCAP(self, o:UniqueItem[\T\]): Maybe[\T\]
    opr SQCUP(self, o:UniqueItem[\T\]): UniqueItem[\T\]
    unique(self): Maybe[\T\]
end

(** %Nothing% will become a non-parametric singleton when we get where
    clauses working. *)
value object Nothing[\T\] extends Maybe[\T\]
    getter size(): ZZ32
    getter holds(): Boolean
    getter get(): T
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
    opr SQCAP(self, o: Maybe[\T\]): Nothing[\T\]
    opr SQCAP(self, o: UniqueItem[\T\]): Nothing[\T\]
    opr SQCUP(self, o: UniqueItem[\T\]): UniqueItem[\T\]
end

value trait AnyUniqueItem extends Equality[\AnyUniqueItem\] excludes Number
    getter holds() : Boolean
    opr =(self, other:AnyUniqueItem): Boolean
end

(** The type %UniqueItem[\T\]% extends %Maybe[\T\]% from a semilattice
    to a lattice by adjoining a top element %NotUnique[\T\]%.  An
    object of type %UniqueItem[\T\]% can be used as a generator; it is either
    empty (%Nothing% or %NotUnique%) or generates the single element yielded by
    %get%, so there is no issue of canonical order or parallelism. *)
value trait UniqueItem[\T\]
        extends { AnyUniqueItem, Condition[\T\] }
        comprises { NotUnique[\T\], Maybe[\T\] }
    opr SQCAP(self, o: UniqueItem[\T\]): UniqueItem[\T\]
    opr SQCUP(self, o: UniqueItem[\T\]): UniqueItem[\T\]
    unique(self): Maybe[\T\]
end

value object NotUnique[\T\] extends UniqueItem[\T\]
    getter size():ZZ32
    getter holds():Boolean
    getter get():T
    getter asString():String
    opr |self| : ZZ32
    getDefault(t:T):T
    cond[\R\](_:T->R, e:()->R): R
    generate[\R\](r:Reduction[\R\],_:T->R): R
    opr[_:ZZ32]: T

    map[\G\](f: T->G): NotUnique[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\(T,G)\]

    mapReduce[\R\](_: T->R, _:(R,R)->R, z:R): R
    reduce(_:(T,T)->T, z:T):T
    reduce(r: Reduction[\T\]):T
    loop(f:T->()): ()
    opr IN(x:T, self): Boolean
    opr =(self,_:NotUnique[\T\]): Boolean
    opr SQCAP(self, o:UniqueItem[\T\]): UniqueItem[\T\]
    opr SQCUP(self, o:UniqueItem[\T\]): NotUnique[\T\]
end

object UniqueItemMeetReduction[\T\]
        extends { CommutativeMonoidReduction[\UniqueItem[\T\]\],
                  ReductionWithZeroes[\UniqueItem[\T\],UniqueItem[\T\]\] }
    getter asString(): String
    empty(): UniqueItem[\T\]
    join(a: UniqueItem[\T\], b: UniqueItem[\T\]): UniqueItem[\T\]
    isZero(a: UniqueItem[\T\]): Boolean
end

opr BIG SQCAP[\T\](): BigReduction[\UniqueItem[\T\],UniqueItem[\T\]\]

opr BIG SQCAP[\T\](g: Generator[\UniqueItem[\T\]\]): UniqueItem[\T\]

object UniqueItemJoinReduction[\T\]
        extends { CommutativeMonoidReduction[\UniqueItem[\T\]\],
                  ReductionWithZeroes[\UniqueItem[\T\],UniqueItem[\T\]\] }
    getter asString(): String
    empty(): UniqueItem[\T\]
    join(a: UniqueItem[\T\], b: UniqueItem[\T\]): UniqueItem[\T\]
    isZero(a: UniqueItem[\T\]): Boolean
end

opr BIG SQCUP[\T\](): BigReduction[\UniqueItem[\T\],UniqueItem[\T\]\]

opr BIG SQCUP[\T\](g: Generator[\UniqueItem[\T\]\]): UniqueItem[\T\]

(************************************************************
* \subsection*{Exception hierarchy}
************************************************************)

trait Exception comprises { UncheckedException, CheckedException }
end

(* Exceptions which are not checked *)

trait UncheckedException extends Exception excludes CheckedException
end

object FailCalled(s:String) extends UncheckedException
end

object DivisionByZero extends UncheckedException
end

object UnpastingError extends UncheckedException
end

object CallerViolation extends UncheckedException
end

object CalleeViolation extends UncheckedException
end

object LabelException extends UncheckedException
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

object IndexOutOfBounds[\I\](range:Range[\I\],index:I) extends UncheckedException
end

object EmptyReduction extends UncheckedException
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
    getter nonEmpty(): Boolean
    (** %size()% is deprecated; use %|self|% instead. *)
    abstract getter size(): ZZ32
    (** %bounds()% yields a range of indices that are valid for the
        indexed generator. *)
    abstract getter bounds(): CompactFullRange[\I\]
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
    (** reverse gives us an indexed object which is back-to-front:
        the reversed object from the lower bound to the upper bound is the
        original object from upper to lower *)
    getter reverse():Indexed[\E,I\]
    (** %indices()% yields the indices corresponding to the elements of
        the indexed object---it corresponds to the index component of
        %indexValuePairs()%.  This may in general be a subset of all the
        valid indices represented by %bounds()%.  This generator
        attempts to follow the structure of the underlying object as
        closely as possible. *)
    getter indices(): Generator[\I\]
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
    opr[_:TrivialOpenRange] : Indexed[\E,I\]

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


    (** indexOf(e) returns an index at which e can be found,
        or Nothing if no such index exists. **)
    indexOf(e:E): Maybe[\I\]
end

trait ZeroIndexed[\E\] extends Indexed[\E,ZZ32\]
    bounds(): CompactFullRange[\ZZ32\]
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
    opr[_:TrivialOpenRange]:=(v:Indexed[\E,I\]) : ()
end

(** Array whose bounds are implicit rather than static, and which may
    be either mutable or immutable. *)
trait ReadableArray[\E,I\]
        extends { HasRank, Indexed[\E,I\], DelegatedIndexed[\E,I\] }
    (** CONCRETE GETTERS
        Default implementations of getters based on abstract methods
        below. **)
    getter indices(): Generator[\I\]
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
    init(i:I, v:E): ()

    generate[\R\](r: Reduction[\R\], body: E->R): R
    seq(self): SequentialGenerator[\E\]

    (** 0-based non-bounds-checked indexing. **)
    abstract get(i:I): E
    abstract init0(i:I, e:E): ()
    abstract zeroIndices(): CompactFullRange[\I\]
    (** Convert from %base%-based indexing to 0-based indexing,
        performing bounds checking. **)
    abstract offset(i:I): I
    (** Convert from 0-based indexing to %base%-based indexing. **)
    abstract toIndex(i:I): I
    (** Indexed functionality with more specific type information. **)
    abstract opr[r:Range[\I\]] : ReadableArray[\E,I\]
    abstract opr[_:TrivialOpenRange] : ReadableArray[\E,I\]
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
    abstract opr[_:TrivialOpenRange] : ImmutableArray[\E,I\]
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
    abstract opr[_:TrivialOpenRange] : Array[\E,I\]
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
    getter bounds():CompactFullRange[\ZZ32\]
    abstract getter mutability():String
    opr |self| : ZZ32

    subarray[\nat b, nat s, nat o\](m: ZZ32): ReadableArray1[\T, b, s\]

    (** Offset converts from %b0%-indexing to 0-indexing,
        bounds checking en route. *)
    offset(i:ZZ32):ZZ32
    toIndex(i:ZZ32):ZZ32

    zeroIndices(): CompactFullRange[\ZZ32\]
end

trait ImmutableArray1[\T, nat b0, nat s0\]
    extends { StandardImmutableArrayType[\ImmutableArray1[\T,b0,s0\],T,ZZ32\],
              ImmutableArray[\T,ZZ32\], ReadableArray1[\T,b0,s0\] }
    getter mutability():String
    shift(o:ZZ32): ImmutableArray[\T,ZZ32\]
    opr[r: Range[\ZZ32\]] : ImmutableArray[\T,ZZ32\]
    opr[_:OpenRange[\ZZ32\]] : ImmutableArray1[\T,0,s0\]
    opr[_:TrivialOpenRange] : ImmutableArray1[\T,0,s0\]

    (** %subarray% selects a subarray of this array based on static parameters.
        %b#s% are the new bounds of the array; %o% is
        the index of the subarray within the current array. **)
    subarray[\nat b, nat s, nat o\](m:ZZ32):ImmutableArray1[\T, b, s\]

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
    opr[_:TrivialOpenRange] : Array1[\T,0,s0\]

    subarray[\nat b, nat s, nat o\](m:ZZ32):Array1[\T, b, s\]

    replica[\U\]():Array1[\U,b0,s0\]

    copy():Array1[\T,b0,s0\]
    freeze():ImmutableArray1[\T,b0,s0\]
    map[\R\](f:T->R): Array1[\R,b0,s0\]
    ivmap[\R\](f:(ZZ32,T)->R): Array1[\R,b0,s0\]
end

trait AnyVector end

trait Vector[\T extends Number, nat s0\]
        extends { AnyVector, Array1[\T,0,s0\], AdditiveGroup[\Vector[\T,s0\]\] }
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
__immutableFactory1[\T, nat b0, nat s0\]():ReadableArray1[\T,b0,s0\]

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
  getter bounds():CompactFullRange[\(ZZ32,ZZ32)\]
  opr |self| : ZZ32
  (** Translate from %b0%, %b1%-indexing to 0-indexing, checking bounds. **)
  offset(t1:(ZZ32,ZZ32)):(ZZ32,ZZ32)
  toIndex(t1:(ZZ32,ZZ32)):(ZZ32,ZZ32)
  opr[x:ZZ32,y:ZZ32]:=(v:T):()
  opr[r:Range[\(ZZ32,ZZ32)\]]: Array[\T,(ZZ32,ZZ32)\]
  opr[_:OpenRange[\ZZ32\]] : Array2[\T,0,s0,0,s1\]
  opr[_:TrivialOpenRange] : Array2[\T,0,s0,0,s1\]
  shift(t1:(ZZ32,ZZ32)): Array[\T,(ZZ32,ZZ32)\]

  (** 2-D subarray given static subarray parameters.
      %(bo1,bo2)#(so1,so2)% are output bounds.
      The result is the subarray starting at %(o0,o1)% in the original array,
      striding by (m0,m1).
   **)
  subarray[\nat bo0, nat so0, nat bo1, nat so1, nat o0, nat o1\]
          (m0:ZZ32, m1:ZZ32): Array2[\T,bo0,so0,bo1,so1\]

  zeroIndices():CompactFullRange[\(ZZ32,ZZ32)\]

  replica[\U\]():Array2[\U,b0,s0,b1,s1\]
  copy():Array2[\T,b0,s0,b1,s1\]
  put(t1:(ZZ32, ZZ32), v:T) : ()
  get(t1:(ZZ32, ZZ32)):T
  t():Array2[\T,b1,s1,b0,s0\]
  (* Copied here for better return type information. *)
  map[\R\](f:T->R): Array2[\R,b0,s0,b1,s1\]
  ivmap[\R\](f:((ZZ32,ZZ32),T)->R): Array2[\R,b0,s0,b1,s1\]

  freeze():ImmutableArray[\T,(ZZ32,ZZ32)\]
end

trait AnyMatrix end

trait Matrix[\T extends Number, nat s0, nat s1\]
        extends { AnyMatrix, Array2[\T, 0, s0, 0, s1\], AdditiveGroup[\Matrix[\T,s0,s1\]\] }
        excludes { AnyMultiplicativeRing }
    opr +(self, v:Matrix[\T,s0,s1\]): Matrix[\T,s0,s1\]
    opr -(self, v:Matrix[\T,s0,s1\]): Matrix[\T,s0,s1\]
    opr -(self): Matrix[\T,s0,s1\]
    scale(t1: T): Matrix[\T,s0,s1\]
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
    getter bounds():CompactFullRange[\(ZZ32,ZZ32,ZZ32)\]


    opr |self| : ZZ32

    (** Again, %offset% performs bounds checking and shifts to 0-indexing. *)
    offset(t:(ZZ32,ZZ32,ZZ32)):(ZZ32,ZZ32,ZZ32)
    toIndex(t:(ZZ32,ZZ32,ZZ32)):(ZZ32,ZZ32,ZZ32)

    (** And %get% and %put% are 0-indexed without bounds checks. *)
    abstract put(t:(ZZ32,ZZ32,ZZ32), v:T) : ()
    abstract get(t:(ZZ32,ZZ32,ZZ32)):T

    opr[i:ZZ32, j:ZZ32, k:ZZ32] := (v:T): ()
    opr[r:Range[\(ZZ32,ZZ32,ZZ32)\]]: Array[\T,(ZZ32,ZZ32,ZZ32)\]
    opr[_:OpenRange[\ZZ32\]] : Array3[\T,0,s0,0,s1,0,s2\]
    opr[_:TrivialOpenRange] : Array3[\T,0,s0,0,s1,0,s2\]
    shift(t:(ZZ32,ZZ32,ZZ32)): Array[\T,(ZZ32,ZZ32)\]

    (** 3-D subarray given static subarray parameters.
        %(bo0,bo1,bo2)#(so0,so1,so2)% are output bounds.
        The result is the subarray starting at %(o0,o1,o2)% in the original array,
        striding by (m0,m1,m2).
     **)
    subarray[\nat bo0, nat so0, nat bo1, nat so1, nat bo2, nat so2,
              nat o0, nat o1, nat o2\]
            (m0:ZZ32,m1:ZZ32,m2:ZZ32): Array3[\T,bo0,so0,bo1,so1,bo2,so2\]

    zeroIndices():CompactFullRange[\(ZZ32,ZZ32,ZZ32)\]

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

trait Reduction[\L\]
    getter reverse():Reduction[\L\]
    empty(): L
    join(a: L, b: L): L
end

(** Invariants:
    join must be associative with identity empty
    unlift(lift(x)) = x
 **)
trait ActualReduction[\R,L\] extends Reduction[\L\]
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
    cond[\G\](t:PossibleReductionPair[\R\]->G, e:()->G): G
end

trait SomeReductionPair[\R\] extends PossibleReductionPair[\R\]
    getter holds(): Boolean
    abstract getter outer(): Reduction[\R\]
    abstract getter inner(): Reduction[\R\]
    cond[\G\](t:PossibleReductionPair[\R\]->G, e:()->G): G
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

trait SomeCommutativeReduction end (* mark for commutative *)

trait DistributesOver[\E\] end (* marking for distributivity *)

trait CommutativeReduction[\R\] extends {AssociativeReduction[\R\], SomeCommutativeReduction} end

(** Monoids don't require a special lift and unlift operation. **)
trait MonoidReduction[\R\] extends ActualReduction[\R,R\]
    lift(r:R): R
    unlift(r:R): R
end

trait CommutativeMonoidReduction[\R\] extends {MonoidReduction[\R\], SomeCommutativeReduction} end

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
    empty(): ()
    join(a: (), b: ()): ()
end

(* Hack to permit any Number to work non-parametrically. *)
object SumReduction extends {
CommutativeMonoidReduction[\Number\] ,
DistributesOver[\MaxReductionN\],
DistributesOver[\MinReductionN\] }
    empty(): Number
    join(a: Number, b: Number): Number
end

opr SUM[\T extends Number\](): Comprehension[\T,Number,Number,Number\]

opr SUM[\T extends Number\](g: Generator[\T\]): Number

object ProdReduction extends {CommutativeMonoidReduction[\Number\],
DistributesOver[\SumReduction\],
DistributesOver[\MinReductionN\],  (* actually, we need to limit elements positive *)
DistributesOver[\MaxReductionN\]
 }
    empty(): Number
    join(a:Number, b:Number): Number
end

opr PROD[\T extends Number\](): Comprehension[\T,Number,Number,Number\]

opr PROD[\T extends Number\](g: Generator[\T\]): Number

object MaxReductionN extends {CommutativeMonoidReduction[\Number\] }
    getter toString(): String
    empty(): Number
    join(a: Number, b: Number): Number
end
opr BIG MAXN[\T extends Number\](): Comprehension[\T,Number,Number,Number\]
opr BIG MAXN[\T extends Number\](g: Generator[\T\]): Number

object MinReductionN extends {CommutativeMonoidReduction[\Number\] }
    getter toString(): String
    empty(): Number
    join(a: Number, b: Number): Number
end
opr BIG MINN[\T extends Number\](): Comprehension[\T,Number,Number,Number\]
opr BIG MINN[\T extends Number\](g: Generator[\T\]): Number

object MinMaxReductionN extends {CommutativeMonoidReduction[\(Number,Number)\] }
    getter asString(): String
    empty(): Number
    join(a: (Number, Number), b: (Number, Number)): (Number, Number)
end
opr BIG MINMAXN[\T extends Number\]():
        Comprehension[\T,(Number,Number),(Number,Number),(Number,Number)\]
opr BIG MINMAXN[\T extends Number\](g: Generator[\T\]): (Number, Number)

object MinReduction[\T extends StandardMin[\T\]\] extends CommutativeReduction[\T\]
    simpleJoin(a:T, b:T): T
end

opr BIG MIN[\T extends StandardMin[\T\]\](): BigReduction[\T,AnyMaybe\]

opr BIG MIN[\T extends StandardMin[\T\]\](g: Generator[\T\]): T

object MaxReduction[\T extends StandardMax[\T\]\] extends CommutativeReduction[\T\]
    simpleJoin(a:T, b:T): T
end

opr BIG MAX[\T extends StandardMax[\T\]\](): BigReduction[\T,AnyMaybe\]

opr BIG MAX[\T extends StandardMax[\T\]\](g: Generator[\T\]): T

object MinMaxReduction[\T extends StandardMinMax[\T\]\] extends CommutativeReduction[\(T,T)\]
    getter asString(): String
    simpleJoin(a:(T,T),b:(T,T)): (T,T)
end
opr BIG MINMAX[\T extends StandardMinMax[\T\]\]():
        Comprehension[\T,AnyMaybe,AnyMaybe,AnyMaybe\]
opr BIG MINMAX[\T extends StandardMinMax[\T\]\](g:Generator[\T\]):(T,T)

opr BIG MINNUM(): BigReduction[\RR64,RR64\]

opr BIG MINNUM(g: Generator[\RR64\]): RR64

opr BIG MAXNUM(): BigReduction[\RR64,RR64\]

opr BIG MAXNUM(g: Generator[\RR64\]): RR64

(** AndReduction and OrReduction take advantage of natural zeroes for early exit. **)
object AndReduction
        extends { CommutativeMonoidReduction[\Boolean\],
                  ReductionWithZeroes[\Boolean,Boolean\] }
    empty(): Boolean
    join(a: Boolean, b: Boolean): Boolean
    isZero(a:Boolean): Boolean
end

opr BIG AND[\T\](): BigReduction[\Boolean,Boolean\]

opr BIG AND[\T\](g: Generator[\Boolean\]): Boolean

object OrReduction
        extends { CommutativeMonoidReduction[\Boolean\],
                  ReductionWithZeroes[\Boolean,Boolean\] }
    empty(): Boolean
    join(a: Boolean, b: Boolean): Boolean
    isZero(a:Boolean): Boolean
end

opr BIG OR[\T\](): BigReduction[\Boolean, Boolean\]

opr BIG OR[\T\](g: Generator[\Boolean\]): Boolean

(** Operator reductions on integers *)

(*
object BitXorReduction[\T extends Integral[\T\]\]
        extends { CommutativeMonoidReduction[\T\] }
    getter asString(): String
    empty(): T
    join(a: T, b: T): T
end

opr BIG BITXOR[\T extends Integral[\T\]\](): BigReduction[\T, T\]

opr BIG BITXOR[\T extends Integral[\T\]\](g: Generator[\T\]): T
*)

object BitXorReduction
        extends { CommutativeMonoidReduction[\ZZ32\] }
    getter asString(): String
    empty(): ZZ32
    join(a: ZZ32, b: ZZ32): ZZ32
end

opr BIG BITXOR(): BigReduction[\ZZ32, ZZ32\]

opr BIG BITXOR(g: Generator[\ZZ32\]): ZZ32

(** A reduction performing String concatenation **)
object StringReduction extends MonoidReduction[\String\]
    empty(): String
    join(a:String, b:String): String
end

(** A reduction performing String concatenation with a space **)
object SpaceReduction extends MonoidReduction[\String\]
    empty(): String
    join(a:String, b:String): String
end

(** A reduction performing String concatenation with newline separation **)
object NewlineReduction extends AssociativeReduction[\String\]
    simpleJoin(a:String, b:String): String
end

(** This operator performs string concatenation, first converting
    its inputs (of type Any) to String if necessary. **)
opr BIG ||(): Comprehension[\Any,String,String,String\]

opr BIG ||(g: Generator[\Any\]): String

(** This operator performs string concatenation, first converting
    its inputs (of type Any) to String if necessary, and separating
    non-empty components by a space. **)
opr BIG |||(): Comprehension[\Any,String,String,String\]

opr BIG |||(g: Generator[\Any\]): String

(** This operator performs string concatenation with newline
    separation, first converting its inputs (of type Any) to String if
    necessary. **)
opr BIG //(): Comprehension[\Any,String,String,AnyMaybe\]

opr BIG //[\T\](g: Generator[\T\]): String

(** A %MapReduceReduction% takes an associative binary function %j% on
    arguments of type %R%, and the identity of that function %z%, and
    returns the corresponding reduction. **)
object MapReduceReduction[\R\](j:(R,R)->R, z:R) extends MonoidReduction[\R\]
    empty(): R
    join(a:R, b:R): R
end

(** A %MIMapReduceReduction% takes an associative binary function %j% on
    arguments of type %R%, and the identity of that function %z%, and
    returns the corresponding reduction. **)
object MIMapReduceReduction[\R\](j:(Any,Any)->R, z:Any) extends MonoidReduction[\Any\]
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
    generate[\R\](r:Reduction[\R\], m: E->R): R
    reduce(r: Reduction[\E\]): E
    filter(p': E -> Condition[\()\]): FilterGenerator[\E\]  (* ' *)
    seq(self): SequentialGenerator[\E\]
end

(************************************************************
* \subsection*{Ranges}
************************************************************

** Ranges in general represent uses of the %#% and %:% operators.
    It is mostly subtypes of %Range% that are interesting.

    The partial order on ranges describes containment:
      %a < b% if and only if all points in %a% are strictly contained in %b%.
 **)
trait Range[\I\] extends { StandardPartialOrder[\Range[\I\]\], Contains[\I\] }
    abstract getter stride(): I
    abstract getter left(): Maybe[\I\]
    abstract getter right(): Maybe[\I\]
    abstract getter extent(): Maybe[\I\]
    abstract getter isEmpty(): Boolean
    getter isLeftBounded(): Boolean
    getter isAnyBounded(): Boolean
    abstract truncL(l: I): RangeWithLeft[\I\]
    truncR(r: I): RangeWithRight[\I\]
    abstract flip(): Range[\I\]
    abstract forward(): Range[\I\]
    abstract every(s: I): Range[\I\]
    abstract imposeStride(s:I): Range[\I\]
    abstract atMost(n: I): Range[\I\]
    abstract opr INTERSECTION(self, other: Range[\I\]): Range[\I\]
    narrowToRange(other:Range[\I\]): Range[\I\]
    narrowToRange(other:OpenRange[\I\]): Range[\I\]
    opr =(self, b: Range[\I\]): Boolean
    abstract opr IN(n: I, self): Boolean
    opr CMP(self, other:Range[\I\]): Comparison
    abstract opr FORWARD_CMP(self, other:Range[\I\]): Comparison
    asDebugStriing(): String
    check(): Range[\I\]
    shiftLeft(shift: I): Range[\I\]
    shiftRight(shift: I): Range[\I\]
    opr << (self, shift: I): Range[\I\]
    opr >> (self, shift: I): Range[\I\]
end

trait PartialRange[\I\] extends Range[\I\] end

trait OpenRange[\I\] extends PartialRange[\I\]
    getter left(): Nothing[\I\]
    getter right(): Nothing[\I\]
    getter extent(): Nothing[\I\]
    getter isEmpty(): Boolean
    narrowToRange(other:OpenRange[\I\]): OpenRange[\I\]
    opr FORWARD_CMP(self, other:OpenRange[\I\]): Comparison
    opr FORWARD_CMP(self, other:Range[\I\]): Comparison
    opr IN(n: I, self): Boolean
end

object TrivialOpenRange extends OpenRange[\Any\]
    flip(): TrivialOpenRange
    forward(): TrivialOpenRange
    check(): TrivialOpenRange
end

trait RangeWithExtent[\I\] extends Range[\I\]
    getter extent(): Just[\I\]
end

trait ExtentRange[\I\] extends { RangeWithExtent[\I\], PartialRange[\I\] }
    getter left(): Nothing[\I\]
    getter right(): Nothing[\I\]
    getter isEmpty(): Boolean
    opr IN(n: I, self): Boolean
    opr FORWARD_CMP(self, other: Range[\I\]): Comparison
end

trait BoundedRange[\I\] extends Range[\I\]
    getter leftOrRight(): I
    every(s: I): BoundedRange[\I\]
    atMost(n: I): FullRange[\I\]
    opr INTERSECTION(self, other: Range[\I\]): BoundedRange[\I\]
    narrowToRange(other:OpenRange[\I\]): BoundedRange[\I\]
    narrowToRange(other:Range[\I\]): BoundedRange[\I\]
end

trait RangeWithLeft[\I\] extends BoundedRange[\I\]
    getter left(): Just[\I\]
    getter leftOrRight(): I
end

trait LeftRange[\I\] extends { RangeWithLeft[\I\], PartialRange[\I\] }
    getter right(): Nothing[\I\]
    getter extent(): Nothing[\I\]
    getter isEmpty(): Boolean
    opr FORWARD_CMP(self, other: Range[\I\]): Comparison
end

trait RangeWithRight[\I\] extends BoundedRange[\I\]
    getter right(): Just[\I\]
end

trait RightRange[\I\] extends { RangeWithRight[\I\], PartialRange[\I\] }
    getter left(): Nothing[\I\]
    getter leftOrRight(): Just[\I\]
    getter extent(): Nothing[\I\]
    getter isEmpty(): Boolean
    opr FORWARD_CMP(self, other: Range[\I\]): Comparison
end

trait FullRange[\I\] extends { RangeWithLeft[\I\], RangeWithRight[\I\], RangeWithExtent[\I\], Indexed[\I, I\] }
    getter extent(): Just[\I\]
    narrowToRange(other:OpenRange[\I\]): FullRange[\I\]
    narrowToRange(other:Range[\I\]): FullRange[\I\]
    opr FORWARD_CMP(self, other: Range[\I\]): Comparison
    opr FORWARD_CMP(self, other: FullRange[\I\]): Comparison
end

trait CompactFullRange[\I\] extends FullRange[\I\]
    getter lower(): I
    getter upper(): I
    opr |self| : I
    forward(): CompactFullRange[\I\]
end

trait StridedFullRange[\I\] extends FullRange[\I\] end

(** The %#% and %:% operators serve as factories for parallel ranges. **)
opr #[\I extends AnyIntegral\](lo:I, ex:I): CompactFullRange[\I\]
opr #[\I extends AnyIntegral, J extends AnyIntegral\]
     (lo:(I,J), ex:(I,J)): CompactFullRange[\(I,J)\]
opr #[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\]
     (lo:(I,J,K), ex:(I,J,K)): CompactFullRange[\(I,J,K)\]

opr :[\I extends AnyIntegral\](lo:I, hi:I): CompactFullRange[\I\]
opr :[\I extends AnyIntegral, J extends AnyIntegral\]
     (lo:(I,J), hi:(I,J)): CompactFullRange[\(I,J)\]
opr :[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\]
     (lo:(I,J,K), hi:(I,J,K)): CompactFullRange[\(I,J,K)\]

(** Factories for incomplete ranges. **)
opr (x:I)#[\I extends AnyIntegral\] : LeftRange[\I\]
opr (p:(I,J))#[\I extends AnyIntegral, J extends AnyIntegral\] : LeftRange[\(I,J)\]
opr (t:(I,J,K))#[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\] :
         LeftRange[\(I,J,K)\]

opr (x:I):[\I\] : LeftRange[\I\]

opr #[\I extends AnyIntegral\](x:I) : ExtentRange[\I\]
opr #[\I extends AnyIntegral, J extends AnyIntegral\](xy:(I,J)) : ExtentRange[\(I,J)\]
opr #[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](xyz:(I,J,K)) :
         ExtentRange[\(I,J,K)\]

opr :[\I extends AnyIntegral\](x:I) : RightRange[\I\]
opr :[\I extends AnyIntegral, J extends AnyIntegral\](xy:(I,J)) : RightRange[\(I,J)\]
opr :[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](xyz:(I,J,K)) :
         RightRange[\(I,J,K)\]

(* Actually want:
opr (x:T)#[\T\] : LeftRange[\T\]
opr (x:T):[\T\] : LeftRange[\T\]
opr #[\T\](x:T) : ExtentRange[\T\]
opr :[\T\](x:T) : RightRange[\T\]
*)

opr #(): TrivialOpenRange
opr :(): TrivialOpenRange
openRange[\I\](): OpenRange[\I\]

(** Factories for bare strided ranges.

    The %::% operator is used to construct strided ranges with information missing:
    %A::% is equivalent to %A:% or %A#%.
    %::S% takes every %S%'th element.
    %A::S% is a range open to the right, starting with %A% and striding by %S%.

*)

opr (l:I)::[\I\] : LeftRange[\I\]

opr ::[\I extends AnyIntegral\](s:I) : OpenRange[\I\]
opr ::[\I extends AnyIntegral, J extends AnyIntegral\](ij:(I,J)) : OpenRange[\(I,J)\]
opr ::[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\]
      (ijk:(I,J,K)) : OpenRange[\(I,J,K)\]

opr ::[\I\](l:I,s:I): LeftRange[\I\]

(** Operators on ranges.

    Most of these build on an existing range; however, due to operator
    associativity we can't always handle every case gracefully.  Here
    are fully-parenthesized versions of tight uses of the range
    construction operators.  We indicate which ones are handled:

%((A:B):C)%
%((A#B):C)%
%((:A):B)%
%((#A):B)%
%((::A):B)%

   All impose striding on the left-hand range.  Note that in the case of %A#B% this will
   in general *decrease* the size of the range.  You might want to actually write %(A::B)#C%,
   with the parentheses, if that's not what you want.

%(A:(B#C))%  For now we reject this.
%(A:(B:))%   Don't use these, just write %A:B% instead.
%(A:(B#))%
%(A:(B::))%

%((A#B)#C)%  Reject for now.
%((:A)#B)%   The remaining 3 are sized ranges specifying an upper bound.
%((#A)#B)%
%((::A)#B)%

%(A#(B:))%   Use %A#B% instead.
%(A#(B#))%
%(A#(B::))%

The following are rejected at present:

%((A::B)::C)%
%((A#B)::C)%
%((A:B)::C)%
%((:A)::B)%
%((#A)::B)%
%((::A)::B)%

%(A::(B#C))%
    Note that you probably wanted %(A::B)#C% (%C% elements, starting
    from %A% and striding by %B%).  For the moment, parenthesize this.
    In this form we can't distinguish it from the next line, and
    the two should behave differently when written %A::B#C% and %A::B:C%.
%(A::(B:C))%
%(A::(B:))%
%(A::(B#))%
%(A::(B::))%

*)

opr :[\I\](r: Range[\I\], stride:I): Range[\I\]
(* This doesn't obey the subtyping rule due to the genericity involved!
opr :[\I\](r: FullRange[\I\], stride:I): FullRange[\I\] = r.imposeStride(stride)
*)

opr #[\I\](r: PartialRange[\I\], size:I): Range[\I\]

(************************************************************
* STRINGS
************************************************************)

trait String extends { StandardTotalOrder[\String\], ZeroIndexed[\Char\] }
    getter size() : ZZ32
    getter indices() : CompactFullRange[\ZZ32\]
    getter left(): Maybe[\Char\]
    getter right(): Maybe[\Char\]
    getter depth() : ZZ32
    getter asFlatString(): String
    getter isBalanced(): Boolean


    verify() : ()       (* Verify the data structure invaraints of self *)
    opr |self| : ZZ32
    opr CASE_INSENSITIVE_CMP(self, other:String): TotalComparison

    opr [i:ZZ32]: Char
    (** As a convenience, we permit LeftRange indexing to go 1 past the bounds
        of the string, returning the empty string, in order to permit some convenient
        string-trimming idioms. **)
    opr[r0:Range[\ZZ32\]] : String

    (** This version is like [ ] above, but does not do the bounds checking.  Really, this
         should be in a "friends" interface.  It is the responsibility of uncheckedSubstring to do
         whatever optimizationms for empty ranges and trivial (whole string) ranges may be
         appropriate for the representation.*)
    uncheckedSubstring(r0: Range[\ZZ32\]) : String
    allButLast(): String
    allButFirst(): String

    abstract splitWithOffsets(): Generator[\(ZZ32, String)\]
    abstract split(): Generator[\String\]


    rangeContains(r: Range[\ZZ32\], c: Char) : Boolean

    (** Answers a subdivision of self into substrings.  This method must take time
        proportional to the number of pairs generated.   If there is no such subdivision,
        it's fine to answer the empty generator Nothing, but in this case the
        implementation of the subtrait must offer linear time indexing.
        The pairs (start, str) that are generated by this method must be such that
            start[0] = 0,      and
            start[i] = | str[0] || ... || str[i-1] |,       and
            str[0] || str [1] || ... || str[n] = self
    **)

    splitWithOffsets(): Generator[\(ZZ32, String)\]
    split(): Generator[\String\]

    (**  A balanced version of the receiver  **)
    balanced(): String (*  ensures {outcome.isAlmostBalanced AND outcome = self} *)

    opr ^(self, n: ZZ32): String

    upto(c: Char): String
    beyond(c: Char): String

    (** The operator %||% with at least one String argument converts to string and
        appends **)
    opr ||(self, b:String):String
    opr ||(self, b:Number):String
    opr ||(self, c:Char):String
    opr ||(self, b:()):String
    opr ||(self, b:(Any,Any)):String
    opr ||(self, b:(Any,Any,Any)):String
    opr ||(a:Any, self):String
    opr ||(self, b:Any):String

    (** The operator %|||% with at least one String argument converts to string,
        then concatenates with a whitespace separator unless one of the two arguments is
        empty.  If there is an empty argument, the other argument is returned. **)
    opr |||(self, b:String): String
    opr |||(self, b:Any): String
    opr |||(a:Any, self): String

    (** Right now for backward compatibility juxtaposition works like %||% **)
    opr juxtaposition(a:Any, self):String
    opr juxtaposition(self, b:String):String
    opr juxtaposition(self, b:Any):String

    (** opr // concatenates with a single newline separator. **)
    opr //(self) : String
    opr //(self, a:String): String
    opr //(self, a:Any): String
    opr //(a:Any, self): String

    (** opr /// concatenates with a double newline separator **)
    opr ///(self) : String
    opr ///(self, a:String): String
    opr ///(self, a:Any): String
    opr ///(a:Any, self): String

   abstract writeOn(s: WriteStream): ()
   stats(): StringStats

   join[\E\](g:Generator[\E\]):String

end String

object StringJoinReduction(s:String) extends Reduction[\Maybe[\String\]\] end

reverse(sequence: String): String

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
randomZZ32(x:ZZ32): ZZ32

match(regex:FlatString,some:FlatString):Boolean

(** %char% converts an integer unicode code point into the
    corresponding 16-bit %Char%.  Note that we don't presently deal
    gracefully with %Char%s outside the 16-bit plane. **)
char(a:ZZ32):Char

print(a: String):()
println(a: String): ()
errorPrint(a: String):()
errorPrintln(a: String): ()

print(a:Number):()
println(a:Number):()
errorPrintln(a:Number):()
print(a:Boolean):()
println(a:Boolean):()
errorPrintln(a:Boolean):()
print(a:Any):()
println(a:Any):()
errorPrint(a:Any):()
errorPrintln(a:Any):()
(** 0-argument versions handle passing of () to single-argument versions. **)
print():()
println():()
errorPrint():()
errorPrintln():()

forDigit(x:ZZ32, radix:ZZ32): Maybe[\Char\]
forDigit(x:ZZ32, radixString:String): Maybe[\Char\]

(** Convert string to arbitrary integral type.  String may contain
    leading - or +.  Valid radixes are 2,8,10,12, and 16.  No spaces
    may occur in the string, and it must have at least one digit.

    Right now we're oblivious to overflow!  This ought to be fixed.

    This ought to support arbitrary integer types, but right now
    there's no clean way to convert all the arithmetic to use a
    provided type without having an instance of that type in hand.
    Unsigned types in particular are a bit sticky.
 **)
strToInt(s: String, radix: ZZ32): ZZ32

(** Radix-10 digit conversion.  All caveats of the flexible-radix conversion above apply. *)
strToInt(s: String): ZZ32

strToFloat(s: String): RR64

(** opr // appends a single newline separator. **)
opr (x:Any)// : String

(** opr /// appends a double newline separator **)
opr (x:Any)/// : String

(** The following three functions are useful temporary hacks for
    debugging multi-threaded programs. **)
printThreadInfo(a:String):()
printThreadInfo(a:Number):()
printWithThread(a:String):()
printlnWithThread(a:String):()

(* time to get rid of this:
throwError(a:String):()
*)

opr SEQV(a:Any, b:Any):Boolean

opr XOR(a:Boolean, b:Boolean):Boolean
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


(*---------------------------- for Generators-of-Generators ---------------*)
switchDispatching(r : Boolean) : ()
dispatchingEnabled() : Boolean

trait SomeGenerator2 end  (* marking for GG *)

(* non-filtered GG *)
trait Generator2[\ E \]
  extends {Generator[\ Generator[\ E \] \], SomeGenerator2}
  getter seed() : Generator[\ E \]

  generate2[\ R, L1, L2 \](q : ActualReduction[\ R, L1 \], r : ActualReduction[\ R, L2 \], f : E -> R) : R

  theorems[\R, L1, L2\]() : Generator[\((ActualReduction[\ R, L1 \], ActualReduction[\ R, L2 \], E -> R)->Boolean, (ActualReduction[\ R, L1 \], ActualReduction[\ R, L1 \], E -> R)->R)\]

  (* this is just for internal use *)
  __generate2filtered[\ R, L1, L2 \](q : ActualReduction[\ R, L1 \], r : ActualReduction[\ R, L2 \], p : Generator[\E\] -> Condition[\()\], f : E -> R) : R

  theoremsFiltered[\R, L1, L2\]() : Generator[\((ActualReduction[\ R, L1 \], ActualReduction[\ R, L2 \], Generator[\E\] -> Condition[\()\], E -> R)->Boolean, (ActualReduction[\ R, L1 \], ActualReduction[\ R, L2 \], Generator[\E\] -> Condition[\()\], E -> R)->R)\]

  filter(p: Generator[\ E \] -> Condition[\()\]): Generator2[\E\]
end

(*---------------------------Relational Predicates -------------------------*)
(* base of relational predicates condition *)
trait RelationalPredicateCondition[\E\] extends { Condition[\()\] } excludes Condition[\()\]
  relation() : (E, E) -> Boolean
  target() : Generator[\ E \]
  cond[\G\](t: () -> G, e: () -> G) : G
end

(* Shortcut to create a relational predicate *)
relationalPredicate[\E\](relation : (E, E) -> Boolean) : E -> RelationalPredicateCondition[\ E \]

end
