(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api GrammarComposition

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar A extends { Expression }
      Expr |:=
          foo => <[ 1 ]>
        | bar => <[ 2 ]>
    end

    grammar B extends { Expression, A }
      Expr |:=
          baz => <[ (foo) + (bar) ]> (* implicit private import from A *)
    end

    grammar C extends { Expression, A }
      Expr |:=
          baz => <[ (foo) + (bar) ]> (* explicit private import from A *)
        | private Expr from A
    end

    grammar D extends { Expression, A }
      Expr |:=
          baz => <[ (foo) + (bar) ]> (* public import from A *)
        | Expr from A
    end

    (* The following grammars test uses of identifiers within
     * templates. Consequently, they don't have corresponding
     * .fss source files to test them.
     *)

    grammar UseC extends { Expression, C }
      Expr |:=
          quux => <[ fn(foo) => baz ]> (* illegal if foo were imported *)
        | private Expr from C
    end

    grammar UseD extends { Expression, D }
      Expr |:=
          quux => <[ fn(x) => ((foo) + (baz)) ]>
        | private Expr from D
    end

    grammar RefuseA extends { Expression, A }
      Expr |:=
          snark => <[ fn(foo) => foo ]> (* illegal if foo were imported *)
        | without Expr from A
    end

end
