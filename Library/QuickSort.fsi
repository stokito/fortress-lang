(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api QuickSort
import List.{List}

(** \url{http://en.wikipedia.org/wiki/Quicksort} *)
quicksort[\T\](lt:(T,T)->Boolean, arr:Array[\T,ZZ32\], left:ZZ32, right:ZZ32):()
quicksort[\T\](lt:(T,T)->Boolean, arr:Array[\T,ZZ32\]):()
quicksort[\T extends StandardTotalOrder[\T\]\](arr:Array[\T,ZZ32\]):()
quicksort[\T\](lt:(T,T)->Boolean, xs:List[\T\]):List[\T\]
quicksort[\T\](xs:List[\T\]):List[\T\]

end
