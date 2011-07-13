(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api FortressSyntax

  import FortressAst.{...}
  import List.{...}

(* Do we want separate the ablity to reference a nonterminal from the ability to
   extend the nonterminal? *)

  native grammar Compilation
    File : CompilationUnit
    CompilationUnit : CompilationUnit
    Component : Component
    Api : Api
    Exports : List[\APIName\]
    Imports : List[\Import\]
    Import : Import
  end


(* We should move all declarations together in the declaration native grammar *)
  native grammar Declaration
      Decls : List[\Decl\]
      Decl : List[\Decl\]
      AbsDecls : List[\Decl\]
      AbsDecl : Decl
  end

  native grammar TraitObject (* rename to TraitsAndObjectDecls *)
    TraitDecl : TraitDecl
    AbsTraitDecl : TraitDecl
    ObjectDecl : ObjectDecl
    AbsObjectDecl : ObjectDecl
  end

  native grammar Function
    FnDecl : Decl
    FnSig : FnDecl
    AbsFnDecl : FnDecl
  end

  native grammar Method
    MdDecl : FnDecl
    MdDef : FnDecl
    AbsMdDecl : FnDecl
  end


  (* We should collapse Expression, NoNewlineExpr, LocalVarFnDecl *)
  native grammar Expression
      Expr : Expr
  end

  native grammar NoNewlineExpr
      Expr : Expr
  end

  native grammar LocalVarFnDecl
    LocalVarFnDecl : LetExpr
    LocalFnDecl : FnDecl
    LocalVarDecl : LocalVarDecl
  end

  native grammar Variable
    VarDecl : VarDecl
    AbsVarDecl : VarDecl
  end

  native grammar AbsField
    AbsFldDecl : VarDecl
  end

  native grammar Field
    FldDecl : VarDecl
  end

  native grammar OtherDecl
    DimUnitDecl : List[\DimUnitDecl\]
    TypeAlias : TypeAlias
    TestDecl : TestDecl
    PropertyDecl : PropertyDecl
  end

(*   native grammar Spacing *)
(*     w : () *)
(*     wr : () *)
(*     nl : () *)
(*     br : () *)
(*   end *)

  native grammar Parameter
    ValParam : List[\Param\]
    AbsValParam : List[\Param\]
    Params : List[\Param\]
    AbsParams : List[\Param\]
  end

  native grammar Identifier
    Id : Id
    BindId : Id
    BindIdOrBindIdTuple : List[\Id\]
    APIName : APIName
    SimpleName : IdOrOpOrAnonymousName
    QualifiedName : Id
  end

  native grammar Symbol
    Encloser : Op
    LeftEncloser : Op
    RightEncloser : Op
    ExponentOp : Op
    EncloserPair : Op
    Op : Op
    CompoundOp : Op
    Accumulator : Op
  end

  native grammar Type
    Type : BaseType
    TupleType : TupleType
    TypeRef : Type
    VoidType : Type
    ParenthesizedType : Type
  end

  native grammar Unicode
    UnicodeIdStart : String
    UnicodeIdRest : String
  end

  native grammar Literal
      LiteralExpr : Expr
  end
end
