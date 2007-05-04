(*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

api Set

trait Tree[\E\] comprises {Node[\E\], Empty[\E\]}
   size():ZZ32
   empty():Boolean
   getVal():E
   getLeftChild():Tree[\E\]
   getRightChild():Tree[\E\]
   printTree():()
   toString():String
   member(x:E):Boolean
   minimum():E
   add(x:E):Tree[\E\]
   delete(x:E):Tree[\E\]
   union(t2:Tree[\E\]):Tree[\E\]
   intersection(t2:Tree[\E\]):Tree[\E\]
   difference(t2:Tree[\E\]):Tree[\E\]
end

object Empty[\E\]() extends Tree[\E\] end

object Node[\E\](val:E,  left:Tree[\E\], right:Tree[\E\]) extends Tree[\E\] end

end
