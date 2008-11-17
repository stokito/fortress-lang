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
 
(*

  (*    Concatenable and Balanceable are implementation traits, 
        and thus they should not appear in the api *)
          
  trait Concatenable  extends String
    opr || (self, other: String): String
    opr || (self, other: EmptyString): String
    opr || (self, other: Char): String
  end Concatenable

  trait Balanceable extends String
    getter isBalanced(): Boolean
    getter isAlmostBalanced(): Boolean
    getter isExtremelyUnbalanced(): Boolean
  end Balanceable
  *)
  
  printStats(s: String): ()
 
  (** A string containing n spaces **)
  spaces(n: ZZ32): String
  
  (** The maximum size to which we grow a leaf node (by 
        copying) before switching to a CatString of the pieces **)
  var maxLeafSize: ZZ32

end


