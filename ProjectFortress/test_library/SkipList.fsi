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

object SkipList[\Key,Val,nat pInverse\](root:Node[\Key,Val\]) end

NewList[\Key,Val,nat pInverse\]():SkipList[\Key,Val,pInverse\]

trait Node[\Key,Val\] comprises {EmptyNode[\Key,Val\], LeafNode[\Key,Val\],
  InternalNode[\Key,Val\], WhiteNode[\Key,Val\]}

  getHeight():ZZ32
  getSize():ZZ32

  isLeaf():Boolean
  isTreeEmpty():Boolean
  isNodeEmpty():Boolean

  search(k:Key):Maybe[\Val\]

  toString():String

  add(leaf:LeafNode[\Key,Val\], level:ZZ32):Node[\Key,Val\]
  add_helper(leaf:LeafNode[\Key,Val\], level:ZZ32):(Node[\Key,Val\], Boolean)

  remove(k:Key):(Node[\Key,Val\],Maybe[\Val\])

  (* merge must always be invoked with at least one element in the merge list *)
  merge(nodes:List[\Node[\Key,Val\]\]):Node[\Key,Val\]

  (* return the list of leaves that are under the current subtree *)
  getLeaves():List[\LeafNode[\Key,Val\]\]

  (* splits the new child in half and sucks the split key up to this level *)
  split(index:ZZ32, heir:Node[\Key,Val\]):InternalNode[\Key,Val\]

  (* breaks the keys of this node into two new nodes *)
  break():(Node[\Key,Val\],Node[\Key,Val\],Key)

end

generate_tail[\Key,Val\](node:Node[\Key,Val\], length:ZZ32):Node[\Key,Val\]

(* The four types of nodes: EmptyNode, WhiteNode, LeafNode, and InternalNode *)
object EmptyNode[\Key,Val\]() extends Node[\Key,Val\] end
object WhiteNode[\Key,Val\](child:Node[\Key,Val\]) extends Node[\Key,Val\] end
object LeafNode[\Key,Val\](pair: (Key, Val)) extends Node[\Key,Val\] end
object InternalNode[\Key,Val\](keys:Array[\Key,ZZ32\], children:Array[\Node[\Key,Val\],ZZ32\]) extends Node[\Key,Val\] end

end
