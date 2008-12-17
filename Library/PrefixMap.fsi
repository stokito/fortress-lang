api PrefixMap
import List.{...}
import Map.{...}
import PrefixSet.{...}
trait PrefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\]
    extends { Generator[\(F, V)\], Equality[\PrefixMap[\E, F, V\]\] }
    abstract content(): Maybe[\V\]
    abstract children(): Map[\E, PrefixMap[\E, F, V\]\]
    isMember(): Boolean
    isEmpty(): Boolean
    abstract size(): ZZ32
    opr | self |: ZZ32
    asString(): String
    prefixGenerate[\R\](prefix: F, r: Reduction[\R\], body: (F, V) -> R): R
    generate[\R\](r: Reduction[\R\], body: (F, V) -> R): R
    prefixSeqgen[\R\](prefix: F, r: Reduction[\R\], body: (F, V) -> R): R
    seqgen[\R\](r: Reduction[\R\], body: (F, V) -> R): R
    seq(self): SequentialGenerator[\(F, V)\]
    dom(self): PrefixSet[\E, F\]
    opr [ k: F ]: V throws NotFound
    member(k: F): Maybe[\V\]
    member(k: F, v: V): V
    minimum(): Maybe[\(F, V)\]
    getMinimum(): (F, V) throws NotFound
    maximum(): Maybe[\(F, V)\]
    getMaximum(): F throws NotFound
    deleteMinimum(): PrefixMap[\E, F, V\]
    deleteMaximum(): PrefixMap[\E, F, V\]
    extractMinimum(): Maybe[\(F, V, PrefixMap[\E, F, V\])\]
    extractMaximum(): Maybe[\(F, V, PrefixMap[\E, F, V\])\]
    add(x: F, v: V): PrefixMap[\E, F, V\]
    update(x: F, v: V): PrefixMap[\E, F, V\]
    delete(x: F): PrefixMap[\E, F, V\]
    updateWith(f: Maybe[\V\] -> Maybe[\V\], k: F): PrefixMap[\E, F, V\]
    opr CUP(self, other: PrefixMap[\E, F, V\]): PrefixMap[\E, F, V\]
    opr UPLUS(self, other: PrefixMap[\E, F, V\]): PrefixMap[\E, F, V\]
    union(f: (F, V, V) -> V, other: PrefixMap[\E, F, V\]): PrefixMap[\E, F, V\]
    splitAt(k: F): (PrefixMap[\E, F, V\], Maybe[\V\], PrefixMap[\E, F, V\])
    concat(other: PrefixMap[\E, F, V\]): PrefixMap[\E, F, V\]
    concat3(k: F, v: V, other: PrefixMap[\E, F, V\]): PrefixMap[\E, F, V\]
    combine[\V2, R\](doBoth: (F, V, V2) -> Maybe[\R\], doThis: (F, V) -> Maybe[\R\],
            doThat: (F, V2) -> Maybe[\R\],
            mapThis: (F, PrefixMap[\E, F, V\]) -> PrefixMap[\E, F, R\],
            mapThat: (F, PrefixMap[\E, F, V2\]) -> PrefixMap[\E, F, R\],
            that: PrefixMap[\E, F, V2\]): PrefixMap[\E, F, R\]
    combine[\V2, R\](doBoth: (F, V, V2) -> Maybe[\R\], doThis: (F, V) -> Maybe[\R\],
            doThat: (F, V2) -> Maybe[\R\],
            that: PrefixMap[\E, F, V2\]): PrefixMap[\E, F, R\]
    mapFilter[\R\](f: (F, V) -> Maybe[\R\]): PrefixMap[\E, F, R\]
end

object fastPrefixMap[\E extends StandardTotalOrder[\E\],
        F extends List[\E\],
        V\](v: Maybe[\V\], c: Map[\E, PrefixMap[\E, F, V\]\])
    extends PrefixMap[\E, F, V\]
    fixedsize: ZZ32
    content(): Maybe[\V\]
    children(): Map[\E, PrefixMap[\E, F, V\]\]
    size(): ZZ32
end

prefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](v: Maybe[\V\], c: Map[\E, PrefixMap[\E, F, V\]\]): PrefixMap[\E, F, V\]

emptyPrefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](): PrefixMap[\E, F, V\]

singletonPrefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](x: F, v: V): PrefixMap[\E, F, V\]

prefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](g: Generator[\(F, V)\]): PrefixMap[\E, F, V\]

object SeqPrefixMapGenerator[\E extends StandardTotalOrder[\E\],
        F extends List[\E\],
        V\](o: PrefixMap[\E, F, V\])
    extends SequentialGenerator[\(F, V)\]
    generate[\R\](r: Reduction[\R\], body: (F, V) -> R): R
end

object DisjointPrefixMapUnion[\E extends StandardTotalOrder[\E\],
        F extends List[\E\],
        V\]
    extends MonoidReduction[\PrefixMap[\E, F, V\]\]
    getter asString(): String

    empty(): PrefixMap[\E, F, V\]
    join(a: PrefixMap[\E, F, V\], b: PrefixMap[\E, F, V\]): PrefixMap[\E, F, V\]
end

end