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

api ChurchBooleans

  import FortressAst.{...}
  import FortressSyntax.{...}

  trait Boolean comprises {true, false}
    apply[\T\](x: T, y: T):T
  end
  object true extends Boolean end
  object false extends Boolean end


  grammar IfThenElse extends { Expression, Literal } 
    Expr[\T\]:T |Expr:=
      a1:if e1:Expr:Boolean then e2:Expr:T else e3:Expr:T end
      <[ e1.apply(e2, e3) ]>
      (* expands to  <[ (e1 as Boolean).apply(e2, e3) ]> *)

    LiteralExpr[\T\] |Expr:=
      true <[ true ]>
    | false <[ false ]>

  end

end

