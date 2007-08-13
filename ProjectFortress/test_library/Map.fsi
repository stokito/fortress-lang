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

trait TreeMap[\Key,Val\] extends Generator[\(Key,Val)\]
    comprises {NodeMap[\Key,Val\], EmptyMap[\Key,Val\]}
  getPair():(Key, Val)  (* DEPRACATED *)
  getKey():Key          (* DEPRACATED *)
  getVal():Val          (* DEPRACATED *)
  getLeftChild():TreeMap[\Key,Val\]     (* DEPRACATED *)
  getRightChild():TreeMap[\Key,Val\]    (* DEPRACATED *)
  printTree():()
  toString():String
  member(x:Key): Maybe[\Val\]
  deleteMinimum():TreeMap[\Key,Val\]
  removeMinimum():((Key,Val), TreeMap[\Key,Val\])
  add(k:Key, v:Val):TreeMap[\Key,Val\]
  update(k:Key, v:Val):TreeMap[\Key,Val\]
  delete(k:Key):TreeMap[\Key,Val\]
  updateWith(f:Maybe[\Val\]->Maybe[\Val\], k:Key): TreeMap[\Key,Val\]
(*
  union(other: TreeMap[\Key,Val\]): TreeMap[\Key,Val\]
  union(f:(Key,Val,Val)->Val, other: TreeMap[\Key,Val\]): TreeMap[\Key,Val\]
*)
  balancedDelete(r:TreeMap[\Key,Val\]):TreeMap[\Key,Val\]
  balancedAdd(x:(Key,Val), left:TreeMap[\Key,Val\], right:TreeMap[\Key,Val\]):NodeMap[\Key,Val\]
end

singleton[\Key,Val\](k:Key, v:Val): TreeMap[\Key,Val\]

mapping[\Key,Val\](g: Generator[\(Key,Val)\]): TreeMap[\Key,Val\]

(*
object MapUnion[\Key,Val\]() extends Reduction[\TreeMap[\Key,Val\]\] end
*)

object EmptyMap[\Key,Val\]() extends TreeMap[\Key,Val\] end

end
