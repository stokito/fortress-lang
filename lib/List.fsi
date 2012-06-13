api List

  trait View[\T\] comprises { Cons[\T\] , Nil[\T\] } end
  object Cons[\T\](hd: T, tl: List[\T\]) extends View[\T\] end
  object Nil[\T\] extends View[\T\] end

  trait List[\T\] 
    abstract cons(x: T, self) : List[\T\]
    abstract isEmpty(self): Boolean
    abstract head(self): T
    abstract tail(self): List[\T\]
    abstract view(self): View[\T\]
    fold[\U\](self,init: U, f: (T,U) -> U): U
    opr ::(x:T, self): List[\T\]
    abstract reverse(self): List[\T\]
    index(self,n: ZZ32): T
    opr +(self,l: List[\T\]): List[\T\] 
    opr |self|: ZZ32
    opr [n:ZZ32]: T
    drop(self,n: ZZ32): List[\T\]
  end
  

  opr <|[\T extends Object\]|>: List[\T\]
  opr <|[\T extends Object\]x1: T|>: List[\T\]
  opr <|[\T extends Object\]x1: T, x2: T|>: List[\T\]
  opr <|[\T extends Object\]x1: T, x2: T, x3: T|>: List[\T\]
  opr <|[\T extends Object\]x1: T, x2: T, x3: T, x4: T|>: List[\T\]


  emptyList[\T\](): List[\T\]

end