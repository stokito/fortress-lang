(*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
