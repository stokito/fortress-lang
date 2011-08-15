(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api String

  concatAndBalanceIfNecessary(s1: String, s2: String): String

  object CatString(left: String, right:String) extends String
  end

  value object EmptyString extends String
  end

  object SubString extends String
  end
 
  object StringStats() extends Object
    getter minFlat(): ZZ32
    getter maxFlat(): ZZ32
    getter numFlat(): ZZ32
    getter ssize(): ZZ32
    getter sdepth(): ZZ32
    collectStatsFor(s: String): ()
  end StringStats

  (** A string containing n spaces **)
  spaces(n: ZZ32): String
  (** Platform-dependent line separator sequence **)
  newline: String
  doubleNewline: String
  
  (** The maximum size to which we grow a leaf node (by 
        copying) before switching to a CatString of the pieces.
        Clients may assign to this variable **)
  var maxLeafSize: ZZ32

end


