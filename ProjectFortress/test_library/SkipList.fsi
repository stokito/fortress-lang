(*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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


api SkipList

trait Node[\Key,Val\] comprises {HeadNode[\Key,Val\],FullNode[\Key,Val\],NilNode[\Key,Val\]}
  getPair():(Key, Val)
  getKey():Key
  getVal():Val

  getLevel():ZZ32
  generate[\R\](r: Reduction[\R\], body: (Key,Val)->R): R
  generate2[\R\](r: Reduction[\R\], body: ZZ32->R): R
  toString():String
end

object HeadNode[\Key,Val,nat maxLevel\](forward:Array[\Node[\Key,Val\],ZZ32\]) end
object NilNode[\Key,Val\]() end
object SkipNode[\Key,Val\](pair:(Key,Val),forward:Array[\Node[\Key,Val\],ZZ32\]) end

object SkipList[\Key,Val,nat maxLevel,nat pInverse\](header:HeadNode[\Key,Val\],level:ZZ32) end

EmptyList[\Key,Val,nat maxLevel,nat pInverse\]():SkipList[\Key,Val,maxLevel,pInverse\]

end
