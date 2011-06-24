(*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api CompilerLibrary

(************************************************************
 * Value bindings
 ************************************************************)

(************************************************************
 * Simple Combinators
 ************************************************************)

ignore(_:Any):()

opr ===(a:Any, b:Any):Boolean
opr NEQV(a:Any, b:Any):Boolean

fail(s: String): None

assert(flag: Boolean): ()
assert(flag: Boolean, failMsg: String): ()
deny(flag: Boolean): ()
deny(flag: Boolean, failMsg: String): ()

(************************************************************
* \subsection*{Exception hierarchy}
************************************************************)
trait Exception comprises { UncheckedException, CheckedException }
end

(* Exceptions which are not checked *)

trait UncheckedException extends Exception excludes CheckedException end

object FailCalled(s:String) extends UncheckedException end

object DivisionByZero extends UncheckedException end

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

(*
object IndexOutOfBounds[\I\](range:Range[\I\],index:I) extends UncheckedException end
*)

object EmptyReduction extends UncheckedException end

object NegativeLength extends UncheckedException end

object IntegerOverflow extends UncheckedException end

object RationalComparisonError extends UncheckedException end

object FloatingComparisonError extends UncheckedException end


(* Checked Exceptions *)

trait CheckedException extends Exception excludes UncheckedException end


(************************************************************
 * Simple Range support
 ************************************************************)

trait GeneratorZZ32 excludes { Boolean }
    abstract getter asString(): String
    seq(self): SeqGeneratorZZ32
    abstract loop(body:ZZ32->()): ()
    abstract generate(r: ReductionString, body: ZZ32->String): String
    abstract seqloop(body:ZZ32->()): ()
    abstract seqgenerate(r: ReductionString, body: ZZ32->String): String
    abstract filter(f: ZZ32 -> Boolean): GeneratorZZ32
    opr IN(x:ZZ32, self): Boolean
end

trait SeqGeneratorZZ32 extends GeneratorZZ32
    abstract filter(f: ZZ32 -> Boolean): SeqGeneratorZZ32
end

opr =(left:GeneratorZZ32, right:GeneratorZZ32): Boolean

__bigOperator(o:ReductionString,
              desugaredClauses:(ReductionString, String->String)->String): String

__generate(g: GeneratorZZ32, r: ReductionString, f:ZZ32->String): String
__generate(p: Boolean, r: ReductionString, f:()->String): String

__loop(g: GeneratorZZ32, body: ZZ32->()): ()

trait ReductionString
    abstract empty(): String
    abstract join(a: String, b: String): String
end

object StringConcatenation extends ReductionString
    empty(): String
    join(a: String, b: String): String
end

opr :(lo:ZZ32, hi:ZZ32): GeneratorZZ32
opr #(lo:ZZ32, sz:ZZ32): GeneratorZZ32

(*
opr BIG ||(): ReductionString
*)

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

end
