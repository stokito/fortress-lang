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

api Map

trait Map[\Key,Val\] extends Generator[\(Key,Val)\]
    comprises {NodeMap[\Key,Val\], EmptyMap[\Key,Val\]}
  printTree():()
  toString():String
  member(x:Key): Maybe[\Val\]
  deleteMinimum():Map[\Key,Val\]
  removeMinimum():((Key,Val), Map[\Key,Val\])
  add(k:Key, v:Val):Map[\Key,Val\]
  update(k:Key, v:Val):Map[\Key,Val\]
  delete(k:Key):Map[\Key,Val\]
  updateWith(f:Maybe[\Val\]->Maybe[\Val\], k:Key): Map[\Key,Val\]
(*
  union(other: Map[\Key,Val\]): Map[\Key,Val\]
  union(f:(Key,Val,Val)->Val, other: Map[\Key,Val\]): Map[\Key,Val\]
*)
end

mapping[\Key,Val\](g: Generator[\Mapping[\Key,Val\]\]): Map[\Key,Val\]

object NodeMap[\Key,Val\](pair:(Key,Val), left:Map[\Key,Val\],
                          right:Map[\Key,Val\]) extends Map[\Key,Val\]
end

object EmptyMap[\Key,Val\]() extends Map[\Key,Val\] end

end
