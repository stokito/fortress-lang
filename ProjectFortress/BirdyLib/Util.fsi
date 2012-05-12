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

trait Generator[\E\]
  abstract generate[\R\](r: Reduction[\R\], body: E -> R): R 
end  

__generate[\E,R\](g:Generator[\E\], r: Reduction[\R\], b:E->R): R 

trait BigOperator[\I,R\]
    abstract getter reduction(): Reduction[\R\]
    abstract getter body(): I->R
end

__bigOperator[\I,R\](o:BigOperator[\I,R\],desugaredClauses:(Reduction[\R\],I->R)->R): R

object BigReduction[\R\](r:Reduction[\R\]) extends BigOperator[\R,R\]
    getter body(): R->R
end

object Comprehension[\I,R\](reduction: Reduction[\R\], body:I->R) extends BigOperator[\I,R\]
end

end