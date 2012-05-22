(*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api CompilerLibrary

import CompilerBuiltin.{...}
import CompilerAlgebra.{...}

(************************************************************
 * Value bindings
 ************************************************************)

(************************************************************
 * Simple Combinators
 ************************************************************)

ignore(_:Any):()
identity[\T extends Any\](x: T): T
instanceOf[\T extends Any\](x: Any) : Boolean
cast[\T extends Any\](x: Any): T

opr ===(a:Any, b:Any):Boolean
opr NEQV(a:Any, b:Any):Boolean

fail(s: String): None

assert(flag: Boolean): ()
assert(flag: Boolean, failMsg: String): ()
deny(flag: Boolean): ()
deny(flag: Boolean, failMsg: String): ()

debugString(x: Any): String

(************************************************************
* \subsection*{Exception hierarchy}
************************************************************)

object FailCalled(s:String) extends UncheckedException end

object UnpastingError extends UncheckedException end

object CallerViolation extends UncheckedException end

object CalleeViolation extends UncheckedException end

object LabelException extends UncheckedException end

object TestFailure extends UncheckedException end

object ContractHierarchyViolation extends UncheckedException end

object NoEqualityOnFunctions extends UncheckedException end

object InvalidRange extends UncheckedException end

object ForbiddenException(chain : Exception) extends UncheckedException end

(* Should this be called "IndexNotFound" instead? *)
object NotFound extends UncheckedException end

object NoValueForKey(x: Any) extends UncheckedException end

(*
object IndexOutOfBounds[\I\](range:Range[\I\],index:I) extends UncheckedException end
*)

object EmptyReduction extends UncheckedException end

object RationalComparisonError extends UncheckedException end

object FloatingComparisonError extends UncheckedException end

object MatchFailure extends UncheckedException end

object CastException extends UncheckedException end

(************************************************************
 * Simple Range support
 ************************************************************)

trait GeneratorZZ32 excludes { Boolean }   (*) excludes { Boolean, AllGenerators  }
    abstract getter asString(): String
    seq(self): SeqGeneratorZZ32
    abstract loop(body:ZZ32->()): ()
    abstract generate(r: ReductionString, body: ZZ32->String): String
    abstract generate(r: ReductionZZ32, body: ZZ32->ZZ32): ZZ32
    abstract seqloop(body:ZZ32->()): ()
    abstract seqgenerate(r: ReductionString, body: ZZ32->String): String
    abstract seqgenerate(r: ReductionZZ32, body: ZZ32->ZZ32): ZZ32
    abstract filter(f: ZZ32 -> Boolean): GeneratorZZ32
    opr IN(x:ZZ32, self): Boolean
end

trait SeqGeneratorZZ32 extends GeneratorZZ32
    abstract getter asString(): String
    abstract filter(f: ZZ32 -> Boolean): SeqGeneratorZZ32
end

opr =(left:GeneratorZZ32, right:GeneratorZZ32): Boolean

__bigOperator(o:ReductionString,
              desugaredClauses:(ReductionString, String->String)->String): String
__bigOperator(o:ReductionZZ32,
              desugaredClauses:(ReductionZZ32, ZZ32->ZZ32)->ZZ32): ZZ32

__generate(g: GeneratorZZ32, r: ReductionString, f:ZZ32->String): String
__generate(g: GeneratorZZ32, r: ReductionZZ32, f:ZZ32->ZZ32): ZZ32
__generate(p: Boolean, r: ReductionString, f:()->String): String
__generate(p: Boolean, r: ReductionZZ32, f:()->ZZ32): ZZ32

__loop(g: GeneratorZZ32, body: ZZ32->()): ()

trait Range extends GeneratorZZ32 excludes { Number, String, Boolean, Character }
  abstract getter lowerBound(): ZZ32
  abstract getter upperBound(): ZZ32
end

trait ReductionString
    abstract empty(): String
    abstract join(a: String, b: String): String
end

trait ReductionZZ32 excludes ReductionString
    abstract empty(): ZZ32
    abstract join(a: ZZ32, b: ZZ32): ZZ32
end

object StringConcatenation extends ReductionString
    empty(): String
    join(a: String, b: String): String
end

object ZZ32Addition extends ReductionZZ32
    empty(): ZZ32
    join(a: ZZ32, b: ZZ32): ZZ32
end

object ZZ32Max extends ReductionZZ32
    empty(): ZZ32
    join(a: ZZ32, b: ZZ32): ZZ32
end

opr :(lo:ZZ32, hi:ZZ32): GeneratorZZ32
opr #(lo:ZZ32, sz:ZZ32): GeneratorZZ32

(*
opr BIG ||(): ReductionString
*)

opr BIG MAX(): ReductionZZ32
opr BIG MAX(g: GeneratorZZ32): ZZ32

opr PREFIX_SUM(x: ZZ32Vector): ZZ32Vector
opr +(x: ZZ32Vector, y: ZZ32): ZZ32Vector
opr -(x: ZZ32Vector, y: ZZ32): ZZ32Vector
opr MIN(x: ZZ32Vector, y: ZZ32): ZZ32Vector
opr MAX(x: ZZ32Vector, y: ZZ32): ZZ32Vector


(*) trait Condition[\E extends Equality[\E\]\]   (*) extends { ZeroIndexed[\E\], SequentialGenerator[\E\] }
(*)     getter isEmpty(): Boolean
(*)     getter isNotEmpty(): Boolean
(*)     getter holds(): Boolean
(*)     getter size(): ZZ32
(*)     getter get(): E throws NotFound
(*)     getter indices(): Generator[\ZZ32\]
(*)     getter indexValuePairs(): Condition[\(ZZ32,E)\]
(*)     getter reverse() : Condition[\E\]
(*)     opr |self|: ZZ32
(*)     opr [i:ZZ32]:E throws NotFound
(*)     getDefault(e:E): E
(*)     cond[\G\](t: E -> G, e: () -> G): G
(*)     generate[\G\](r:Reduction[\G\], body: E -> G): G

(*)     map[\G\](f: E->G): Condition[\G\]
(*)     ivmap[\G\](f: (ZZ32,E)->G): Condition[\G\]
(*)     nest[\G\](f: E -> Generator[\G\]): Generator[\G\]
(*)     cross[\G\](g: Generator[\G\]): Generator[\(E,G)\]
(*)     mapReduce[\R\](body: E->R, _:(R,R)->R, id:R): R
(*)     reduce(_:(E,E)->E, z:E):E
(*)     reduce(r: Reduction[\E\]):E
(*)     loop(f:E->()): ()
(*)     opr IN(x:E, self):Boolean
(*) end Condition

(*) value trait Maybe[\T\] extends Condition[\T\]
(*)         comprises { Just[\T\], NothingObject[\T\] }
(*)     coerce(x: Nothing)
(*)     opr SQCAP(self, o: Maybe[\T\]): Maybe[\T\]
(*) end

(*) value object Just[\T\](x:T) extends Maybe[\T\] end

(*) value object NothingObject[\T\] extends Maybe[\T\]
(*)     coerce(x: Nothing)
(*) end

(*) object Nothing end    


(************************************************************
* Making vectors
************************************************************)

makeZZ32Vector(d1:ZZ32): ZZ32Vector
makeZZ32Vector(d1:ZZ32, d2:ZZ32): ZZ32Vector
makeZZ32Vector(r: Range): ZZ32Vector
makeZZ32Vector(r1: Range, r2: Range): ZZ32Vector

(************************************************************
* Random numbers
************************************************************)

(*
random(i:RR64): RR64
randomZZ32(x:ZZ32): ZZ32
*)

(************************************************************
 * Matrices (stub)
 ************************************************************)
trait Matrix[\T, nat s0, nat s1\] extends Object end

(************************************************************
 * Number properties
 ************************************************************)

ZZ32_MIN: ZZ32
ZZ32_MAX: ZZ32
ZZ64_MIN: ZZ64
ZZ64_MAX: ZZ64
NN32_MIN: NN32
NN32_MAX: NN32
NN64_MIN: NN64
NN64_MAX: NN64

(************************************************************
 * Character properties
 ************************************************************)

characterMinSupplementaryCodePoint: ZZ32
characterMinRadix: ZZ32
characterMaxRadix: ZZ32

(*) Character categories
characterCombiningSpacingMark: ZZ32
characterConnectorPunctuation: ZZ32
characterControl: ZZ32
characterCurrencySymbol: ZZ32
characterDashPunctuation: ZZ32
characterDecimalDigitNumber: ZZ32
characterEnclosingMark: ZZ32
characterEndPunctuation: ZZ32
characterFinalQuotePunctuation: ZZ32
characterFormat: ZZ32
characterInitialQuotePunctuation: ZZ32
characterLetterNumber: ZZ32
characterLineSeparator: ZZ32
characterLowercaseLetter: ZZ32
characterMathSymbol: ZZ32
characterModifierLetter: ZZ32
characterModifierSymbol: ZZ32
characterNonSpacingMark: ZZ32
characterOtherLetter: ZZ32
characterOtherNumber: ZZ32
characterOtherPunctuation: ZZ32
characterOtherSymbol: ZZ32
characterParagraphSeparator: ZZ32
characterPrivateUse: ZZ32
characterSpaceSeparator: ZZ32
characterStartPunctuation: ZZ32
characterSurrogate: ZZ32
characterTitlecaseLetter: ZZ32
characterUnassigned: ZZ32
characterUppercaseLetter: ZZ32

(*) Character directionality
characterDirectionalityArabicNumber: ZZ32
characterDirectionalityBoundaryNeutral: ZZ32
characterDirectionalityCommonNumberSeparator: ZZ32
characterDirectionalityEuropeanNumber: ZZ32
characterDirectionalityEuropeanNumberSeparator: ZZ32
characterDirectionalityEuropeanNumberTerminator: ZZ32
characterDirectionalityLeftToRight: ZZ32
characterDirectionalityLeftToRightEmbedding: ZZ32
characterDirectionalityLeftToRightOverride: ZZ32
characterDirectionalityNonspacingMark: ZZ32
characterDirectionalityOtherNeutrals: ZZ32
characterDirectionalityParagraphSeparator: ZZ32
characterDirectionalityPopDirectionalFormat: ZZ32
characterDirectionalityRightToLeft: ZZ32
characterDirectionalityRightToLeftArabic: ZZ32
characterDirectionalityRightToLeftEmbedding: ZZ32
characterDirectionalityRightToLeftOverride: ZZ32
characterDirectionalitySegmentSeparator: ZZ32
characterDirectionalityUndefined: ZZ32
characterDirectionalityWhitespace: ZZ32

end
