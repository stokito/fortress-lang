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

api Set
import CovariantCollection.{...}

(**
 * Sets represented using a tree structure.  The underlying type E
 * must support comparison using < and =.  When generated these sets
 * produce their elements in sorted order.
 **)
trait Set[\E\] extends Indexed[\E,ZZ32\] comprises {NodeSet[\E\], EmptySet[\E\]}
   printTree():()
   toString():String
   minimum():E
   maximum():E
   deleteMinimum():Set[\E\]
   deleteMaximum():Set[\E\]
   removeMinimum():(E, Set[\E\])
   removeMaximum():(E, Set[\E\])
   add(x:E):Set[\E\]
   delete(x:E):Set[\E\]
   opr UNION(self,t2:Set[\E\]):Set[\E\]
   opr INTERSECTION(self,t2:Set[\E\]):Set[\E\]
   opr DIFFERENCE(self,t2:Set[\E\]):Set[\E\]
   splitAt(e:E):(Set[\E\],Boolean,Set[\E\])
end

singleton[\E\](x:E): Set[\E\]
set[\E\](g: Generator[\E\]): Set[\E\]
opr [\E\]{ es: E... }: Set[\E\]
opr BIG [\T,U\]{ g: ( Reduction[\SomeCovariantCollection\],
                      T -> SomeCovariantCollection) ->
                    SomeCovariantCollection } : Set[\U\]

object Union[\E\]() extends Reduction[\Set[\E\]\] end

object NodeSet[\E\](val:E, left:Set[\E\], right:Set[\E\]) extends Set[\E\] end
object EmptySet[\E\]() extends Set[\E\] end

end
