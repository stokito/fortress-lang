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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sun.fortress.compiler.typechecker.TraitTable;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Catch;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.GeneratedExpr;
import com.sun.fortress.nodes.GenericDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Spawn;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.While;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;


// TODO: TypeEnv does not handle OpRef and DimRef
// TODO: Remove the turnOnTypeChecker in shell under desugar phase to false
// TODO: Getter/Setter is turned off, because it corrupts TypeEnvAtNode 
//       data structure; Need to figure out how to get around that
// TODO: TypeChecker by default is turned off, which is problematic, because
//       when it's turned off, it does not create typeEnvAtNode

public class ObjectExpressionVisitor extends NodeUpdateVisitor {
	private List<ObjectDecl> liftedObjectExprs;
	private Component enclosingComponent;
	private int uniqueId;
	private Map<Pair<Node,Span>, TypeEnv> typeEnvAtNode;
	// a stack keeping track of all nodes that can create a new lexical scope
	// this does not include the top level component
	private Stack<Node> scopeStack;  
	private TraitTable traitTable;
	private GenericDecl enclosingTraitOrObject;
    private int levelOfObjExprNesting;
    private static final String ENCLOSING_NAME = "enclosing";

	public ObjectExpressionVisitor(TraitTable traitTable, 
						Map<Pair<Node,Span>,TypeEnv> _typeEnvAtNode) {
		uniqueId = 0;
		typeEnvAtNode = _typeEnvAtNode;
		scopeStack = new Stack<Node>();
		this.traitTable = traitTable;
		enclosingTraitOrObject = null;
	    levelOfObjExprNesting = 0;
	}

	@Override
	public Node forComponentOnly(Component that, APIName name_result,
			                     List<Import> imports_result, 
                                 List<Export> exports_result,
			                     List<Decl> decls_result) {
		if(liftedObjectExprs != null) {
			decls_result.addAll(liftedObjectExprs);
		}
		return super.forComponentOnly(that, name_result, 
                            imports_result, exports_result, decls_result);
	}

	@Override
	public Node forComponent(Component that) {
		enclosingComponent = that;
		Node returnValue = super.forComponent(that);
		enclosingComponent = null;
		return returnValue;
	}

	@Override
    public Node forTraitDecl(TraitDecl that) {
		scopeStack.push(that);
		enclosingTraitOrObject = that;
    	Node returnValue = super.forTraitDecl(that);
    	enclosingTraitOrObject = null;
		scopeStack.pop();
    	return returnValue;
    }

	@Override
    public Node forObjectDecl(ObjectDecl that) {
		scopeStack.push(that);
		enclosingTraitOrObject = that;
     	Node returnValue = super.forObjectDecl(that);
     	enclosingTraitOrObject = null;
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
		Node returnValue = super.forLocalVarDecl(that);
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
	public Node forSpawn(Spawn that) {
		scopeStack.push(that);
		Node returnValue = super.forSpawn(that);
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
	public Node forDoFront(DoFront that) {
		scopeStack.push(that);
		Node returnValue = super.forDoFront(that);
		scopeStack.pop();
		return returnValue;		
	}
	
	@Override
    public Node forObjectExpr(ObjectExpr that) {
	    levelOfObjExprNesting++;
		scopeStack.push(that);
		
        FreeNameCollector freeNameCollector = 
            new FreeNameCollector(scopeStack, typeEnvAtNode, 
            					  traitTable, enclosingTraitOrObject);
        that.accept(freeNameCollector);
		FreeNameCollection freeNames = freeNameCollector.getResult();
		
System.err.println("Free names: " + freeNames);

		ObjectDecl lifted = liftObjectExpr(that, freeNames);  
		if(liftedObjectExprs == null) {
			liftedObjectExprs = new LinkedList<ObjectDecl>();
		}
		liftedObjectExprs.add(lifted);
		TightJuxt callToLifted = makeCallToLiftedObj(lifted, that, freeNames);
		
		scopeStack.pop();
		
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
		
		/* Now make the call to construct the lifted object */
		/* Use default value for parenthesized */
		FnRef fnRef = new FnRef(span, originalName, fns, staticArgs);
		List<Expr> exprs = makeArgsForCallToLiftedObj(objExpr, freeNames);
		exprs.add(0, fnRef);
		
		TightJuxt callToConstructor = 
            new TightJuxt(span, objExpr.isParenthesized(), exprs);
		
		return callToConstructor;
	}

	private List<Expr> 
    makeArgsForCallToLiftedObj(ObjectExpr objExpr,
                               FreeNameCollection freeNames) {
	    Span span = objExpr.getSpan();

        List<VarRef> freeVarRefs = freeNames.getFreeVarRefs();
        List<FnRef> freeFnRefs = freeNames.getFreeFnRefs();
        List<FnRef> freeMethodRefs = freeNames.getFreeMethodRefs();
        // List<Id> lhsOfAssignmt = freeNames.getLhsOfAssignment();
        List<Expr> exprs = new LinkedList<Expr>();

        // FIXME: Need to handle mutated vars
        if(freeVarRefs != null) {
        	for(VarRef var : freeVarRefs) {
        	    // FIXME: is it ok to get rid of parenthesis around it?
        	    VarRef newVar = new VarRef(var.getSpan(), false, 
        	                               var.getVar(), var.getLexicalDepth());
                exprs.add(newVar);
        	}
        }

        if(freeFnRefs != null) {
        	for(FnRef fn : freeFnRefs) {
                exprs.add(fn);
        	}
        }
        
//        // Need to pass in the implicit "self" from the enclosing 
//        // trait / object decl
//        if(freeMethodRefs != null) {
//            // id of the enclosing trait or object
//            Id enclosingId = null;
//            // id of the newly created param for implicit self 
//            Id enclosingParamId = null;
//            // FIXME: what span should I use? 
//            Span paramSpan = freeMethodRefs.get(0).getSpan();
//
//            if(enclosingTraitOrObject == null) {
//                throw new DesugarerError("enclosingTraitOrObject " + 
//                    "is null when a dotted method is referenced.");
//            } 
//            if(enclosingTraitOrObject instanceof TraitDecl) {
//            	TraitDecl cast = (TraitDecl) enclosingTraitOrObject;
//                enclosingId = cast.getName();
//            } else if(enclosingTraitOrObject instanceof ObjectDecl) {
//            	ObjectDecl cast = (ObjectDecl) enclosingTraitOrObject;
//                enclosingId = cast.getName();
//            } 
//            type = typeEnv.type(enclosingId);
//            
//            Option<APIName> inApi = Option.wrap(enclosingComponent.getName());
//            enclosingParamId = new Id(paramSpan, inApi, 
//                       ENCLOSING_NAME + "$" + levelOfObjExprNesting);
//            param = new NormalParam(paramSpan, enclosingParamId, type);
//            params.add(param);
//        }
        
        if( exprs.size() == 0 ) {
            VoidLiteralExpr voidLit = new VoidLiteralExpr(span);  
            exprs.add(voidLit);
        } else {
            TupleExpr tuple = new TupleExpr(span, exprs);
            exprs = new LinkedList<Expr>();
            exprs.add(tuple);
        }

        return exprs;       
    }

    private ObjectDecl liftObjectExpr(ObjectExpr target, 
                                      FreeNameCollection freeNames) {
		String name = getMangledName(target);
		Span span = target.getSpan();
		Id id = new Id(Option.some(enclosingComponent.getName()), name);
		List<StaticParam> staticParams = 
            makeStaticParamsForLiftedObj(freeNames);
		List<TraitTypeWhere> extendsClauses = target.getExtendsClause();
		Option<List<Param>> params = makeParamsForLiftedObj(freeNames);
        // FIXME: need to rewrite all decls w/ the freeNames.
        List<Decl> decls = target.getDecls();   

        /* Use default value for modifiers, where clauses, 
           throw clauses, contract */
		ObjectDecl lifted = new ObjectDecl(span, id, staticParams, 
                                           extendsClauses, params, decls);

		return lifted;
	}

	private Option<List<Param>> 
    makeParamsForLiftedObj(FreeNameCollection freeNames) {
		// TODO: need to figure out shadowed self via FnRef
		// need to box any var that's mutabl  
		Node currentScope = scopeStack.get(0);
        TypeEnv typeEnv = typeEnvAtNode.get(
                new Pair<Node,Span>(currentScope, currentScope.getSpan()));
		
		Option<Type> type = null;
        NormalParam param = null;
        List<Param> params = new LinkedList<Param>();
        List<VarRef> freeVarRefs = freeNames.getFreeVarRefs();
        List<FnRef> freeFnRefs = freeNames.getFreeFnRefs();
        List<FnRef> freeMethodRefs = freeNames.getFreeMethodRefs();
        // List<Id> lhsOfAssignmt = freeNames.getLhsOfAssignment();

        // FIXME: Need to handle mutated vars
        if(freeVarRefs != null) {
        	for(VarRef var : freeVarRefs) {
        		// Default value for modifier and default expression
                // FIXME: What if it has a type that's not visible at top level?
                // FIXME: what span should I use? 
        		type = typeEnv.type(var.getVar());
        	    param = new NormalParam(var.getSpan(), var.getVar(), type);
        	    
        		params.add(param);
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
        
        // Need to pass in the implicit "self" from the enclosing 
        // trait / object decl
        // FIXME: still need to write the FnRef into MethodInvocation
        if(freeMethodRefs != null) {
            // id of the enclosing trait or object
            Id enclosingId = null;
            // id of the newly created param for implicit self 
            Id enclosingParamId = null;
            // FIXME: what span should I use? 
            Span paramSpan = freeMethodRefs.get(0).getSpan();

            if(enclosingTraitOrObject == null) {
                throw new DesugarerError("enclosingTraitOrObject " + 
                    "is null when a dotted method is referenced.");
            } 
            if(enclosingTraitOrObject instanceof TraitDecl) {
            	TraitDecl cast = (TraitDecl) enclosingTraitOrObject;
                enclosingId = cast.getName();
            } else if(enclosingTraitOrObject instanceof ObjectDecl) {
            	ObjectDecl cast = (ObjectDecl) enclosingTraitOrObject;
                enclosingId = cast.getName();
            } 
            type = typeEnv.type(enclosingId);
            
            Option<APIName> inApi = Option.wrap(enclosingComponent.getName());
            enclosingParamId = new Id(paramSpan, inApi, 
                       ENCLOSING_NAME + "$" + levelOfObjExprNesting);
            param = new NormalParam(paramSpan, enclosingParamId, type);
            params.add(param);
        }
        
        return Option.<List<Param>>some(params);       
	}

    private List<StaticParam> 
    makeStaticParamsForLiftedObj(FreeNameCollection freeNames) {
		// TODO: Fill this in - get the VarTypes(?) that's free and 
        // generate static param using it
        return Collections.<StaticParam>emptyList();
	}

	private String getMangledName(ObjectExpr target) {
		String compName = NodeUtil.nameString(enclosingComponent.getName());
		String mangled = compName + "$" + nextUniqueId();
		return mangled;
	}

	private int nextUniqueId() {
		return uniqueId++;
	}


}
