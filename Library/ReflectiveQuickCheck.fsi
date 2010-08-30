(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api ReflectiveQuickCheck

import QuickCheck.{...}
import Reflect.{...}

(**********************************************************)
(* More test generators *)

(** A generator for type objects which are subtypes of given type.

    For now, it is able to generate types that are connected with `comprises`
    clauses. If the top type doesn't have `comprises` clause at all then
    it will generate only that type. **)
object genType(top:Type) extends Gen[\Type\]
    getter asString(): String
    generate(c:TestContext): Type
end

(** A generator for any type objects. It is equal to genType(anyType), and
    implemented as a fixed list of possible types for now. **)
object genAnyType extends Gen[\Type\]
    getter asString(): String
    generate(c:TestContext): Type
end

(** A generator for values of given type or its subtype. It requires the
    `Arbitrary` object used is reflective, and gathers a random generator
    via it. **)
object genAnyOf[\T\] extends Gen[\T\]
    getter asString(): String
    generate(c:TestContext): T
end

(** Same as `genAnyOf` but accepts a type object. **)
object genAnyOf'(top:Type) extends Gen[\Any\]
    getter asString(): String
    generate(c:TestContext): Any
end

(** A generator for `Any` type, which is actually a generator for the specific
    type in disguise. **)
object genAnyLifted[\T\](gen:Gen[\T\]) extends Gen[\Any\]
    getter asString(): String
    generate(c:TestContext): Any
end

(**********************************************************)
(* `ReflectiveArbitrary` instance *)

(** `Arbitrary` factory extended with reflective features. It is capable for
    returning a generator from staticically given types as well as from run-time
    type objects. Subclasses of `ReflectiveArbitrary` will want to override
    `genFromType` method. **)
trait ReflectiveArbitrary extends DefaultArbitrary
    (** Calls given constructor with `Gen` instances for given types via
        `genFromType`. If `genFromType` returns `Nothing` for any types given,
        it also returns `Nothing`. **)
    genFromTypeList(def:Boolean, types:Generator[\Type\], ctor:Any->AnyGen):
        Maybe[\AnyGen\]

    (** Returns a generator for given type object, or `Nothing` if there doesn't
        exist an appropriate generator. It will try to find out the generator
        for unknown types as supported by `genAnyOf'` generator when `def` is
        `true`. **)
    genFromType(def:Boolean, t:Type): Maybe[\AnyGen\]

    (** A wrapper for `genFromType` above. **)
    gen[\T\](): Gen[\T\]
end

(** An instance of `ReflectiveArbitrary`. Intended as a default argument to
    `checkGeneric` function below. **)
object reflectiveArbitrary extends ReflectiveArbitrary
end

(**********************************************************)
(* Generic properties and testing routines *)

(** A container of bulk of generic properties. Use `runTests` method or
    `checkGeneric` routine to test them all.

    Every generic or non-generic properties should have a name starts with
    `prop`, return a value of type `Boolean`, `TestStatus` or `TestResult`,
    and their types have to be completely specified. (The last requirement will
    be relaxed when the Fortress interpreter got a proper type inference,
    though.) **)
trait GenericProperties
    (** Returns a list of properties found in the container. **)
    getter properties(): Generator[\(String,Type,(Object,Any...)->Any)\]

    (** Given a property, returns `Testable` instance and corresponding `Gen`
        instance that can be readily used for testing. **)
    toProperty(prop:(String,GenericArrowType,(Object,Any...)->Any), c:TestContext):
            (Testable[\Any\],Gen[\Any\])

    (** Runs every tests found in the container. **)
    runTests(c:TestContext): TestResult
end

(** Tests a bulk of generic properties by running test generator with given
    test context. **)
checkGeneric(props:GenericProperties, c:TestContext): ()

(** Same as above but creates a test generator using `Arbitrary` factory. **)
checkGeneric(props:GenericProperties, arb:ReflectiveArbitrary, numTests:ZZ32): ()
checkGeneric(props:GenericProperties, arb:ReflectiveArbitrary): ()

(** Same as above but uses `reflectiveArbitrary` factory. **)
checkGeneric(props:GenericProperties, numTests:ZZ32): ()
checkGeneric(props:GenericProperties): ()

end
