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

api Syntax
import List.{...}

(* Initialize the abstract syntax id to 0. *)
initSyntax(): ()

(* abstract syntax tree *)
trait Ast
  getId(): ZZ32
  toString(): String
  toSource(): String
end

(* program *)
(* p  ::= fd... e *)
object Program(decls: List[\FnDecl\], expr: Expr) extends Ast end

(* function declaration *)
(* fd ::= f(x) = e *)
object FnDecl(name: String, param: String, body: Expr) extends Ast end

(* expressions *)
(* e ::= x
       | e e
       | if e then e else e end
       | v
 *)
trait Expr extends Ast comprises { Value, Var, App, If } end

object Var(name: String) extends Expr end

object App(function: Expr, argument: Expr) extends Expr end

object If(cond: Expr, thenB: Expr, elseB: Expr) extends Expr end

(* values *)
(* v ::= fn x:t => e
       | true
       | false
 *)
trait Value extends Expr comprises { FnExpr, True, False } end

object FnExpr(param: String, body: Expr) extends Value end

object True extends Value end

object False extends Value end

end
