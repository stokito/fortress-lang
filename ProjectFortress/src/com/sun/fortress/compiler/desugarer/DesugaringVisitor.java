/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

import com.sun.fortress.scala_src.typechecker.STypeChecker;
import com.sun.fortress.scala_src.typechecker.STypeCheckerFactory;

/* Getter/setter desugaring
 */
/* For each trait declaration,
 * 1) extends the scope of an existing DesugaringVisitor
 *    with abstract fields declared in the trait;
 * 2) removes abstract field declarations from the trait;
 * 3) makes getter/setter declarations for the removed abstract fields;
 * 4) adds the generated getter/setter declarations to the trait.
 */
/* For each object declaration,
 * 1) extends the scope of an existing DesugaringVisitor
 *    with fields and parameters declared in the object;
 * 2) mangles parameter and field names;
 * 3) makes getter/setter declarations for the removed abstract fields;
 * 4) adds the generated getter/setter declarations to the trait.
 */
/* For each object expression,
 * 1) extends the scope of an existing DesugaringVisitor
 *    with fields declared in the object;
 * 2) mangles field names;
 * 3) makes getter/setter declarations for the removed abstract fields;
 * 4) adds the generated getter/setter declarations to the trait.
 */
/* For each explicit getter/setter declaration,
 * remove the getter/setter modifier.
 * After this desugaring, the getter/setter modifiers are eliminated.
 */
/* For each reference of a field,
 * change the reference to a mangled name.
 */
public class DesugaringVisitor extends NodeUpdateVisitor {
    private List<Id> fieldsInScope;
    private Option<Map<Pair<Id,Id>,FieldRef>> boxedRefMap =
        Option.<Map<Pair<Id,Id>,FieldRef>>none();
     
    public DesugaringVisitor( Option<Map<Pair<Id,Id>,FieldRef>> boxedRefMap ) {
        fieldsInScope = new ArrayList<Id>();
        this.boxedRefMap = boxedRefMap;
    }
    
    public DesugaringVisitor( List<Id> _fieldsInScope,
                              Option<Map<Pair<Id,Id>,FieldRef>> boxedRefMap ) {
        fieldsInScope = _fieldsInScope;
        this.boxedRefMap = boxedRefMap;
    }

    private boolean hidden(Binding field) {
        return field.getMods().isHidden();
    }

    private boolean settable(Binding field) {
        return field.getMods().isSettable();
    }

    private boolean mutable(Binding field) {
        if ( field instanceof LValue )
            return ((LValue)field).isMutable();
        return field.getMods().isMutable();
    }

    /**
     * Takes an Id and a list of declarations and determines
     * whether the list of declarations contains an explicit getter with a name equal
     * to the given Id.
     */
    private boolean hasExplicitGetter(Id name, List<Decl> decls) {
        for (Decl decl: decls) {
            if (decl instanceof FnDecl) {
                FnDecl _decl = (FnDecl) decl;
                if (NodeUtil.getName(_decl).equals(name) && NodeUtil.isGetter(_decl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Takes an Id and a list of declarations and determines
     * whether the list of declarations contains an explicit setter with a name equal
     * to the given Id.
     */
    private boolean hasExplicitSetter(Id name, List<Decl> decls) {
        for (Decl decl: decls) {
            if (decl instanceof FnDecl) {
                FnDecl _decl = (FnDecl) decl;
                if (NodeUtil.getName(_decl).equals(name) && NodeUtil.isSetter(_decl)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Modifiers removeGetterSetterMod(Modifiers mods) {
        return mods.remove(Modifiers.GetterSetter);
    }

    private static final Id mangleName(Id fieldName) {
        return NodeFactory.makeId(fieldName, "$" + fieldName.getText());
    }

    private static Option<List<Param>> mangleParams(Option<List<Param>> params) {
        return new NodeUpdateVisitor() {
            public Node forParam(Param that) {
                if ( ! NodeUtil.isVarargsParam(that) )
                    return NodeFactory.makeParam(that, mangleName(that.getName()));
                else
                    return that;
            }
        }.recurOnOptionOfListOfParam(params);
    }

    /* Extends the scope of a DesugaringVisitor
       with fields including object parameters, if any.
     */
    private DesugaringVisitor extend(Option<List<Param>> params, List<Decl> decls) {
        List<Id> newScope = new ArrayList<Id>();

        if (params.isSome()) {
            for (Param param: params.unwrap()) {
                if ( ! NodeUtil.isVarargsParam(param) )
                    newScope.add(param.getName());
            }
        }
        for (Decl decl: decls) {
            if (decl instanceof VarDecl) {
                for (LValue binding : (((VarDecl)decl).getLhs())) {
                    newScope.add(binding.getName());
                }
            }
        }
        newScope.addAll(fieldsInScope);
        return new DesugaringVisitor( newScope, boxedRefMap );
    }

    /* Extends the scope of a DesugaringVisitor
       with fields (for object expressions)
       and abstract fields (for trait declarations).
     */
    private DesugaringVisitor extend(List<Decl> decls) {
        List<Id> newScope = new ArrayList<Id>();

        for (Decl decl: decls) {
            if (decl instanceof VarDecl) {
                for (LValue binding : (((VarDecl)decl).getLhs())) {
                    newScope.add(binding.getName());
                }
            }
        }
        newScope.addAll(fieldsInScope);
        return new DesugaringVisitor( newScope, boxedRefMap );
    }

    private List<Decl> removeVarDecls(List<Decl> decls) {
        // System.err.println("decls.size() = " + decls.size());
        final List<Decl> result = new ArrayList<Decl>();

        for (Decl decl : decls) {
            if (decl instanceof VarDecl) {
                // skip it
            } else {
                result.add(decl);
            }
        }
        // System.err.println("result.size() = " + result.size());
        return result;
    }

    private static final Modifiers getterNonMods =
        Modifiers.combine(Modifiers.Settable,Modifiers.Var,Modifiers.Wrapped);

    private FnDecl makeGetter(boolean inTrait, Id owner, Binding field) {
        Modifiers mods = field.getMods().remove(getterNonMods);
        Id name = field.getName();
        Span span = NodeUtil.getSpan(field);
        Expr body;
        if ( boxedRefMap.isSome() ) {
            Map<Pair<Id,Id>,FieldRef> map = boxedRefMap.unwrap();
            Pair<Id,Id> keyPair = new Pair( owner, name );
            if ( map.containsKey( keyPair ) ) {
                FieldRef fieldRef = map.get( keyPair );
                FieldRef rewrite = ExprFactory.makeFieldRef(fieldRef, span);
                assert( rewrite.getObj() instanceof VarRef );
                VarRef obj = (VarRef)rewrite.getObj();
                obj = ExprFactory.makeVarRef(obj, NodeUtil.getExprType(obj),
                                             mangleName(obj.getVarId()));
                ExprInfo info = NodeFactory.makeExprInfo(span,
                                                         NodeUtil.isParenthesized(rewrite),
                                                         NodeUtil.optTypeOrPatternToType(field.getIdType()));
                body = (Expr)forFieldRefOnly(rewrite, info,
                                             obj, rewrite.getField());
            } else {
                body = ExprFactory.makeVarRef(span, mangleName(name));
            }
        } else
            body = ExprFactory.makeVarRef(span, mangleName(name));

        if ( inTrait )
            return NodeFactory.makeFnDecl(span, mods, field.getName(),
                                          Collections.<Param>emptyList(),
                                          NodeUtil.optTypeOrPatternToType(field.getIdType()),
                                          Option.<Expr>none());
        else
            return NodeFactory.makeFnDecl(span, mods, field.getName(),
                                          Collections.<Param>emptyList(),
                                          NodeUtil.optTypeOrPatternToType(field.getIdType()),
                                          Option.<Expr>some(body));
    }

    private static final Modifiers setterNonMods =
        Modifiers.Settable.combine(Modifiers.Var.combine(Modifiers.Hidden.combine(Modifiers.Wrapped)));

    private FnDecl makeSetter(boolean inTrait, Id owner, Binding field) {
        Span span = NodeUtil.getSpan(field);
        Type voidType = NodeFactory.makeVoidType(span);
        Option<TypeOrPattern> ty = field.getIdType();
        Modifiers mods = field.getMods().remove(setterNonMods);
        Id name = field.getName();
        List<Param> params = new ArrayList<Param>();
        Id param = NodeFactory.makeId(span, "param_"+name);
        params.add((Param)NodeFactory.makeParam(param, ty));
        Option<Type> typ = NodeUtil.optTypeOrPatternToType(ty);
        Expr rhs = ExprFactory.makeVarRef(span, typ, param);
        Expr assign;
        if ( boxedRefMap.isSome() ) {
            Map<Pair<Id,Id>,FieldRef> map = boxedRefMap.unwrap();
            Pair<Id,Id> keyPair = new Pair( owner, name );
            if ( map.containsKey( keyPair ) ) {
                FieldRef fieldRef = map.get( keyPair );
                FieldRef rewrite = ExprFactory.makeFieldRef(fieldRef, NodeUtil.getSpan(field));
                assert( rewrite.getObj() instanceof VarRef );
                VarRef obj = (VarRef)rewrite.getObj();
                obj = ExprFactory.makeVarRef(obj, NodeUtil.getExprType(obj),
                                             mangleName(obj.getVarId()));
                assign = ExprFactory.makeMethodInvocation(NodeUtil.getSpan(rewrite), false,
                                                          Option.some(voidType), obj,
                                                          rewrite.getField(), rhs);
            } else {
                List<Lhs> lhs = new ArrayList<Lhs>();
                lhs.add(ExprFactory.makeVarRef(span, typ, mangleName(name)));
                assign = ExprFactory.makeAssignment(span, Option.some(voidType),
                                                    lhs, rhs);
            }
        } else {
            List<Lhs> lhs = new ArrayList<Lhs>();
            lhs.add(ExprFactory.makeVarRef(span, typ, mangleName(name)));
            assign = ExprFactory.makeAssignment(span, Option.some(voidType),
                                                lhs, rhs);
        }
        if ( inTrait )
            return NodeFactory.makeFnDecl(span, mods, name, params,
                                          Option.some(voidType), Option.<Expr>none());
        else
            return NodeFactory.makeFnDecl(span, mods, name, params,
                                          Option.some(voidType),
                                          Option.<Expr>some(assign));
    }

    private LinkedList<Decl> makeGetterSetters(final Id owner,
                                               Option<List<Param>> params,
                                               final List<Decl> decls) {
        final LinkedList<Decl> result = new LinkedList<Decl>();
        if (params.isSome()) {
            for (Param param : params.unwrap()) {
                if ( ! NodeUtil.isVarargsParam(param) ) {
                    if (! hidden(param) &&
                        ! hasExplicitGetter(param.getName(), decls))
                        result.add(makeGetter(false, owner, param));
                    if ((settable(param) || mutable(param)) &&
                        ! hasExplicitSetter(param.getName(), decls)) {
                        result.add(makeSetter(false, owner, param));
                    }
                }
            }
        }
        for (Decl decl : decls) {
            decl.accept(new NodeAbstractVisitor_void() {
                public void forVarDecl(VarDecl decl) {
                    for (LValue binding : decl.getLhs()) {
                        if (! hidden(binding) &&
                            ! hasExplicitGetter(binding.getName(), decls)) {
                            result.add(makeGetter(false, owner, binding));
                        }
                        if ((settable(binding) || mutable(binding)) &&
                            ! hasExplicitSetter(binding.getName(), decls)) {
                            result.add(makeSetter(false, owner, binding));
                        }
                    }
                }
            });
        }
        return result;
    }

    private LinkedList<Decl> makeGetterSetters(final boolean inTrait,
                                               final Id owner,
                                               final List<Decl> decls) {
        final LinkedList<Decl> result = new LinkedList<Decl>();

        for (Decl decl : decls) {
            decl.accept(new NodeAbstractVisitor_void() {
                public void forVarDecl(VarDecl dec) {
                    for (LValue binding : dec.getLhs()) {
                        if (! hidden(binding) &&
                            ! hasExplicitGetter(binding.getName(), decls)) {
                            result.add(makeGetter(inTrait, owner, binding));
                        }
                        if ((settable(binding) || mutable(binding)) &&
                            ! hasExplicitSetter(binding.getName(), decls)) {
                            result.add(makeSetter(inTrait, owner, binding));
                        }
                    }
                }
            });
        }
        return result;
    }

    private List<Decl> mangleDecls(List<Decl> decls) {
        return new NodeUpdateVisitor() {
            public Node forVarDecl(VarDecl that) {
                List<LValue> newLVals = new ArrayList<LValue>();

                for (LValue lval : that.getLhs()) {
                    // System.err.println(mangleName(lval.getName()));
                    newLVals.add(NodeFactory.makeLValue(lval, mangleName(lval.getName())));
                }
                return NodeFactory.makeVarDecl(NodeUtil.getSpan(that), newLVals, that.getInit());
            }

            /* Do not descend into object expressions. Instead, we mangle their
             * declarations when they're visited by DesugaringVisitor.
             */
            public Node forObjectExpr(ObjectExpr that) {
                return that;
            }
        }.recurOnListOfDecl(decls);
    }

    private Expr mangleLhs(final Assignment that,
                           final Expr rhs_result,
                           final Option<Type> voidType,
                           final Lhs lhs) {
        assert(that.getLhs().size() == 1);
        final Span span = NodeUtil.getSpan(that);
        final boolean paren = NodeUtil.isParenthesized(that);
        return (Expr)lhs.accept(new NodeUpdateVisitor() {
                    public Node forVarRef(VarRef lhs_that) {
                        List<Lhs> left = new ArrayList<Lhs>();
                        left.add((Lhs)lhs_that.accept(DesugaringVisitor.this));
                        return ExprFactory.makeAssignment(span, paren, voidType, left,
                                                          that.getAssignOp(), rhs_result, that.getAssignmentInfos());
                    }
                    public Node forSubscriptExpr(SubscriptExpr lhs_that) {
                        List<Lhs> left = new ArrayList<Lhs>();
                        left.add((Lhs)lhs_that.accept(DesugaringVisitor.this));
                        return ExprFactory.makeAssignment(span, paren, voidType, left,
                                                          that.getAssignOp(), rhs_result, that.getAssignmentInfos());
                    }
                    public Node forFieldRef(FieldRef lhs_that) {
                        Expr obj = (Expr) lhs_that.getObj().accept(DesugaringVisitor.this);
                        Expr rhs = rhs_result;
                        if ( that.getAssignOp().isSome() ) {
                            Expr _lhs = (Expr)lhs_that.accept(DesugaringVisitor.this);
                            FunctionalRef op;
                            Option<Type> type;
                            if (!that.getAssignmentInfos().isEmpty()) {
                                op = that.getAssignmentInfos().get(0).getOpForLhs();
                                type = Option.some(((ArrowType) NodeUtil.getExprType(op).unwrap()).getRange());
                            } else {
                                op = that.getAssignOp().unwrap();
                                type = NodeUtil.getExprType(lhs_that);
                            }
                            rhs = ExprFactory.makeOpExpr(span,
                                                         NodeUtil.isParenthesized(lhs_that),
                                                         type,
                                                         op,
                                                         Arrays.asList(_lhs, rhs));
                        }
                        return ExprFactory.makeMethodInvocation(span, NodeUtil.isParenthesized(that),
                                                                voidType, obj,
                                                                lhs_that.getField(),
                                                                rhs);
                    }
                });
    }

    @Override
    public Node forAssignment(final Assignment that) {
        final Span span = NodeUtil.getSpan(that);
        final boolean paren = NodeUtil.isParenthesized(that);
        List<Lhs> lhs = that.getLhs();
        int size = lhs.size();
        final Expr rhs_result = (Expr) that.getRhs().accept(this);
        final Option<Type> voidType = Option.<Type>some(NodeFactory.makeVoidType(span));
        if ( size == 1 ) {
            return mangleLhs(that, rhs_result, voidType, lhs.get(0));
        } else {
            /* Desugars
                 (x, y.f, z) := e
               to
                 tuple_3 = e
                 (tuple_0, tuple_1, tuple_2) = tuple_3
                 (x := tuple0, y.f(tuple1), z := tuple2)
            */
            Option<FunctionalRef> opr = that.getAssignOp();
            List<Expr>   assigns   = new ArrayList<Expr>();
            List<LValue> secondLhs = new ArrayList<LValue>();
            List<CompoundAssignmentInfo> allAssignmentInfos = that.getAssignmentInfos();
            // Possible shadowing!
            for (int i = size-1; i >= 0; i--) {
                Id id = NodeFactory.makeId(span, "tuple_"+i);
                List<Lhs> left = new ArrayList<Lhs>();
                Lhs _lhs = lhs.get(i);
                left.add(_lhs);
                Expr right = ExprFactory.makeVarRef(span, id);

                // This is the ith constituent assignment, so extract out the
                // ith assignment info.
                List<CompoundAssignmentInfo> assignmentInfo = Collections.emptyList();
                if (!allAssignmentInfos.isEmpty())
                    assignmentInfo = Collections.singletonList(allAssignmentInfos.get(i));
                
                Assignment assign = ExprFactory.makeAssignment(span, paren,
                                                               voidType,
                                                               left, opr,
                                                               right, assignmentInfo);
                assigns.add(0, mangleLhs(assign, right, voidType, _lhs));
                secondLhs.add(0, NodeFactory.makeLValue(id));
            }
            Id id = NodeFactory.makeId(span, "tuple_"+size);
            Expr third = ExprFactory.makeTupleExpr(span, assigns);
            LocalVarDecl second = ExprFactory.makeLocalVarDecl(span, secondLhs,
                                                               ExprFactory.makeVarRef(span, id),
                                                               third);
            Expr first = ExprFactory.makeLocalVarDecl(span, id, rhs_result, second);
            return ExprFactory.makeDo(span, voidType, first);
        }
    }

    /*
     * Recur on VarRef to change to a mangled name if it's a field ref.
     */
    @Override
    public Node forVarRefOnly(VarRef that, ExprInfo info,
                              Id varResult, List<StaticArg> staticArg_result) {
        // After disambiguation, the Id in a VarRef should have an empty API.
        assert(varResult.getApiName().isNone());

        if (fieldsInScope.contains(varResult)) {
            return ExprFactory.makeVarRef(that, NodeUtil.getExprType(that),
                                          mangleName(varResult));
        } else {
            return that;
        }
    }

    @Override
    public Node forFieldRefOnly(FieldRef that, ExprInfo info,
                                Expr obj_result, Id field_result) {
        return ExprFactory.makeMethodInvocation(that, obj_result, field_result,
                                                ExprFactory.makeVoidLiteralExpr(NodeUtil.getSpan(that)));
    }

    @Override
    public Node forFnRefOnly(FnRef that, ExprInfo info,
                             List<StaticArg> staticArgs_result,
                             IdOrOp fnResult, List<IdOrOp> fns_result,
                             List<Overloading> interpOverloadings_result,
                             List<Overloading> newOverloadings_result,
                             Option<Type> type_result,
                             Option<Type> schema_result) {
        // After disambiguation, the Id in a FnRef should have an empty API.
        assert(fnResult.getApiName().isNone());
        if ( ! (fnResult instanceof Id) )
            bug(fnResult, "The name field of FnRef should be Id.");
        Id name = (Id)fnResult;

        if (fieldsInScope.contains(name)) {
            List<IdOrOp> newFns = new ArrayList<IdOrOp>();
            for (IdOrOp n : fns_result) {
                if ( ! (n instanceof Id) )
                    return bug(n, "The name field of FnRef should be Id.");
                newFns.add(mangleName((Id)n));
            }

            return ExprFactory.makeFnRef(that, NodeUtil.getExprType(that),
                                         mangleName(name), newFns,
                                         staticArgs_result, interpOverloadings_result, newOverloadings_result,
                                         type_result, schema_result);
        } else {
            return that;
        }
    }

    @Override
    public Node forObjectExpr(ObjectExpr that) {
        DesugaringVisitor newVisitor = extend(NodeUtil.getDecls(that));
        Span span = NodeUtil.getSpan(that);
        List<Decl> decls_result = mangleDecls(newVisitor.recurOnListOfDecl(NodeUtil.getDecls(that)));

        LinkedList<Decl> gettersAndDecls = makeGetterSetters(false,
                                                             NodeFactory.makeTemporaryId(span),
                                                             NodeUtil.getDecls(that));
        for (int i = decls_result.size() - 1; i >= 0; i--) {
            gettersAndDecls.addFirst(decls_result.get(i));
        }
        TraitTypeHeader header = NodeFactory.makeTraitTypeHeader(NodeFactory.makeId(span,"_"),
                                                                 NodeUtil.getExtendsClause(that),
                                                                 gettersAndDecls);
        return forObjectExprOnly(that, that.getInfo(), header, that.getSelfType());
    }

    @Override
    public Node forObjectDecl(ObjectDecl that) {
        DesugaringVisitor newVisitor = extend(NodeUtil.getParams(that), NodeUtil.getDecls(that));

        Option<List<Param>> params_result = mangleParams(newVisitor.recurOnOptionOfListOfParam(NodeUtil.getParams(that)));
        Option<Contract> contract_result = newVisitor.recurOnOptionOfContract(NodeUtil.getContract(that));
        List<Decl> decls_result = mangleDecls(newVisitor.recurOnListOfDecl(NodeUtil.getDecls(that)));

        LinkedList<Decl> gettersAndDecls = makeGetterSetters(NodeUtil.getName(that),
                                                             NodeUtil.getParams(that), NodeUtil.getDecls(that));
        for (int i = decls_result.size() - 1; i >= 0; i--) {
            gettersAndDecls.addFirst(decls_result.get(i));
        }

        TraitTypeHeader header = NodeFactory.makeTraitTypeHeader(that.getHeader(), gettersAndDecls,
                                                                 contract_result, params_result);
        return forObjectDeclOnly(that, that.getInfo(), header, that.getSelfType());
    }

    @Override
    public Node forTraitDecl(TraitDecl that) {
        DesugaringVisitor newVisitor = extend(NodeUtil.getDecls(that));
        List<Decl> decls_result = removeVarDecls(newVisitor.recurOnListOfDecl(NodeUtil.getDecls(that)));

        // System.err.println("decls_result size = " + decls_result.size());
        LinkedList<Decl> gettersAndDecls = makeGetterSetters(true,
                                                             NodeUtil.getName(that),
                                                             NodeUtil.getDecls(that));

        // System.err.println("before: gettersAndDecls size = " + gettersAndDecls.size());
        for (int i = decls_result.size() - 1; i >= 0; i--) {
            gettersAndDecls.addFirst(decls_result.get(i));
        }
        // System.err.println("after: gettersAndDecls size = " + gettersAndDecls.size());

        TraitTypeHeader header = NodeFactory.makeTraitTypeHeaderWithDecls(that.getHeader(), gettersAndDecls);

        return forTraitDeclOnly(that, that.getInfo(), header, that.getSelfType(),
                                NodeUtil.getExcludesClause(that),
                                NodeUtil.getComprisesClause(that));
    }

    @Override
    public Node forFnDecl(FnDecl that) {
        FnHeader header = that.getHeader();
        IdOrOpOrAnonymousName name_result = (IdOrOpOrAnonymousName) recur(header.getName());
        List<StaticParam> staticParams_result = recurOnListOfStaticParam(header.getStaticParams());
        Option<WhereClause> where_result = recurOnOptionOfWhereClause(header.getWhereClause());
        Option<List<Type>> throwsClause_result = recurOnOptionOfListOfType(header.getThrowsClause());
        Option<Contract> contract_result = recurOnOptionOfContract(header.getContract());
        List<Param> params_result = recurOnListOfParam(header.getParams());
        Option<Type> returnType_result = recurOnOptionOfType(header.getReturnType());
        IdOrOp unambiguousName_result = (IdOrOp) recur(that.getUnambiguousName());
        Option<Expr> body_result = recurOnOptionOfExpr(that.getBody());
        Option<IdOrOp> implementsUnambiguousName_result = recurOnOptionOfIdOrOp(that.getImplementsUnambiguousName());
        return  NodeFactory.makeFnDecl(NodeUtil.getSpan(that), removeGetterSetterMod(NodeUtil.getMods(that)),
                                       name_result, staticParams_result, params_result,
                                       returnType_result, throwsClause_result,
                                       where_result, contract_result, unambiguousName_result,
                                       body_result, implementsUnambiguousName_result);
    }


        
}
