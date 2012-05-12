(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Util

import Maybe.{...}

trait Reduction[\L\]
    abstract empty(): L
    abstract join(a: L, b: L): L
end

trait ActualReduction[\R,L\] extends Reduction[\L\]
  abstract lift(r: R): L
  abstract unlift(l:L): R
end

trait MonoidReduction[\R\] extends ActualReduction[\R,R\]
    lift(r:R): R 
    unlift(r:R): R 
end

trait AssociativeReduction[\R\] extends ActualReduction[\R,Maybe[\R\]\]
    empty(): Nothing[\R\] 
    join(a: Maybe[\R\], b: Maybe[\R\]): Maybe[\R\] 
    abstract simpleJoin(a:R, b:R): R
    lift(r:R): Maybe[\R\] 
    unlift(r:Maybe[\R\]): R 
end

trait ReductionWithZeroes[\R,L\] extends ActualReduction[\R,L\]
    isLeftZero(l:L): Boolean 
    isRightZero(l:L): Boolean
    isZero(l:L): Boolean 
end

trait Generator[\E\]
  abstract generate[\R\](r: Reduction[\R\], body: E -> R): R 
end  

__generate[\E,R\](g:Generator[\E\], r: Reduction[\R\], b:E->R): R 

trait BigOperator[\I,O,R,L\]
    abstract getter reduction(): ActualReduction[\R,L\]
    abstract getter body(): I->R
    abstract getter unwrap(): R->O
end

__bigOperator[\I,O,R,L\](o:BigOperator[\I,O,R,L\],desugaredClauses:(Reduction[\L\],I->L)->L): O

object BigReduction[\R,L\](r:ActualReduction[\R,L\]) extends BigOperator[\R,R,R,L\]
    getter body(): R->R
    getter unwrap(): R -> R
end

object Comprehension[\I,O,R,L\](unwrap: R -> O, reduction: ActualReduction[\R,L\], body:I->R) 
   extends BigOperator[\I,O,R,L\]
end

end