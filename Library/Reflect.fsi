(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api Reflect

(** A reflected type object. **)
trait Type extends StandardTotalOrder[\Type\]
           comprises {GenericType, ObjectOrTraitType, ArrowType, TupleType,
                      RestType, BottomType}
    getter asString(): String

    (** Two type objects are same if types represented by them are same;
        the comparison is nominal and correctly distinguishes different
        types with a same name but in the different APIs. **)
    opr =(self, other:Type): Boolean

    (** An arbitrary comparison operator to allow the construction of a set of
        types. **)
    opr <(self, other:Type): Boolean

    (** Returns true if it is a subtype of given type. **)
    opr SUBTYPEOF(self, other:Type): Boolean

    (** Returns true if it is a supertype of given type. **)
    opr SUPERTYPEOF(self, other:Type): Boolean

    (** Calculates a join of two types, that is, a minimal set of common
        supertypes. It is "minimal" in the sense that it will transitively
        generate all other supertypes, including %Any%. **)
    join(self, other:Type): Generator[\Type\]

    (** Calculates a meet of two types, that is, a minimal set of common
        subtypes. It is "minimal" in the sense that it will transitively
        generate all other subtypes, including %BOTTOM%. **)
    meet(self, other:Type): Generator[\Type\]
end

(** A type object which represents a generic type, which can be instantiated
    (via `apply` method) to a concrete object or trait type. **)
trait GenericType extends Type comprises {GenericObjectOrTraitType, GenericArrowType}
                  excludes {ObjectOrTraitType, ArrowType, TupleType, RestType, BottomType}
    (** Returns a list of name of static parameters and their type restrictions.
        For now, the second element of tuple gives types in extends clause of
        each static parameters. **)
    getter staticParams(): Generator[\(String,Generator[\Type\])\]

    (** Instantiates a concrete type from given static arguments. **)
    apply(args:Type...): Type
end

trait GenericObjectOrTraitType extends GenericType comprises {...}
                               excludes GenericArrowType
end

trait GenericArrowType extends GenericType comprises {...}
                       excludes GenericObjectOrTraitType
end

(** A type object which represents an object or a trait. **)
trait ObjectOrTraitType extends Type comprises {ObjectType, TraitType}
                        excludes {GenericType, ArrowType, TupleType, RestType, BottomType}
    (** Returns a set of types in %extends% clause. If it is not given,
        it defaults to %Object%. **)
    getter typeExtends(): Generator[\TraitType\]

    (** Returns a set of types in %excludes% clause and types that define
        the given type in their %excludes% clause. This is because the exclusion
        is symmetric. **)
    getter typeExcludes(): Generator[\ObjectOrTraitType\]

    (** Returns a set of types in %comprises% clause. If the clauses is not
        known, the type is open to subtyping and returns %Nothing%.
        Note that it is an empty set (and not %Nothing%) for %ObjectType%s. **)
    getter typeComprises(): Maybe[\Generator[\ObjectOrTraitType\]\]

    (** Returns a list of static arguments if any, or an empty list otherwise. **)
    getter staticArgs(): Generator[\Type\]

    (** Returns the generic type from which the type is instantiated if any,
        or the type itself otherwise. **)
    getter generic(): Type

    (** Returns a list of every members declared in the object or trait.
        It doesn't cover inherited members, but it may include constructor
        arguments which are desugared into method-like forms.

        More specifically, for every members it returns a name, a type (which
        would be a function for methods and a non-function for variables), and
        a function value which actually calls the method when it called with
        an actual object and arguments.

        Names are not unique (but name-type pairs *are* unique); there may be
        no function value if the method doesn't have a body. **)
    getter members(): Generator[\(String,Type,Maybe[\(Object,Any...)->Any\])\]
end

(** A type object which represents an object. **)
trait ObjectType extends ObjectOrTraitType comprises {...} excludes TraitType
end

(** A type object which represents a trait. **)
trait TraitType extends ObjectOrTraitType comprises {...} excludes ObjectType
end

(** A type object which represents an arrow type, i.e. A->B. **)
trait ArrowType extends Type comprises {...}
                excludes {GenericType, ObjectOrTraitType, TupleType, RestType, BottomType}
    (** Returns an arity of given arrow type. If `isVararg` method returns
        true, it represents a minimal arity instead. **)
    getter arity(): ZZ32

    (** Returns true if an arrow type receives varadic arguments. **)
    getter isVararg(): Boolean

    (** Returns a domain of given arrow type. It can be a tuple type. **)
    getter domain(): Type

    (** Returns a domain of given arrow type, which is guaranteed to be
        a proper generator. **)
    getter arguments(): Generator[\Type\]

    (** Returns a range of given arrow type. **)
    getter range(): Type
end

(** A type object which represents a tuple type, i.e. (A,B,C). It can be used
    as a generator for types contained in. **)
trait TupleType extends {Type, ZeroIndexed[\Type\]} comprises {...}
                excludes {GenericType, ObjectOrTraitType, ArrowType, RestType, BottomType}
end

(** A type object which represents a rest type, which appears as the last
    element of the domain of varadic-argument functions. **)
trait RestType extends Type comprises {...}
               excludes {GenericType, ObjectOrTraitType, ArrowType, TupleType, BottomType}
    (** Returns the type of varadic arguments. **)
    getter base(): Type
end

(** A type object which represents a bottom type, i.e. an uninhabited type
    that is a subtype of every other types. It commonly appears in the range of
    arrow types. **)
trait BottomType extends Type comprises {...}
                 excludes {GenericType, ObjectOrTraitType, ArrowType, TupleType, RestType}
end

(** Objects for frequently used types and special types. **)
anyType: Type
objectType: Type
voidType: Type
bottomType: Type

(** Creates an arrow type with given domain and range. **)
arrowType(domain:Type, range:Type): ArrowType

(** Creates a tuple type with given types as elements. It may return a void type
    or any other types if zero or one argument is given. **)
tupleType(types:Type...): Type

(** Creates a rest type with given argument. **)
restType(ty:Type): RestType

(** Returns a type object that represents a type in the static parameter. **)
theType[\T\](): Type

(** Returns a type object that represents a (dynamic) type of given argument. **)
typeOf(obj:Any): Type

(** Prints a type information recursively. **)
dumpType(t:Type): ()

end
