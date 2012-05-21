(*******************************************************************************
    Copyright 2011 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api QuickCheck

import List.{...}
import Set.{...}
import Map.{...}
import QuickSort.{...}
import Random.{...}

(** A test context. **)
object TestContext(arbitrary:Arbitrary, g:AnySeededRandomGen,
                   numTests:ZZ32, maxTests:ZZ32, size:ZZ32)
    (** Zero `size` means the generator should avoid recursion from now on. **)
    getter leaf(): Boolean

    (** Generates a test generator of given type, using specified `Arbitrary`
        factory. **)
    gen[\P\](): Gen[\P\]

    (** Returns a copy of `TestContext` which is resized. **)
    resized(newsize:ZZ32): TestContext
    (** Returns a copy of `TestContext` which random number generator is
        replaced to given one. **)
    withRandomGen(g':AnySeededRandomGen): TestContext

    (** Generates a `ZZ32` value uniformly from the current random number
        generator. It is most suitable for the implementation of `Gen[\ZZ32\]`. **)
    random(): ZZ32
    (** Generates a random number from `0` to `n-1` (inclusive) uniformly. **)
    random(n:ZZ32): ZZ32
    (** Generates a random number from `lo` to `hi-1` (inclusive) uniformly. **)
    random(lo:ZZ32, hi:ZZ32): ZZ32

    (** Generates a random choice from given choices. **)
    oneOf[\T\](choices:Indexed[\T,ZZ32\]): T

    (** Generates a `ZZ32` value which range is determined by `level`.
        It will produce small numbers and large numbers fairly uniformly. **)
    randomByLevel(level:ZZ32): ZZ32
end

(** Generates a random test context from a given random number generator.
    See also `genTestContext`. **)
TestContext(arb:Arbitrary, g:AnySeededRandomGen): TestContext

(** Generates a random test context from a given random number generator and
    a given number of maximum tests. **)
TestContext(arb:Arbitrary, g:AnySeededRandomGen, numTests:ZZ32): TestContext

(**********************************************************)
(* `Gen` interface and utilities *)

(** A version of `Gen[\T\]` without static parameters. **)
trait AnyGen
    abstract generate(c:TestContext): Any
    abstract perturb(obj:Any, g:AnySeededRandomGen): AnySeededRandomGen
end

(** A test generator for type `T`. Minimal complete definition of `Gen` is
    `generate`, but one should provide `perturb` and `shrink` methods if
    applicable.

    `Gen[\T\]` is a generator for type `T`, //exactly//. Naturally if `T <: U`
    then it is appropriate that `Gen[\T\]` is usable for `U` too (as any
    instance of `T` can be used in place of instance of `U`), and this is
    supported by the fact that `Gen[\T\] <: Gen[\U\]`. The problem is, however,
    an arrow type which has a contravariant context. Assuming `P' <: P` and
    `Q <: Q'`:

    {{{
      Gen[\P->Q\]     <:     Gen[\P'->Q\]     <:    Gen[\P'->Q'\]
          ..                      ..                     ..
          \/                      \/                     \/
    genArrow[\P,Q\]   :>   genArrow[\P',Q\]   <:  genArrow[\P',Q'\]
    }}}

    What this means is `Gen[\P->Q\] :> genArrow[\P',Q\]`, which enables the use
    of an instance of `Gen[\P'->Q\]` in place of `Gen[\P->Q\]` while it is
    actually invalid. Also we may unintentionally omit the testing of some
    values, if trait `T` comprises of object `T1` and `T2` and we only have
    an instance of `Gen[\T1\]`. (In this case we should report `Gen[\T2\]` is
    missing when `Gen[\T\]` is requested.) Thus we use exact matching,
    eliminating these subtle problems. **)
trait Gen[\T\] extends AnyGen
    (** Generates an instance of `T` in the context of `c`. It should depend
        only on `c`, as the test should be deterministic. **)
    abstract generate(c:TestContext): T

    (** Perturbs a random number generator using a given `obj`. It should return
        a copy of the generator which is independent of `g`, and its return
        should depend on `obj` and `g` only. See `genArrow` test generator for
        the use of this method. **)
    perturb(obj:T, g:AnySeededRandomGen): AnySeededRandomGen

    (** Returns a list of objects which are similar to `obj` but also smaller.
        The default implementation is to return nothing. See `shrink` function
        for the use of this method. **)
    shrink(obj:T): Generator[\T\]
end

(** A default implementation of test generators for `Generator[\E\]`. **)
trait GenGenerator[\E,T extends Generator[\E\]\] extends Gen[\T\]
    (** A test generator for `E`. **)
    genE: Gen[\E\]
    (** Transforms a resulting `Generator[\E\]` back to a desired type, `T`. **)
    abstract fromGenerator(obj:Generator[\E\]): T

    generate(c:TestContext): Generator[\E\]
    shrink(obj:T): Generator[\T\]
end

(**********************************************************)
(* `Gen` instances *)

object genVoid extends Gen[\()\]
    getter asString(): String
    generate(c:TestContext): ()
    perturb(_:(), g:AnySeededRandomGen): AnySeededRandomGen
end

object genBoolean extends Gen[\Boolean\]
    getter asString(): String
    generate(c:TestContext): Boolean
    perturb(obj:Boolean, g:AnySeededRandomGen): AnySeededRandomGen
end

object genZZ32 extends Gen[\ZZ32\]
    getter asString(): String
    generate(c:TestContext): ZZ32
    perturb(obj:ZZ32, g:AnySeededRandomGen): AnySeededRandomGen
end

object genZZ64 extends Gen[\ZZ64\]
    getter asString(): String
    generate(c:TestContext): ZZ64
    perturb(obj:ZZ64, g:AnySeededRandomGen): AnySeededRandomGen
end

object genZZ extends Gen[\ZZ\]
    getter asString(): String
    generate(c:TestContext): ZZ
    perturb(obj:ZZ, g:AnySeededRandomGen): AnySeededRandomGen
end

object genQQ extends Gen[\QQ\]
    getter asString(): String
    generate(c:TestContext): QQ
    perturb(obj:QQ, g:AnySeededRandomGen): AnySeededRandomGen
end

object genRR32 extends Gen[\RR32\]
    getter asString(): String
    generate(c:TestContext): RR32
    perturb(obj:RR32, g:AnySeededRandomGen): AnySeededRandomGen
end

object genRR32small extends Gen[\RR32\]
    getter asString(): String
    generate(c:TestContext): RR32
    perturb(obj:RR32, g:AnySeededRandomGen): AnySeededRandomGen
end

object genRR64 extends Gen[\RR64\]
    getter asString(): String
    generate(c:TestContext): RR64
    perturb(obj:RR64, g:AnySeededRandomGen): AnySeededRandomGen
end

object genRR64small extends Gen[\RR64\]
    getter asString(): String
    generate(c:TestContext): RR64
    perturb(obj:RR64, g:AnySeededRandomGen): AnySeededRandomGen
end

object genChar extends Gen[\Char\]
    getter asString(): String
    generate(c:TestContext): Char
    perturb(obj:Char, g:AnySeededRandomGen): AnySeededRandomGen
end

object genString extends GenGenerator[\Char,String\]
    getter asString(): String
    genE: Gen[\Char\]
    fromGenerator(obj:Generator[\Char\]): String
    perturb(obj:String, g:AnySeededRandomGen): AnySeededRandomGen
end

object genGenerator[\E\](genE:Gen[\E\]) extends GenGenerator[\E,Generator[\E\]\]
    getter asString(): String
    fromGenerator(obj:Generator[\E\]): Generator[\E\]
    generate(c:TestContext): Generator[\E\]
end

object genMaybe[\T\](genT:Gen[\T\]) extends Gen[\Maybe[\T\]\]
    getter asString(): String
    generate(c:TestContext): Maybe[\T\]
    perturb(_:Nothing[\T\], g:AnySeededRandomGen): AnySeededRandomGen
    perturb(obj:Just[\T\], g:AnySeededRandomGen): AnySeededRandomGen
    shrink(_:Nothing[\T\]): Generator[\Maybe[\T\]\]
    shrink(obj:Just[\T\]): Generator[\Maybe[\T\]\]
end

object genList[\E\](genE0:Gen[\E\]) extends GenGenerator[\E,List[\E\]\]
    getter asString(): String
    genE: Gen[\E\]
    fromGenerator(obj:Generator[\E\]): List[\E\]
    perturb(obj:List[\E\], g:AnySeededRandomGen): AnySeededRandomGen
end

object genSet[\E\](genE0:Gen[\E\]) extends GenGenerator[\E,Set[\E\]\]
    getter asString(): String
    genE: Gen[\E\]
    fromGenerator(obj:Generator[\E\]): Set[\E\]
    perturb(obj:Set[\E\], g:AnySeededRandomGen): AnySeededRandomGen
end

object genMap[\K,V\](genK:Gen[\K\], genV:Gen[\V\]) extends GenGenerator[\(K,V),Map[\K,V\]\]
    getter asString(): String
    genE: Gen[\(K,V)\]
    fromGenerator(obj:Generator[\(K,V)\]): Map[\K,V\]
    perturb(obj:Map[\K,V\], g:AnySeededRandomGen): AnySeededRandomGen
end

object genRange[\I\](genI:Gen[\I\]) extends Gen[\Range[\I\]\]
    getter asString(): String
    generate(c:TestContext): Range[\I\]
    perturb(obj:Range[\I\], g:AnySeededRandomGen): AnySeededRandomGen
end

object genTuple2[\T1,T2\](genT1:Gen[\T1\], genT2:Gen[\T2\]) extends Gen[\(T1,T2)\]
    getter asString(): String
    generate(c:TestContext): (T1,T2)
    perturb(obj:(T1,T2), g:AnySeededRandomGen): AnySeededRandomGen
    shrink(obj:(T1,T2)): Generator[\(T1,T2)\]
end

object genTuple3[\T1,T2,T3\](genT1:Gen[\T1\], genT2:Gen[\T2\], genT3:Gen[\T3\])
    extends Gen[\(T1,T2,T3)\]

    getter asString(): String
    generate(c:TestContext): (T1,T2,T3)
    perturb(obj:(T1,T2,T3), g:AnySeededRandomGen): AnySeededRandomGen
    shrink(obj:(T1,T2,T3)): Generator[\(T1,T2,T3)\]
end

object genTuple4[\T1,T2,T3,T4\](genT1:Gen[\T1\], genT2:Gen[\T2\], genT3:Gen[\T3\],
                                genT4:Gen[\T4\])
    extends Gen[\(T1,T2,T3,T4)\]

    getter asString(): String
    generate(c:TestContext): (T1,T2,T3,T4)
    perturb(obj:(T1,T2,T3,T4), g:AnySeededRandomGen): AnySeededRandomGen
    shrink(obj:(T1,T2,T3,T4)): Generator[\(T1,T2,T3,T4)\]
end

object genTuple5[\T1,T2,T3,T4,T5\](genT1:Gen[\T1\], genT2:Gen[\T2\], genT3:Gen[\T3\],
                                   genT4:Gen[\T4\], genT5:Gen[\T5\])
    extends Gen[\(T1,T2,T3,T4,T5)\]

    getter asString(): String
    generate(c:TestContext): (T1,T2,T3,T4,T5)
    perturb(obj:(T1,T2,T3,T4,T5), g:AnySeededRandomGen): AnySeededRandomGen
    shrink(obj:(T1,T2,T3,T4,T5)): Generator[\(T1,T2,T3,T4,T5)\]
end

object genTuple6[\T1,T2,T3,T4,T5,T6\](genT1:Gen[\T1\], genT2:Gen[\T2\], genT3:Gen[\T3\],
                                      genT4:Gen[\T4\], genT5:Gen[\T5\], genT6:Gen[\T6\])
    extends Gen[\(T1,T2,T3,T4,T5,T6)\]

    getter asString(): String
    generate(c:TestContext): (T1,T2,T3,T4,T5,T6)
    perturb(obj:(T1,T2,T3,T4,T5,T6), g:AnySeededRandomGen): AnySeededRandomGen
    shrink(obj:(T1,T2,T3,T4,T5,T6)): Generator[\(T1,T2,T3,T4,T5,T6)\]
end

object genArrow[\T,U\](genT:Gen[\T\], genU:Gen[\U\]) extends Gen[\T->U\]
    getter asString(): String
    generate(c:TestContext): T->U
    perturb(obj:T->U, g:AnySeededRandomGen): AnySeededRandomGen
end

object genTestContext extends Gen[\TestContext\]
    getter asString(): String
    generate(c:TestContext): TestContext
end

(**********************************************************)
(* `Arbitrary` interface *)

(** A factory for `Gen[\T\]` instances. This is intended to allow users to
    extend a set of types testable easily: it will automatically handle
    types with static parameters (e.g. `Maybe[\E\]`) if possible. **)
trait Arbitrary excludes AnyGen
    (** Chooses an appropriate `Gen[\T\]` for given type parameter `T`. **)
    gen[\T\](): Gen[\T\]
end

(** Default `Arbitrary` factory. Use this trait for a starting point. **)
trait DefaultArbitrary extends Arbitrary
    (* XXX should be usable with (self asif DefaultArbitrary).gen, but the current
       interpreter seems to reject this... *)
    gen0[\T\](arb:Arbitrary): Gen[\T\]

    gen[\T\](): Gen[\T\]
end

(** An instance of `DefaultArbitrary`. Intended as a default argument to
    `check` function below. **)
object defaultArbitrary extends DefaultArbitrary
end

(**********************************************************)
(* `Testable` interface *)

(** Test Status. There are three statuses: passed (`TestPass`), failed
    (`TestFail`) and skipped (`TestSkip`). The skipped test is a test that
    passes due to the false assumption, so `check` will ignore that result
    and try again.

    Test statuses form three-valued logic, but we have different criteria
    from the traditional three-valued logic (in which the third value is
    "unknown"): the third logic value should encode a vacuous truth (say,
    `F IMPLIES T`) but it doesn't fit with the normal definition of the
    implication: `F IMPLIES T = (NOT F) OR T`. We may choose `NOT F` to be
    that third value but it will collide with the truthness of the third value.
    After the many futile attempts, we omitted `NOT` operator from our logic
    system at all. **)
trait TestStatus extends Equality[\TestStatus\] comprises { TestPass, TestFail, TestSkip }
    abstract getter asString(): String
    opr =(self, _:TestStatus): Boolean
    abstract opr AND(self, rhs:TestStatus): TestStatus
    abstract opr OR(self, rhs:TestStatus): TestStatus
end

object TestPass extends TestStatus
    getter asString(): String
    opr =(self, _:TestPass): Boolean
    opr AND(self, rhs:TestStatus): TestStatus
    opr OR(self, rhs:TestStatus): TestStatus
end

object TestFail extends TestStatus
    getter asString(): String
    opr =(self, _:TestFail): Boolean
    opr AND(self, rhs:TestStatus): TestStatus
    opr OR(self, rhs:TestStatus): TestStatus
    opr OR(self, rhs:TestPass): TestStatus
end

object TestSkip extends TestStatus
    getter asString(): String
    opr =(self, _:TestSkip): Boolean
    opr AND(self, rhs:TestStatus): TestStatus
    opr AND(self, rhs:TestFail): TestStatus
    opr OR(self, rhs:TestStatus): TestStatus
end

(** A test result returned by `Testable[\T\]` instances. **)
object TestResult(status:TestStatus, tags:List[\String\], collected:List[\Any\])
    (** Returns a copy of `TestResult` with a given tag. **)
    tagged(tag:String): TestResult

    (** Returns a copy of `TestResult` with a given collected value. **)
    collect(obj:Any): TestResult

    (** Collecting void has no effect. **)
    collect(_:()): TestResult
end

(** A test result initialized with a status only. **)
TestResult(status:TestStatus): TestResult

(** A test result initialized with a Boolean status (`true` for pass, `false`
    for fail) only. **)
TestResult(ret:Boolean): TestResult

(** A copy of test result from the other test result. (This function is
    provided for convenience.) **)
TestResult(result:TestResult): TestResult

(** A testable instance with arguments of type `P`. **)
trait Testable[\P\] excludes String
    (** Runs a test with given arguments and returns a result. The test should
        be deterministic as much as possible: some routines like `shrink`, for
        example, rely on this behavior. **)
    abstract run(arg:P): TestResult

    (** `Testable[\P\]` can be also used as an arrow type `P->TestResult`. **)
    opr juxtaposition(self, arg:P): TestResult
end

(** Represents a test that has been finished already and returns a fixed
    result. **)
object Tested(result:TestResult) extends Testable[\()\]
    run(_:()): TestResult
end
Tested(status:TestStatus): Tested
Tested(ret:Boolean): TestResult

(* The public interfaces from here: (exists due to various overloading issues) *)

forAll[\P\](f:P->Any): Testable[\P\]

(* `cond ==> prop...` *)
opr ==>(p:Boolean, q:Boolean): Testable[\()\]
opr ==>(p:Boolean, q:TestResult): Testable[\()\]
opr ==>(p:Boolean, q:Testable[\()\]): Testable[\()\]
opr ==>(p:TestResult, q:Boolean): Testable[\()\]
opr ==>(p:TestResult, q:TestResult): Testable[\()\]
opr ==>(p:TestResult, q:Testable[\()\]): Testable[\()\]
opr ==>(p:Testable[\()\], q:Boolean): Testable[\()\]
opr ==>(p:Testable[\()\], q:TestResult): Testable[\()\]
opr ==>(p:Testable[\()\], q:Testable[\()\]): Testable[\()\]

(* `"tag" |: prop...` or `prop... :| "tag"` *)
opr |(tag:String, p:()->Any): Testable[\()\]
opr |(tag:String, p:Testable[\()\]): Testable[\()\]
(* (*) Not supported by Fortress yet
opr |(p:()->Any, tag:String): Testable[\()\]
opr |(p:Testable[\()\], tag:String): Testable[\()\]
*)

(* `prop... AND prop...` *)
opr AND(p:Boolean, q:TestResult): Testable[\()\]
opr AND(p:Boolean, q:Testable[\()\]): Testable[\()\]
opr AND(p:TestResult, q:Boolean): Testable[\()\]
opr AND(p:TestResult, q:TestResult): Testable[\()\]
opr AND(p:TestResult, q:Testable[\()\]): Testable[\()\]
opr AND(p:TestResult, q:()->Any): Testable[\()\]
opr AND(p:Testable[\()\], q:Boolean): Testable[\()\]
opr AND(p:Testable[\()\], q:TestResult): Testable[\()\]
opr AND(p:Testable[\()\], q:Testable[\()\]): Testable[\()\]
opr AND(p:Testable[\()\], q:()->Any): Testable[\()\]

(* `prop... OR prop...` *)
opr OR(p:Boolean, q:TestResult): Testable[\()\]
opr OR(p:Boolean, q:Testable[\()\]): Testable[\()\]
opr OR(p:TestResult, q:Boolean): Testable[\()\]
opr OR(p:TestResult, q:TestResult): Testable[\()\]
opr OR(p:TestResult, q:Testable[\()\]): Testable[\()\]
opr OR(p:TestResult, q:()->Any): Testable[\()\]
opr OR(p:Testable[\()\], q:Boolean): Testable[\()\]
opr OR(p:Testable[\()\], q:TestResult): Testable[\()\]
opr OR(p:Testable[\()\], q:Testable[\()\]): Testable[\()\]
opr OR(p:Testable[\()\], q:()->Any): Testable[\()\]

trait PrefixJuxt excludes {Testable[\()\], AnyMatrix, AnyVector, AnyMultiplicativeRing, String}
    opr juxtaposition(self, prop:Boolean): Testable[\()\]
    opr juxtaposition(self, prop:TestResult): Testable[\()\]
    abstract opr juxtaposition(self, prop:Testable[\()\]): Testable[\()\]
    opr juxtaposition(self, obj:PrefixJuxt): PrefixJuxtList
end

object PrefixJuxtList(first:PrefixJuxt, second:PrefixJuxt) extends PrefixJuxt
    opr juxtaposition(self, prop:Testable[\()\]): Testable[\()\]
end

(* `collect(obj) prop...` *)
object collect(obj:Any) extends PrefixJuxt
    opr juxtaposition(self, prop:Testable[\()\]): Testable[\()\]
end

(* `classify(cond,obj) prop...` or `classify(cond,obj,obj) prop...` *)
object classify(cond:Boolean, trueobj:Any, falseobj:Any) extends PrefixJuxt
    opr juxtaposition(self, prop:Testable[\()\]): Testable[\()\]
end
classify(cond:Boolean, trueobj:Any): classify

(**********************************************************)
(* Testing routines *)

(** Try to shrink a (failed) test instance in `n` tests. Returns a number of
    additional tests done (zero if it cannot be shrunk) and the resulting
    instance. **)
shrink[\P\](instance:P, t:Testable[\P\], g:Gen[\P\], n:ZZ32): (Boolean,ZZ32,P)

(** Tests a property by running given test generator with given test generator
    and test context.

    It returns a `TestResult` with `TestPass` status when every tests are passed,
    even though it has to be properly pointed out that there are other possible
    bugs which haven't caught. It returns `TestFail` when one or more tests are
    failed, and `TestSkip` when it reached the maximum number of tests. **)
checkResult[\P\](t:Testable[\P\], g:Gen[\P\], c:TestContext): TestResult

(** Same as `checkResult` but discards its result. Useful for actual testing. **)
check[\P\](t:Testable[\P\], g:Gen[\P\], c:TestContext): ()

(** Same as above but creates a test generator using `Arbitrary` factory. **)
check[\P\](t:Testable[\P\], c:TestContext): ()
check[\P\](t:Testable[\P\], arb:Arbitrary, numTests:ZZ32): ()
check[\P\](t:Testable[\P\], arb:Arbitrary): ()

(** Same as above but uses `defaultArbitrary` factory. **)
check[\P\](t:Testable[\P\], numTests:ZZ32): ()
check[\P\](t:Testable[\P\]): ()

end
