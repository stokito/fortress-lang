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

import java.util.List;
import java.util.LinkedList;
import com.sun.fortress.interpreter.useful.PureList;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.nodes.*;


public class TypeChecker extends NodeVisitor<FType> {
    public final Types typeAnalyzer = Types.ONLY;

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
    
    private List<FType> checkList(List<? extends NodeVisitorHost> hosts) {
        List<FType> result = new LinkedList<FType>();
        for (NodeVisitorHost h : hosts) result.add(h.accept(this));
        return result;
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
        if (c.getImports().isEmpty()) { // TODO: handle imports
            for (DefOrDecl d : c.getDefs()) { // TODO: implement these things
                if (d instanceof ObjectDefOrDecl || d instanceof TraitDefOrDecl ||
                    d instanceof VarDefOrDecl || d instanceof PropertyDecl ||
                    d instanceof TestDecl) {
                    return BottomType.ONLY;
                }
            }
            for (DefOrDecl d : c.getDefs()) d.accept(this);
        }
        return BottomType.ONLY;
    }
    
    public FType forApi(Api a) {
        // Api(Span span, DottedId name, List<Import> imports, List<? extends DefOrDecl> defs)
        return BottomType.ONLY;
    }
    
    public FType forFnDecl(FnDecl d) {
        // FnDecl(Span span, List<Modifier> mods, FnName name, Option<List<StaticParam>> staticParams,
        //        List<Param> params, Option<TypeRef> returnType, List<TypeRef> throwss,
        //        List<WhereClause> where, Contract contract, Expr body)
        if (!d.getStaticParams().isPresent()) { // TODO: static param bindings
            PureList<String> newEnv = e;
            for (Param p : d.getParams()) {
                newEnv = newEnv.cons(p.getName().getName());
            }
            d.getBody().accept(new TypeChecker(newEnv));
        }
        return BottomType.ONLY;
    }
    
    /******** Expressions: **********/
    
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
        // TODO: make sure the spec allows an empty block
        FType result = FTypeVoid.ONLY;
        for (Expr e : b.getExprs()) result = e.accept(this);
        return result;
    }
    
    // TODO: eliminate this
    private static final java.util.Set<String> LIB_NAMES = new java.util.HashSet<String>();
    static {
        LIB_NAMES.add("true");
        LIB_NAMES.add("false");
    }        

    public FType forVarRefExpr(VarRefExpr v) {
        // VarRefExpr(Span span, Id var)
        String s = v.getVar().getName();
        if (! e.contains(s) && !LIB_NAMES.contains(s)) {
            throw new TypeError("Reference to undefined variable: " + s, v);
        }
        return BottomType.ONLY;
    }
    
    public FType forAssignment(Assignment a) {
        List<FType> left = checkList(a.getLhs());
        FType right = a.getRhs().accept(this);
        // TODO: check for coercions
        //if (!typeAnalyzer.isSubtype(right, left))
        //    throw new TypeError("Type " + left + " is not assignable to type " + right);
        return FTypeVoid.ONLY;
    }
    
    public FType forSubscriptExpr(SubscriptExpr s) {
        FType obj = s.getObj().accept(this);
        List<FType> subs = checkList(s.getSubs());
        // TODO: application
        return BottomType.ONLY;
    }
    
    public FType forAtomicExpr(AtomicExpr ae) {
        return ae.getExpr().accept(this);
    }
    
    public FType forIf(If e) {
        List<FType> clauseTypes = new LinkedList<FType>();
        
        for (IfClause clause : e.getClauses()) {
            FType testType = clause.getTest().accept(this);
            FType bodyType = clause.getBody().accept(this);
            
            if (! typeAnalyzer.isSubtype(testType, typeAnalyzer.BOOLEAN)) { 
                throw new TypeError("Test in if clause is not a Boolean", clause.getTest()); 
            }
            clauseTypes.add(bodyType);
        }
        Option<Expr> else_ = e.getElse_();
        if (else_.isPresent()) {
            clauseTypes.add(else_.getVal().accept(this));
        }
        return typeAnalyzer.union(clauseTypes);
    }
    
    public FType forLabel(Label e) {
        // TODO: Add e.getName() to environment for use by exit
        // (but don't do this unti we're testing for it!)
        return e.getBody().accept(this);
    }
//    public FType forTightJuxt
                                  
}
