(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api simpleNameTest

import List.{...} except { emptyList, singleton }
import Set.{ opr { } }
import AliasTest.{ opr OPLUS => MYPLUS }
import api Map
import api { File, FileSupport }

trait T end

end
