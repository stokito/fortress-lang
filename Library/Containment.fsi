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