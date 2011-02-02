(******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(******************************************************************************

  Prefix trees, aka Tries: implementing Maps; purely functional

  PrefixMap[\E,F,V\] is the type of maps, indexed by F, and valued in V. Here F is a zero-indexed data type storing elements of type E, supporting an operator addLeft.

  At present F must be a subtype of List[\E\] (where List is defined as in List.fss).

  Future improvements:

   - When the standard collection trait hierarchy is done, we can make this code work more pleasantly and in more generality:
       - we want F to be any type modelling List[\E\]
       - we could demand that PrefixMap[\E,F,V\] model Map[\F,V\].
       - we could vary the indexing data structure for each node. For some prefix trees, it would be best to implement their children as an array. If we are indexing by lists of booleans, we would want an ad-hoc indexing structure whose lookup algorithm is an "if".

 ******************************************************************************)

api PrefixMap
import List.{...}
import Map.{...} except { opr BIG UNION, opr BIG UPLUS }
import PrefixSet.{...}
import CovariantCollection.{...}
trait PrefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\]
    extends { Generator[\(F, V)\], Equality[\PrefixMap[\E, F, V\]\] }
    getter isEmpty(): Boolean
    abstract getter size(): ZZ32
    abstract content(): Maybe[\V\]
    abstract children(): Map[\E, PrefixMap[\E, F, V\]\]
    isMember(): Boolean
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
    getter size(): ZZ32
    content(): Maybe[\V\]
    children(): Map[\E, PrefixMap[\E, F, V\]\]
end

prefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](v: Maybe[\V\], c: Map[\E, PrefixMap[\E, F, V\]\]): PrefixMap[\E, F, V\]

emptyPrefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](): PrefixMap[\E, F, V\]

singletonPrefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](x: F, v: V): PrefixMap[\E, F, V\]

prefixMap[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\](g: Generator[\(F, V)\]): PrefixMap[\E, F, V\]

opr {/|->[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\] fs: (F, V)... /}: PrefixMap[\E, F, V\]

opr BIG {/|->[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\] g: Generator[\(F, V)\] /}: PrefixMap[\E, F, V\]

opr BIG {/|->[\E extends StandardTotalOrder[\E\], F extends List[\E\],
        V\]  /}: Comprehension[\(F, V), PrefixMap[\E, F, V\], AnyCovColl,
        AnyCovColl\]

opr BIG UNION[\E extends StandardTotalOrder[\E\], F extends List[\E\],V\]():Comprehension[\PrefixMap[\E,F,V\],PrefixMap[\E,F,V\],Any,Any\]

opr BIG UNION[\E extends StandardTotalOrder[\E\], F extends List[\E\],V\](g: Generator[\PrefixMap[\E,F,V\]\]):PrefixMap[\E,F,V\]

opr BIG UPLUS[\E extends StandardTotalOrder[\E\], F extends List[\E\],V\]():Comprehension[\PrefixMap[\E,F,V\],PrefixMap[\E,F,V\],Any,Any\]

opr BIG UPLUS[\E extends StandardTotalOrder[\E\], F extends List[\E\],V\](g: Generator[\PrefixMap[\E,F,V\]\]):PrefixMap[\E,F,V\]

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
