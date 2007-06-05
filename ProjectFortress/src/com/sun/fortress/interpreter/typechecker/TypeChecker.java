/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.typechecker;

import com.sun.fortress.interpreter.useful.PureList;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.nodes.*;


public class TypeChecker extends NodeVisitor<FType> {

    public static void check(CompilationUnit p) throws TypeError {
        p.accept(new TypeChecker());
    }

    private final PureList<String> e;

    public TypeChecker() {
        this.e = PureList.make();
    }

    public TypeChecker(PureList<String> _e) {
        this.e = _e;
    }
    
    public FType NI(com.sun.fortress.interpreter.useful.HasAt x, String s) {
        return BottomType.ONLY;
    }
    
    private TypeChecker extend(PureList<String> names) {
        return new TypeChecker(names.append(e));
    }
    

    public FType forComponent(Component c) {
        // Component(Span span, DottedId name, List<Import> imports, List<Export> exports,
        //           List<? extends DefOrDecl> defs)
        for (DefOrDecl d : c.getDefs()) d.accept(this);
        return BottomType.ONLY;
    }
    
    public FType forApi(Api a) {
        // Api(Span span, DottedId name, List<Import> imports, List<? extends DefOrDecl> defs)
        return BottomType.ONLY;
    }
    
    public FType forFnDecl(FnDecl d) {
        // FnDecl(Span span, List<Modifier> mods, FnName name, Option<List<StaticParam>> staticParams,
        //        List<param> params, Option<TypeRef> returnType, List<TypeRef> throwss,
        //        List<WhereClause> where, Contract contract, Expr body)
        d.getBody().accept(this);
        return BottomType.ONLY;
    }
    
    // Expressions:
    
    public FType forLocalVarDecl(LocalVarDecl d) {
        // LocalVarDecl(Span span, List<Expr> body, List<LValue> lhs, Option<Expr> rhs)
        PureList<String> newEnv = e;
        for (LValue l : d.getLhs()) {
            if (l instanceof LValueBind) newEnv = newEnv.cons(((LValueBind) l).getName().getName());
            else if (l instanceof Unpasting) return FTypeVoid.ONLY; // TODO: handle
        }
        TypeChecker newChecker = new TypeChecker(newEnv);
        
        FType result = FTypeVoid.ONLY;
        for (Expr e : d.getBody()) result = e.accept(newChecker);
        return result;
    }
    
    public FType forBlock(Block b) {
        // Block(Span span, List<Expr> exprs)
        if (b.getExprs().isEmpty()) throw new TypeError("Block is empty", b);
        FType result = FTypeVoid.ONLY;
        for (Expr e : b.getExprs()) result = e.accept(this);
        return result;
    }

    public FType forVarRefExpr(VarRefExpr v) {
        // VarRefExpr(Span span, Id var)
        String s = v.getVar().getName();
        if (! e.contains(s))
            throw new TypeError("Reference to undefined variable: " + s, v);
        return BottomType.ONLY;
    }
    
//    public FType forTightJuxt

}
