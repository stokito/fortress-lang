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

trait Node[\Key,Val\] comprises {EmptyNode[\Key,Val\], LeafNode[\Key,Val\],
  InternalNode[\Key,Val\], WhiteNode[\Key,Val,height\]}

  getHeight():ZZ32

  isLeaf():Boolean
  isTreeEmpty():Boolean
  isNodeEmpty():Boolean

  search(k:Key):Maybe[\Val\]

  toString():String

  add(k:Key, v:Val, level:ZZ32):Node[\Key,Val\]

  (* splits the new child in half and sucks the split key up to this level *)
  split(index:ZZ32, heir:Node[\Key,Val\]):InternalNode[\Key,Val\]

  (* breaks the keys of this node into two new nodes *)
  break():(Node[\Key,Val\],Node[\Key,Val\],Key)

end

trait SkipList[\Key,Val,nat pInverse\] comprises {EmptyList[\Key,Val,pInverse\],
  FullList[\Key,Val,pInverse\]}

  toString():String
  search(k:Key):Maybe[\Val\]
  add(k:Key, v:Val):SkipList[\Key,Val,pInverse\]

end

generate_tail[\Key,Val\](node:Node[\Key,Val\], length:ZZ32):Node[\Key,Val\]
object EmptyNode[\Key,Val\]() extends Node[\Key,Val\] end
object WhiteNode[\Key,Val\](child:Node[\Key,Val\], height:ZZ32) extends Node[\Key,Val\] end
object LeafNode[\Key,Val\](pair: (Key, Val)) extends Node[\Key,Val\] end
object InternalNode[\Key,Val\](keys:Array[\Key,ZZ32\], children:Array[\Node[\Key,Val\],ZZ32\], height:ZZ32) extends Node[\Key,Val\] end
object FullList[\Key,Val,nat pInverse\](root:Node[\Key,Val\]) extends SkipList[\Key,Val,pInverse\] end
object EmptyList[\Key,Val,nat pInverse\](root:EmptyNode[\Key,Val\]) extends SkipList[\Key,Val,pInverse\] end

NewList[\Key,Val,nat pInverse\]():SkipList[\Key,Val,pInverse\]


end
