/*******************************************************************************
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
 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

/* Getter/setter desugaring
 * After this desugaring, the getter/setter modifiers are eliminated.
 */
public class DesugaringVisitor extends NodeUpdateVisitor {
    private boolean inTrait = false;
    private List<Id> fieldsInScope;

    public DesugaringVisitor() {
        fieldsInScope = new ArrayList<Id>();
    }

    public DesugaringVisitor(List<Id> _fieldsInScope) {
        fieldsInScope = _fieldsInScope;
    }

    public DesugaringVisitor(boolean _inTrait, List<Id> _fieldsInScope) {
        inTrait = _inTrait;
        fieldsInScope = _fieldsInScope;
    }

    private boolean hidden(ImplicitGetterSetter field) {
        for (Modifier mod : field.getMods()) {
            if (mod instanceof ModifierHidden) {
                return true;
            }
        }
        return false;
    }

    private boolean trans(ImplicitGetterSetter field) {
        for (Modifier mod : field.getMods()) {
            if (mod instanceof ModifierTransient) {
                return true;
            }
        }
        return false;
    }

    private boolean settable(ImplicitGetterSetter field) {
        for (Modifier mod : field.getMods()) {
            if (mod instanceof ModifierSettable) {
                return true;
            }
        }
        return false;
    }

    private boolean isGetter(FnAbsDeclOrDecl decl) {
        for (Modifier mod : decl.getMods()) {
            if (mod instanceof ModifierGetter) {
                return true;
            }
        }
        return false;
    }

    private boolean isSetter(FnAbsDeclOrDecl decl) {
        for (Modifier mod : decl.getMods()) {
            if (mod instanceof ModifierSetter) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes an Id and a list of declarations and determines
     * whether the list of declarations contains an explicit getter with a name equal
     * to the given Id.
     */
    private boolean hasExplicitGetter(Id name, List<Decl> decls) {
        for (Decl decl: decls) {
            if (decl instanceof FnAbsDeclOrDecl) {
                FnAbsDeclOrDecl _decl = (FnAbsDeclOrDecl) decl;
                if (_decl.getName().equals(name) && isGetter(_decl)) {
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
            if (decl instanceof FnAbsDeclOrDecl) {
                FnAbsDeclOrDecl _decl = (FnAbsDeclOrDecl) decl;
                if (_decl.getName().equals(name) && isSetter(_decl)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Modifier> removeGetterSetterMod(List<Modifier> mods) {
        List<Modifier> result = new LinkedList<Modifier>();

        for (Modifier mod : mods) {
            if (! (mod instanceof ModifierGetter) &&
                ! (mod instanceof ModifierSetter)) { result.add(mod); }
        }
        return result;
    }

    private static final Id mangleName(Id fieldName) {
        return new Id(fieldName.getSpan(), fieldName.getApi(), "$" + fieldName.getText());
    }

    private static Option<List<Param>> mangleParams(Option<List<Param>> params) {
        return new NodeUpdateVisitor() {
            public Node forNormalParam(NormalParam that) {
                return new NormalParam(that.getSpan(), that.getMods(),
                                       mangleName(that.getName()), that.getType(),
                                       that.getDefaultExpr());
            }
        }.recurOnOptionOfListOfParam(params);
    }

    private DesugaringVisitor extend(Option<List<Param>> params, List<Decl> decls) {
        List<Id> newScope = new ArrayList<Id>();

        if (params.isSome()) {
            for (Param param: params.unwrap()) {
                if (param instanceof NormalParam)
                    newScope.add(param.getName());
            }
        }
        for (Decl decl: decls) {
            if (decl instanceof VarDecl) {
                for (LValueBind binding : (((VarDecl)decl).getLhs())) {
                    newScope.add(binding.getName());
                }
            }
        }
        newScope.addAll(fieldsInScope);
        return new DesugaringVisitor(newScope);
    }

    private DesugaringVisitor extend(List<Decl> decls) {
        List<Id> newScope = new ArrayList<Id>();

        for (Decl decl: decls) {
            if (decl instanceof VarDecl) {
                for (LValueBind binding : (((VarDecl)decl).getLhs())) {
                    newScope.add(binding.getName());
                }
            }
        }
        newScope.addAll(fieldsInScope);
        return new DesugaringVisitor(newScope);
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

    private FnDef makeGetter(ImplicitGetterSetter field) {
        List<Modifier> mods = new LinkedList<Modifier>();
        for (Modifier mod : field.getMods()) { mods.add(mod); }
        mods.add(new ModifierAtomic(field.getSpan()));

        return new FnDef(field.getSpan(), mods, field.getName(),
                         new ArrayList<StaticParam>(),
                         new ArrayList<Param>(),
                         field.getType(),
                         Option.<List<BaseType>>none(),
                         Option.<WhereClause>none(),
                         Option.<Contract>none(),
                         new VarRef(field.getSpan(), false,
                                    mangleName(field.getName())));
    }

    /*
    private FnDef makeSetter(ImplicitGetterSetter field) {
        List<Modifier> mods = new LinkedList<Modifier>();
        for (Modifier mod : field.getMods()) { mods.add(mod); }
        mods.add(new ModifierAtomic(field.getSpan()));

        return new FnDef(field.getSpan(), mods, field.getName(),
                         new ArrayList<StaticParam>(),
                         new ArrayList<Param>(),
                         field.getType(),
                         Option.<List<BaseType>>none(),
                         Option.<WhereClause>none(),
                         Option.<Contract>none(),
                         new VarRef(field.getSpan(), false,
                                    mangleName(field.getName())));
    }
    */

    private LinkedList<Decl> makeGetters(Option<List<Param>> params,
                                         final List<Decl> decls) {
        final LinkedList<Decl> result = new LinkedList<Decl>();
        if (params.isSome()) {
            for (Param param : params.unwrap()) {
                if (param instanceof NormalParam) {
                    NormalParam _param = (NormalParam)param;
                    if (! hidden(_param) && ! trans(_param) &&
                        ! hasExplicitGetter(_param.getName(), decls))
                        result.add(makeGetter(_param));
                }
            }
        }
        for (Decl decl : decls) {
            decl.accept(new NodeAbstractVisitor_void() {
                public void forVarDecl(VarDecl decl) {
                    for (LValueBind binding : decl.getLhs()) {
                        if (! hidden(binding) && ! trans(binding) &&
                            ! hasExplicitGetter(binding.getName(), decls)) {
                            result.add(makeGetter(binding));
                        }
                    }
                }
            });
        }
        return result;
    }

    private LinkedList<Decl> makeGetters(final List<Decl> decls) {
        final LinkedList<Decl> result = new LinkedList<Decl>();

        for (Decl decl : decls) {
            decl.accept(new NodeAbstractVisitor_void() {
                public void forVarDecl(VarDecl decl) {
                    for (LValueBind binding : decl.getLhs()) {
                        if (! hidden(binding) && ! trans(binding) &&
                            ! hasExplicitGetter(binding.getName(), decls)) {
                            result.add(makeGetter(binding));
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
                List<LValueBind> newLVals = new ArrayList<LValueBind>();

                for (LValueBind lval : that.getLhs()) {
                    // System.err.println(mangleName(lval.getName()));
                    newLVals.add(NodeFactory.makeLValue(lval, mangleName(lval.getName())));
                }
                return new VarDecl(that.getSpan(), newLVals, that.getInit());
            }
            public Node forAbsVarDecl(AbsVarDecl that) {
                List<LValueBind> newLVals = new ArrayList<LValueBind>();

                for (LValueBind lval : that.getLhs()) {
                    newLVals.add(NodeFactory.makeLValue(lval, mangleName(lval.getName())));
                }
                return new AbsVarDecl(that.getSpan(), newLVals);
            }
            /* Do not descend into object expressions. Instead, we mangle their
             * declarations when they're visited by DesugaringVisitor.
             */
            public Node forObjectExpr(ObjectExpr that) {
                return that;
            }
        }.recurOnListOfDecl(decls);
    }

    /**
     * Walk over a list of Lhs and recur on everything but FieldRefs.
     * For FieldRefs, recur only on their receivers.
     * In this way, we avoid turning them into MethodRefs.
     */
    private List<Lhs> mangleLhs(List<Lhs> elts) {
        final List<Lhs> result = new ArrayList<Lhs>();

        for (Lhs lhs : elts) {
            lhs.accept(new NodeAbstractVisitor_void() {
                    public void defaultCase(Node that) {
                        result.add((Lhs)that.accept(DesugaringVisitor.this));
                    }
                    public void forFieldRef(FieldRef that) {
                        result.add((Lhs)new FieldRef(that.getSpan(),
                                                     that.isParenthesized(),
                                                     that.getExprType(),
                                                     (Expr)that.getObj().accept(DesugaringVisitor.this),
                                                     mangleName(that.getField())));
                    }
                    public void for_RewriteFieldRef(_RewriteFieldRef that) {
                        Name name = that.getField();
                        if ( name instanceof Id )
                            name = mangleName((Id)name);
                        result.add((Lhs)new _RewriteFieldRef(that.getSpan(),
                                                             that.isParenthesized(),
                                                             that.getExprType(),
                                                             (Expr)that.getObj().accept(DesugaringVisitor.this),
                                                             name));
                    }
                });
        }
        return result;
    }

    /**
     * Be sure not to recur on FieldRefs that might occur in that.getLhs().
     * TODO: Rewrite assignments to single fields as setters.
     */
    @Override
    public Node forAssignment(Assignment that) {
        // To handle setters, we must process the lhs. For now, do not recur on lhs.
        // Instead, walk over the Lhs and recur on everything but FieldRefs.
        // For FieldRefs, recur only on their receivers.
        // In this way, we avoid turning them into MethodRefs.
        List<Lhs> lhs_result = mangleLhs(that.getLhs());
        Expr rhs_result = (Expr) that.getRhs().accept(this);
        return forAssignmentOnly(that, that.getExprType(), lhs_result,
                                 that.getOpr(), rhs_result);
    }

    /*
     * Recur on VarRef to change to a mangled name if it's a field ref.
     */
    @Override
    public Node forVarRefOnly(VarRef that, Option<Type> exprType_result,
                              Id varResult) {
        // After disambiguation, the Id in a VarRef should have an empty API.
        assert(varResult.getApi().isNone());

        if (fieldsInScope.contains(varResult)) {
            return new VarRef(that.getSpan(), that.isParenthesized(),
                              exprType_result, mangleName(varResult),
                              that.getLexicalDepth());
        } else {
        	return that;
        }
    }

    @Override
    public Node forFieldRefOnly(FieldRef that, Option<Type> exprType_result,
                                Expr obj_result, Id field_result) {
        return new MethodInvocation(that.getSpan(), that.isParenthesized(),
                                    that.getExprType(), obj_result, field_result,
                                    new ArrayList<StaticArg>(),
                                    NodeFactory.makeVoidLiteralExpr());
    }

    @Override
    public Node forObjectExpr(ObjectExpr that) {
        DesugaringVisitor newVisitor = extend(that.getDecls());
        List<Decl> decls_result = mangleDecls(newVisitor.recurOnListOfDecl(that.getDecls()));

        LinkedList<Decl> gettersAndDecls = makeGetters(that.getDecls());
        for (int i = decls_result.size() - 1; i >= 0; i--) {
            gettersAndDecls.addFirst(decls_result.get(i));
        }
        return forObjectExprOnly(that, that.getExprType(),
                                 that.getExtendsClause(), gettersAndDecls);
    }

    @Override
    public Node forObjectDecl(ObjectDecl that) {
        DesugaringVisitor newVisitor = extend(that.getParams(), that.getDecls());

        Option<List<Param>> params_result = mangleParams(newVisitor.recurOnOptionOfListOfParam(that.getParams()));
        Option<Contract> contract_result = newVisitor.recurOnOptionOfContract(that.getContract());
        List<Decl> decls_result = mangleDecls(newVisitor.recurOnListOfDecl(that.getDecls()));

        LinkedList<Decl> gettersAndDecls = makeGetters(that.getParams(), that.getDecls());
        for (int i = decls_result.size() - 1; i >= 0; i--) {
            gettersAndDecls.addFirst(decls_result.get(i));
        }

        return forObjectDeclOnly(that, that.getMods(), that.getName(),
                                 that.getStaticParams(), that.getExtendsClause(),
                                 that.getWhere(), params_result,
                                 that.getThrowsClause(), contract_result,
                                 gettersAndDecls);
    }

    @Override
    public Node forTraitDecl(TraitDecl that) {
        DesugaringVisitor newVisitor = extend(that.getDecls());
        List<Decl> decls_result = removeVarDecls(newVisitor.recurOnListOfDecl(that.getDecls()));

        // System.err.println("decls_result size = " + decls_result.size());
        LinkedList<Decl> gettersAndDecls = makeGetters(that.getDecls());

        // System.err.println("before: gettersAndDecls size = " + gettersAndDecls.size());
        for (int i = decls_result.size() - 1; i >= 0; i--) {
        	gettersAndDecls.addFirst(decls_result.get(i));
        }
        // System.err.println("after: gettersAndDecls size = " + gettersAndDecls.size());

        return forTraitDeclOnly(that, that.getMods(), that.getName(),
                                that.getStaticParams(), that.getExtendsClause(),
                                that.getWhere(), that.getExcludes(),
                                that.getComprises(), gettersAndDecls);
    }

    @Override
    public Node forAbsFnDeclOnly(AbsFnDecl that, List<Modifier> mods_result,
                                 IdOrOpOrAnonymousName name_result,
                                 List<StaticParam> staticParams_result,
                                 List<Param> params_result,
                                 Option<Type> returnType_result,
                                 Option<List<BaseType>> throwsClause_result,
                                 Option<WhereClause> where_result,
                                 Option<Contract> contract_result)
    {
        return new AbsFnDecl(that.getSpan(), removeGetterSetterMod(mods_result),
                             name_result, staticParams_result, params_result,
                             returnType_result, throwsClause_result,
                             where_result, contract_result, that.getSelfName());
    }

    @Override
    public Node forFnDefOnly(FnDef that, List<Modifier> mods_result,
                             IdOrOpOrAnonymousName name_result,
                             List<StaticParam> staticParams_result,
                             List<Param> params_result,
                             Option<Type> returnType_result,
                             Option<List<BaseType>> throwsClause_result,
                             Option<WhereClause> where_result,
                             Option<Contract> contract_result,
                             Expr body_result)
    {
        return new FnDef(that.getSpan(), removeGetterSetterMod(mods_result),
                         name_result, staticParams_result, params_result,
                         returnType_result, throwsClause_result,
                         where_result, contract_result, that.getSelfName(),
                         body_result);
    }
}
