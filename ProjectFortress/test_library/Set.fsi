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

trait TreeSet[\E\] extends Generator[\E\] comprises {NodeSet[\E\], EmptySet[\E\]}
   getter isEmpty(): Boolean
   getter size(): ZZ32
   generate[\R\](r: Reduction[\R\], body: E->R): R
   seq(): SequentialGenerator[\E\]
   getVal():E
   printTree():()
   toString():String
   member(x:E):Boolean
   minimum():E
   maximum():E
   deleteMinimum():TreeSet[\E\]
   deleteMaximum():TreeSet[\E\]
   removeMinimum():(E, TreeSet[\E\])
   removeMaximum():(E, TreeSet[\E\])
   add(x:E):TreeSet[\E\]
   delete(x:E):TreeSet[\E\]
   union(t2:TreeSet[\E\]):TreeSet[\E\]
   intersection(t2:TreeSet[\E\]):TreeSet[\E\]
   difference(t2:TreeSet[\E\]):TreeSet[\E\]
end

singleton[\E\](x:E): TreeSet[\E\]
set[\E\](g: Generator[\E\]): TreeSet[\E\]
opr [\E\]{ es: E... }: TreeSet[\E\]

object Union[\E\]() extends Reduction[\TreeSet[\E\]\] end

object EmptySet[\E\]() extends TreeSet[\E\] end

object NodeSet[\E\](val:E,  left:TreeSet[\E\], right:TreeSet[\E\]) extends TreeSet[\E\] end

end
