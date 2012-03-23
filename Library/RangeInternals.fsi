(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api RangeInternals

roundToStride[\I extends Integral[\I\]\](amt: I, stride: I): I

atOrAboveGoingUp[\I extends Integral[\I\]\](start: I, bound: I, stride: I): I

atOrBelowGoingUp[\I extends Integral[\I\]\](start: I, bound: I, stride: I): I

atOrBelowGoingDown[\I extends Integral[\I\]\](start: I, bound: I, stride: I): I

atOrAboveGoingDown[\I extends Integral[\I\]\](start: I, bound: I, stride: I): I

atOrBelow[\I extends Integral[\I\]\](start: I, bound: I, stride: I): I

atOrAbove[\I extends Integral[\I\]\](start: I, bound: I, stride: I): I

meetingPoint[\I extends Integral[\I\]\](init0: I, stride0: I, init1: I, stride1: I, stride: I): Maybe[\I\]

opr SCMP[\I extends Integral[\I\]\](a: I, b: I): Comparison

opr SCMP[\I extends Integral[\I\], J extends Integral[\J\]\](a: (I, J), b: (I, J)): Comparison

opr SCMP[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](a: (I, J, K), b: (I, J, K)): Comparison

opr PCMP[\I extends Integral[\I\]\](a: I, b: I): Comparison

opr PCMP[\I extends Integral[\I\], J extends Integral[\J\]\](a: (I, J), b: (I, J)): Comparison

opr PCMP[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](a: (I, J, K), b: (I, J, K)): Comparison

checkSelection[\R extends Range[\I\], I\](this: Range[\I\], other: Range[\I\], r: R): R

trait ScalarRange[\I extends Integral[\I\]\] extends Range[\I\]
    truncL(l: I): ScalarRangeWithLeft[\I\]
    opr CAP(self, other: Range[\I\]): Range[\I\]
    intersectWithExtent(e: ExtentScalarRange[\I\]): ScalarRangeWithExtent[\I\]
    check(): ScalarRange[\I\]
end

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: ScalarRange[\I\], j: ScalarRange[\J\]): Range2D[\I, J\]

trait Range2D[\I extends Integral[\I\], J extends Integral[\J\]\]
    extends Range[\(I, J)\]
    getter range1(): ScalarRange[\I\]
    getter range2(): ScalarRange[\J\]
    getter stride(): (I, J)

    every(s_i: I, s_j: J): Range2D[\I, J\]
    atMost(n_i: I, n_j: J): Range2D[\I, J\]
    truncL(l_i: I, l_j: J): RangeWithLeft[\(I, J)\]
    truncR(r_i: I, r_j: J): RangeWithRight[\(I, J)\]
    opr CAP(self, other: Range[\(I, J)\]): Range[\(I, J)\]
    opr CAP(self, other: Range2D[\I, J\]): Range[\(I, J)\]
    opr IN(n: (I, J), self): Boolean
    opr =(self, other: Range2D[\I, J\]): Boolean
    check(): Range2D[\I, J\]
end

trait ActualRange2D[\I extends Integral[\I\], J extends Integral[\J\],
        T extends ActualRange2D[\I, J, T, Scalar1, Scalar2\],
        Scalar1 extends ScalarRange[\I\],
        Scalar2 extends ScalarRange[\J\]\]
    extends Range2D[\I, J\]
    getter range1(): Scalar1
    getter range2(): Scalar2

    every(s_i: I, s_j: J): T
    imposeStride(s_i: I, s_j: J): T
    recombine(a: Scalar1, b: Scalar2): T
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: ScalarRange[\I\], j: ScalarRange[\J\], k: ScalarRange[\K\]): Range3D[\I, J, K\]

trait Range3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\]
    extends Range[\(I, J, K)\]
    getter range1(): ScalarRange[\I\]
    getter range2(): ScalarRange[\J\]
    getter range3(): ScalarRange[\K\]
    getter stride(): (I, J, K)
    getter isEmpty(): Boolean

    every(s_i: I, s_j: J, s_k: K): Range3D[\I, J, K\]
    atMost(n_i: I, n_j: J, n_k: K): Range3D[\I, J, K\]
    truncL(l_i: I, l_j: J, l_k: K): RangeWithLeft[\(I, J, K)\]
    truncR(r_i: I, r_j: J, r_k: K): RangeWithRight[\(I, J, K)\]
    opr CAP(self, other: Range[\(I, J, K)\]): Range[\(I, J, K)\]
    opr CAP(self, other: Range3D[\I, J, K\]): Range[\(I, J, K)\]
    opr IN(n: (I, J, K), self): Boolean
    opr =(self, other: Range3D[\I, J, K\]): Boolean
    check(): Range3D[\I, J, K\]
end

trait ActualRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\],
        T extends ActualRange3D[\I, J, K, T, Scalar1, Scalar2, Scalar3\],
        Scalar1 extends ScalarRange[\I\],
        Scalar2 extends ScalarRange[\J\],
        Scalar3 extends ScalarRange[\K\]\]
    extends Range3D[\I, J, K\]
    getter range1(): Scalar1
    getter range2(): Scalar2
    getter range3(): Scalar3

    every(s_i: I, s_j: J, s_k: K): T
    imposeStride(s_i: I, s_j: J, s_k: K): T
    recombine(a: Scalar1, b: Scalar2, c: Scalar3): T
end

trait PartialScalarRange[\I extends Integral[\I\]\]
    extends { ScalarRange[\I\], PartialRange[\I\] }
end

object OpenScalarRange[\I extends Integral[\I\]\](str: I)
    extends { PartialScalarRange[\I\], OpenRange[\I\] }
    getter stride(): I

    truncL(l: I): LeftScalarRange[\I\]
    flip(): OpenScalarRange[\I\]
    forward(): OpenScalarRange[\I\]
    every(s: I): OpenScalarRange[\I\]
    imposeStride(s: I): OpenScalarRange[\I\]
    atMost(n: I): ScalarRangeWithExtent[\I\]
    opr =(self, b: OpenRange[\I\]): Boolean
    opr CAP(self, other: ScalarRange[\I\]): ScalarRange[\I\]
    intersectWithExtent(e: ExtentScalarRange[\I\]): ScalarRangeWithExtent[\I\]
    openEveryParam(r: ScalarRange[\I\]): I
end

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: OpenScalarRange[\I\], j: OpenScalarRange[\J\]): OpenRange2D[\I, J\]

object OpenRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](str_i: I, str_j: J)
    extends { OpenRange[\(I, J)\], ActualRange2D[\I, J, OpenRange2D[\I, J\], OpenScalarRange[\I\],
        OpenScalarRange[\J\]\] }
    getter stride(): (I, J)
    getter range1(): OpenScalarRange[\I\]
    getter range2(): OpenScalarRange[\J\]

    flip(): OpenRange2D[\I, J\]
    forward(): OpenRange2D[\I, J\]
    recombine(i: OpenScalarRange[\I\], j: OpenScalarRange[\J\]): OpenRange2D[\I, J\]
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: OpenScalarRange[\I\], j: OpenScalarRange[\J\], k: OpenScalarRange[\K\]): OpenRange3D[\I, J, K\]

object OpenRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\](str_i: I, str_j: J, str_k: K)
    extends { OpenRange[\(I, J, K)\], ActualRange3D[\I, J, K, OpenRange3D[\I, J, K\], OpenScalarRange[\I\],
        OpenScalarRange[\J\],
        OpenScalarRange[\K\]\] }
    getter stride(): (I, J, K)
    getter range1(): OpenScalarRange[\I\]
    getter range2(): OpenScalarRange[\J\]
    getter range3(): OpenScalarRange[\K\]

    flip(): OpenRange3D[\I, J, K\]
    forward(): OpenRange3D[\I, J, K\]
    recombine(i: OpenScalarRange[\I\], j: OpenScalarRange[\J\], k: OpenScalarRange[\K\]): OpenRange3D[\I, J, K\]
end

open[\I extends Integral[\I\]\](): OpenScalarRange[\I\]

trait ScalarRangeWithExtent[\I extends Integral[\I\]\]
    extends { ScalarRange[\I\], RangeWithExtent[\I\] }
end

object ExtentScalarRange[\I extends Integral[\I\]\](ex: I, str: I)
    extends { ScalarRangeWithExtent[\I\], PartialScalarRange[\I\],
        ExtentRange[\I\] }
    getter stride(): I
    getter extent(): Just[\I\]
    getter fromLeft(): Boolean

    truncL(s: I): FullScalarRange[\I\]
    flip(): ExtentScalarRange[\I\]
    forward(): ExtentScalarRange[\I\]
    every(s: I): ExtentScalarRange[\I\]
    imposeStride(s: I): ExtentScalarRange[\I\]
    atMost(n: I): ScalarRange[\I\]
    opr CAP(self, other: Range[\I\]): ScalarRangeWithExtent[\I\]
    intersectWithExtent(e: ExtentScalarRange[\I\]): ScalarRangeWithExtent[\I\]
    opr =(self, b: ExtentScalarRange[\I\]): Boolean
    shiftLeft(shift: I): ExtentScalarRange[\I\]
    shiftRight(shift: I): ExtentScalarRange[\I\]
end

extentScalarRange[\I extends Integral[\I\]\](ex: I, str: I): ScalarRangeWithExtent[\I\]

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: ExtentScalarRange[\I\], j: ExtentScalarRange[\J\]): ExtentRange2D[\I, J\]

object ExtentRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](ex_i: I, ex_j: J, str_i: I, str_j: J)
    extends { ExtentRange[\(I, J)\], ActualRange2D[\I, J, ExtentRange2D[\I, J\], ExtentScalarRange[\I\],
        ExtentScalarRange[\J\]\] }
    getter stride(): (I, J)
    getter extent(): Just[\(I, J)\]
    getter range1(): ExtentScalarRange[\I\]
    getter range2(): ExtentScalarRange[\J\]

    flip(): ExtentRange2D[\I, J\]
    forward(): ExtentRange2D[\I, J\]
    recombine(i: ExtentScalarRange[\I\], j: ExtentScalarRange[\J\]): ExtentRange2D[\I, J\]
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: ExtentScalarRange[\I\], j: ExtentScalarRange[\J\],
        k: ExtentScalarRange[\K\]): ExtentRange3D[\I, J, K\]

object ExtentRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\](ex_i: I, ex_j: J, ex_k: K, str_i: I, str_j: J, str_k: K)
    extends { ExtentRange[\(I, J, K)\], ActualRange3D[\I, J, K, ExtentRange3D[\I, J, K\], ExtentScalarRange[\I\],
        ExtentScalarRange[\J\],
        ExtentScalarRange[\K\]\] }
    getter stride(): (I, J, K)
    getter extent(): Just[\(I, J, K)\]
    getter range1(): ExtentScalarRange[\I\]
    getter range2(): ExtentScalarRange[\J\]
    getter range3(): ExtentScalarRange[\K\]

    flip(): ExtentRange3D[\I, J, K\]
    forward(): ExtentRange3D[\I, J, K\]
    recombine(i: ExtentScalarRange[\I\], j: ExtentScalarRange[\J\],
            k: ExtentScalarRange[\K\]): ExtentRange3D[\I, J, K\]
end

trait BoundedScalarRange[\I extends Integral[\I\]\]
    extends { ScalarRange[\I\], BoundedRange[\I\] }
    opr CAP(self, other: ScalarRange[\I\]): BoundedScalarRange[\I\]
    intersectWithExtent(e: ExtentScalarRange[\I\]): FullScalarRange[\I\]
    forwardIntersection(other: BoundedScalarRange[\I\]): BoundedScalarRange[\I\]
    nonemptyUpwardIntersection(other: BoundedScalarRange[\I\], resultStride: I): BoundedScalarRange[\I\]
    nonemptyUpwardIntersectionWithPoint(other: BoundedScalarRange[\I\], resultStride: I, p: I): BoundedScalarRange[\I\]
end

trait ScalarRangeWithLeft[\I extends Integral[\I\]\]
    extends { BoundedScalarRange[\I\], RangeWithLeft[\I\] }
    maxLeft(other: ScalarRange[\I\]): I
end

object LeftScalarRange[\I extends Integral[\I\]\](l: I, str: I)
    extends { ScalarRangeWithLeft[\I\], PartialScalarRange[\I\],
        LeftRange[\I\] }
    getter stride(): I
    getter left(): Just[\I\]

    flip(): RightScalarRange[\I\]
    forward(): BoundedScalarRange[\I\]
    every(s: I): ScalarRange[\I\]
    imposeStride(s: I): LeftScalarRange[\I\]
    atMost(n: I): ScalarRange[\I\]
    opr =(self, b: LeftScalarRange[\I\]): Boolean
    opr IN(n: I, self): Boolean
    nonemptyUpwardIntersectionWithPoint(other: BoundedScalarRange[\I\], resultStride: I, p: I): ScalarRangeWithLeft[\I\]
    shiftLeft(shift: I): LeftScalarRange[\I\]
    shiftRight(shift: I): LeftScalarRange[\I\]
end

leftScalarRange[\I extends Integral[\I\]\](l: I, str: I): LeftScalarRange[\I\]

leftScalarRangeInter[\I extends Integral[\I\]\](l: I, str: I, p: I): LeftScalarRange[\I\]

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: LeftScalarRange[\I\], j: LeftScalarRange[\J\]): LeftRange2D[\I, J\]

object LeftRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](l_i: I, l_j: J, str_i: I, str_j: J)
    extends { LeftRange[\(I, J)\], ActualRange2D[\I, J, LeftRange2D[\I, J\], LeftScalarRange[\I\],
        LeftScalarRange[\J\]\] }
    getter stride(): (I, J)
    getter left(): Just[\(I, J)\]
    getter range1(): LeftScalarRange[\I\]
    getter range2(): LeftScalarRange[\J\]

    flip(): RightRange2D[\I, J\]
    forward(): BoundedRange[\(I, J)\]
    recombine(i: LeftScalarRange[\I\], j: LeftScalarRange[\J\]): LeftRange2D[\I, J\]
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: LeftScalarRange[\I\], j: LeftScalarRange[\J\], k: LeftScalarRange[\K\]): LeftRange3D[\I, J, K\]

object LeftRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\](l_i: I, l_j: J, l_k: K, str_i: I, str_j: J, str_k: K)
    extends { LeftRange[\(I, J, K)\], ActualRange3D[\I, J, K, LeftRange3D[\I, J, K\], LeftScalarRange[\I\],
        LeftScalarRange[\J\],
        LeftScalarRange[\K\]\] }
    getter stride(): (I, J, K)
    getter left(): Just[\(I, J, K)\]
    getter range1(): LeftScalarRange[\I\]
    getter range2(): LeftScalarRange[\J\]
    getter range3(): LeftScalarRange[\K\]

    flip(): RightRange3D[\I, J, K\]
    forward(): BoundedRange[\(I, J, K)\]
    recombine(i: LeftScalarRange[\I\], j: LeftScalarRange[\J\], k: LeftScalarRange[\K\]): LeftRange3D[\I, J, K\]
end

trait ScalarRangeWithRight[\I extends Integral[\I\]\]
    extends { BoundedScalarRange[\I\], RangeWithRight[\I\] }
    minRight(other: ScalarRange[\I\]): I
end

object RightScalarRange[\I\](r: I, str: I)
    extends { ScalarRangeWithRight[\I\], PartialScalarRange[\I\],
        RightRange[\I\] }
    getter stride(): I
    getter right(): Just[\I\]

    flip(): LeftScalarRange[\I\]
    forward(): BoundedScalarRange[\I\]
    every(s: I): RightScalarRange[\I\]
    imposeStride(s: I): RightScalarRange[\I\]
    atMost(n: I): ScalarRange[\I\]
    opr =(self, b: RightScalarRange[\I\]): Boolean
    opr IN(n: I, self): Boolean
    nonemptyUpwardIntersectionWithPoint(other: BoundedScalarRange[\I\], resultStride: I, p: I): ScalarRangeWithRight[\I\]
    shiftLeft(shift: I): RightScalarRange[\I\]
    shiftRight(shift: I): RightScalarRange[\I\]
end

rightScalarRange[\I extends Integral[\I\]\](r: I, str: I): RightScalarRange[\I\]

rightScalarRangeInter[\I extends Integral[\I\]\](r: I, str: I, p: I): RightScalarRange[\I\]

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: RightScalarRange[\I\], j: RightScalarRange[\J\]): RightRange2D[\I, J\]

object RightRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](r_i: I, r_j: J, str_i: I, str_j: J)
    extends { RightRange[\(I, J)\], ActualRange2D[\I, J, RightRange2D[\I, J\], RightScalarRange[\I\],
        RightScalarRange[\J\]\] }
    getter stride(): (I, J)
    getter right(): Just[\(I, J)\]
    getter range1(): RightScalarRange[\I\]
    getter range2(): RightScalarRange[\J\]

    flip(): LeftRange2D[\I, J\]
    forward(): BoundedRange[\(I, J)\]
    recombine(i: RightScalarRange[\I\], j: RightScalarRange[\J\]): RightRange2D[\I, J\]
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: RightScalarRange[\I\], j: RightScalarRange[\J\],
        k: RightScalarRange[\K\]): RightRange3D[\I, J, K\]

object RightRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\](r_i: I, r_j: J, r_k: K, str_i: I, str_j: J, str_k: K)
    extends { RightRange[\(I, J, K)\], ActualRange3D[\I, J, K, RightRange3D[\I, J, K\], RightScalarRange[\I\],
        RightScalarRange[\J\],
        RightScalarRange[\K\]\] }
    getter stride(): (I, J, K)
    getter right(): Just[\(I, J, K)\]
    getter range1(): RightScalarRange[\I\]
    getter range2(): RightScalarRange[\J\]
    getter range3(): RightScalarRange[\K\]

    flip(): LeftRange3D[\I, J, K\]
    forward(): BoundedRange[\(I, J, K)\]
    recombine(i: RightScalarRange[\I\], j: RightScalarRange[\J\],
            k: RightScalarRange[\K\]): RightRange3D[\I, J, K\]
end

trait FullScalarRange[\I extends Integral[\I\]\]
    extends { ScalarRangeWithLeft[\I\], ScalarRangeWithRight[\I\],
        ScalarRangeWithExtent[\I\],
        FullRange[\I\] }
    getter extent(): Just[\I\]
    getter bounds(): CompactFullScalarRange[\I\]

    flip(): FullScalarRange[\I\]
    every(s: I): FullScalarRange[\I\]
    imposeStride(s: I): FullScalarRange[\I\]
    atMost(n: I): FullScalarRange[\I\]
    opr =(self, b: FullScalarRange[\I\]): Boolean
    forwardIntersection(other: BoundedScalarRange[\I\]): FullScalarRange[\I\]
    nonemptyUpwardIntersection(other: BoundedScalarRange[\I\], resultStride: I): FullScalarRange[\I\]
    nonemptyUpwardIntersectionWithPoint(other: BoundedScalarRange[\I\], resultStride: I, p: I): FullScalarRange[\I\]
    opr [ r: Range[\I\] ]: FullScalarRange[\I\]
    opr[i: I]: I
    indexOf(i: I): Maybe[\I\]
end

trait FullRange2D[\I extends Integral[\I\], J extends Integral[\J\]\]
    extends { FullRange[\(I, J)\], Range2D[\I, J\], DelegatedIndexed[\(I, J), (I, J)\],
        ActualRange2D[\I, J, FullRange2D[\I, J\], FullScalarRange[\I\],
        FullScalarRange[\J\]\] }
    getter extent(): Just[\(I, J)\]
    getter generator(): Generator[\(I, J)\]
    getter indices(): Generator[\(I, J)\]

    opr | self |: ZZ32
    flip(): FullRange2D[\I, J\]
    opr [ ij: (I, J) ]: (I, J)
    opr [ r: Range[\(I, J)\] ]: FullRange2D[\I, J\]
    indexOf(n: (I,J)): Maybe[\(I,J)\]
end

tupleFlatten[\I, J, K\](t: (I, J), k: K): (I, J, K)

trait FullRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\]
    extends { FullRange[\(I, J, K)\], Range3D[\I, J, K\], DelegatedIndexed[\(I, J, K), (I, J, K)\],
        ActualRange3D[\I, J, K, FullRange3D[\I, J, K\], FullScalarRange[\I\],
        FullScalarRange[\J\],
        FullScalarRange[\K\]\] }
    getter extent(): Just[\(I, J, K)\]
    getter generator(): Generator[\(I, J, K)\]
    getter indices(): Generator[\(I, J)\]

    opr | self |: ZZ32
    flip(): FullRange3D[\I, J, K\]
    opr [ ij: (I, J, K) ]: (I, J, K)
    opr [ r: Range[\(I, J, K)\] ]: FullRange3D[\I, J, K\]
    indexOf(n: (I,J,K)): Maybe[\(I,J,K)\]
end

trait CompactFullScalarRange[\I extends Integral[\I\]\]
    extends { FullScalarRange[\I\], CompactFullRange[\I\] }
    getter stride(): I
    getter size(): ZZ32
    getter isEmpty(): Boolean
    getter indexValuePairs(): Indexed[\(I, I), I\]
    getter indices(): Indexed[\I, I\]

    opr [ i: I ]: I
    opr IN(n: I, self): Boolean
    indexOf(n: I): Maybe[\I\]
    shiftLeft(shift: I): CompactFullScalarRange[\I\]
    shiftRight(shift: I): CompactFullScalarRange[\I\]
end

object CompactFullParScalarRange[\I extends Integral[\I\]\](l: I, r: I)
    extends CompactFullScalarRange[\I\]
    getter lower(): I
    getter upper(): I
    getter left(): Just[\I\]
    getter right(): Just[\I\]

    seq(self): CompactFullSeqScalarRange[\I\]
    generate[\T\](red: Reduction[\T\], body: (I -> T)): T
    loop(body: (I -> ())): ()
end

object CompactFullSeqScalarRange[\I extends Integral[\I\]\](l: I, r: I)
    extends { CompactFullScalarRange[\I\], SequentialGenerator[\I\] }
    getter lower(): I
    getter upper(): I
    getter left(): Just[\I\]
    getter right(): Just[\I\]

    generate[\T\](red: Reduction[\T\], body: (I -> T)): T
    loop(body: (I -> ())): ()
end

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: CompactFullScalarRange[\I\], j: CompactFullScalarRange[\J\]): CompactFullRange2D[\I, J\]

object CompactFullRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](l_i: I, l_j: J, r_i: I, r_j: J)
    extends { CompactFullRange[\(I, J)\], FullRange2D[\I, J\] }
    getter lower(): (I, J)
    getter upper(): (I, J)
    getter bounds(): CompactFullRange2D[\I, J\]
    getter indices(): Generator[\(I, J)\]
    getter indexValuePairs(): Generator[\((I, J), (I, J))\]
    getter stride(): (I, J)
    getter left(): Just[\(I, J)\]
    getter right(): Just[\(I, J)\]
    getter range1(): CompactFullScalarRange[\I\]
    getter range2(): CompactFullScalarRange[\J\]

    recombine(i: FullScalarRange[\I\], j: FullScalarRange[\J\]): FullRange2D[\I, J\]
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: CompactFullScalarRange[\I\], j: CompactFullScalarRange[\J\],
        k: CompactFullScalarRange[\K\]): CompactFullRange3D[\I, J, K\]

object CompactFullRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\](l_i: I, l_j: J, l_k: K, r_i: I, r_j: J, r_k: K)
    extends { CompactFullRange[\(I, J, K)\], FullRange3D[\I, J, K\] }
    getter lower(): (I, J, K)
    getter upper(): (I, J, K)
    getter bounds(): CompactFullRange3D[\I, J, K\]
    getter indices(): Generator[\(I, J, K)\]
    getter indexValuePairs(): Generator[\((I, J, K), (I, J, K))\]
    getter stride(): (I, J, K)
    getter left(): Just[\(I, J, K)\]
    getter right(): Just[\(I, J, K)\]
    getter range1(): CompactFullScalarRange[\I\]
    getter range2(): CompactFullScalarRange[\J\]
    getter range3(): CompactFullScalarRange[\K\]

    recombine(i: FullScalarRange[\I\], j: FullScalarRange[\J\], k: FullScalarRange[\K\]): FullRange3D[\I, J, K\]
end

trait StridedFullScalarRange[\I extends Integral[\I\]\]
    extends { FullScalarRange[\I\], StridedFullRange[\I\] }
    getter size(): ZZ32
    getter isEmpty(): Boolean
    getter indexValuePairs(): Indexed[\(I, I), I\]
    getter indices(): Indexed[\I, I\]

    opr [ i: I ]: I
    opr IN(n: I, self): Boolean
    indexOf(n: I): Maybe[\I\]
    forward(): FullScalarRange[\I\]
end

object StridedFullParScalarRange[\I extends Integral[\I\]\](l: I, r: I, str: I)
    extends StridedFullScalarRange[\I\]
    getter stride(): I
    getter left(): Just[\I\]
    getter right(): Just[\I\]

    seq(self): StridedFullSeqScalarRange[\I\]
    generate[\T\](red: Reduction[\T\], body: (I -> T)): T
    loop(body: (I -> ())): ()
end

object StridedFullSeqScalarRange[\I extends Integral[\I\]\](l: I, r: I, str: I)
    extends { StridedFullScalarRange[\I\], SequentialGenerator[\I\] }
    getter stride(): I
    getter left(): Just[\I\]
    getter right(): Just[\I\]

    seq(self): StridedFullSeqScalarRange[\I\]
    generate[\T\](red: Reduction[\T\], body: (I -> T)): T
    loop(body: (I -> ())): ()
end

combine2D[\I extends Integral[\I\], J extends Integral[\J\]\](i: FullScalarRange[\I\], j: FullScalarRange[\J\]): FullRange2D[\I, J\]

object StridedFullRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](l_i: I, l_j: J, r_i: I, r_j: J, str_i: I, str_j: J)
    extends { StridedFullRange[\(I, J)\], FullRange2D[\I, J\] }
    getter bounds(): CompactFullRange2D[\I, J\]
    getter indices(): Generator[\(I, J)\]
    getter indexValuePairs(): Generator[\((I, J), (I, J))\]
    getter stride(): (I, J)
    getter left(): Just[\(I, J)\]
    getter right(): Just[\(I, J)\]
    getter range1(): FullScalarRange[\I\]
    getter range2(): FullScalarRange[\J\]

    forward(): FullRange2D[\I, J\]
    recombine(i: FullScalarRange[\I\], j: FullScalarRange[\J\]): StridedFullRange2D[\I, J\]
end

combine3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\](i: FullScalarRange[\I\], j: FullScalarRange[\J\], k: FullScalarRange[\K\]): FullRange3D[\I, J, K\]

object StridedFullRange3D[\I extends Integral[\I\], J extends Integral[\J\],
        K extends Integral[\K\]\](l_i: I, l_j: J, l_k: K, r_i: I, r_j: J, r_k: K, str_i: I,
        str_j: J,
        str_k: K)
    extends { StridedFullRange[\(I, J, K)\], FullRange3D[\I, J, K\] }
    getter bounds(): CompactFullRange3D[\I, J, K\]
    getter indices(): Generator[\(I, J, K)\]
    getter indexValuePairs(): Generator[\((I, J, K), (I, J, K))\]
    getter stride(): (I, J, K)
    getter left(): Just[\(I, J, K)\]
    getter right(): Just[\(I, J, K)\]
    getter range1(): FullScalarRange[\I\]
    getter range2(): FullScalarRange[\J\]
    getter range3(): FullScalarRange[\K\]

    forward(): FullRange3D[\I, J, K\]
    recombine(i: FullScalarRange[\I\], j: FullScalarRange[\J\], k: FullScalarRange[\K\]): StridedFullRange3D[\I, J, K\]
end

fullScalarRange[\I extends Integral[\I\]\](l: I, r: I, str: I): FullScalarRange[\I\]

fullScalarRangeInter[\I extends Integral[\I\]\](l: I, r: I, str: I, p: I): FullScalarRange[\I\]

fullRange2D[\I extends Integral[\I\], J extends Integral[\J\]\](l_i: I, l_j: J, r_i: I, r_j: J, str_i: I, str_j: J): FullRange2D[\I, J\]

fullRange3D[\I extends Integral[\I\], J extends Integral[\J\], K extends Integral[\K\]\]
           (l_i:I, l_j:J, l_k:K, r_i:I, r_j:J, r_k:K, str_i:I, str_j:J, str_k:K): FullRange3D[\I,J,K\]
emptyScalarRange[\I\](): FullScalarRange[\I\]

sized1Range[\I extends AnyIntegral\](_: I, lo: I, ex: I): CompactFullParScalarRange[\I\]

sized2Range[\I extends AnyIntegral, J extends AnyIntegral\](_: I, _: J, l1: I, l2: J, ex1: I, ex2: J): CompactFullRange2D[\I, J\]

sized3Range[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: I, _: J, _: K, l1: I, l2: J, l3: K, ex1: I, ex2: J,
        ex3: K): CompactFullRange3D[\I, J, K\]

bounded1Range[\I extends AnyIntegral\](_: I, lo: I, hi: I): CompactFullParScalarRange[\I\]

bounded2Range[\I extends AnyIntegral, J extends AnyIntegral\](_: I, _: J, l1: I, l2: J, hi1: I, hi2: J): CompactFullRange2D[\I, J\]

bounded3Range[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: I, _: J, _: K, l1: I, l2: J, l3: K, hi1: I, hi2: J,
        hi3: K): CompactFullRange3D[\I, J, K\]

left1Range[\I extends AnyIntegral\](_: I, x: I): LeftRange[\I\]

left2Range[\I extends AnyIntegral, J extends AnyIntegral\](_: I, _: J, x: I, y: J): LeftRange[\(I, J)\]

left3Range[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: I, _: J, _: K, x: I, y: J, z: K): LeftRange[\(I, J, K)\]

extent1Range[\I extends AnyIntegral\](_: I, x: I): ExtentRange[\I\]

extent2Range[\I extends AnyIntegral, J extends AnyIntegral\](_: I, _: J, x: I, y: J): ExtentRange[\(I, J)\]

extent3Range[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: I, _: J, _: K, x: I, y: J, z: K): ExtentRange[\(I, J, K)\]

right1Range[\I extends AnyIntegral\](_: I, x: I): RightRange[\I\]

right2Range[\I extends AnyIntegral, J extends AnyIntegral\](_: I, _: J, x: I, y: J): RightRange[\(I, J)\]

right3Range[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: I, _: J, _: K, x: I, y: J, z: K): RightRange[\(I, J, K)\]

openRangeHelper[\I extends AnyIntegral\](_: ()->I): OpenScalarRange[\I\]

openRangeHelper[\I extends AnyIntegral, J extends AnyIntegral\](_: ()->(I, J)): OpenRange2D[\I, J\]

openRangeHelper[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: ()->(I, J, K)): OpenRange3D[\I, J, K\]

open1Range[\I extends AnyIntegral\](_: I, x: I): OpenRange[\I\]

open2Range[\I extends AnyIntegral, J extends AnyIntegral\](_: I, _: J, x: I, y: J): OpenRange[\(I, J)\]

open3Range[\I extends AnyIntegral, J extends AnyIntegral, K extends AnyIntegral\](_: I, _: J, _: K, x: I, y: J, z: K): OpenRange[\(I, J, K)\]

end
