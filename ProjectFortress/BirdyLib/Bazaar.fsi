(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Bazaar

  import Util.{...}
  import Maybe.{...}
  import List.{...}

  opr BIG AND() :BigReduction[\Boolean,Boolean\]

  opr BIG +() :BigReduction[\ZZ32,ZZ32\]
  
  opr PROD() :BigReduction[\ZZ32,ZZ32\]

  opr BIG BITXOR(): BigReduction[\ZZ32,Maybe[\ZZ32\]\]

  (*) opr PROD() :BigReduction[\RR64,RR64\]

  opr PROD(g: Generator[\RR64\]) :RR64
  
  strToFloat(s: String): RR64  (* FAKE *)

  opr BIG MIN() : BigReduction[\RR64,Maybe[\RR64\]\]

  opr <|g: Generator[\ZZ32\]|>: List[\ZZ32\]

  opr |r: Range|: ZZ32

  opr BIG ||() : BigReduction[\String,String\]

end