(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api Reflect

(** A reflected type object. **)
trait Type extends StandardTotalOrder[\Type\]
           comprises {ObjectOrTraitType, ArrowType, TupleType, BottomType}
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

(** A type object which represents an object or a trait. **)
trait ObjectOrTraitType extends Type comprises {ObjectType, TraitType}
                        excludes {ArrowType, TupleType, BottomType}
    (** Returns a set of types in %extends% clause. If it is not given,
        it defaults to %Object%. **)
    getter typeExtends(): Generator[\Type\]

    (** Returns a set of types in %excludes% clause and types that define
        the given type in their %excludes% clause. This is because the exclusion
        is symmetric. **)
    getter typeExcludes(): Generator[\Type\]

    (** Returns a set of types in %comprises% clause. If the clauses is not
        known, the type is open to subtyping and returns %Nothing%.
        Note that it is an empty set (and not %Nothing%) for %ObjectType%s. **)
    getter typeComprises(): Maybe[\Generator[\Type\]\]
end

(** A type object which represents an object. **)
trait ObjectType extends ObjectOrTraitType comprises {...} excludes TraitType
end

(** A type object which represents a trait. **)
trait TraitType extends ObjectOrTraitType comprises {...} excludes ObjectType
end

(** A type object which represents an arrow type, i.e. A->B. **)
trait ArrowType extends Type comprises {...}
                excludes {ObjectOrTraitType, TupleType, BottomType}
    (** Returns an arity of given arrow type. **)
    getter arity(): ZZ32

    (** Returns a domain of given arrow type. It can be a tuple type. **)
    getter domain(): Type

    (** Returns a range of given arrow type. **)
    getter range(): Type
end

(** A type object which represents a tuple type, i.e. (A,B,C). It can be used
    as a generator for types contained in. **)
trait TupleType extends {Type, ZeroIndexed[\Type\]} comprises {...}
                excludes {ObjectOrTraitType, ArrowType, BottomType}
end

(** A type object which represents a bottom type, i.e. an uninhabited type
    that is a subtype of every other types. It commonly appears in the range of
    arrow types. **)
trait BottomType extends Type comprises {...}
                 excludes {ObjectOrTraitType, ArrowType, TupleType}
end

anyType: Type
objectType: Type
voidType: Type
bottomType: Type

(** Returns a type object that represents a type in the static parameter. **)
theType[\T\](): Type

(** Returns a type object that represents a (dynamic) type of given argument. **)
typeOf(obj:Any): Type

(** Prints a type information recursively. **)
dumpType(t:Type): ()

end
