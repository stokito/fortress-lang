(*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Set
import Containment.{...}
import CovariantCollection.{...}

(** Thrown when taking big intersection of no sets. **)
object EmptyIntersection extends UncheckedException end

(**
 * Sets represented using a (size-balanced) tree structure.  The underlying type $E$
 * must support comparison using $<$ and $=$.  When generated, these sets
 * produce their elements in sorted order.
 **)
trait Set[\E\]
      extends { ZeroIndexed[\E\], ContainmentGenerator[\E,Set[\E\]\] }
      comprises { ... }
   printTree():()
   minimum():Maybe[\E\]
   maximum():Maybe[\E\]
   deleteMinimum():Set[\E\]
   deleteMaximum():Set[\E\]
   extractMinimum():Maybe[\(E, Set[\E\])\]
   extractMaximum():Maybe[\(E, Set[\E\])\]
   add(x:E):Set[\E\]
   delete(x:E):Set[\E\]
   opr UNION(self,t2:Set[\E\]):Set[\E\]
   opr INTERSECTION(self,t2:Set[\E\]):Set[\E\]
   opr DIFFERENCE(self,t2:Set[\E\]):Set[\E\]
   (** Symmetric difference: all elements in exactly one of the two sets. *)
   opr SYMDIFF(self,t2:Set[\E\]):Set[\E\]
   splitAt(e:E):(Set[\E\],Boolean,Set[\E\])
   opr SUBSET(self, other:Set[\E\]): Boolean
   opr SUBSETEQ(self, other:Set[\E\]): Boolean
   opr SUPSET(self, other:Set[\E\]): Boolean
   opr SUPSETEQ(self, other:Set[\E\]): Boolean
   opr SETCMP(self, other:Set[\E\]): Comparison
   (** Ordered concatenation; use only if you know what you are doing. **)
   concat(t2:Set[\E\]):Set[\E\]
   concat3(v:E, t2:Set[\E\]):Set[\E\]
end

singleton[\E\](x:E): Set[\E\]
set[\E\](): Set[\E\]
set[\E\](g: Generator[\E\]): Set[\E\]
opr {[\E\] es: E... }: Set[\E\]
opr BIG {[\T extends StandardTotalOrder[\T\]\]} : Comprehension[\T,Set[\T\],AnyCovColl,AnyCovColl\]
opr BIG {[\T extends StandardTotalOrder[\T\]\] g: Generator[\T\]} : Set[\T\]

opr BIG UNION[\R extends StandardTotalOrder[\R\]\](): BigReduction[\Set[\R\],Set[\R\]\]
opr BIG UNION[\R extends StandardTotalOrder[\R\]\](g: Generator[\Set[\R\]\]): Set[\R\]

object Union[\E\] extends CommutativeMonoidReduction[\Set[\E\]\] end

opr BIG INTERSECTION[\R extends StandardTotalOrder[\R\]\]():
        BigReduction[\Set[\R\],AnyMaybe\]
opr BIG INTERSECTION[\R extends StandardTotalOrder[\R\]\](g: Generator[\Set[\R\]\]): Set[\R\]

end
