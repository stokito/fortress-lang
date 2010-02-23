(*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

api Treap

trait Treap comprises {...}
    getter isEmpty(): Boolean
    getter asString(): String
    getter asDebugString(): String
    getter min(): Treap
    getter max(): Treap
    opr UNION(self, other: Treap): Treap
    opr INTERSECTION(self, other: Treap): Treap
    opr DIFFERENCE(self, other: Treap): Treap
    opr SYMDIFF(self, other: Treap): Treap
    containsKey(key:ZZ32): Boolean
    replace(key: ZZ32, val: String): Treap
    add(key: ZZ32, val: String): Treap
    remove(key: ZZ32): Treap
    lookup(key: ZZ32, defaultValue: String): String
    rootKey(defaultKey: ZZ32): ZZ32
    rootValue(defaultValue: String): String
end

singleton(key: ZZ32, val: String): Treap

empty: Treap

end