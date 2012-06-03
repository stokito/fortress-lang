(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(* Helper library to create fortress ast nodes. Somewhat like nodes_util.NodeFactory *)
api FortressAstUtil

import List.{...}
import FortressAst.{...}

LooseJuxt1(exprs:List[\Expr\]):Juxt
TightJuxt1(exprs:List[\Expr\]):Juxt

APIName1(in_ids:List[\Id\]):APIName

end
