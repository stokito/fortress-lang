(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api IntMap

object KeyOverlap[\Val\](key: ZZ64, val1: Val, val2: Val)
    extends UncheckedException
    getter asString(): String
end

trait IntMap[\Val\]
    extends { Generator[\(ZZ64, Val)\], Equality[\IntMap[\Val\]\] }
    comprises { ... }
    abstract getter isEmpty(): Boolean
    abstract getter asDebugString(): String
    abstract getter asString(): String
    abstract asDot(pre:String): String
    abstract dotLabel(pre:String): String
    toDot(): String

    seq(self): SequentialGenerator[\(ZZ64, Val)\]
    opr =(self, other: IntMap[\Val\]): Boolean
    abstract opr | self |: ZZ32
    abstract opr [ k: ZZ64 ]: Val throws NotFound
    abstract member(x: ZZ64): Maybe[\Val\]
    abstract member(x: ZZ64, v: Val): Val
    abstract minimum(): Maybe[\(ZZ64, Val)\]
    abstract deleteMinimum(): IntMap[\Val\]
    abstract extractMinimum(): Maybe[\(ZZ64, Val, IntMap[\Val\])\]
    abstract maximum(): Maybe[\(ZZ64, Val)\]
    abstract deleteMaximum(): IntMap[\Val\]
    abstract extractMaximum(): Maybe[\(ZZ64, Val, IntMap[\Val\])\]
    abstract add(k: ZZ64, v: Val): NonEmptyIntMap[\Val\]
    addNew(k: ZZ64, v: Val): NonEmptyIntMap[\Val\]
    update(k: ZZ64, v: Val): NonEmptyIntMap[\Val\]
    abstract delete(k: ZZ64): IntMap[\Val\]
    abstract updateWith(f: Maybe[\Val\] -> Maybe[\Val\], k: ZZ64): IntMap[\Val\]
    abstract opr CUP(self, other: IntMap[\Val\]): IntMap[\Val\]
    abstract opr UPLUS(self, other: IntMap[\Val\]): IntMap[\Val\]
    abstract union(f: (ZZ64, Val, Val) -> Val, other: IntMap[\Val\]): IntMap[\Val\]
    abstract intersection[\V, R\](f: (ZZ64, Val, V) -> R, other: IntMap[\V\]): IntMap[\R\]
    abstract opr SYMDIFF(self, other: IntMap[\Val\]): IntMap[\Val\]
    combine[\That, Result\](f: (ZZ64, Val, That) -> Maybe[\Result\], mapThis: NonEmptyIntMap[\Val\] -> IntMap[\Result\],
            mapThat: NonEmptyIntMap[\That\] -> IntMap[\Result\],
            that: IntMap[\That\]): IntMap[\Result\]
    abstract mapFilter[\Result\](f: (ZZ64, Val) -> Maybe[\Result\]): IntMap[\Result\]
    abstract map[\R\](f:(ZZ64,Val)->R): IntMap[\R\]
    check(): IntMap[\Val\]
end

(** Work around bug with generic functional method type inference in interpreter. **)
opr INTERSECTION[\V,W\](this:IntMap[\V\], other:IntMap[\W\]): IntMap[\(V,W)\]
opr DIFFERENCE[\V,W\](this:IntMap[\V\], other:IntMap[\W\]):IntMap[\V\]

trait NonEmptyIntMap[\Val\] extends IntMap[\Val\] comprises { ... }
    getter isEmpty(): Boolean
    abstract getter topKey(): ZZ64

    abstract minimum(): Just[\(ZZ64, Val)\]
    abstract extractMinimum(): Just[\(ZZ64, Val, IntMap[\Val\])\]
    abstract maximum(): Just[\(ZZ64, Val)\]
    abstract extractMaximum(): Just[\(ZZ64, Val, IntMap[\Val\])\]
    abstract opr CUP(self, other: IntMap[\Val\]): NonEmptyIntMap[\Val\]
    abstract opr UPLUS(self, other: IntMap[\Val\]): NonEmptyIntMap[\Val\]
    abstract union(f: (ZZ64, Val, Val) -> Val, other: IntMap[\Val\]): NonEmptyIntMap[\Val\]
end

keySplit(k1: ZZ64, k2: ZZ64): ZZ64

opr {|->[\Val\] kvs: (ZZ64, Val)... }: IntMap[\Val\]

opr {[\Val\]  }: IntMap[\Val\]

opr BIG {|->[\Val\]  }: IntMap[\Val\]

opr BIG {|->[\Val\] g: Generator[\(ZZ64, Val)\] }: IntMap[\Val\]

opr BIG CUP[\Val\](): Comprehension[\IntMap[\Val\], IntMap[\Val\], Any, Any\]

opr BIG CUP[\Val\](g: Generator[\IntMap[\Val\]\]): IntMap[\Val\]

opr BIG UPLUS[\Val\](): Comprehension[\IntMap[\Val\], IntMap[\Val\], Any, Any\]

opr BIG UPLUS[\Val\](g: Generator[\IntMap[\Val\]\]): IntMap[\Val\]

toDots[\Val\](maps: IntMap[\Val\]...): String

end
