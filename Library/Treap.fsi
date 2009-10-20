api Treap

trait Treap comprises {...}
    getter isEmpty(): Boolean
    getter asString(): String
    getter asDebugString(): String
    getter min(): Treap
    getter max(): Treap
    opr CUP(self, other: Treap): Treap
    opr CAP(self, other: Treap): Treap
    opr DIFFERENCE(self, other: Treap): Treap
    opr SYMDIFF(self, other: Treap): Treap
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