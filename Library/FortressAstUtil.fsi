(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

(* Helper library to create fortress ast nodes. Somewhat like nodes_util.NodeFactory *)
api FortressAstUtil

import List.{...}
import FortressAst.{...}

LooseJuxt1(exprs:List[\Expr\]):LooseJuxt
TightJuxt1(exprs:List[\Expr\]):TightJuxt

APIName1(in_ids:List[\Id\]):APIName

end
