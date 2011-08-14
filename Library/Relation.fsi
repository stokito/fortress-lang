(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Relation
import Containment.{...}
import Map.{...} except { opr BIG UNION }
import Set.{...}

trait Relation[\T\] extends ContainmentBySubset[\Relation[\T\]\] comprises {...}
  dom(self): Set[\T\]
  range(self): Set[\T\]
  pairs(): Generator[\(T,T)\]
  image(x:T): Set[\T\]
  preimage(y:T): Set[\T\]
  compose(other:Relation[\T\]): Relation[\T\]
  opr INVERSE(self): Relation[\T\]
  opr UNION(self, other:Relation[\T\]): Relation[\T\]
  opr INTERSECTION(self, other:Relation[\T\]): Relation[\T\]
  opr DIFFERENCE(self, other:Relation[\T\]): Relation[\T\]
  opr =(self, other: Relation[\T\]): Boolean
  opr IN(p:(T,T), self): Boolean

  (* Relation predicates *)
  opr SUBSETEQ(self, other:Relation[\T\]): Boolean
  opr SUBSET(self, other:Relation[\T\]): Boolean
  opr SUPSET(self, other:Relation[\T\]): Boolean
  opr SUPSETEQ(self, other:Relation[\T\]): Boolean

  (* Closure predicates *)
  isSymmetric(): Boolean
  isTransitive(): Boolean
  isReflexive(): Boolean
  isFunctional(): Boolean
  isIrreflexive(): Boolean
  isEquivalence(): Boolean

  (* Closure operations *)
  symmetricClosure(): Relation[\T\]
  reflexiveClosure(): Relation[\T\]
  transitiveClosure(): Relation[\T\]
  symmetricTransitiveClosure(): Relation[\T\]
end

relation[\T\](): Relation[\T\]
relation[\T\](domain: Set[\T\], f: T -> Set[\T\]): Relation[\T\]
relation[\T\](rel: Map[\T,Set[\T\]\]): Relation[\T\]
relation[\T\](pairs: Generator[\(T,T)\]): Relation[\T\]

opr BIG RELATION[\T\](): Comprehension[\(T,T), Relation[\T\], Map[\T,Set[\T\]\], Map[\T,Set[\T\]\]\]
opr BIG RELATION[\T\](g: Generator[\(T,T)\]): Comprehension[\(T,T), Relation[\T\], Map[\T,Set[\T\]\], Map[\T,Set[\T\]\]\]

end
