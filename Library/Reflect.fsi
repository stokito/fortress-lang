(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api Reflect

trait Type extends StandardTotalOrder[\Type\]
           comprises {ObjectOrTraitType, ArrowType, TupleType, BottomType}
    getter asString(): String
    join(self, other:Type): Type
    meet(self, other:Type): Type
    opr =(self, other:Type): Boolean
    opr <(self, other:Type): Boolean
    opr SUBTYPEOF(self, other:Type): Boolean
    opr SUPERTYPEOF(self, other:Type): Boolean
end

trait ObjectOrTraitType extends Type comprises {ObjectType, TraitType}
                        excludes {ArrowType, TupleType, BottomType}
    getter typeExtends(): Generator[\Type\]
    getter typeExcludes(): Generator[\Type\]
    getter typeComprises(): Maybe[\Generator[\Type\]\]
end

trait ObjectType extends ObjectOrTraitType comprises {...} excludes TraitType
end

trait TraitType extends ObjectOrTraitType comprises {...} excludes ObjectType
end

trait ArrowType extends Type comprises {...}
                excludes {ObjectOrTraitType, TupleType, BottomType}
    getter domain(): Type
    getter range(): Type
end

trait TupleType extends {Type, ZeroIndexed[\Type\]} comprises {...}
                excludes {ObjectOrTraitType, ArrowType, BottomType}
end

trait BottomType extends Type comprises {...}
                 excludes {ObjectOrTraitType, ArrowType, TupleType}
end

anyType: Type
objectType: Type
voidType: Type
bottomType: Type

theType[\T\](): Type
typeOf(obj:Any): Type
dumpType(t:Type): ()

end
