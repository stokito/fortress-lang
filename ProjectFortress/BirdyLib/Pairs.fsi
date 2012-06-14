(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Pairs

import List.{...}
import Map.{...}
import Util.{...}
import CompilerAlgebra.{...}

pairs[\T\](g: Generator[\T\]): Generator[\(T,T)\]

triples[\T\](g: Generator[\T\]): Generator[\(T,T,T)\]
runRanges(x: List[\Boolean\]): List[\Range\]
geometricMean(xs: List[\RR64\]): RR64

opr UNIONCAT[\T extends StandardTotalOrder[\T\] ,U\](a: Map[\T, List[\U\]\], b: Map[\T, List[\U\]\]): Map[\T, List[\U\]\] 
opr BIG UNIONCAT[\T extends StandardTotalOrder[\T\],U\](): Comprehension[\Map[\T, List[\U\]\],Map[\T, List[\U\]\],Map[\T, List[\U\]\],Map[\T, List[\U\]\]\] (*) BigReduction[\Map[\T, List[\U\]\],Map[\T, List[\U\]\]\]
opr UNIONPLUS[\T extends StandardTotalOrder[\T\]\](a: Map[\T, ZZ32\], b: Map[\T, ZZ32\]): Map[\T, ZZ32\] 
opr BIG UNIONPLUS[\T extends StandardTotalOrder[\T\]\]():   Comprehension[\Map[\T, ZZ32\],Map[\T, ZZ32\],Map[\T, ZZ32\],Map[\T, ZZ32\]\]



end
