(*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

tupleFromIndexed[\T\](x:Indexed[\T,ZZ32\]): Any

end
