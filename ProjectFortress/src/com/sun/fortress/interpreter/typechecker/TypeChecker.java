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

import com.sun.fortress.interpreter.useful.Option;
import java.util.List;
import java.util.LinkedList;

import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.PureList;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.nodes.*;


public final class TypeChecker extends NodeVisitor<TypeCheckerResult> {
    public final Types typeAnalyzer = Types.ONLY;

    public static TypeCheckerResult check(CompilationUnit p) {
        return p.accept(new TypeChecker());
    }

    private final PureList<String> e;

    public TypeChecker() {
        this.e = PureList.make();
    }

    public TypeChecker(PureList<String> _e) {
        this.e = _e;
    }
    
    private List<TypeCheckerResult> checkList(List<? extends NodeVisitorHost> hosts) {
        List<TypeCheckerResult> result = new LinkedList<TypeCheckerResult>();
        for (NodeVisitorHost h : hosts) result.add(h.accept(this));
        return result;
    }
    
    public TypeCheckerResult NI(com.sun.fortress.interpreter.useful.HasAt x, String s) {
        return TypeCheckerResult.VALID;
    }
    
    private TypeChecker extend(PureList<String> names) {
        return new TypeChecker(names.append(e));
    }
    

    public TypeCheckerResult forComponent(Component c) {
        // Component(Span span, DottedId name, List<Import> imports, List<Export> exports,
        //           List<? extends DefOrDecl> defs)
        TypeCheckerResult result = TypeCheckerResult.VALID; // Components are innocent until proven guilty.       
                
        PureList<String> topLevelNames = PureList.make();
        
        if (c.getImports().isEmpty()) { // TODO: handle imports
            for (DefOrDecl d : c.getDefs()) { 
                if (d instanceof ObjectDefOrDecl) {
                    ObjectDefOrDecl _d = (ObjectDefOrDecl)d;
                    topLevelNames = topLevelNames.cons(_d.getName().getName());
                }
                else if (d instanceof FnDefOrDecl) {
                    FnDefOrDecl _d = (FnDefOrDecl)d;
                    FnName fnName = _d.getFnName();
                 
                    // There are many types of fnNames, such as OprName.
                    // But only names of functions are placed in the top-level scope of variables.
                    if (fnName instanceof Fun) { 
                        Fun fun = (Fun)fnName;
                        topLevelNames = topLevelNames.cons(fun.getName().getName());  
                    }
                }
                else if (d instanceof VarDefOrDecl) {
                    VarDefOrDecl _d = (VarDefOrDecl)d;
                    
                    for (String name : _d.stringNames()) {
                        topLevelNames = topLevelNames.cons(name);
                    }
                }
                else  if (d instanceof TraitDefOrDecl || // TODO: implement these things
                          d instanceof PropertyDecl ||
                          d instanceof TestDecl) {
                    return TypeCheckerResult.VALID;
                }
                else { 
                    throw new RuntimeException("Missed case of top-level definition!");
                }
            }
            TypeChecker typeChecker = extend(topLevelNames);
            for (DefOrDecl d : c.getDefs()) { result = result.combine(d.accept(typeChecker)); }
        }
        return result;   
    }
    
    public TypeCheckerResult forApi(Api a) {
        // Api(Span span, DottedId name, List<Import> imports, List<? extends DefOrDecl> defs)
        return TypeCheckerResult.VALID;
    }
    
    public TypeCheckerResult forFnDecl(FnDecl d) {
        // FnDecl(Span span, List<Modifier> mods, FnName name, Option<List<StaticParam>> staticParams,
        //        List<Param> params, Option<TypeRef> returnType, List<TypeRef> throwss,
        //        List<WhereClause> where, Contract contract, Expr body)
        if (!d.getStaticParams().isPresent()) { // TODO: static param bindings
            PureList<String> newEnv = e;
            for (Param p : d.getParams()) {
                newEnv = newEnv.cons(p.getName().getName());
            }
            return d.getBody().accept(new TypeChecker(newEnv)); // TODO: Check result of body
        }
        return TypeCheckerResult.VALID;
    }
    
    /******** Expressions: **********/
    
    public TypeCheckerResult forLocalVarDecl(LocalVarDecl d) {
        // LocalVarDecl(Span span, List<Expr> body, List<LValue> lhs, Option<Expr> rhs)
        PureList<String> newEnv = e;
        for (LValue l : d.getLhs()) {
            if (l instanceof LValueBind) newEnv = newEnv.cons(((LValueBind) l).getName().getName());
            else if (l instanceof Unpasting) return TypeCheckerResult.VALID; // TODO: handle
        }
        TypeChecker newChecker = new TypeChecker(newEnv);
        
        TypeCheckerResult result = TypeCheckerResult.VALID;
        for (Expr e : d.getBody()) { result = result.combine(e.accept(newChecker)); }
        return result;
    }
    
    public TypeCheckerResult forBlock(Block b) {
        // Block(Span span, List<Expr> exprs)
        // TODO: make sure the spec allows an empty block
        TypeCheckerResult result = TypeCheckerResult.VALID;
        for (Expr e : b.getExprs()) { result = result.combine(e.accept(this)); }
        return result;
    }
    
    // TODO: eliminate this
    private static final java.util.Set<String> LIB_NAMES = new java.util.HashSet<String>();
    static {
        LIB_NAMES.add("true");
        LIB_NAMES.add("false");
    }        

    public TypeCheckerResult forVarRefExpr(VarRefExpr v) {
        // VarRefExpr(Span span, Id var)
        String s = v.getVar().getName();
        if ((!e.contains(s)) && (!LIB_NAMES.contains(s))) {
            return new TypeCheckerResult(new TypeError("Reference to undefined variable " + s, v));
        }
        return TypeCheckerResult.VALID;
    }
    
    public TypeCheckerResult forAssignment(Assignment a) {
        List<TypeCheckerResult> left = checkList(a.getLhs());
        TypeCheckerResult right = a.getRhs().accept(this);
        // TODO: check for coercions
        //if (!typeAnalyzer.isSubtype(right, left))
        //    return new TypeCheckerResult(new TypeError("Type " + left + " is not assignable to type " + right));
        return right.combine(TypeCheckerResult.combine(PureList.fromJavaList(left))); 
    }
    
    public TypeCheckerResult forSubscriptExpr(SubscriptExpr s) {
        TypeCheckerResult obj = s.getObj().accept(this);
        List<TypeCheckerResult> subs = checkList(s.getSubs());
        // TODO: application
        return obj.combine(TypeCheckerResult.combine(PureList.fromJavaList(subs))); 
    }
    
    public TypeCheckerResult forAtomicExpr(AtomicExpr ae) {
        return ae.getExpr().accept(this);
    }
    
    public TypeCheckerResult forIf(If e) {
        TypeCheckerResult result = TypeCheckerResult.VALID;
        
        // Collect all clause type results in a list so we can return the union
        // of all clause types as the type of this If.
        List<TypeCheckerResult> clauseTypes = new LinkedList<TypeCheckerResult>();
        
        for (IfClause clause : e.getClauses()) {
            TypeCheckerResult testType = clause.getTest().accept(this);
            TypeCheckerResult bodyType = clause.getBody().accept(this);
            
//            if (! typeAnalyzer.isSubtype(testType, typeAnalyzer.BOOLEAN)) { 
//                return new TypeCheckerResult(new TypeError("Test in if clause is not a Boolean", clause.getTest()); 
//            }
            clauseTypes.add(bodyType);
            result = result.combine(testType.combine(bodyType));
        }
        Option<Expr> else_ = e.getElse_();
        if (else_.isPresent()) {
            TypeCheckerResult elseResult = else_.getVal().accept(this);
            clauseTypes.add(elseResult);
            result = result.combine(elseResult);
        }
        return result;
//      return typeAnalyzer.union(clauseTypes); // TODO: Compute return type (we need a test for this).
    }
    
    public TypeCheckerResult forLabel(Label e) {
        // TODO: Add e.getName() to environment for use by exit
        // (but don't do this unti we're testing for it!)
        return e.getBody().accept(this);
    }
    
    public TypeCheckerResult forFor(For e) {
        TypeCheckerResult genResults = TypeCheckerResult.VALID;
        PureList<String> newVars = PureList.make();
        
        for (Generator gen : e.getGens()) {
            for (Id id : gen.getBind()) {
                newVars = newVars.cons(id.getName());
            }
            genResults = genResults.combine(gen.accept(this));
        }
        return genResults.combine(e.getBody().accept(new TypeChecker(this.e.append(newVars))));
    }
//    public TypeCheckerResult forTightJuxt
                                  
}
