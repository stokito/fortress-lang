(******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(******************************************************************************

  Prefix trees, aka Tries: implementing Sets; purely functional

  PrefixSet[\E,F\] is the type of sets where F is a zero-indexed data type storing elements of type E, supporting an operator addLeft. Indexing gives lexicographic ordering.

  At present F must be a subtype of List[\E\] (where List is defined as in List.fss).

  Future improvements:

   - When the standard collection trait hierarchy is done, we can make this code work more pleasantly and in more generality:
       - we want F to be any type modelling List[\E\]
       - we could demand that PrefixSet[\E,F\] model Set[\F\].
       - we could vary the indexing data structure for each node. For some prefix trees, it would be best to implement their children as an array. If we are indexing by lists of booleans, we would want an ad-hoc indexing structure whose lookup algorithm is an "if".

   - It would be nice to implement efficient indexing for PrefixSet. This is apparently impossible, if we are to base it on the existing Map.fss. We would need a variant of Map.fss, which disambiguates references to the number of nodes of a binary tree (which is an internal notion) and the number of elements stored (which is an external notion). Each node of a map, since it represents a whole prefix tree, can contribute many indices, not just one.

  *****************************************************************************)

api PrefixSet
import Containment.{...}
import CovariantCollection.{AnyCovColl}
import List.{...}
import Map.{...} except { opr BIG UNION, opr BIG INTERSECTION, opr BIG SYMDIFF }

trait PrefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\]
    extends ContainmentGenerator[\F,PrefixSet[\E,F\]\]
    getter indexValuePairs(): ZeroIndexed[\(ZZ32, F)\]
    abstract getter size(): ZZ32
    getter isEmpty(): Boolean

    abstract isMember(): Boolean
    abstract children(): Map[\E, PrefixSet[\E, F\]\]
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
    indexOf(x: F): Maybe[\ZZ32\]
    indexOfMember(x: F): ZZ32
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
    getter size(): ZZ32
    fixedsize: ZZ32
    isMember(): Boolean
    children(): Map[\E, PrefixSet[\E, F\]\]
end

prefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](v: Boolean, c: Map[\E, PrefixSet[\E, F\]\]): PrefixSet[\E, F\]

emptyPrefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](): PrefixSet[\E, F\]

singletonPrefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](x: F): PrefixSet[\E, F\]

prefixSet[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](g: Generator[\F\]): PrefixSet[\E, F\]

opr {/[\E extends StandardTotalOrder[\E\], F extends List[\E\]\] fs: F... /}: PrefixSet[\E, F\]

opr BIG {/[\E extends StandardTotalOrder[\E\], F extends List[\E\]\]  /}: Comprehension[\F, PrefixSet[\E, F\], AnyCovColl, AnyCovColl\]

opr BIG {/[\E extends StandardTotalOrder[\E\], F extends List[\E\]\] g: Generator[\F\] /}: PrefixSet[\E, F\]


object Union[\E extends StandardTotalOrder[\E\], F extends List[\E\]\]
    extends CommutativeMonoidReduction[\PrefixSet[\E, F\]\]
    getter asString(): String

    empty(): PrefixSet[\E, F\]
    join(a: PrefixSet[\E, F\], b: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

opr BIG UNION[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](): BigReduction[\PrefixSet[\E, F\], PrefixSet[\E, F\]\]

opr BIG UNION[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](g: Generator[\PrefixSet[\E, F\]\]): PrefixSet[\E, F\]

object Intersection[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\]
    extends CommutativeMonoidReduction[\PrefixSet[\E, F\]\]
    getter asString(): String

    empty(): PrefixSet[\E, F\]
    join(a: PrefixSet[\E, F\], b: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

opr BIG INTERSECTION[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](): BigReduction[\PrefixSet[\E, F\], PrefixSet[\E, F\]\]

opr BIG INTERSECTION[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](g: Generator[\PrefixSet[\E, F\]\]): PrefixSet[\E, F\]

object SymmetricDifference[\E extends StandardTotalOrder[\E\],
        F extends List[\E\]\]
    extends CommutativeMonoidReduction[\PrefixSet[\E, F\]\]
    getter asString(): String

    empty(): PrefixSet[\E, F\]
    join(a: PrefixSet[\E, F\], b: PrefixSet[\E, F\]): PrefixSet[\E, F\]
end

opr BIG SYMDIFF[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](): BigReduction[\PrefixSet[\E, F\], PrefixSet[\E, F\]\]

opr BIG SYMDIFF[\E extends StandardTotalOrder[\E\], F extends List[\E\]\](g: Generator[\PrefixSet[\E, F\]\]): PrefixSet[\E, F\]

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
