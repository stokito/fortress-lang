(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

api String

  concatAndBalanceIfNecessary(s1: String, s2: String): String

  object CatString(left: String, right:String) extends String
  end

  value object EmptyString extends String
  end

  object SubString extends String
  end
    
  trait StringDebug
    getter asDebugString(): String
    abstract asDebugStringIndented(indent: ZZ32): String
  end
  
  trait Concatenable 
    opr || (self, other: String): String
    opr || (self, other: EmptyString): String
    opr || (self, other: Char): String
  end Concatenable

  trait Balanceable 
    getter isBalanced(): Boolean
    getter isAlmostBalanced(): Boolean
    getter isExtremelyUnbalanced(): Boolean
  end Balanceable
  
  printStats(s: String): ()
 
  (** A string containing n spaces **)
  spaces(n: ZZ32): String

end


