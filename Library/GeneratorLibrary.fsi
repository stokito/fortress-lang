(*******************************************************************************
    Copyright 2011,2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api GeneratorLibrary
import CompilerAlgebra.{ ... }

trait Container[\T extends Equality[\T\]\] extends Generator[\T\]
    abstract opr IN(elt: T, self): Boolean
    opr NOTIN(elt: T, self): Boolean
    opr NI(self, elt: T): Boolean
    opr NOTNI(self, elt: T): Boolean
end

trait DefaultGeneratorImplementation[\E1\] extends Generator[\E1\] excludes { Number, Character }
    getter reverse(): Generator[\E1\]
    abstract generate[\R\](r: Reduction[\R\], body: E1->R): R
    map[\Gyy\](f: E1->Gyy): Generator[\Gyy\]
(*)    seq(self): SequentialGenerator[\E1\]
    seq(): SequentialGenerator[\E1\]
    nest[\G1\](f: E1 -> Generator[\G1\]): Generator[\G1\]
    filter(f: E1 -> Condition[\()\]): Generator[\E1\]
    cross[\G2\](g: Generator[\G2\]): Generator[\(E1,G2)\]
    mapReduce[\R\](body: E1->R, join: (R,R)->R, id: R): R
    reduce(r: Reduction[\E1\]): E1
    reduce(join: (E1,E1)->E1, id: E1): E1
    loop(body :E1->()): ()
end DefaultGeneratorImplementation

trait DefaultSequentialGeneratorImplementation[\E2\] extends { SequentialGenerator[\E2\], DefaultGeneratorImplementation[\E2\] }
    getter reverse(): SequentialGenerator[\E2\]
    seq(): SequentialGenerator[\E2\]
    map[\G3\](f: E2->G3): SequentialGenerator[\G3\]
    nest[\G4\](f: E2 -> SequentialGenerator[\G4\]): SequentialGenerator[\G4\]
    filter(f: E2 -> Condition[\()\]): SequentialGenerator[\E2\]
    cross[\F1\](g: SequentialGenerator[\F1\]): SequentialGenerator[\(E2,F1)\]
end DefaultSequentialGeneratorImplementation


(* Reversed generators *)

trait ReversedGenerator[\E3\] extends DefaultGeneratorImplementation[\E3\] comprises { SimpleReversedGenerator[\E3\], SimpleSequentialReversedGenerator[\E3\] }
    abstract getter g(): Generator[\E3\]
    getter reverse(): Generator[\E3\]
    generate[\R\](r:Reduction[\R\], body:E3->R): R
    map[\G5\](f: E3->G5): Generator[\G5\]
    filter(f: E3 -> Condition[\()\]): Generator[\E3\]
end

object SimpleReversedGenerator[\E4\](g0:Generator[\E4\]) extends ReversedGenerator[\E4\]
    getter g(): Generator[\E4\]
    getter asString(): String
    seq(): SimpleSequentialReversedGenerator[\E4\]
end

object SimpleSequentialReversedGenerator[\E5\](g0:SequentialGenerator[\E5\])
        extends { ReversedGenerator[\E5\], DefaultSequentialGeneratorImplementation[\E5\] }
    getter g(): SequentialGenerator[\E5\]
    getter asString(): String
    getter reverse(): SequentialGenerator[\E5\]
    map[\G6\](f: E5->G6): SequentialGenerator[\G6\]
    filter(f: E5 -> Condition[\()\]): SequentialGenerator[\E5\]
end


(* Mapped generators *)

trait AnyMappedGenerator end

trait MappedGenerator[\E6,F2\] extends { DefaultGeneratorImplementation[\F2\], AnyMappedGenerator }
        comprises {  SimpleMappedGenerator[\E6,F2\], SimpleSequentialMappedGenerator[\E6,F2\] }
    abstract getter g(): Generator[\E6\]
    abstract getter f(): E6 -> F2
    getter reverse(): MappedGenerator[\E6,F2\]
    generate[\R\](r: Reduction[\R\], m: F2->R): R
    reduce(r: Reduction[\F2\]): F2
    map[\G7\](f': F2->G7): MappedGenerator[\E6,G7\]
    seq(): SimpleSequentialMappedGenerator[\E6,F2\]
end

object SimpleMappedGenerator[\E7,F3\](g0: Generator[\E7\], f0: E7->F3)
        extends MappedGenerator[\E7,F3\]
    getter g(): Generator[\E7\]
    getter f(): E7->F3
    getter asString(): String
end

object SimpleSequentialMappedGenerator[\E8,F4\](g0: SequentialGenerator[\E8\], f0: E8->F4)
        extends { MappedGenerator[\E8,F4\], DefaultSequentialGeneratorImplementation[\F4\] }
    getter g(): SequentialGenerator[\E8\]
    getter f(): E8->F4
    getter asString(): String
    getter reverse(): SimpleSequentialMappedGenerator[\E8,F4\]
    map[\G73\](f': F4->G73): SimpleSequentialMappedGenerator[\E8,G73\]
    seq(): SimpleSequentialMappedGenerator[\E8,F4\]
end


(* Nested generators *)

trait NestedGenerator[\E9,F5\] extends DefaultGeneratorImplementation[\F5\]
        comprises { SimpleNestedGenerator[\E9,F5\], SimpleSequentialNestedGenerator[\E9,F5\] }
    abstract getter g(): Generator[\E9\]
    abstract getter f(): E9 -> Generator[\F5\]
    getter reverse(): NestedGenerator[\E9,F5\]
    generate[\R\](r: Reduction[\R\], body: F5->R): R
    map[\G8\](h:F5->G8): Generator[\G8\]
    nest[\G9\](h:F5->Generator[\G9\]): Generator[\G9\]
    nest[\G19\](h:F5->SequentialGenerator[\G19\]): Generator[\G19\]
    mapReduce[\R\](body: F5->R, join:(R,R)->R, id:R): R
    reduce(r: Reduction[\F5\]): F5
    reduce(join:(F5,F5)->F5, id:F5):F5
    loop(body:F5->()): ()
end

object SimpleNestedGenerator[\E10,F6\](g0: Generator[\E10\], f0: E10->Generator[\F6\])
        extends { NestedGenerator[\E10,F6\] }
    getter g(): Generator[\E10\]
    getter f(): E10->Generator[\F6\]
    getter asString(): String
    seq(): SequentialGenerator[\F6\]
end

object SimpleSequentialNestedGenerator[\E11,F7\]
        (g0: SequentialGenerator[\E11\], f0: E11->SequentialGenerator[\F7\])
        extends { NestedGenerator[\E11,F7\], DefaultSequentialGeneratorImplementation[\F7\] }
    getter g(): SequentialGenerator[\E11\]
    getter f(): E11->SequentialGenerator[\F7\]
    getter asString(): String
    map[\G83\](h:F7->G83): SequentialGenerator[\G83\]
    nest[\G20\](h:F7->SequentialGenerator[\G20\]): SequentialGenerator[\G20\]
end

(* Cross-product generators *)

trait PairGenerator[\E12,F8\] extends DefaultGeneratorImplementation[\(E12,F8)\]
  comprises { SimplePairGenerator[\E12,F8\], SimpleSequentialPairGenerator[\E12,F8\] }
    abstract getter e(): Generator[\E12\]
    abstract getter f(): Generator[\F8\]
    getter reverse(): PairGenerator[\E12,F8\]
    generate[\R\](r: Reduction[\R\], m:(E12,F8)->R): R
end

object SimplePairGenerator[\E13,F9\](e0: Generator[\E13\], f0: Generator[\F9\])
        extends PairGenerator[\E13,F9\]
    getter e(): Generator[\E13\]
    getter f(): Generator[\F9\]
    getter asString(): String
    seq(): SequentialGenerator[\(E13,F9)\]
end

object SimpleSequentialPairGenerator[\E14,F10\]
        (e0: SequentialGenerator[\E14\], f0: SequentialGenerator[\F10\])
        extends { PairGenerator[\E14,F10\], DefaultSequentialGeneratorImplementation[\(E14,F10)\] }
    getter e(): SequentialGenerator[\E14\]
    getter f(): SequentialGenerator[\F10\]
    getter asString(): String
    getter reverse(): SimpleSequentialPairGenerator[\E14,F10\]
end

(* Filters *)

trait FilterGenerator[\E15\] extends DefaultGeneratorImplementation[\E15\]    (*) excludes { AnyMappedGenerator }
        comprises { SimpleFilterGenerator[\E15\], SimpleSequentialFilterGenerator[\E15\] }
    abstract getter g(): Generator[\E15\]
    abstract getter p(): E15 -> Condition[\()\]
    getter reverse(): FilterGenerator[\E15\] 
    generate[\R\](r:Reduction[\R\], m: E15->R): R
    reduce(r: Reduction[\E15\]): E15
    filter(p': E15 -> Condition[\()\]): FilterGenerator[\E15\]
    seq(): SimpleSequentialFilterGenerator[\E15\]
end

object SimpleFilterGenerator[\E16\](g0:Generator[\E16\], p0: E16->Condition[\()\])
        extends FilterGenerator[\E16\]
    getter g(): Generator[\E16\]
    getter p(): E16 -> Condition[\()\]
    getter asString(): String
end

object SimpleSequentialFilterGenerator[\E17\](g0: SequentialGenerator[\E17\], p0: E17->Condition[\()\])
        extends { FilterGenerator[\E17\], DefaultSequentialGeneratorImplementation[\E17\] }
    getter g(): SequentialGenerator[\E17\]
    getter p(): E17 -> Condition[\()\]
    getter asString(): String
    getter reverse(): SimpleSequentialFilterGenerator[\E17\]
    seq(): SimpleSequentialFilterGenerator[\E17\]
    filter(p': E17 -> Condition[\()\]): SimpleSequentialFilterGenerator[\E17\]
end


(************************************************************
* Reductions
************************************************************)

(* Reduction that projects the "body type" `B` onto type `R`,
   reduces within type `R`, then projects final result onto type `F21`.
   This is the trait that the overall implementation of a big operator
   or a comprehension has to deal with. *)

trait GeneralReduction[\B,R,F21\] extends Reduction[\R\]
    getter reverse(): GeneralReduction[\B,R,F21\]
    abstract lift(x: B): R
    abstract finish(y: R): F21
end

object ReversedGeneralReduction[\B,R,F22\](r: GeneralReduction[\B,R,F22\]) extends GeneralReduction[\B,R,F22\]
  getter asString(): String
  getter reverse(): GeneralReduction[\B,R,F22\]
  getter id(): R
  join(x:R, y:R): R
  lift(x: B): R
  finish(y: R): F22
end



(** The usual lifting to Option for identity-less operators **)
(*) trait NonemptyReduction[\B\] extends GeneralReduction[\B, Option[\B\], B\]
(*)     getter id() = NoneObject[\B\]
(*)     join(a: Option[\B\], b: Option[\B\]): Option[\B\] =
(*)         if av <- a then
(*)             if bv <- b then
(*)                 Some(simpleJoin(av,bv))
(*)             else
(*)                 a
(*)             end
(*)         else
(*)             b
(*)         end
(*)     simpleJoin(a:Any, b:Any): Any
(*)     lift(r: B) = Some r
(*)     unlift(r: Option[\B\]): B =
(*)         if res <- r then
(*)             res
(*)         else
(*)             throw EmptyReduction
(*)         end
(*) end

trait CommutativeReduction[\R\] extends Reduction[\R\]
    getter reverse(): CommutativeReduction[\R\]
(*)     property FORALL(a: R, b: R) join(a,b) = join(b,a)
end

trait CommutativeGeneralReduction[\B,R,F23\]
      extends { CommutativeReduction[\R\], GeneralReduction[\B,R,F23\] }
    getter reverse(): CommutativeGeneralReduction[\B,R,F23\]
end

(** Monoids have lift and finish operation that are the identity function. **)
trait MonoidReduction[\R\] extends GeneralReduction[\R,R,R\]
    lift(r: R): R
    finish(r: R): R
end

(** A `MapReduceReduction` takes an associative binary function `j` on
    arguments of type `R`, and the identity of that function `id`, and
    returns the corresponding reduction. **)
object MapReduceReduction[\R\](j:(R,R)->R, id:R) extends MonoidReduction[\R\]
    getter asString(): String
    join(a:R, b:R): R
end

trait CommutativeMonoidReduction[\R\]
      extends { MonoidReduction[\R\], CommutativeGeneralReduction[\R,R,R\] }
end

trait ReductionWithZeroes[\R\] extends Reduction[\R\]
    isLeftZero(r: R): Boolean
    isRightZero(r: R): Boolean
    isZero(r: R): Boolean
end

trait GeneralReductionWithZeroes[\B,R,F24\]
      extends { ReductionWithZeroes[\R\],  GeneralReduction[\B,R,F24\] }
end

trait BigOperator[\I,B,R,F25,O\]
    abstract getter body(): I->B
    abstract getter reduction(): GeneralReduction[\B,R,F25\]
    abstract getter unwrap(): F25->O
end

object BigReduction[\B,R,F26\](reduction:GeneralReduction[\B,R,F26\]) extends BigOperator[\B,B,R,F26,F26\]
    getter body(): B->B
    getter unwrap(): F26->F26
end

object Comprehension[\I,B,R,F27,O\](body:I->B, reduction: GeneralReduction[\B,R,F27\], unwrap: F27->O)
        extends BigOperator[\I,B,R,F27,O\]
end

(** VoidReduction is usually done for effect, so we pretend that
    the completion performs the effects.  This rules out things
    distributing over void (that would change the number of effects in
    our program) but not void distributing over other things. **)
object VoidReduction extends { CommutativeMonoidReduction[\()\] }
    getter asString(): String
    getter reverse(): VoidReduction
    getter id(): ()
    join(a: (), b: ()): ()
end


end
