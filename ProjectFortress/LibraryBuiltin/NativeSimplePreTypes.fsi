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

api NativeSimplePreTypes

trait PreBoolean
        comprises { Boolean }
    getter toString(): Boolean
    generate[\R\](r:Reduction[\R\],b:()->R): R
    map[\G\](f: ()->G): Maybe[\G\]
    cross[\G\](g: Generator[\G\]): Generator[\((),G)\]
    mapReduce[\R\](b: ()->R, _:(R,R)->R, z:R): R
    loop(f:()->()): ()

    opr =(self, other:Boolean): Boolean
    opr <(self, other:Boolean): Boolean
    opr CMP(self, other:Boolean): Boolean
end

end
