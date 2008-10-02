(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************)

api RangeInternals

roundToStride[\ I extends { Integral[\ I \] } \](amt : I, stride : I)
atOrAboveGoingUp[\ I extends { Integral[\ I \] } \](start : I, bound : I, stride : I) : I
atOrBelowGoingUp[\ I extends { Integral[\ I \] } \](start : I, bound : I, stride : I) : I
atOrBelowGoingDown[\ I extends { Integral[\ I \] } \](start : I, bound : I, stride : I) : I
atOrAboveGoingDown[\ I extends { Integral[\ I \] } \](start : I, bound : I, stride : I) : I
atOrBelow[\ I extends { Integral[\ I \] } \](start : I, bound : I, stride : I) : I
atOrAbove[\ I extends { Integral[\ I \] } \](start : I, bound : I, stride : I) : I
meetingPoint[\ I extends { Integral[\ I \] } \](init0 : I, stride0 : I, init1 : I, stride1 : I, stride : I) : Maybe[\ I \]
trait Rng [\ I \] extends { StandardPartialOrder[\ Rng[\ I \] \], Contains[\ I \] }
getter stride() : I
getter left() : Maybe[\ I \]
getter right() : Maybe[\ I \]
getter extent() : Maybe[\ I \]
getter isEmpty() : Boolean
getter isLeftBounded() : Boolean
getter isAnyBounded() : Boolean
getter toString() : String
truncL(l : I) : RngWithLeft[\ I \]
truncR(r : I) : RngWithRight[\ I \]
flip() : Rng[\ I \]
every(s : I) : Rng[\ I \]
atMost(n : I) : Rng[\ I \]
opr CAP(self, other : Rng[\ I \]) : Rng[\ I \]
opr =(self, b : Rng[\ I \]) : Boolean
opr IN(n : I, self) : Boolean
dump() : String
check() : Rng[\ I \]
end

trait ScalarRange [\ I extends { Integral[\ I \] } \] extends { Rng[\ I \] }
truncL(l : I) : ScalarRangeWithLeft[\ I \]
opr CAP(self, other : Rng[\ I \]) : Rng[\ I \]
intersectWithExtent(e : ExtentScalarRange[\ I \]) : ScalarRangeWithExtent[\ I \]
check() : ScalarRange[\ I \]
end

combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : ScalarRange[\ I \], j : ScalarRange[\ J \]) : Range2D[\ I, J \]
trait Range2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] extends { Rng[\ (I, J) \] }
getter range1() : ScalarRange[\ I \]
getter range2() : ScalarRange[\ J \]
getter stride() : (I, J)
getter isEmpty() : Boolean
getter toString() : String
every(s_i : I, s_j : J) : Range2D[\ I, J \]
atMost(n_i : I, n_j : J) : Range2D[\ I, J \]
truncL(l_i : I, l_j : J) : RngWithLeft[\ (I, J) \]
truncR(r_i : I, r_j : J) : RngWithRight[\ (I, J) \]
opr CAP(self, other : Rng[\ (I, J) \]) : Rng[\ (I, J) \]
opr CAP(self, other : Range2D[\ I, J \]) : Rng[\ (I, J) \]
opr IN(n : (I, J), self) : Boolean
opr =(self, other : Range2D[\ I, J \]) : Boolean
check() : Range2D[\ I, J \]
end

trait ActualRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] }, Self extends { ActualRange2D[\ I, J, Self, Scalar1, Scalar2 \] }, Scalar1 extends { ScalarRange[\ I \] }, Scalar2 extends { ScalarRange[\ J \] } \] extends { Range2D[\ I, J \] }
getter range1() : Scalar1
getter range2() : Scalar2
every(s_i : I, s_j : J) : Self
recombine(a : Scalar1, b : Scalar2) : Self
end

trait PartialRng [\ I \] extends { Rng[\ I \] }

end

trait PartialScalarRange [\ I extends { Integral[\ I \] } \] extends { ScalarRange[\ I \], PartialRng[\ I \] }

end

trait OpenRng [\ I \] extends { PartialRng[\ I \] }
getter left() : Nothing[\ I \]
getter right() : Nothing[\ I \]
getter extent() : Nothing[\ I \]
getter isEmpty() : Boolean
opr =(self, b : OpenScalarRange[\ I \]) : Boolean
opr IN(n : I, self) : Boolean
end

object OpenScalarRange [\ I extends { Integral[\ I \] } \] (str : I) extends { PartialScalarRange[\ I \], OpenRng[\ I \] }
    getter stride() : I
    getter toString() : String
    truncL(l : I) : LeftScalarRange[\ I \]
    flip() : OpenScalarRange[\ I \]
    every(s : I) : ScalarRange[\ I \]
    atMost(n : I) : ScalarRangeWithExtent[\ I \]
    opr CAP(self, other : ScalarRange[\ I \]) : ScalarRange[\ I \]
    intersectWithExtent(e : ExtentScalarRange[\ I \]) : ScalarRangeWithExtent[\ I \]
    openEveryParam(r : ScalarRange[\ I \]) : I
    dump() : String
end

combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : OpenScalarRange[\ I \], j : OpenScalarRange[\ J \]) : OpenRange2D[\ I, J \]
object OpenRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] (str_i : I, str_j : J) extends { OpenRng[\ (I, J) \], ActualRange2D[\ I, J, OpenRange2D[\ I, J \], OpenScalarRange[\ I \], OpenScalarRange[\ J \] \] }
    getter stride() : (I, J)
    getter range1() : OpenScalarRange[\ I \]
    getter range2() : OpenScalarRange[\ J \]
    flip() : OpenRange2D[\ I, J \]
    recombine(i : OpenScalarRange[\ I \], j : OpenScalarRange[\ J \]) : OpenRange2D[\ I, J \]
    dump() : String
end

open[\ I extends { Integral[\ I \] } \]()
trait RngWithExtent [\ I \] extends { Rng[\ I \] }
getter extent() : Just[\ I \]
end

trait ScalarRangeWithExtent [\ I extends { Integral[\ I \] } \] extends { ScalarRange[\ I \], RngWithExtent[\ I \] }

end

trait ExtentRng [\ I \] extends { RngWithExtent[\ I \], PartialRng[\ I \] }
getter left() : Nothing[\ I \]
getter right() : Nothing[\ I \]
getter isEmpty() : Boolean
opr IN(n : I, self) : Boolean
end

object ExtentScalarRange [\ I extends { Integral[\ I \] } \] (ex : I, str : I) extends { ScalarRangeWithExtent[\ I \], PartialScalarRange[\ I \], ExtentRng[\ I \] }
    getter stride() : I
    getter extent() : Just[\ I \]
    getter fromLeft() : Boolean
    getter toString() : String
    truncL(s : I) : FullScalarRange[\ I \]
    flip() : ExtentScalarRange[\ I \]
    every(s : I) : ExtentScalarRange[\ I \]
    atMost(n : I) : ScalarRange[\ I \]
    opr CAP(self, other : Rng[\ I \]) : ScalarRangeWithExtent[\ I \]
    intersectWithExtent(e : ExtentScalarRange[\ I \]) : ScalarRangeWithExtent[\ I \]
    opr =(self, b : ExtentScalarRange[\ I \]) : Boolean
    dump() : String
end

extentScalarRange[\ I extends { Integral[\ I \] } \](ex : I, str : I) : ScalarRangeWithExtent[\ I \]
combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : ExtentScalarRange[\ I \], j : ExtentScalarRange[\ J \]) : ExtentRange2D[\ I, J \]
object ExtentRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] (ex_i : I, ex_j : J, str_i : I, str_j : J) extends { ExtentRng[\ (I, J) \], ActualRange2D[\ I, J, ExtentRange2D[\ I, J \], ExtentScalarRange[\ I \], ExtentScalarRange[\ J \] \] }
    getter stride() : (I, J)
    getter extent() : Just[\ (I, J) \]
    getter range1() : ExtentScalarRange[\ I \]
    getter range2() : ExtentScalarRange[\ J \]
    flip() : ExtentRange2D[\ I, J \]
    recombine(i : ExtentScalarRange[\ I \], j : ExtentScalarRange[\ J \]) : ExtentRange2D[\ I, J \]
    dump() : String
end

trait BoundedRng [\ I \] extends { Rng[\ I \] }
getter leftOrRight() : I
every(s : I) : BoundedRng[\ I \]
atMost(n : I) : FullRng[\ I \]
opr CAP(self, other : Rng[\ I \]) : BoundedRng[\ I \]
end

trait BoundedScalarRange [\ I extends { Integral[\ I \] } \] extends { ScalarRange[\ I \], BoundedRng[\ I \] }
opr CAP(self, other : ScalarRange[\ I \]) : BoundedScalarRange[\ I \]
intersectWithExtent(e : ExtentScalarRange[\ I \]) : FullScalarRange[\ I \]
forwardIntersection(other : BoundedScalarRange[\ I \]) : BoundedScalarRange[\ I \]
nonemptyUpwardIntersection(other : BoundedScalarRange[\ I \], resultStride : I) : BoundedScalarRange[\ I \]
nonemptyUpwardIntersectionWithPoint(other : BoundedScalarRange[\ I \], resultStride : I, p : I) : BoundedScalarRange[\ I \]
end

trait RngWithLeft [\ I \] extends { BoundedRng[\ I \] }
getter left() : Just[\ I \]
getter leftOrRight() : I
end

trait ScalarRangeWithLeft [\ I extends { Integral[\ I \] } \] extends { BoundedScalarRange[\ I \], RngWithLeft[\ I \] }
maxLeft(other : ScalarRange[\ I \]) : I
end

trait LeftRng [\ I \] extends { RngWithLeft[\ I \], PartialRng[\ I \] }
getter right() : Nothing[\ I \]
getter extent() : Nothing[\ I \]
getter isEmpty() : Boolean
end

object LeftScalarRange [\ I extends { Integral[\ I \] } \] (l : I, str : I) extends { ScalarRangeWithLeft[\ I \], PartialScalarRange[\ I \], LeftRng[\ I \] }
    getter stride() : I
    getter left() : Just[\ I \]
    getter toString() : String
    flip() : RightScalarRange[\ I \]
    every(s : I) : ScalarRange[\ I \]
    atMost(n : I) : ScalarRange[\ I \]
    dump() : String
    opr =(self, b : LeftScalarRange[\ I \]) : Boolean
    opr IN(n : I, self) : Boolean
    nonemptyUpwardIntersectionWithPoint(other : BoundedScalarRange[\ I \], resultStride : I, p : I) : ScalarRangeWithLeft[\ I \]
end

leftScalarRange[\ I extends { Integral[\ I \] } \](l : I, str : I) : LeftScalarRange[\ I \]
leftScalarRangeInter[\ I extends { Integral[\ I \] } \](l : I, str : I, p : I) : LeftScalarRange[\ I \]
combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : LeftScalarRange[\ I \], j : LeftScalarRange[\ J \]) : LeftRange2D[\ I, J \]
object LeftRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] (l_i : I, l_j : J, str_i : I, str_j : J) extends { LeftRng[\ (I, J) \], ActualRange2D[\ I, J, LeftRange2D[\ I, J \], LeftScalarRange[\ I \], LeftScalarRange[\ J \] \] }
    getter stride() : (I, J)
    getter left() : Just[\ (I, J) \]
    getter range1() : LeftScalarRange[\ I \]
    getter range2() : LeftScalarRange[\ J \]
    flip() : RightRange2D[\ I, J \]
    recombine(i : LeftScalarRange[\ I \], j : LeftScalarRange[\ J \]) : LeftRange2D[\ I, J \]
    dump() : String
end

trait RngWithRight [\ I \] extends { BoundedRng[\ I \] }
getter right() : Just[\ I \]
end

trait ScalarRangeWithRight [\ I extends { Integral[\ I \] } \] extends { BoundedScalarRange[\ I \], RngWithRight[\ I \] }
minRight(other : ScalarRange[\ I \]) : I
end

trait RightRng [\ I \] extends { RngWithRight[\ I \], PartialRng[\ I \] }
getter left() : Nothing[\ I \]
getter leftOrRight() : Just[\ I \]
getter extent() : Nothing[\ I \]
getter isEmpty() : Boolean
end

object RightScalarRange [\ I \] (r : I, str : I) extends { ScalarRangeWithRight[\ I \], PartialScalarRange[\ I \], RightRng[\ I \] }
    getter stride() : I
    getter right() : Just[\ I \]
    getter toString() : String
    flip() : LeftScalarRange[\ I \]
    every(s : I) : RightScalarRange[\ I \]
    atMost(n : I) : ScalarRange[\ I \]
    dump() : String
    opr =(self, b : RightScalarRange[\ I \]) : Boolean
    opr IN(n : I, self) : Boolean
    nonemptyUpwardIntersectionWithPoint(other : BoundedScalarRange[\ I \], resultStride : I, p : I) : ScalarRangeWithRight[\ I \]
end

rightScalarRange[\ I extends { Integral[\ I \] } \](r : I, str : I) : RightScalarRange[\ I \]
rightScalarRangeInter[\ I extends { Integral[\ I \] } \](r : I, str : I, p : I) : RightScalarRange[\ I \]
combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : RightScalarRange[\ I \], j : RightScalarRange[\ J \]) : RightRange2D[\ I, J \]
object RightRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] (r_i : I, r_j : J, str_i : I, str_j : J) extends { RightRng[\ (I, J) \], ActualRange2D[\ I, J, RightRange2D[\ I, J \], RightScalarRange[\ I \], RightScalarRange[\ J \] \] }
    getter stride() : (I, J)
    getter right() : Just[\ (I, J) \]
    getter range1() : RightScalarRange[\ I \]
    getter range2() : RightScalarRange[\ J \]
    flip() : LeftRange2D[\ I, J \]
    recombine(i : RightScalarRange[\ I \], j : RightScalarRange[\ J \]) : RightRange2D[\ I, J \]
    dump() : String
end

trait FullRng [\ I \] extends { RngWithLeft[\ I \], RngWithRight[\ I \], RngWithExtent[\ I \], Indexed[\ I, I \] }
getter extent() : Just[\ I \]
end

trait FullScalarRange [\ I extends { Integral[\ I \] } \] extends { ScalarRangeWithLeft[\ I \], ScalarRangeWithRight[\ I \], ScalarRangeWithExtent[\ I \], FullRng[\ I \] }
getter bounds() : CompactFullScalarRange[\ I \]
getter toString() : String
flip() : FullScalarRange[\ I \]
every(s : I) : FullScalarRange[\ I \]
atMost(n : I) : FullScalarRange[\ I \]
opr =(self, b : FullScalarRange[\ I \]) : Boolean
forwardIntersection(other : BoundedScalarRange[\ I \]) : FullScalarRange[\ I \]
nonemptyUpwardIntersection(other : BoundedScalarRange[\ I \], resultStride : I) : FullScalarRange[\ I \]
nonemptyUpwardIntersectionWithPoint(other : BoundedScalarRange[\ I \], resultStride : I, p : I) : FullScalarRange[\ I \]
opr [ r : Range[\ I \] ] : FullScalarRange[\ I \]
end

r2r[\ I extends { Integral[\ I \] } \](r : Range[\ I \]) : ScalarRange[\ I \]
rng2r[\ I extends { Integral[\ I \] } \](r : ScalarRange[\ I \]) : Range[\ I \]
trait FullRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] extends { FullRng[\ (I, J) \], Range2D[\ I, J \], DelegatedIndexed[\ (I, J), (I, J) \] }
getter extent() : Just[\ (I, J) \]
getter generator() : Generator[\ (I, J) \]
getter indices() : Generator[\ (I, J) \]
getter indexValuePairs() : Generator[\ ((I, J), (I, J)) \]
flip() : FullRange2D[\ I, J \]
opr [ ij : (I, J) ] : (I, J)
opr [ r : Range[\ (I, J) \] ] : FullScalarRange[\ I \]
end

trait CompactFullRng [\ I \] extends { FullRng[\ I \] }

end

trait CompactFullScalarRange [\ I extends { Integral[\ I \] } \] extends { FullScalarRange[\ I \], CompactFullRng[\ I \] }
getter stride() : I
getter extent() : Just[\ I \]
getter size() : ZZ32
getter isEmpty() : Boolean
getter indexValuePairs() : Indexed[\ (I, I), I \]
getter indices() : Indexed[\ I, I \]
opr [ i : I ] : I
opr IN(n : I, self) : Boolean
selectIndices(i : ScalarRange[\ I \]) : FullScalarRange[\ I \]
end

object CompactFullParScalarRange [\ I extends { Integral[\ I \] } \] (l : I, r : I) extends { CompactFullScalarRange[\ I \] }
    getter left() : Just[\ I \]
    getter right() : Just[\ I \]
    seq(self) : CompactFullSeqScalarRange[\ I \]
    dump() : String
    generate[\ T \](red : Reduction[\ T \], body : (I -> T)) : T
    loop(body : (I -> ())) : ()
end

object CompactFullSeqScalarRange [\ I extends { Integral[\ I \] } \] (l : I, r : I) extends { CompactFullScalarRange[\ I \], SequentialGenerator[\ I \] }
    getter left() : Just[\ I \]
    getter right() : Just[\ I \]
    dump() : String
    generate[\ T \](red : Reduction[\ T \], body : (I -> T)) : T
    loop(body : (I -> ())) : ()
end

combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : CompactFullScalarRange[\ I \], j : CompactFullScalarRange[\ J \]) : CompactFullRange2D[\ I, J \]
object CompactFullRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] (l_i : I, l_j : J, r_i : I, r_j : J) extends { CompactFullRng[\ (I, J) \], FullRange2D[\ I, J \], ActualRange2D[\ I, J, CompactFullRange2D[\ I, J \], CompactFullScalarRange[\ I \], CompactFullScalarRange[\ J \] \] }
    getter bounds() : CompactFullRange2D[\ I, J \]
    getter stride() : (I, J)
    getter left() : Just[\ (I, J) \]
    getter right() : Just[\ (I, J) \]
    getter range1() : CompactFullScalarRange[\ I \]
    getter range2() : CompactFullScalarRange[\ J \]
    recombine(i : CompactFullScalarRange[\ I \], j : CompactFullScalarRange[\ J \]) : CompactFullRange2D[\ I, J \]
    dump() : String
end

trait StridedFullRng [\ I \] extends { FullRng[\ I \] }

end

trait StridedFullScalarRange [\ I extends { Integral[\ I \] } \] extends { FullScalarRange[\ I \], StridedFullRng[\ I \] }
getter size() : ZZ32
getter isEmpty() : Boolean
getter indexValuePairs() : Indexed[\ (I, I), I \]
getter indices() : Indexed[\ I, I \]
opr [ i : I ] : I
dump() : String
opr IN(n : I, self) : Boolean
end

object StridedFullParScalarRange [\ I extends { Integral[\ I \] } \] (l : I, r : I, str : I) extends { StridedFullScalarRange[\ I \] }
    getter stride() : I
    getter left() : Just[\ I \]
    getter right() : Just[\ I \]
    seq(self) : StridedFullSeqScalarRange[\ I \]
    dump() : String
    generate[\ T \](red : Reduction[\ T \], body : (I -> T)) : T
    loop(body : (I -> ())) : ()
end

object StridedFullSeqScalarRange [\ I extends { Integral[\ I \] } \] (l : I, r : I, str : I) extends { StridedFullScalarRange[\ I \], SequentialGenerator[\ I \] }
    getter stride() : I
    getter left() : Just[\ I \]
    getter right() : Just[\ I \]
    seq(self) : StridedFullSeqScalarRange[\ I \]
    dump() : String
    generate[\ T \](red : Reduction[\ T \], body : (I -> T)) : T
    loop(body : (I -> ())) : ()
end

combine2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](i : FullScalarRange[\ I \], j : FullScalarRange[\ J \]) : FullRange2D[\ I, J \]
object StridedFullRange2D [\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \] (l_i : I, l_j : J, r_i : I, r_j : J, str_i : I, str_j : J) extends { StridedFullRng[\ (I, J) \], FullRange2D[\ I, J \], ActualRange2D[\ I, J, StridedFullRange2D[\ I, J \], FullScalarRange[\ I \], FullScalarRange[\ J \] \] }
    getter bounds() : StridedFullRange2D[\ I, J \]
    getter stride() : (I, J)
    getter left() : Just[\ (I, J) \]
    getter right() : Just[\ (I, J) \]
    getter range1() : FullScalarRange[\ I \]
    getter range2() : FullScalarRange[\ J \]
    recombine(i : FullScalarRange[\ I \], j : FullScalarRange[\ J \]) : StridedFullRange2D[\ I, J \]
    dump() : String
end

fullScalarRange[\ I extends { Integral[\ I \] } \](l : I, r : I, str : I) : FullScalarRange[\ I \]
fullScalarRangeInter[\ I extends { Integral[\ I \] } \](l : I, r : I, str : I, p : I) : FullScalarRange[\ I \]
fullRange2D[\ I extends { Integral[\ I \] }, J extends { Integral[\ J \] } \](l_i : I, l_j : J, r_i : I, r_j : J, str_i : I, str_j : J) : FullRange2D[\ I, J \]
emptyScalarRange[\ I \]() : FullScalarRange[\ I \]

end