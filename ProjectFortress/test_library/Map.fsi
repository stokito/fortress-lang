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

api Map

trait Tree[\Key,Val\] comprises {Node[\Key,Val\], Empty[\Key,Val\]}
  size():ZZ32
  empty():Boolean
  getPair():(Key, Val)
  getKey():Key
  getVal():Val
  getLeftChild():Tree[\Key,Val\]
  getRightChild():Tree[\Key,Val\]
  printTree():()
  toString():String
  member(x:Key): Maybe[\Val\]
  add(k:Key, v:Val):Tree[\Key,Val\]
  update(k:Key, v:Val):Tree[\Key,Val\]
  delete(k:Key):Tree[\Key,Val\]
end

object Empty[\Key,Val\]() extends Tree[\Key,Val\] end

object Node[\Key,Val\](pair:(Key,Val), left:Tree[\Key,Val\],
                       right:Tree[\Key,Val\]) extends Tree[\Key,Val\]
end

end
