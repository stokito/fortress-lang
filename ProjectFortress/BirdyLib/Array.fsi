api Array

  import Util.{...}

  trait Array[\T\] extends Generator[\T\] 
    init(i: ZZ32, v: T): ()
    opr [GeneratorZZ32]: Array[\T\]
    loop(f:T->()): () 
  end
  
  array[\T\](s: ZZ32): Array[\T\] 

end