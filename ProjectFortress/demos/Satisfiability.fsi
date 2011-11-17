(*******************************************************************************
    Copyright 2010 Joonhyung Hwang and Chulwoo Lee, KAIST
    All rights reserved.
 ******************************************************************************)

(* Satisfiability Library using DPLL *)

api Satisfiability
import File.{...}
import List.{...}

parseLine(e:List[\ZZ32\], l:String, s:ZZ32, n:ZZ32):List[\ZZ32\]
parseFile(f:FileReadStream):List[\List[\ZZ32\]\]
readQuery(filename:String):List[\List[\ZZ32\]\]
unitPropagation(f:List[\List[\ZZ32\]\], theta:List[\ZZ32\]):(List[\List[\ZZ32\]\], List[\ZZ32\])
isSatisfied(f:List[\List[\ZZ32\]\], theta:List[\ZZ32\]):Boolean
isConflicting(f:List[\List[\ZZ32\]\], theta:List[\ZZ32\]):ZZ32
chooseFreeVariable(f:List[\List[\ZZ32\]\], theta:List[\ZZ32\]):ZZ32
dpll(f:List[\List[\ZZ32\]\], theta:List[\ZZ32\]):List[\ZZ32\]
satisfiability(f:List[\List[\ZZ32\]\]):List[\ZZ32\]

end
