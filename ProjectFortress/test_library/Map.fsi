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

trait TreeMap[\Key,Val\] comprises {NodeMap[\Key,Val\], EmptyMap[\Key,Val\]}
  size():ZZ32
  empty():Boolean
  getPair():(Key, Val)
  getKey():Key
  getVal():Val
  getLeftChild():TreeMap[\Key,Val\]
  getRightChild():TreeMap[\Key,Val\]
  printTree():()
  toString():String
  member(x:Key): Maybe[\Val\]
  add(k:Key, v:Val):TreeMap[\Key,Val\]
  update(k:Key, v:Val):TreeMap[\Key,Val\]
  delete(k:Key):TreeMap[\Key,Val\]
end

object EmptyMap[\Key,Val\]() extends TreeMap[\Key,Val\] end

object NodeMap[\Key,Val\](pair:(Key,Val), left:TreeMap[\Key,Val\],
                          right:TreeMap[\Key,Val\]) extends TreeMap[\Key,Val\]
end

end
