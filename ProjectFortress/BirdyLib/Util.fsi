(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Util

import Maybe.{...}

trait ActualReduction[\R extends Any,L extends Any\] extends Reduction[\L\]
  abstract lift(r: R): L
  abstract unlift(l:L): R
end

trait MonoidReduction[\R extends Any\] extends ActualReduction[\R,R\]
    lift(r:R): R 
    unlift(r:R): R 
end

trait AssociativeReduction[\R extends Any\] extends ActualReduction[\R,Maybe[\R\]\]
    getter id(): Nothing[\R\] 
    join(a: Maybe[\R\], b: Maybe[\R\]): Maybe[\R\] 
    abstract simpleJoin(a:R, b:R): R
    lift(r:R): Maybe[\R\] 
    unlift(r:Maybe[\R\]): R 
end

trait ReductionWithZeroes[\R extends Any,L extends Any\] extends ActualReduction[\R,L\]
    isLeftZero(l:L): Boolean 
    isRightZero(l:L): Boolean
    isZero(l:L): Boolean 
end

trait Indexed[\E, I\] extends Generator[\E\] end

__generate[\E extends Any,R extends Any\](g:Generator[\E\], r: Reduction[\R\], b:E->R): R 

trait BigOperator[\I extends Any,O extends Any,R extends Any,L extends Any\]
    abstract getter reduction(): ActualReduction[\R,L\]
    abstract getter body(): I->R
    abstract getter unwrap(): R->O
end

__bigOperator[\I extends Any,O extends Any,R extends Any,L extends Any\](o:BigOperator[\I,O,R,L\],desugaredClauses:(Reduction[\L\],I->L)->L): O

__bigOperatorSugar[\I extends Any,O extends Any,R extends Any,L extends Any\](o:BigOperator[\I,O,R,L\],g:Generator[\I\]): O

object BigReduction[\R extends Any,L extends Any\](r:ActualReduction[\R,L\]) extends BigOperator[\R,R,R,L\]
    getter body(): R->R
    getter unwrap(): R -> R
end

object Comprehension[\I extends Any,O extends Any,R extends Any,L extends Any\](unwrap: R -> O, reduction: ActualReduction[\R,L\], body:I->R) 
   extends BigOperator[\I,O,R,L\]
end

embiggen[\T\](j:(T,T)->T, z:T) : Comprehension[\T,T,T,T\]

  object VoidReduction extends Reduction[\()\]
    getter asString(): String 
    getter id(): ()
    join(a: (), b: ()): () 
  end

end