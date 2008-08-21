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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sun.fortress.compiler.typechecker.TraitTable;
import com.sun.fortress.compiler.typechecker.TypeCheckerOutput;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;


// TODO: TypeEnv does not handle OpRef and DimRef
// TODO: Remove the turnOnTypeChecker in shell under desugar phase to false
// TODO: TypeChecker by default is turned off, which is problematic, because
//       when it's turned off, it does not create typeCheckerOutput

public class ObjectExpressionVisitor extends NodeUpdateVisitor {
    // Type info passed down from type checking phase
    private TypeCheckerOutput typeCheckerOutput; 
    // A list of newly created ObjectDecls (i.e. lifted obj expressions and
    // objectDecls for boxed mutable var refs they capture) 
    private List<ObjectDecl> newObjectDecls;
    // A map mapping from mutable VarRef to its coresponding container
    // info (i.e. its boxed ObjectDecl, the new VarDecl to create the
    // boxed ObjectDecl instance, the VarRef to the new VarDecl ...  etc.)
    // this map gets updated/reset when entering/leaving ObjectDecl and
    // LocalVarDecl, which are the only places that can introduce new
    // mutable vars captured by object expr.  When leaving ObjectDecl, it
    // gets reset entirely.  When leaving LocalVarDecl, simply the VarRef
    // corresponding to the LocalVarRef gets removed.
    private Map<VarRef, VarRefContainer> mutableVarRefContainerMap;

    // a stack keeping track of all nodes that can create a new lexical scope
    // this does not include the top level component
    private Stack<Node> scopeStack;
    private TraitTable traitTable;

    private Component enclosingComponent;
    private TraitDecl enclosingTraitDecl;
    private ObjectDecl enclosingObjectDecl;
    private int objExprNestingLevel;
    private int uniqueId;

    /* The following two things are results returned by FreeNameCollector */
    /* Map key: object expr, value: free names captured by object expr */
    private Map<Span, FreeNameCollection> objExprToFreeNames;

    /* 
     * Map key: created with node (using pair of its name and Span - see
     *          FreeNameCollector.genKeyForDeclSite for more details)
     *          which the captured mutable varRef is declared under 
     *          (which should be either ObjectDecl or LocalVarDecl), 
     * value: list of pairs 
     *        pair.first is the varRef 
     *        pair.second is the decl node where the varRef is declared 
     *              (which is either a Param, LValueBind, or LocalVarDecl)
     *
     * IMPORTANT: Need to use Pair of String & Span as key! 
     * Span alone does not work, because the newly created nodes have the 
     * same span as the original decl node that we are rewriting
     * Node + Span is too strong, because sometimes decl nodes can nest each
     * other (i.e. LocalVarDecl), and once we rewrite the subtree, the decl
     * node corresponding to the key in this Map will change.
     */
    private Map<Pair<String,Span>, List<Pair<VarRef,Node>>> declSiteToVarRefs;

    // data structure to pass to getter setter desugarer pass so that it 
    // knows what references to rewrite into corresponding boxed FieldRefs
    private Map<Pair<Id,Id>,FieldRef> boxedRefMap;

    public static final String MANGLE_CHAR = "$";
    private static final String ENCLOSING_PREFIX = "enclosing";


    // Constructor
    public ObjectExpressionVisitor(TraitTable traitTable,
                    TypeCheckerOutput typeCheckerOutput) {
        this.typeCheckerOutput = typeCheckerOutput;

        newObjectDecls = new LinkedList<ObjectDecl>();
        mutableVarRefContainerMap = new HashMap<VarRef, VarRefContainer>();

        scopeStack = new Stack<Node>();
        this.traitTable = traitTable;

        enclosingComponent = null;
        enclosingTraitDecl = null;
        enclosingObjectDecl = null;
        objExprNestingLevel = 0;
        uniqueId = 0;

        boxedRefMap = new HashMap<Pair<Id,Id>,FieldRef>();
    }

    public Option<Map<Pair<Id,Id>,FieldRef>> getBoxedRefMap() {
        if( boxedRefMap.isEmpty() ) {
            return Option.<Map<Pair<Id,Id>,FieldRef>>none();
        }
        return Option.<Map<Pair<Id,Id>,FieldRef>>some( boxedRefMap );
    }
 
    @Override
	public Node forComponent(Component that) {
        FreeNameCollector freeNameCollector =
            new FreeNameCollector(traitTable, typeCheckerOutput);
        that.accept(freeNameCollector);

        objExprToFreeNames = freeNameCollector.getObjExprToFreeNames();
        declSiteToVarRefs  = freeNameCollector.getDeclSiteToVarRefs();

        // No object expression found in this component. We are done.
        if(objExprToFreeNames.isEmpty()) {
            return that;
        }

        enclosingComponent = that;
        // Only traverse the tree if we find any object expression
        Node returnValue = super.forComponent(that);
        enclosingComponent = null;

        return returnValue;
    }

    @Override
    public Node forComponentOnly(Component that, APIName name_result,
                                     List<Import> imports_result,
                                     List<Export> exports_result,
                                     List<Decl> decls_result) {
        decls_result.addAll(newObjectDecls);
        return super.forComponentOnly(that, name_result,
                        imports_result, exports_result, decls_result);
    }

    @Override
        public Node forTraitDecl(TraitDecl that) {
        scopeStack.push(that);
        enclosingTraitDecl = that;
    	Node returnValue = super.forTraitDecl(that);
    	enclosingTraitDecl = null;
        scopeStack.pop();
    	return returnValue;
    }

    @Override
    public Node forObjectDecl(ObjectDecl that) {
        scopeStack.push(that);
        enclosingObjectDecl = that;

        ObjectDecl returnValue = that;
        Pair<String,Span> key = FreeNameCollector.genKeyForDeclSite(that);
        List<Pair<VarRef,Node>> rewriteList = declSiteToVarRefs.get(key); 

        // Some rewriting required for this ObjectDecl (i.e. it has var
        // params being captured and mutated by some object expression(s) 
        if( rewriteList != null ) {
            String uniqueSuffix = that.getName().getText() + nextUniqueId(); 

            List<VarRef> mutableVarRefsForThisNode 
                = updateMutableVarRefContainerMap(uniqueSuffix, rewriteList);

            for(VarRef var : mutableVarRefsForThisNode) {
                VarRefContainer container = mutableVarRefContainerMap.get(var);
                newObjectDecls.add( container.containerDecl() );   
                Pair<Id,Id> keyPair = new Pair( that.getName(), var.getVar() );
                // Use an empty span; the correct span will be filled in
                // later at the use site
                boxedRefMap.put( keyPair, 
                                 container.containerFieldRef(new Span()) );
            }

            // The rewriter also inserts newly declared container VarDecls
            // into this ObjectDecl.
            MutableVarRefRewriteVisitor rewriter = 
                new MutableVarRefRewriteVisitor(that,
                                                mutableVarRefContainerMap,
                                                mutableVarRefsForThisNode);
            returnValue = (ObjectDecl) that.accept(rewriter);
        }

        // if rewriteList == null, returnValue = that; otherwise,
        // returnValue is updated by the MutableVarRefRewriteVisitor.
        // Note that we must update mutableVarRefContainerMap first 
        // before recursing on its subtree, because the info in the 
        // map is relevant to lifting object expr within this ObjectDecl 
        // In addition, we do the rewrite first before we recur on subtree,
        // although I believe the order of these two visitors should not
        // conflict.
        returnValue = (ObjectDecl) super.forObjectDecl(returnValue);

        // reset the mutableVarRefContainerMap
        mutableVarRefContainerMap = new HashMap<VarRef, VarRefContainer>();

        enclosingObjectDecl = null;
     	scopeStack.pop();
    	return returnValue;
    }

    @Override
	public Node forFnExpr(FnExpr that) {
        scopeStack.push(that);
        Node returnValue = super.forFnExpr(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forFnDef(FnDef that) {
        scopeStack.push(that);
        Node returnValue = super.forFnDef(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forIfClause(IfClause that) {
        scopeStack.push(that);
        Node returnValue = super.forIfClause(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forFor(For that) {
        scopeStack.push(that);
        Node returnValue = super.forFor(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forLetFn(LetFn that) {
        scopeStack.push(that);
        Node returnValue = super.forLetFn(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
    public Node forLocalVarDecl(LocalVarDecl that) {
        scopeStack.push(that);

        LocalVarDecl returnValue = that;
        Pair<String,Span> key = FreeNameCollector.genKeyForDeclSite(that);
        List<Pair<VarRef,Node>> rewriteList = declSiteToVarRefs.get(key); 

        List<VarRef> mutableVarRefsForThisNode = null;
        
        // Some rewriting required for this ObjectDecl (i.e. it has var
        // params being captured and mutated by some object expression(s) 
        if( rewriteList != null ) {
            String uniqueSuffix = "";
            if( enclosingObjectDecl != null ) {
                uniqueSuffix += enclosingObjectDecl.getName().getText();
            }
            uniqueSuffix += nextUniqueId(); 

            mutableVarRefsForThisNode = 
                updateMutableVarRefContainerMap( uniqueSuffix, rewriteList );
            for(VarRef var : mutableVarRefsForThisNode) {
                VarRefContainer container = mutableVarRefContainerMap.get(var);
                newObjectDecls.add( container.containerDecl() );   
            }

            MutableVarRefRewriteVisitor rewriter = 
               new MutableVarRefRewriteVisitor(that,
                                               mutableVarRefContainerMap,
                                               mutableVarRefsForThisNode); 
            returnValue = (LocalVarDecl) returnValue.accept(rewriter);
        }

        // Traverse the subtree regardless rewriting is needed or not
        // returnValue = that if no rewriting required for this node
        returnValue = (LocalVarDecl) super.forLocalVarDecl(returnValue);

        if(mutableVarRefsForThisNode != null) {
            // reset the mutableVarRefContainerMap
            for(VarRef varRef : mutableVarRefsForThisNode) {
                mutableVarRefContainerMap.remove(varRef);
            }
        }

        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forLabel(Label that) {
        scopeStack.push(that);
        Node returnValue = super.forLabel(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forCatch(Catch that) {
        scopeStack.push(that);
        Node returnValue = super.forCatch(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forTypecase(Typecase that) {
        scopeStack.push(that);
        Node returnValue = super.forTypecase(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forGeneratedExpr(GeneratedExpr that) {
        scopeStack.push(that);
        Node returnValue = super.forGeneratedExpr(that);
        scopeStack.pop();
        return returnValue;
    }

    @Override
	public Node forWhile(While that) {
		scopeStack.push(that);
		Node returnValue = super.forWhile(that);
		scopeStack.pop();
		return returnValue;
	}

	@Override
    public Node forObjectExpr(ObjectExpr that) {
	    objExprNestingLevel++;
		scopeStack.push(that);

        FreeNameCollection freeNames = objExprToFreeNames.get(that.getSpan());

       // System.err.println("Free names: " + freeNames);

        ObjectDecl lifted = liftObjectExpr(that, freeNames);
        newObjectDecls.add(lifted);
        TightJuxt callToLifted = makeCallToLiftedObj(lifted, that, freeNames);

        scopeStack.pop();
        objExprNestingLevel--;

        return callToLifted;
    }

   
    private TightJuxt makeCallToLiftedObj(ObjectDecl lifted,
                                          ObjectExpr objExpr,
                                          FreeNameCollection freeNames) {
        Span span = objExpr.getSpan();
        Id originalName = lifted.getName();
        List<Id> fns = new LinkedList<Id>();
        fns.add(originalName);
        // TODO: Need to figure out what Static params are captured.
        List<StaticArg> staticArgs = Collections.<StaticArg>emptyList();
        List<FnRef> freeMethodRefs = freeNames.getFreeMethodRefs();
        VarRef enclosingSelf = null;

        /* Now make the call to construct the lifted object */
        /* Use default value for parenthesized and exprType */
        // FIXME: I didn't initialize its exprType
        FnRef fnRef = ExprFactory.makeFnRef(span, false,
                                            originalName, fns, staticArgs);

        if (freeMethodRefs != null && freeMethodRefs.size() != 0) {
            enclosingSelf = ExprFactory.makeVarRef(span, "self");
        }

        List<Expr> exprs = makeArgsForCallToLiftedObj(objExpr,
                                                      freeNames, enclosingSelf);
        exprs.add(0, fnRef);

        TightJuxt callToConstructor =
            ExprFactory.makeTightJuxt(span, objExpr.isParenthesized(), exprs);

        return callToConstructor;
    }

    private List<Expr> makeArgsForCallToLiftedObj(ObjectExpr objExpr,
                                                  FreeNameCollection freeNames,
                                                  VarRef enclosingSelf) {
        Span span = objExpr.getSpan();

        List<VarRef> freeVarRefs = freeNames.getFreeVarRefs();
        List<FnRef> freeFnRefs = freeNames.getFreeFnRefs();
        List<Expr> exprs = new LinkedList<Expr>();

        // FIXME: Need to handle mutated vars
        if(freeVarRefs != null) {
            for(VarRef var : freeVarRefs) {
                TypeEnv typeEnv = typeCheckerOutput.getTypeEnv(objExpr);
                Option<Boolean> mutableOp = typeEnv.mutable( var.getVar() ); 
                if( mutableOp.isNone() ) {
                    throw new DesugarerError(objExpr.getSpan(), 
                        "Can't find " + var.getVar() + " in typeEnv for " 
                        + objExpr);
                }

                boolean isMutable = mutableOp.unwrap().booleanValue(); 
                if(isMutable) {
                    VarRefContainer container =
                        mutableVarRefContainerMap.get(var);
                    if(container != null) {
                        exprs.add( container.containerVarRef(var.getSpan()) );
                    } else {
                        throw new DesugarerError(objExpr.getSpan(), 
                            var.getVar() + " is mutable but not found in " 
                            + "the mutableVarRefContainerMap!");
                    }
                }  else {
                    VarRef newVar = 
                        ExprFactory.makeVarRef( var.getSpan(), var.getVar() );
                    exprs.add(newVar);
                }
            }
        }

        if(freeFnRefs != null) {
            for(FnRef fn : freeFnRefs) {
                exprs.add(fn);
            }
        }

        if(enclosingSelf != null) {
            exprs.add(enclosingSelf);
        }

        if( exprs.size() == 0 ) {
            VoidLiteralExpr voidLit = ExprFactory.makeVoidLiteralExpr(span);
            exprs.add(voidLit);
        } else if( exprs.size() > 1 ) {
            TupleExpr tuple = ExprFactory.makeTuple(span, exprs);
            exprs = new LinkedList<Expr>();
            exprs.add(tuple);
        }

        return exprs;
    }

    private ObjectDecl liftObjectExpr(ObjectExpr target,
                                      FreeNameCollection freeNames) {
        String name = getMangledName(target);
        Span span = target.getSpan();
        Id liftedObjId = NodeFactory.makeId(span, name);
        List<StaticParam> staticParams =
            makeStaticParamsForLiftedObj(freeNames);
        List<TraitTypeWhere> extendsClauses = target.getExtendsClause();
        // FIXME: need to rewrite all decls w/ the freeNames.
        List<Decl> decls = target.getDecls();

        NormalParam enclosingSelf = null;
        Option<List<Param>> params = null;
        List<FnRef> freeMethodRefs = freeNames.getFreeMethodRefs();
        // FIXME: Temp hack to use Map<Span,TypeEnv>
        // FIXME: Will change it when the TypeCheckResult.getTypeEnv is done
        TypeEnv typeEnv = typeCheckerOutput.getTypeEnv( scopeStack.peek() );
        enclosingSelf = makeEnclosingSelfParam(typeEnv, target, freeMethodRefs);

        params = makeParamsForLiftedObj(target, freeNames, 
                                        typeEnv, enclosingSelf);
        /* Use default value for modifiers, where clauses,
           throw clauses, contract */
        ObjectDecl lifted = new ObjectDecl(span, liftedObjId, staticParams,
                                           extendsClauses,
                                           Option.<WhereClause>none(),
                                           params, decls);

        if(enclosingSelf != null) {
            VarRef receiver = makeVarRefFromNormalParam(enclosingSelf);
            DottedMethodRewriteVisitor rewriter =
                new DottedMethodRewriteVisitor(receiver, freeMethodRefs);
            lifted = (ObjectDecl) lifted.accept(rewriter);
        }

        return lifted;
    }

    private Option<List<Param>>
    makeParamsForLiftedObj(ObjectExpr target, FreeNameCollection freeNames,
                           TypeEnv typeEnv, NormalParam enclosingSelfParam) {
        // TODO: need to figure out shadowed self via FnRef
        // need to box any var that's mutabl

        Option<Type> type = null;
        NormalParam param = null;
        List<Param> params = new LinkedList<Param>();
        List<VarRef> freeVarRefs = freeNames.getFreeVarRefs();
        List<FnRef> freeFnRefs = freeNames.getFreeFnRefs();

        // FIXME: Need to handle mutated vars
        if(freeVarRefs != null) {
            for(VarRef var : freeVarRefs) {
                Option<Boolean> mutableOp = typeEnv.mutable( var.getVar() ); 
                if( mutableOp.isNone() ) {
                    throw new DesugarerError(target.getSpan(), 
                        "Can't find " + var.getVar() + " in typeEnv for " 
                        + target);
                }

                boolean isMutable = mutableOp.unwrap().booleanValue(); 

                if(isMutable) {
                    VarRefContainer container =
                        mutableVarRefContainerMap.get(var);
                    if(container != null) {
                        params.add( container.containerTypeParam() );
                    } else {
                        throw new DesugarerError(target.getSpan(), 
                            var.getVar() + " is mutable but not found in " 
                            + "the mutableVarRefContainerMap!");
                    }
                }  else {
                    // Default value for modifier and default expression
                    // FIXME: What if it has a type that's not visible at top level?
                    // FIXME: what span should I use?
                    type = typeEnv.type(var.getVar());
                    param = new NormalParam(var.getSpan(), var.getVar(), type);
                    params.add(param);
                }
            }
        }

        if(freeFnRefs != null) {
            for(FnRef fn : freeFnRefs) {
                // Default value for modifier and default expression
                // FIXME: What if it has a type that's not visible at top level?
                // FIXME: what span should I use?
                type = typeEnv.type(fn.getOriginalName());
                param = new NormalParam(fn.getSpan(),
                                        fn.getOriginalName(), type);
                params.add(param);
            }
        }

        if(enclosingSelfParam != null) {
            params.add(enclosingSelfParam);
        }
        
        return Option.<List<Param>>some(params);
    }


    private NormalParam makeEnclosingSelfParam(TypeEnv typeEnv,
                                               ObjectExpr objExpr,
                                               List<FnRef> freeMethodRefs) {
        Option<Type> type;
        NormalParam param = null;

        if(freeMethodRefs != null && freeMethodRefs.size() != 0) {
            // Just sanity check
            if(enclosingTraitDecl == null && enclosingObjectDecl == null) {
                throw new DesugarerError("No enclosing trait or object " +
                            "decl found when a dotted method is referenced.");
            }

            // Use the span for the obj expr that we are lifting
            // FIXME: Is this the right span to use??
            Span paramSpan = objExpr.getSpan();

            // use the "self" id to get the right type of the
            // enclosing object / trait decl
            type = typeEnv.type( new Id("self") );

            // id of the newly created param for implicit self
            Id enclosingParamId = NodeFactory.makeId(paramSpan,
                    MANGLE_CHAR + ENCLOSING_PREFIX + "_" + objExprNestingLevel);
            param = new NormalParam(paramSpan, enclosingParamId, type);
        }

        return param;
    }

    private List<StaticParam>
    makeStaticParamsForLiftedObj(FreeNameCollection freeNames) {
        // TODO: Fill this in - get the VarTypes(?) that's free and
        // generate static param using it
        return Collections.<StaticParam>emptyList();
    }

    // Generate a map mapping from mutable VarRef to its coresponding 
    // container info (i.e. its boxed ObjectDecl, the new VarDecl to create 
    // the boxed ObjectDecl instance, the VarRef to the new VarDecl, etc.)
    // based on the info stored in the rewriteList
    private List<VarRef> 
    updateMutableVarRefContainerMap(String uniqueSuffix,
                                    List<Pair<VarRef,Node>> rewriteList) {
        List<VarRef> addedVarRefs = new LinkedList<VarRef>(); 
        for( Pair<VarRef,Node> varPair : rewriteList ) {
            VarRef var = varPair.first();
            Node declNode = varPair.second();
            VarRefContainer container = 
                new VarRefContainer( var, declNode, uniqueSuffix );
            mutableVarRefContainerMap.put(var, container);
            addedVarRefs.add(var);
        }
    
        return addedVarRefs;
    }

    // small helper methods
    private VarRef makeVarRefFromNormalParam(NormalParam param) {
        VarRef varRef = ExprFactory.makeVarRef( param.getSpan(),
                                                param.getName(),
                                                param.getType() );
        return varRef;
    }

    private String getMangledName(ObjectExpr target) {
        String compName = NodeUtil.nameString(enclosingComponent.getName());
        String mangled = MANGLE_CHAR + compName + "_" + nextUniqueId();
        return mangled;
    }

    private int nextUniqueId() {
        return uniqueId++;
    }


//     private ObjectDecl 
//     createContainerForMutableVars(Node originalContainer, 
//                                   String name,
//                                   List<Pair<ObjectExpr,VarRef>> rewriteList,
//                                   List<Expr> argsToContainerObj) {
//         // FIXME: Is this the right span to use?
//         Span containerSpan = originalContainer.getSpan(); 
//         Id containerId = NodeFactory.makeId(containerSpan, name);
//         List<StaticParam> staticParams = Collections.<StaticParam>emptyList();
//         List<TraitTypeWhere> extendClauses = 
//             Collections.<TraitTypeWhere>emptyList();
//         List<Decl> decls = Collections.emptyList(); 
// 
//         List<Param> params = new LinkedList<Param>();
// 
//         // TODO: We can do something fancier later to group varRefs
//         // differently depending on which obj exprs captures them so that the 
//         // grouping reflects the "correct" life span each var should have. 
//         for(Pair<ObjectExpr,VarRef> var : rewriteList) {
//             ObjectExpr objExpr = var.first();
//             VarRef varRef = var.second();
//             // If multiple obj exprs refer to the same varRef, there will 
//             // be duplicates in the rewriteList; don't generate params for 
//             // duplicates
//             if( argsToContainerObj.contains(varRef) == false ) {
//                 argsToContainerObj.add(varRef);
//                 TypeEnv typeEnv = typeCheckerOutput.getTypeEnv(objExpr);
//                 Option<Node> declNodeOp = 
//                     typeEnv.declarationSite( varRef.getVar() );
//                 NormalParam param = null;
//                 if( declNodeOp.isSome() ) {                    
//                     param = makeVarParamFromVarRef( varRef, 
//                                 declNodeOp.unwrap().getSpan(), 
//                                 varRef.getExprType() ); 
//                 } else {
//                     param = makeVarParamFromVarRef( varRef, varRef.getSpan(),
//                                         varRef.getExprType() );   
//                 }
//                 params.add(param);
//             }
//         }
//         
//         ObjectDecl container = new ObjectDecl(containerSpan, 
//                                         containerId, staticParams, 
//                                         extendClauses, 
//                                         Option.<WhereClause>none(),
//                                         Option.<List<Param>>some(params), 
//                                         decls);
//                                     
//         return container;
//     }
}


