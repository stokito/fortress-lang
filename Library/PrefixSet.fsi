api PrefixSet
import List.{...}
import Map.{...}
object StringJoinReduction(s: String) extends MonoidReduction[\String\]
    getter toString(): String

    empty(): String
    join(a: String, b: String): String
end

trait PrefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\]
    extends { Generator[\F\], Equality[\PrefixSet[\E, F\]\] }
    getter indexValuePairs(): ZeroIndexed[\(ZZ32, F)\]

    abstract isMember(): Boolean
    abstract children(): Map[\E, PrefixSet[\E, F\]\]
    abstract size(): ZZ32
    isEmpty(): Boolean
    prefixGenerate[\R\](prefix: F, r: Reduction[\R\], body: F -> R): R
    generate[\R\](r: Reduction[\R\], body: F -> R): R
    prefixSeqgen[\R\](prefix: F, r: Reduction[\R\], body: F -> R): R
    seqgen[\R\](r: Reduction[\R\], body: F -> R): R
    seq(self): SequentialGenerator[\F\]
    asString(): String
    opr IN(x: F, self): Boolean
    add(x: F): PrefixSet[\E, F\]
    delete(x: F): PrefixSet[\E, F\]
    opr =(self, s2: PrefixSet[\E, F\]): Boolean
    opr [ i: ZZ32 ]: F
    indexOf(self, x: F): Maybe[\ZZ32\]
    indexOfMember(self, x: F): ZZ32
    splitIndex(x: ZZ32): (PrefixSet[\E, F\], PrefixSet[\E, F\])
    prefixivgen[\R\](prefix: F, i0: ZZ32, r: Reduction[\R\], body: (ZZ32, F) -> R): R
    ivgen[\R\](i0: ZZ32, r: Reduction[\R\], body: (ZZ32, F) -> R): R
    minimum(): Maybe[\F\]
    getMinimum(): F throws NotFound
    maximum(): Maybe[\F\]
    getMaximum(): F throws NotFound
    deleteMinimum(): PrefixSet[\E, F\]
    deleteMaximum(): PrefixSet[\E, F\]
    extractMinimum(): Maybe[\(F, PrefixSet[\E, F\])\]
    extractMaximum(): Maybe[\(F, PrefixSet[\E, F\])\]
    concat(s2: PrefixSet[\E, F\]): PrefixSet[\E, F\]
    concat3(v: F, s2: PrefixSet[\E, F\]): PrefixSet[\E, F\]
    splitAt(x: F): (PrefixSet[\E, F\], Boolean, PrefixSet[\E, F\])
    opr ⊆(self, other: PrefixSet[\E, F\]): Boolean
    opr SUBSET(self, other: PrefixSet[\E, F\]): Boolean
    opr ⊇(self, other: PrefixSet[\E, F\]): Boolean
    opr SUPSET(self, other: PrefixSet[\E, F\]): Boolean
    opr SETCMP(self, other: PrefixSet[\E, F\]): Comparison
    opr | self |: ZZ32
    opr CUP(self, s2: PrefixSet[\E, F\]): PrefixSet[\E, F\]
    opr CAP(self, s2: PrefixSet[\E, F\]): PrefixSet[\E, F\]
    opr SYMDIFF(self, s2: PrefixSet[\E, F\]): PrefixSet[\E, F\]
    opr DIFFERENCE(self, s2: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

object fastPrefixSet[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\](v: Boolean, c: Map[\E, PrefixSet[\E, F\]\])
    extends PrefixSet[\E, F\]
    fixedsize: ZZ32
    isMember(): Boolean
    children(): Map[\E, PrefixSet[\E, F\]\]
    size(): ZZ32
end

prefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](v: Boolean, c: Map[\E, PrefixSet[\E, F\]\]): PrefixSet[\E, F\]

emptyPrefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](): PrefixSet[\E, F\]

singletonPrefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](x: F): PrefixSet[\E, F\]

prefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](g: Generator[\F\]): PrefixSet[\E, F\]

opr {/[\E extends StandardTotalOrder[\E\], F extends List[\E\]\] fs: F... /}: PrefixSet[\E, F\]

object Union[\E extends StandardTotalOrder[\E\], F extends List[\E\]\]
    extends CommutativeMonoidReduction[\PrefixSet[\E, F\]\]
    getter asString(): String

    empty(): PrefixSet[\E, F\]
    join(a: PrefixSet[\E, F\], b: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

object Intersection[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\]
    extends CommutativeMonoidReduction[\PrefixSet[\E, F\]\]
    getter asString(): String

    empty(): PrefixSet[\E, F\]
    join(a: PrefixSet[\E, F\], b: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

object SymmetricDifference[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\]
    extends CommutativeMonoidReduction[\PrefixSet[\E, F\]\]
    getter asString(): String

    empty(): PrefixSet[\E, F\]
    join(a: PrefixSet[\E, F\], b: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

value object SeqPrefixSetGenerator[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\](s: PrefixSet[\E, F\])
    extends SequentialGenerator[\F\]
    getter size(): ZZ32
    getter isEmpty(): Boolean

    opr | self |: ZZ32
    generate[\R\](r: Reduction[\R\], body: F -> R): R
end

value object IndexValuePrefixSetGenerator[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\](s: PrefixSet[\E, F\])
    extends ZeroIndexed[\(ZZ32, F)\]
    getter size(): ZZ32
    getter indices(): Generator[\ZZ32\]
    getter isEmpty(): Boolean

    opr | self |: ZZ32
    generate[\R\](r: Reduction[\R\], body: (ZZ32, F) -> R): R
    opr [ x: ZZ32 ]: (ZZ32, E)
    opr [ r: Range[\ZZ32\] ]: ZeroIndexed[\(ZZ32, F)\]
    indexOf(i: ZZ32, v: F): Maybe[\ZZ32\]
end

end