(******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

api Containment

trait Containment[\T extends Containment[\T\]\] extends Equality[\T\]
    abstract opr SUBSET(self, other: T): Boolean
    abstract opr ⊆(self, other: T): Boolean
    opr SUPSET(self, other: T): Boolean
    opr ⊇(self, other: T): Boolean
    abstract opr SETCMP(self, other: T): Comparison
end

trait ContainmentBySubset[\T extends Containment[\T\]\]
    extends Containment[\T\]
    opr SUBSET(self, other: T): Boolean
    opr ⊆(self, other: T): Boolean
    opr SETCMP(self, other: T): Comparison
end

trait ContainmentByComparison[\T extends Containment[\T\]\]
    extends Containment[\T\]
    opr SUBSET(self, other: T): Boolean
    opr ⊆(self, other: T): Boolean
end

trait ContainmentGenerator[\E, T extends ContainmentGenerator[\E, T\]\]
    extends { ContainmentByComparison[\T\], Generator[\E\] }
    opr ⊆(self, other: T): Boolean
end

end
