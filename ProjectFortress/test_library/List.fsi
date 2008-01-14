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

api List

(** DEPRACATED.  Please use PureList or ArrayList instead.
    Those offer a Generator implementation and indexability.
    If you are using union/intersection/difference use Set instead.
 **)

trait List[\alpha\] comprises {Cons[\alpha\], Empty[\alpha\]}
   length(): ZZ32
   cons(x: alpha): List[\alpha\]
   append(xs: List[\alpha\]): List[\alpha\]
   append(n: ZZ32, xs: List[\alpha\]): List[\alpha\]
   contains(x: alpha): Boolean
   containsAll(xs: List[\alpha\]): Boolean
   isEmpty(): Boolean
   filter(p: alpha -> Boolean): List[\alpha\]
   map[\beta\](f: alpha -> beta): List[\beta\]
   process(proc: alpha -> ()): ()
   foldL[\beta\](x: beta, f:(beta, alpha) -> beta): beta
   foldR[\beta\](f:(alpha, beta) -> beta, x:beta): beta
   remove(x: alpha): List[\alpha\]
   union(xs: List[\alpha\]): List[\alpha\]
   intersection(xs: List[\alpha\]): List[\alpha\]
   difference(xs: List[\alpha\]): List[\alpha\]
   zip[\beta, gamma\](f: (alpha, beta) -> gamma, xs: List[\beta\]): List[\gamma\]
   forAll(p: alpha -> Boolean): Boolean
   exists(p: alpha -> Boolean): Boolean
   subList(m: ZZ32, n: ZZ32): List[\alpha\]
   reverse(): List[\alpha\]
   asArray(): Array[\alpha\]
end

object Cons[\alpha\](hd':alpha, tl':List[\alpha\]) extends List[\alpha\]
   hd(): alpha
   tl(): List[\alpha\]
   eltAt(n: ZZ32): alpha
   indexOf(x: alpha): ZZ32
   lastIndexOf(x: alpha): ZZ32
   removeAt(n: ZZ32): List[\alpha\]
   updateHd(x: alpha): List[\alpha\]
   updateTl(xs: List[\alpha\]): List[\alpha\]
end
object Empty[\alpha\]() extends List[\alpha\] end

concat[\alpha\](xs: List[\List[\alpha\]\]): List[\alpha\]

end
