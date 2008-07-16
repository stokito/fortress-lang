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

api FortressSyntax

  import FortressAst.{...}
  import List.{...}

(* Do we want separate the ablity to reference a nonterminal from the ability to 
   extend the nonterminal? *)

  grammar Compilation
    File : CompilationUnit
    CompilationUnit : CompilationUnit
    Component : Component
    Api : Api
    Exports : List[\Export\]
    Export : Export
    Imports : List[\Import\]
    Import : Import
  end


(* We should move all declarations together in the declaration grammar *)
  grammar Declaration 
      Decls : List[\Decl\]
      Decl : List[\Decl\]
      AbsDecls : List[\AbsDecl\]
      AbsDecl : AbsDecl
  end

  grammar TraitObject (* rename to TraitsAndObjectDecls *)
    TraitDecl : TraitDecl
    AbsTraitDecl : AbsTraitDecl
    ObjectDecl : ObjectDecl
    AbsObjectDecl : AbsObjectDecl
  end

  grammar Function
    FnDecl : Decl
    FnSig : AbsFnDecl
    AbsFnDecl : AbsFnDecl
  end

  grammar Method
    MdDecl : FnAbsDeclOrDecl
    MdDef : FnDecl
    AbsMdDecl : AbsFnDecl
  end


  (* We should collapse Expression, NoNewlineExpr, LocalVarFnDecl *)  
  grammar Expression 
      Expr : Expr
  end

  grammar NoNewlineExpr
      Expr : Expr
  end

  grammar LocalVarFnDecl
    LocalVarFnDecl : LetExpr
    LocalFnDecl : FnDef
    LocalVarDecl : LocalVarDecl
  end

  grammar Variable
    VarDecl : VarDecl
    AbsVarDecl : AbsVarDecl
  end

  grammar AbsField
    AbsFldDecl : AbsVarDecl
  end
 
  grammar Field
    FldDecl : VarDecl
  end

  grammar OtherDecl
    DimUnitDecl : List[\DimUnitDecl\]
    TypeAlias : TypeAlias
    TestDecl : TestDecl
    PropertyDecl : PropertyDecl
  end

(*   grammar Spacing *)
(*     w : () *)
(*     wr : () *)
(*     nl : () *)
(*     br : () *)
(*   end *)

  grammar Parameter
    ValParam : List[\Param\]
    AbsValParam : List[\Param\]
    Params : List[\Param\]
    AbsParams : List[\Param\]
  end

  grammar Identifier 
    Id : Id
    BindId : Id
    BindIdOrBindIdTuple : List[\Id\]
    APIName : APIName
    SimpleName : IdOrOpOrAnonymousName
    QualifiedName : Id
  end

  grammar Symbol
    Encloser : Op
    LeftEncloser : Op
    RightEncloser : Op
    ExponentOp : Op
    EncloserPair : Enclosing
    Op : Op
    CompoundOp : Op
    Accumulator : Op
  end

  grammar Syntax
    GrammarDef : GrammarDecl
    NonterminalDef : GrammarMemberDecl
    SyntaxDef : SyntaxDef
    SyntaxDefOr : SyntaxDef
    TransformationExpression : TransformerExpressionDef
    TransformationTemplate : PreTransformerDef
  end

  grammar Type
    Type : BaseType
    TupleType : TupleType
    TypeRef : Type
    VoidType : Type
    ParenthesizedType : Type
  end

  grammar Unicode
    UnicodeIdStart : String
    UnicodeIdRest : String    
  end

  grammar Literal 
      LiteralExpr : Expr
  end
end
