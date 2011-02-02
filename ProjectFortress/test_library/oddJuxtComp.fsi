(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api oddJuxtComp

trait T excludes {HasRank, String} end

opr juxtaposition[\L extends T, R extends T\](l: L, r: R): ()

end
