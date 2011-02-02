(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

trait foo end
trait bar extends foo
  bar(): bar
  bar(x: foo): bar
  foo(): foo
end

object obj extends bar end

foo: ZZ32

baz: String
