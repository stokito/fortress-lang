(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(* BooleanInterval *)
trait BooleanInterval
    extends { BooleanAlgebra[\BooleanInterval, INTERSECTION, UNION, SET_COMPLEMENT, SYMDIFF\],
              Set[\Boolean\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,AND\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,OR\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,XOR\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,EQV\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,=\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,IFF\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,NAND\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,NOR\],
              BinaryIntervalContainment[\BooleanInterval,Boolean,IMPLIES\],
              UnaryIntervalContainment[\BooleanInterval,Boolean,NOT\],
              Generator[\Boolean\] }
    comprises { ... }
  coerce (x: Boolean)
  getter hashCode(): ZZ64
  opr AND(self, other: BooleanInterval): BooleanInterval
  opr OR(self, other: BooleanInterval): BooleanInterval
  opr NOT(self): BooleanInterval
  opr XOR(self, other: BooleanInterval): BooleanInterval
  opr OPLUS(self, other: BooleanInterval): BooleanInterval
  opr EQV(self, other: BooleanInterval): BooleanInterval
  opr =(self, other: BooleanInterval): BooleanInterval
  opr IFF(self, other: BooleanInterval): BooleanInterval
  opr IMPLIES(self, other: BooleanInterval): BooleanInterval
  opr NAND(self, other: BooleanInterval): BooleanInterval
  opr NOR(self, other: BooleanInterval): BooleanInterval
  opr IN(other: Boolean, self): Boolean
  opr INTERSECTION(self, other: BooleanInterval): BooleanInterval
  opr UNION(self, other: BooleanInterval): BooleanInterval
  opr SET_COMPLEMENT(self): BooleanInterval
  opr SYMDIFF(self, other: BooleanInterval): BooleanInterval
  opr SETMINUS(self, other: BooleanInterval): BooleanInterval
  possibly(self): Boolean
  necessarily(self): Boolean
  certainly(self): Boolean
  opr ===(self, other: BooleanInterval): Boolean
  property true IN True AND false NOTIN True
  property true NOTIN False AND false IN False
  property true IN Uncertain AND false IN Uncertain
  property true NOTIN Impossible AND false NOTIN Impossible
  property FORALL (a) necessarily(a) === NOT possibly(NOT a)
  property FORALL (a) possibly(a) IFF true IN a
  property FORALL (a) certainly(a) IFF (true IN a AND false NOTIN a)
  property FORALL (a,b) (a NAND b) IFF NOT (a AND b)
  property FORALL (a,b) (a NOR b) IFF NOT (a OR b)
end
True: BooleanInterval
False: BooleanInterval
Uncertain: BooleanInterval
Impossible: BooleanInterval
test testData[ ] = { True, False, Uncertain, Impossible }

(* Exceptions *)
trait Exception comprises { CheckedException, UncheckedException }
  settable message: Maybe[\String\]
  settable chain: Maybe[\Exception\]
end

trait CheckedException
  extends { Exception }
  excludes { UncheckedException }
end

trait UncheckedException
  extends { Exception }
  excludes { CheckedException }
end

(* Threads *)
trait Thread[\T extends Any\]
  val(): T
  wait(): ()
  ready(): Boolean
  stop(): () throws Stopped
end

(* Tests *)
test object TestSuite(testFunctions = {})
  add(f: () -> ()): ()
  run(): ()
end

test fail(message: String): ()
