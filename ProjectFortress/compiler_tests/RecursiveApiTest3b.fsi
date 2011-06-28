(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api RecursiveApiTest3b
import RecursiveApiTest3a.{ MaybeFoo, Foo, Zilch }


trait MaybeBar comprises { Bar, Nada } end

object Bar(x: MaybeFoo) extends MaybeBar
  peek(): MaybeFoo
end

object Nada extends MaybeBar end

end
