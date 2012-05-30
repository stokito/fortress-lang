(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Tuple

(* Should we deprecate tuple and use identity instead?  Decision: no. *)
tuple[\T\](x:T):T

first[\T1,T2\](x:(T1,T2)): T1
second[\T1,T2\](x:(T1,T2)): T2
first[\T1,T2,T3\](x:(T1,T2,T3)): T1
second[\T1,T2,T3\](x:(T1,T2,T3)): T2
third[\T1,T2,T3\](x:(T1,T2,T3)): T3
first[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T1
second[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T2
third[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T3
fourth[\T1,T2,T3,T4\](x:(T1,T2,T3,T4)): T4

end
