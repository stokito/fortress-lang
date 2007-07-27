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

import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Assignment;
import com.sun.fortress.nodes.AtomicExpr;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.DottedId;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.Generator;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.PropertyDecl;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.TestDecl;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.Unpasting;
import com.sun.fortress.nodes.VarAbsDeclOrDecl;
import com.sun.fortress.nodes.VarRef;
import java.util.List;
import java.util.LinkedList;

import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.PureList;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.nodes_util.NodeUtil;


public final class TypeChecker extends NodeAbstractVisitor<TypeCheckerResult> {
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

    private List<TypeCheckerResult> checkList(List<? extends Node> hosts) {
        List<TypeCheckerResult> result = new LinkedList<TypeCheckerResult>();
        for (Node h : hosts) result.add(h.accept(this));
        return result;
    }

    public TypeCheckerResult defaultCase(Node x) {
        return TypeCheckerResult.VALID;
    }

    private TypeChecker extend(PureList<String> names) {
        return new TypeChecker(names.append(e));
    }


    public TypeCheckerResult forComponent(Component c) {
        // Component(Span span, DottedId name, List<Import> imports, List<Export> exports,
        //           List<? extends AbsDeclOrDecl> defs)
        TypeCheckerResult result = TypeCheckerResult.VALID; // Components are innocent until proven guilty.

        PureList<String> topLevelNames = PureList.make();

        if (c.getImports().isEmpty()) { // TODO: handle imports
            for (AbsDeclOrDecl d : c.getDecls()) {
                if (d instanceof ObjectAbsDeclOrDecl) {
                    ObjectAbsDeclOrDecl _d = (ObjectAbsDeclOrDecl)d;
                    topLevelNames = topLevelNames.cons(_d.getId().getName());
                }
                else if (d instanceof FnAbsDeclOrDecl) {
                    FnAbsDeclOrDecl _d = (FnAbsDeclOrDecl)d;
                    FnName fnName = _d.getFnName();

                    // There are many types of fnNames, such as OprName.
                    // But only names of functions are placed in the top-level scope of variables.
                    if (fnName instanceof DottedId) {
                        DottedId fun = (DottedId)fnName;
                        topLevelNames = topLevelNames.cons(NodeUtil.getName(fun));
                    }
                }
                else if (d instanceof VarAbsDeclOrDecl) {
                    VarAbsDeclOrDecl _d = (VarAbsDeclOrDecl)d;

                    for (String name : NodeUtil.stringNames(_d)) {
                        topLevelNames = topLevelNames.cons(name);
                    }
                }
                else  if (d instanceof TraitAbsDeclOrDecl || // TODO: implement these things
                          d instanceof PropertyDecl ||
                          d instanceof TestDecl) {
                    return TypeCheckerResult.VALID;
                }
                else {
                    throw new RuntimeException("Missed case of top-level definition!");
                }
            }
            TypeChecker typeChecker = extend(topLevelNames);
            for (AbsDeclOrDecl d : c.getDecls()) { result = result.combine(d.accept(typeChecker)); }
        }
        return result;
    }

    public TypeCheckerResult forApi(Api a) {
        // Api(Span span, DottedId name, List<Import> imports, List<? extends AbsDeclOrDecl> defs)
        return TypeCheckerResult.VALID;
    }

    public TypeCheckerResult forFnDecl(FnDecl d) {
        // FnDecl(Span span, List<Modifier> mods, FnName name, Option<List<StaticParam>> staticParams,
        //        List<Param> params, Option<TypeRef> returnType, List<TypeRef> throwss,
        //        List<WhereClause> where, Contract contract, Expr body)
        if (!d.getStaticParams().isPresent()) { // TODO: static param bindings
            PureList<String> newEnv = e;
            for (Param p : d.getParams()) {
                newEnv = newEnv.cons(p.getId().getName());
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
            if (l instanceof LValueBind) newEnv = newEnv.cons(((LValueBind) l).getId().getName());
            else if (l instanceof Unpasting) return TypeCheckerResult.VALID; // TODO: handle
        }
        TypeChecker newChecker = new TypeChecker(newEnv);

        TypeCheckerResult result = TypeCheckerResult.VALID;
        for (Expr e : d.getBody()) { result = result.combine(e.accept(newChecker)); }
        return result;
    }

    public TypeCheckerResult forDo(Do b) {
        // Do(Span span, List<DoFront> fronts)
        TypeCheckerResult result = TypeCheckerResult.VALID;
        for (DoFront d : b.getFronts()) { result = result.combine(d.accept(this)); }
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

    public TypeCheckerResult forVarRef(VarRef v) {
        // VarRef(Span span, Id var)
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
        Option<Block> else_ = e.getElseClause();
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

    public TypeCheckerResult forDoFront(DoFront e) {
        // TODO: Option<Expr> _at
        // TODO: boolean _atomic
        return e.getExpr().accept(this);
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
