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
import Set.{Set}
import CovariantCollection.{SomeCovariantCollection}

trait Map[\Key,Val\] extends Generator[\(Key,Val)\]
    comprises { ... }
  dom(self):Set[\Key\]
  opr | self |: ZZ32
  printTree():()
  toString():String
  member(x:Key): Maybe[\Val\]
  deleteMinimum():Map[\Key,Val\]
  removeMinimum():((Key,Val), Map[\Key,Val\])
  add(k:Key, v:Val):Map[\Key,Val\]
  update(k:Key, v:Val):Map[\Key,Val\]
  delete(k:Key):Map[\Key,Val\]
  updateWith(f:Maybe[\Val\]->Maybe[\Val\], k:Key): Map[\Key,Val\]
  opr UNION(other: Map[\Key,Val\]): Map[\Key,Val\]
  union(f:(Key,Val,Val)->Val, other: Map[\Key,Val\]): Map[\Key,Val\]
  combine[\That,Result\](f:(Key,Val,That)->Maybe[\Result\],
                          thisOnly:Map[\Key,Val\]->Map[\Key,Result\],
                          thatOnly:Map[\Key,That\]->Map[\Key,Result\],
                          that: Map[\Key,That\]) : Map[\Key, Result\]
  mapFilter[\Result\](f:(Key,Val)->Maybe[\Result\])
end

mapping[\Key,Val\](): Map[\Key,Val\]
mapping[\Key,Val\](g: Generator[\(Key,Val)\]): Map[\Key,Val\]

opr {|->[\Key,Val\] xs:(Key,Val)... }: Map[\Key,Val\]

opr BIG {|->[\Key,Val\] g: ( Reduction[\SomeCovariantCollection\],
                             (Key,Val) -> SomeCovariantCollection) ->
                           SomeCovariantCollection } : Map[\Key,Val\]

end
