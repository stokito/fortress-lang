(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api CovariantCollection

opr APPCOV[\T, A extends T, B extends T\](a: CovariantCollection[\A\], b: CovariantCollection[\B\]): CovariantCollection[\T\]

opr APPCOV(a: AnyCovColl, b: AnyCovColl): AnyCovColl

trait AnyCovColl
end

trait CovariantCollection[\T\] extends AnyCovColl
        comprises { Empty[\T\], NonEmpty[\T\] }
    toImmutableArray(): ImmutableArray[\T, ZZ32\]
    toArray(): Array[\T, ZZ32\]
    assignToArray(res: Array[\T, ZZ32\]): ()
    abstract cata[\R\](e: R, s: (T -> R), a: ((ZZ32, R, R) -> R)): R
end

object Empty[\T\] extends CovariantCollection[\T\]
    getter isEmpty(): Boolean

    opr | self |: ZZ32
    cata[\R\](e: R, s: (T -> R), a: ((ZZ32, R, R) -> R)): R
end

trait NonEmpty[\T\] extends CovariantCollection[\T\]
    getter isEmpty(): Boolean
end

object Singleton[\T\](x: T) extends NonEmpty[\T\]
    opr | self |: ZZ32
    cata[\R\](e: R, s: (T -> R), a: ((ZZ32, R, R) -> R)): R
end

object CVReduction[\T\] extends MonoidReduction[\AnyCovColl\]
    empty(): AnyCovColl
    join(a: AnyCovColl, b: AnyCovColl): AnyCovColl
end

CVSingleton(a: Any): AnyCovColl

covariantCompr[\T, R\](unwrap: (AnyCovColl -> R)):
    Comprehension[\T, R, AnyCovColl, AnyCovColl\]

end
