(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Set


import Maybe.{...}
import Comparison.{...}
import Util.{...}
import GeneratorLibrary.{DefaultGeneratorImplementation}

(** Thrown when taking big intersection of no sets. **)
object EmptyIntersection extends UncheckedException end

(**
 * Sets represented using a (size-balanced) tree structure.  The underlying type $E$
 * must support comparison using $<$ and $=$.  When generated, these sets
 * produce their elements in sorted order.
 **)
trait Set[\E extends StandardTotalOrder[\E\]\] extends DefaultGeneratorImplementation[\E\]
 
   abstract getter isEmpty(): Boolean

   seq(self): SequentialGenerator[\E\]
   abstract seqgen[\R\](r: Reduction[\R\], body: E->R): R
   abstract ivgen[\R\](i0:ZZ32, r: Reduction[\R\], body: (ZZ32,E)->R): R
   abstract getVal():E
   abstract getLeftChild():Set[\E\]  (*) Not pleasant, revaling implementation detail. Should be part of another trait
   abstract getRightChild():Set[\E\]
   abstract showTree():String      
   abstract indexOfI(i:ZZ32, x:E): Maybe[\ZZ32\] 
   printTree():() 
   abstract opr | self | : ZZ32   

   abstract subscipt(i:ZZ32):E

   abstract minimum():Maybe[\E\]
   abstract maximum():Maybe[\E\]
   abstract extractMinimum():Maybe[\(E, Set[\E\])\]
   abstract extractMaximum():Maybe[\(E, Set[\E\])\]
   deleteMinimum():Set[\E\]
   deleteMaximum():Set[\E\]
   abstract add(x:E):Set[\E\]
   abstract delete(x:E):Set[\E\]
   abstract opr IN(x:E, self):Boolean
   
   abstract balancedDelete(r:Set[\E\]):Set[\E\]
   abstract generate[\R\](r: Reduction[\R\], body: E->R): R
   
   abstract opr UNION(self,t2:Set[\E\]):Set[\E\]
   abstract opr INTERSECTION(self,t2:Set[\E\]):Set[\E\]
   abstract opr DIFFERENCE(self,t2:Set[\E\]):Set[\E\]
   abstract opr SYMDIFF(self,t2:Set[\E\]):Set[\E\]
   abstract splitAt(e:E):(Set[\E\],Boolean,Set[\E\])
   abstract concat(t2:Set[\E\]):Set[\E\]
   abstract concat3( v:E, t2:Set[\E\]):Set[\E\]
   abstract splitIndex( x:ZZ32):(Set[\E\],Set[\E\])
   
   abstract opr SUBSET(self, other:Set[\E\]): Boolean
   opr SUBSETEQ(self, other:Set[\E\]): Boolean
   opr SUPSET(self, other:Set[\E\]): Boolean
   opr SUPSETEQ(self, other:Set[\E\]): Boolean
   abstract opr SETCMP(self, other:Set[\E\]): Comparison
   opr =(self, other:Set[\E\]): Boolean

end 
 
singleton[\E extends StandardTotalOrder[\E\]\](x:E): Set[\E\]
set[\E extends StandardTotalOrder[\E\]\](): Set[\E\]
set[\E extends StandardTotalOrder[\E\]\](g: Generator[\E\]): Set[\E\]

opr BIG {[\T extends StandardTotalOrder[\T\]\]} : Comprehension[\T,Set[\T\],Set[\T\],Set[\T\]\]
opr BIG UNION[\R extends StandardTotalOrder[\R\]\](): BigReduction[\Set[\R\],Set[\R\]\]
opr BIG UNION[\R extends StandardTotalOrder[\R\]\](g: Generator[\Set[\R\]\]): Set[\R\]
opr BIG INTERSECTION[\R extends StandardTotalOrder[\R\]\](): BigReduction[\Set[\R\],Maybe[\Set[\R\]\]\]

(*

opr {[\E\] es: E... }: Set[\E\]
opr BIG {[\T extends StandardTotalOrder[\T\]\]} : Comprehension[\T,Set[\T\],AnyCovColl,AnyCovColl\]
opr BIG {[\T extends StandardTotalOrder[\T\]\] g: Generator[\T\]} : Set[\T\]
opr BIG INTERSECTION[\R extends StandardTotalOrder[\R\]\](g: Generator[\Set[\R\]\]): Set[\R\]

*)

end
