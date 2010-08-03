(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api Reflect

trait Type comprises {ObjectOrTraitType, ArrowType, TupleType, BottomType}
    getter asString(): String
    join(self, other:Type): Type
    meet(self, other:Type): Type
    opr SUBTYPEOF(self, other:Type): Boolean
    opr SUPERTYPEOF(self, other:Type): Boolean
end

trait ObjectOrTraitType extends Type comprises {ObjectType, TraitType}
                        excludes {ArrowType, TupleType, BottomType}
end

trait ObjectType extends ObjectOrTraitType (* %comprises ReflectObject[\T\] where [\T\]% *)
                 excludes TraitType
end

trait TraitType extends ObjectOrTraitType (* %comprises ReflectTrait[\T\] where [\T\]% *)
                excludes ObjectType
end

trait ArrowType extends Type (* %comprises ReflectArrow[\T\] where [\T\]% *)
                excludes {ObjectOrTraitType, TupleType, BottomType}
    getter domain(): Type
    getter range(): Type
end

trait TupleType extends {Type, ZeroIndexed[\Type\], DelegatedIndexed[\Type,ZZ32\]}
                (* %comprises ReflectTuple[\T\] where [\T\]% *)
                excludes {ObjectOrTraitType, ArrowType, BottomType}
end

trait BottomType extends Type (* %comprises ReflectBottom[\T\] where [\T\]% *)
                 excludes {ObjectOrTraitType, ArrowType, TupleType}
end

anyType: Type
objectType: Type
voidType: Type
bottomType: Type

theType[\T\](): Type
typeOf(obj:Any): Type

end
