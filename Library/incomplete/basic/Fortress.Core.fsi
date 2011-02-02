(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

trait Any
  getter hashCode(): NN64
  getter asString(): String
  getter asDebugString(): String
  getter asExprString(): String
  opr ===(self, other: Any): Boolean
  opr IDENTITY(self): Any
  hash(maxval: NN64): NN64
  hash(maxval: NN32): NN32
  property FORALL (x, y, n: NN64) x === y IMPLIES x.hash(n) === y.hash(n)
  property FORALL (x, y, n: NN32) x === y IMPLIES x.hash(n) === y.hash(n)
  property FORALL (x) x.hashCode === x.hash(2^64-1)
  property FORALL (x, y) x === y IMPLIES x.asString = y.asString
  property FORALL (a) (IDENTITY a) === a
  property FORALL (a) a===a
  property FORALL (a,b) a===b IFF b===a
  property FORALL (a,b,c) (a===b AND: b===c) IMPLIES: a===c
end

trait Object extends { Any, EquivalenceRelation[\Object,===\], IdentityOperator[\Object\] }
    excludes { Tuple }
end

trait Tuple extends { Any, EquivalenceRelation[\Tuple,===\], IdentityOperator[\Tuple\] }
    excludes { Object }
end

trait Boolean
    extends { BooleanAlgebra[\Boolean,juxtaposition,OR,NOT,XOR\],
              BooleanAlgebra[\Boolean,juxtaposition,OR,NOT,OPLUS\],
              BooleanAlgebra[\Boolean,AND,OR,NOT,XOR\],
              BooleanAlgebra[\Boolean,AND,OR,NOT,OPLUS\],
              BooleanAlgebra[\Boolean,OR,juxtaposition,NOT,EQV\],
              BooleanAlgebra[\Boolean,OR,juxtaposition,NOT,IFF\],
              BooleanAlgebra[\Boolean,OR,AND,NOT,EQV\],
              BooleanAlgebra[\Boolean,OR,AND,NOT,IFF\],
              TotalOrder[\Boolean,IMPLIES\],
              PartialOrderAndBoundedLattice[\Boolean,IMPLIES,juxtaposition,OR\],
              PartialOrderAndBoundedLattice[\Boolean,IMPLIES,AND,OR\],
              IdentityEquality[\Boolean\],
              EquivalenceRelation[\Boolean,EQV\],
              EquivalenceRelation[\Boolean,IFF\],
              Symmetric[\Boolean,AND\], Symmetric[\Boolean,OR\],
              Symmetric[\Boolean,XOR\], Symmetric[\Boolean,OPLUS\],
              Symmetric[\Boolean,NAND\], Commutative[\Boolean,NAND\],
              Symmetric[\Boolean,NOR\], Commutative[\Boolean,NOR\] }
    comprises { ... }
  coerce[\bool b\](x: BooleanLiteral[\b\])
  coerce(x: Identity[\juxtaposition\])
  coerce(x: Identity[\AND\])
  coerce(x: Identity[\OR\])
  coerce(x: Identity[\XOR\])
  coerce(x: Identity[\OPLUS\])
  coerce(x: Identity[\EQV\])
  coerce(x: Identity[\IFF\])
  coerce(x: ComplementBound[\AND\])
  coerce(x: ComplementBound[\OR\])
  coerce(x: Zero[\AND\])
  coerce(x: Zero[\OR\])
  coerce(x: MaximalElement[\IMPLIES\])
  coerce(x: MinimalElement[\IMPLIES\])
  getter hashCode(): NN64
  opr juxtaposition(self, other: Boolean): Boolean
  opr AND(self, other: Boolean): Boolean
  opr AND(self, other: ()->Boolean): Boolean
  opr OR(self, other: Boolean): Boolean
  opr OR(self, other: ()->Boolean): Boolean
  opr NOT(self): Boolean
  opr XOR(self, other: Boolean): Boolean
  opr OPLUS(self, other: Boolean): Boolean
  opr EQV(self, other: Boolean): Boolean
  opr =(self, other: Boolean): Boolean
  opr IFF(self, other: Boolean): Boolean
  opr IMPLIES(self, other: Boolean): Boolean
  opr IMPLIES(self, other: ()->Boolean): Boolean
  opr NAND(self, other: Boolean):  Boolean
  opr NOR(self, other: Boolean):  Boolean
  opr ===(self, other: Boolean): Boolean
  majority(self, other1: Boolean, other2: Boolean)
  
  property FORALL (a, b) a b = (a AND b)
  property FORALL (a, b) (a XOR b) = (a OPLUS b)
  property FORALL (a, b) (a EQV b) = (a IFF b) = (a = b) = (a === b)
  property FORALL (a, b) (a IMPLIES b) = ((NOT a) OR b)
  property FORALL (a, b) (a NAND b) = NOT (a AND b)
  property FORALL (a, b) (a NOR b) = NOT (a OR b)
end
test testData[~] = { false, true }
true: Boolean
false: Boolean
