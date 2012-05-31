(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

trait UnaryPredicate[\T extends UnaryPredicate[\T,SIM\], opr SIM\]
    extends Any
  abstract opr SIM(self): Boolean
end

trait BinaryPredicate[\T extends BinaryPredicate[\T,SIM\], opr SIM\]
    extends Any
  abstract opr SIM(self, other: T): Boolean
end

trait Reflexive[\T extends Reflexive[\T,SIM\], opr SIM\]
    extends { BinaryPredicate[\T,SIM\] }
  property FORALL (a: T) (a SIM a)
end

trait Irreflexive[\T extends Irreflexive[\T,SIM\], opr SIM\]
    extends { BinaryPredicate[\T,SIM\] }
  property FORALL (a: T) NOT (a SIM a)
end

trait Symmetric[\T extends Symmetric[\T,SIM\], opr SIM\]
    extends { BinaryPredicate[\T,SIM\] }
  property FORALL (a: T, b: T) (a SIM b) IFF (b SIM a)
end

trait Transitive[\T extends Transitive[\T,SIM\], opr SIM\]
    extends { BinaryPredicate[\T,SIM\] }
  property FORALL (a: T, b: T, c: T) ((a SIM b) AND (b SIM c)) IMPLIES (a SIM c)
end

trait EquivalenceRelation[\T extends EquivalenceRelation[\T,SIM\], opr SIM\]
      extends { Reflexive[\T,SIM\], Symmetric[\T,SIM\], Transitive[\T,SIM\] }
end

trait IdentityEquality[\T extends IdentityEquality[\T\]\]
    extends { EquivalenceRelation[\T,=\] }
  opr =(self, other: T): Boolean
  property FORALL (a: T, b: T) (self = other) IFF (self SEQUIV other)
end

trait UnaryPredicateSubstitutionLaws[\T extends UnaryPredicateSubstitutionLaws[\T,SIM,SIMEQ\],
                                      opr SIM, opr SIMEQ\]
    extends { UnaryPredicate[\T,SIM\], BinaryPredicate[\T,SIMEQ\] }
  property FORALL (a: T, a': T) (a SIMEQ a') IMPLIES: ((SIM a) IFF (SIM a'))
end

trait BinaryPredicateSubstitutionLaws[\T extends BinaryPredicateSubstitutionLaws[\T,SIM,SIMEQ\],
                                       opr SIM, opr SIMEQ\]
    extends { BinaryPredicate[\T,SIM\], BinaryPredicate[\T,SIMEQ\] }
  property FORALL (a: T, a': T) (a SIMEQ a') IMPLIES: FORALL (b: T) (a SIM b) IFF (a' SIM b)
  property FORALL (b: T, b': T) (b SIMEQ b') IMPLIES: FORALL (a: T) (a SIM b) IFF (a SIM b')
end
