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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.typechecker.TraitTable;
import com.sun.fortress.compiler.typechecker.TypeCheckerOutput;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.compiler.typechecker.TypeEnv.BindingLookup;
import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public final class FreeNameCollector extends NodeDepthFirstVisitor_void {
    private TypeCheckerOutput typeCheckerOutput;
    // A stack keeping track of all nodes that can create new scope
    // TraitDecl, ObjectDecl, FnDecl, FnExpr, IfClause, For, LetFn, LocalVarDecl,
    // Label, Catch, Typecase, GeneratedExpr, While, and ObjectExpr
    private Stack<Node> scopeStack;
    // A stack keeping track of (potentially nested) object exprs
    private Stack<ObjectExpr> objExprStack;
    // The Trait table enclosing the object expression
    private TraitTable traitTable;
    // The TraitDecl enclosing the object expression
    private Option<TraitDecl> enclosingTraitDecl;
    // The ObjectDecl enclosing the object expression
    private Option<ObjectDecl> enclosingObjectDecl;

    private FreeNameCollection freeNames;

    /* Map
     * key: object expr
     * value: free names captured by the object expr
     */
    private Map<Span, FreeNameCollection> objExprToFreeNames;

    /*
     * Map
     * key: a pair of the name and span of ObjectDecl or LocalVarDecl
     *      where the captured mutable VarRef is declared
     *      created by FreeNameCollector.genKeyForDeclSite
     * value: list of pairs
     *        pair.first is the VarRef
     *        pair.second is the decl node where the VarRef is declared
     *            (which is either a Param, LValue, or LocalVarDecl)
     *
     * IMPORTANT: Need to use Pair of String & Span as key!
     * Span alone does not work, because the newly created nodes have the
     * same span as the original decl node that we are rewriting.
     * Node + Span is too strong, because sometimes decl nodes can nest each
     * other (i.e. LocalVarDecl), and once we rewrite the subtree, the decl
     * node corresponding to the key in this Map will change.
     */
    private Map<Pair<String,Span>, List<Pair<VarRef,Node>>> declSiteToVarRefs;
    // private Map<Span, List<Pair<ObjectExpr, VarRef>>> declSiteToVarRefs;

    /*
     * Map
     * key: pair of <Trait/ObjectDecl.getName(), VarType.getName()>
     * value: TypeParam corresponding to the varType, where it's declared.
     *     We need this info so that we don't lose the extends clauses on the
     *     TypeParam when we make the StaticParam list for the lifted ObjExpr
     *
     * Don't need this now that we are passing all static params
    private Map<Pair<Id,Id>, TypeParam> staticArgToTypeParam;
     */

    private static final int DEBUG_LEVEL = 2;
    private static final int DEBUG_LEVEL0 = 1;

    // This class is used to collect names captured by object expressions.
    // A reference is considered as free with repect to an object expression
    // (call it O), if it is not declared within the body of O, not
    // inherited by O, AND not declared in the top-level environment.
    public FreeNameCollector(TraitTable traitTable,
                             TypeCheckerOutput typeCheckerOutput) {
        this.traitTable = traitTable;
        this.typeCheckerOutput = typeCheckerOutput;
        this.scopeStack = new Stack<Node>();
        this.objExprStack = new Stack<ObjectExpr>();
        this.freeNames = new FreeNameCollection();
        this.objExprToFreeNames = new HashMap<Span, FreeNameCollection>();
        this.declSiteToVarRefs =
            new HashMap<Pair<String,Span>, List<Pair<VarRef,Node>>>();
        // this.staticArgToTypeParam = new HashMap<Pair<Id,Id>, TypeParam>();
        this.enclosingTraitDecl = Option.<TraitDecl>none();
        this.enclosingObjectDecl = Option.<ObjectDecl>none();
    }

    public Map<Span, FreeNameCollection> getObjExprToFreeNames() {
        return objExprToFreeNames;
    }

    public Map<Pair<String,Span>, List<Pair<VarRef,Node>>> getDeclSiteToVarRefs() {
        return declSiteToVarRefs;
    }

    /*
    public Map<Pair<Id,Id>, TypeParam> getStaticArgToTypeParam() {
        return staticArgToTypeParam;
    } */

    @Override
    public void forTraitDecl(TraitDecl that) {
        scopeStack.push(that);
        enclosingTraitDecl = Option.<TraitDecl>some(that);
        super.forTraitDecl(that);
    	enclosingTraitDecl = Option.<TraitDecl>none();
        scopeStack.pop();
    }

    @Override
    public void forObjectDecl(ObjectDecl that) {
        scopeStack.push(that);
        enclosingObjectDecl = Option.<ObjectDecl>some(that);
        super.forObjectDecl(that);
     	enclosingObjectDecl = Option.<ObjectDecl>none();
     	scopeStack.pop();
    }

    @Override
    public void forFnExpr(FnExpr that) {
        scopeStack.push(that);
        super.forFnExpr(that);
        scopeStack.pop();
    }

    @Override
    public void forFnDecl(FnDecl that) {
        scopeStack.push(that);
        super.forFnDecl(that);
        scopeStack.pop();
    }

    @Override
    public void forIfClause(IfClause that) {
        scopeStack.push(that);
        super.forIfClause(that);
        scopeStack.pop();
    }

    @Override
    public void forFor(For that) {
        scopeStack.push(that);
        super.forFor(that);
        scopeStack.pop();
    }

    @Override
    public void forLetFn(LetFn that) {
        scopeStack.push(that);
        super.forLetFn(that);
        scopeStack.pop();
    }

    @Override
    public void forLocalVarDecl(LocalVarDecl that) {
        scopeStack.push(that);
        super.forLocalVarDecl(that);
        scopeStack.pop();
    }

    @Override
    public void forLabel(Label that) {
        scopeStack.push(that);
        super.forLabel(that);
        scopeStack.pop();
    }

    @Override
    public void forCatch(Catch that) {
        scopeStack.push(that);
        super.forCatch(that);
        scopeStack.pop();
    }

    @Override
    public void forTypecase(Typecase that) {
        scopeStack.push(that);
        super.forTypecase(that);
        scopeStack.pop();
    }

    @Override
    public void forGeneratedExpr(GeneratedExpr that) {
        scopeStack.push(that);
        super.forGeneratedExpr(that);
        scopeStack.pop();
    }

    @Override
    public void forWhile(While that) {
        scopeStack.push(that);
        super.forWhile(that);
        scopeStack.pop();
    }

    @Override
    public void forObjectExpr(ObjectExpr that) {
        scopeStack.push(that);
        objExprStack.push(that);

        super.forObjectExpr(that);

        objExprStack.pop();
        scopeStack.pop();

        TypeEnv objExprTypeEnv = typeCheckerOutput.getTypeEnv(that);
        if( objExprTypeEnv == null ) {
            new DesugarerError( that.getSpan(), "The typeEnv associated " +
                "with node " + that + " at span " + that.getSpan() +
                " is null!" );
        }

        if( objExprStack.isEmpty() &&
            (enclosingObjectDecl.isSome() || enclosingTraitDecl.isSome()) ) {
            // use the "self" id to get the right type of the
            // enclosing object / trait decl
            Span s = (enclosingObjectDecl.isSome() ?
                          enclosingObjectDecl :
                          enclosingTraitDecl).unwrap().getSpan();
            Option<Type> type = objExprTypeEnv.type( new Id(s, "self") );
            freeNames.setEnclosingSelfType(type);
        }

        // sometimes a static param can be used in a expr context, in which
        // case, it is parsed as a VarRef; as a result, its reference may be
        // captured in two different list and is redundant.  Remove them so
        // we don't get a name collision in the lifted ObjectDecl.
        freeNames.removeStaticRefsFromFreeVarRefs(objExprTypeEnv);

        List<VarRef> mutableVars =
            filterFreeMutableVarRefs( that, freeNames.getFreeVarRefs() );
        freeNames.setFreeMutableVarRefs(mutableVars);

        objExprToFreeNames.put( that.getSpan(), freeNames.makeCopy() );

        // Update the declsToVarRefs list
        if(mutableVars != null) {
            for(VarRef var : mutableVars) {
            	Option<Node> declNodeOp =
                    objExprTypeEnv.declarationSite( var.getVar() );
        		if(declNodeOp.isNone()) {
        		    throw new DesugarerError( var.getSpan(),
                                "Decl node for " + var + " is null!" );
                }

                // the Node that corresponds to the declaration of the VarRef
                Node declNode = declNodeOp.unwrap();
                // the Node that contains the declNode, which determines the
                // scope of the declNode;
                // must be either ObjectDecl or LocalVarDecl
                Node declSite = null;

        		Debug.debug( Debug.Type.COMPILER, DEBUG_LEVEL0,
                             "decl site is: ", declNode.stringName() );

                if(declNode instanceof LocalVarDecl) {
                    declSite = declNode;
                } else if( declNode instanceof Param ||
                           declNode instanceof LValue ) {
                    if( enclosingObjectDecl.isNone() ) {
    		            throw new DesugarerError( var.getSpan(),
                            "Unexpected decl node for " + var + "; " +
                            "Decl node is: " + declNode +
                            ", and no enclosing object decl found." );
                    } else {
                        declSite = enclosingObjectDecl.unwrap();
                    }
                } else {
    		        throw new DesugarerError( var.getSpan(),
                            "Unexpected type for decl node of " + var +
                            "; Decl node is: " + declNode );
                }

                Pair<String,Span> key = genKeyForDeclSite(declSite);
                List<Pair<VarRef, Node>> refs = declSiteToVarRefs.get(key);
                Pair<VarRef,Node> varPair = new Pair<VarRef,Node>(var, declNode);
                if(refs == null) {
                    refs = new LinkedList<Pair<VarRef,Node>>();
                    refs.add(varPair);
                } else if( refs.contains(varPair) == false ) {
                    refs.add(varPair);
                }
                declSiteToVarRefs.put(key, refs);
    		}
        }

        // only reset freeNames if we are out of outer-most object expr
        if( objExprStack.isEmpty() ) {
            freeNames = new FreeNameCollection();
        }
    }

    @Override
    public void forExit(Exit that) {
	    // Not in object expression; we are done.
	    if( objExprStack.isEmpty() ) {
	        super.forExit(that);
	        return;
	    }

        forExitDoFirst(that);

        // figure out the exit label name;
        // there should be one assigned in Exit._target by the
        // ExprDisambiguator already; if it's not found, throw an error
        Option<Id> targetOp = that.getTarget();
        Id target = null;
        if( targetOp.isSome() ) {
            target = targetOp.unwrap();
        } else {
            /*
            Label innerMostLabel = null;
            ObjectExpr innerMostObjExpr = null;
            for(int i=scopeStack.size()-1; i>=0; i++) {
                Node n = scopeStack.get(i);
                if(n instanceof Label) {
                    innerMostLabel = (Label) n;
                    break;
                } else if(n instanceof ObjectExpr) {
                    innerMostObjExpr = (ObjectExpr) n;
                }
            }

            if(innerMostObjExpr == null) {
                // the label is not captured because it's defined within the
                // inner-most object expr; no need to handle this.
	            super.forExit(that);
	            return;
            }

            // this label _is_ captured
            target = innerMostLabel.getName();  */
            throw new DesugarerError( that.getSpan(),
                        "Exit target label is not disambiguated!" );
        }

        // check wither the target label is declared within obj expr or free
        Label label = null;
        ObjectExpr innerMostObjExpr = null;

        for(int i=scopeStack.size()-1; i>=0; i--) {
            Node n = scopeStack.get(i);
            if(n instanceof Label) {
                label = (Label) n;
                if( label.getName().equals(target) ) {
                    // label found before hitting the inner most obj
                    // expr, so it is not free
                    break;
                }
            } else if(n instanceof ObjectExpr) {
                // found the obj expr before finding the label, so it's free
                freeNames.add(that);
                break;
            }
        }

        recurOnOptionOfType(that.getExprType());
        recurOnOptionOfId(that.getTarget());
        recurOnOptionOfExpr(that.getReturnExpr());
        forExitOnly(that);
    }

	@Override
	public void forVarRef(VarRef that) {
	    // Not in object expression; we are done.
	    if( objExprStack.isEmpty() ) {
	        super.forVarRef(that);
	        return;
	    }

        forVarRefDoFirst(that);
        recurOnOptionOfType(that.getExprType());
        recur(that.getVar());

		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL0, "FreeNameCollector visiting ", that);

        boolean isDecledInObjExpr = isDeclaredInObjExpr(that.getVar());
        boolean isDecledAtTopLevel = isDeclaredAtTopLevel(that.getVar());
        boolean isShadowed = false;

        if(enclosingTraitDecl.isSome()) {
            isShadowed = isShadowedInNode( enclosingTraitDecl.unwrap(),
                                           that.getVar() );
        } else if(enclosingObjectDecl.isSome()) {
            isShadowed = isShadowedInNode( enclosingObjectDecl.unwrap(),
                                           that.getVar() );
        }

        // Even if the binding is found at top level, still need to check
        // for shadowing -- the ObjectDecl or TraitDecl enclosing the
        // object expr can possibly have field or method decl that shadows
        // the top level one
        if( isDecledInObjExpr == false &&
            (isDecledAtTopLevel == false || isShadowed == true) ) {
            freeNames.add(that);
        }

        forVarRefOnly(that);
	}

	@Override
	public void forFnRef(FnRef that) {
	    // Not in object expression; we are done.
        if( objExprStack.isEmpty() ) {
            super.forFnRef(that);
            return;
        }

        Id name = that.getOriginalName();
        forFnRefDoFirst(that);
        recurOnOptionOfType(that.getExprType());
        recur(name);
        recurOnListOfId(that.getFns());
        recurOnListOfStaticArg(that.getStaticArgs());

		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "FreeNameCollector visiting ", that);

        boolean isDottedMethod = isDottedMethod(that);
        boolean isDecledInObjExpr = isDeclaredInObjExpr(name);
        boolean isDecledAtTopLevel = isDeclaredAtTopLevel(name);
        boolean isShadowed = false;

        // Only a dotted method can shadow a top-level function declared with
        // the same name.  It is not shadowing if this FnRef is NOT a dotted
        // method -- functional methods (i.e. with 'self' param) are lifted
        // to the top, so by default, it's considered as isDeclaredAtTopLevel.
        if( isDottedMethod ) {
            if(enclosingTraitDecl.isSome()) {
                isShadowed = isShadowedInNode(enclosingTraitDecl.unwrap(), name);
            } else if(enclosingObjectDecl.isSome()) {
                isShadowed = isShadowedInNode(enclosingObjectDecl.unwrap(), name);
            }
        }

        // Even if the binding is found at top level, still need to check
        // for shadowing -- the ObjectDecl or TraitDecl enclosing the
        // object expr can possibly have field or method decl that shadows
        // the top level one
        if( isDecledInObjExpr == false &&
            (isDecledAtTopLevel == false || isShadowed) ) {
            freeNames.add(that, isDottedMethod);
        }

        forFnRefOnly(that);
	}

    /* FIXME: Not handling Op param right now!
	@Override
	public void forOpRef(OpRef that) {
	    // Not in object expression; we are done.
        if( objExprStack.isEmpty() ) {
            super.forOpRef(that);
            return;
        }

        forOpRefDoFirst(that);
        recurOnOptionOfType(that.getExprType());
        recur(that.getOriginalName());
        recurOnListOfOpName(that.getOps());
        recurOnListOfStaticArg(that.getStaticArgs());

        OpName op = that.getOriginalName();
        // Not handling
        if( (op instanceof Op) == false &&
            (op instanceof Enclosing) == false ) {
            throw new DesugarerError("Unexpected Op type for OpRef " + that);
        }

        boolean isDecledInObjExpr = isDeclaredInObjExpr(op);
        boolean isDecledAtTopLevel = isDeclaredAtTopLevel(op);
        boolean isShadowed = false;

        if(enclosingTraitDecl.isSome()) {
            isShadowed = isShadowedInNode(enclosingTraitDecl.unwrap(), op);
        } else if(enclosingObjectDecl.isSome()) {
            isShadowed = isShadowedInNode(enclosingObjectDecl.unwrap(), op);
        }

        // Even if the binding is found at top level, still need to check
        // for shadowing -- the ObjectDecl or TraitDecl enclosing the
        // object expr can possibly have field or method decl that shadows
        // the top level one
        if( isDecledInObjExpr == false &&
            (isDecledAtTopLevel == false || isShadowed) ) {
            freeNames.add(that);
        }

        forOpRefOnly(that);
	} */

	@Override
	public void forDimRef(DimRef that) {
	    // Not in object expression; we are done.
        if( objExprStack.isEmpty() ) {
            super.forDimRef(that);
            return;
        }

        forDimRefDoFirst(that);
        recur(that.getName());

		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "FreeNameCollector visiting ", that);

        boolean isDecledInObjExpr = isDeclaredInObjExpr(that.getName());
        boolean isDecledAtTopLevel = isDeclaredAtTopLevel(that.getName());
        boolean isShadowed = false;

        if(enclosingTraitDecl.isSome()) {
            isShadowed = isShadowedInNode( enclosingTraitDecl.unwrap(), that.getName() );
        } else if(enclosingObjectDecl.isSome()) {
            isShadowed = isShadowedInNode( enclosingObjectDecl.unwrap(), that.getName() );
        }

        // Even if the binding is found at top level, still need to check
        // for shadowing -- the ObjectDecl or TraitDecl enclosing the
        // object expr can possibly have field or method decl that shadows
        // the top level one
        if( isDecledInObjExpr == false &&
            (isDecledAtTopLevel == false || isShadowed) ) {
			// FIXME: I put this in, but the TypeEnv doesn't actually
            // contain DimRef
            freeNames.add(that);
        }

        forDimRefOnly(that);
	}

	@Override
	public void forIntRef(IntRef that) {
	    // Not in object expression; we are done.
        if( objExprStack.isEmpty() ) {
            super.forIntRef(that);
            return;
        }

        forIntRefDoFirst(that);
        recur(that.getName());

		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "FreeNameCollector visiting ", that);

        // A reference to a static param must be free - static params cannot
        // be declared at top level nor by object expression
		freeNames.add(that);

        forIntRefOnly(that);
	}

	@Override
	public void forBoolRef(BoolRef that) {
	    // Not in object expression; we are done.
        if( objExprStack.isEmpty() ) {
            super.forBoolRef(that);
            return;
        }

        forBoolRefDoFirst(that);
        recur(that.getName());

		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "FreeNameCollector visiting ", that);

        // A reference to a static param must be free - static params cannot
        // be declared at top level nor by object expression
		freeNames.add(that);

        forBoolRefOnly(that);
	}

    @Override
    public void forVarType(VarType that) {
        Id typeName = that.getName();

        // Not in object expression; we are done.
        if( objExprStack.isEmpty() ) {
            super.forVarType(that);
            return;
        }

        forVarTypeDoFirst(that);
        recur(typeName);

        Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "FreeNameCollector visiting ", that);

        // A reference to a static param must be free - static params cannot
        // be declared at top level nor by object expression
        freeNames.add(that);

        /*
         * Don't need to do this now that we are passing all static
         * params
         ObjectExpr innerMostObjExpr = objExprStack.peek();
		TypeEnv objExprTypeEnv =
                typeCheckerOutput.getTypeEnv(innerMostObjExpr);

        Id enclosingId = null;
        if(enclosingTraitDecl.isSome()){
            enclosingId = enclosingTraitDecl.unwrap().getName();
        } else if(enclosingObjectDecl.isSome()) {
            enclosingId = enclosingObjectDecl.unwrap().getName();
        } else {
            throw new DesugarerError( that.getSpan(), "VarType " +
                    that + " found outside of Trait/ObjectDecl!" );
        }

        Pair<Id,Id> key = new Pair<Id,Id>( enclosingId, typeName );
        Option<StaticParam> spOp = objExprTypeEnv.staticParam(typeName);
        if( spOp.isNone() ) {
            throw new DesugarerError( that.getSpan(), "Cannot find the "
                        + "decl site (StaticParam) of VarType " + that );
        } else if( (spOp.unwrap() instanceof TypeParam) == false ) {
            throw new DesugarerError( that.getSpan(), "Unexpected type "
                     + "for decl site of VarType " + that + " found!  "
                     + "Expected: TypeParam; found: " + spOp.unwrap() );
        } else {
            staticArgToTypeParam.put( key, (TypeParam) spOp.unwrap() );
        } */

        forVarTypeDoFirst(that);
    }

    // Given a node, generate its corresponding key for the Map
    // declSiteToVarRefs.  Note that the declSite can only be either
    // ObjectDecl type or LocalVarDecl type.
    public static Pair<String,Span> genKeyForDeclSite(Node declSite) {
        if(declSite instanceof ObjectDecl) {
            ObjectDecl cast = (ObjectDecl) declSite;
            String name = "ObjectDecl_" + cast.getName().getText();
            return new Pair<String,Span>( name, cast.getSpan() );
        } else if(declSite instanceof LocalVarDecl) {
            LocalVarDecl cast = (LocalVarDecl) declSite;
            String name = "LocalVarDecl";
            List<LValue> lhs = cast.getLhs();
            for(LValue lvalue : lhs) {
                name += ( "_" + lvalue.getName().getText() );
            }
            return new Pair<String,Span>( name, cast.getSpan() );
        } else {
            throw new DesugarerError("Unexpected node type to " +
                                     "genKeyForDeclSite: " + declSite);
        }
    }

    private boolean isDottedMethod(FnRef fnRef) {
        Span declSpan = null;
        Id declId = null;
        TraitIndex traitIndex = null;

        if( enclosingTraitDecl.isNone() && enclosingObjectDecl.isNone() ) {
            return false;
        }

        if(enclosingTraitDecl.isSome()) {
            declSpan = enclosingTraitDecl.unwrap().getSpan();
            declId = enclosingTraitDecl.unwrap().getName();
        } else if(enclosingObjectDecl.isSome()) {
            declSpan = enclosingObjectDecl.unwrap().getSpan();
            declId = enclosingObjectDecl.unwrap().getName();
        }
        traitIndex = getTraitIndexForName(declId, declSpan);

        return traitIndex.dottedMethods().containsFirst(fnRef.getOriginalName());
    }

	private boolean isDeclaredInObjExpr(IdOrOpName name) {
        ObjectExpr innerMostObjExpr = objExprStack.peek();
        Span objExprSpan = innerMostObjExpr.getSpan();
		TypeEnv objExprTypeEnv = typeCheckerOutput.getTypeEnv(innerMostObjExpr);

        if(objExprTypeEnv == null) {
            throw new DesugarerError( objExprSpan,
                "TypeEnv corresponding to Object Expr at source " +
                objExprSpan + " is not found!" );
        }

        // FIXME: do I need to check for Op or Enclosing??
        Option<BindingLookup> bindingOutside = Option.<BindingLookup>none();
		if(name instanceof Id) {
		    bindingOutside = objExprTypeEnv.binding((Id) name);
		} else if(name instanceof OpName) {
		    bindingOutside = objExprTypeEnv.binding((OpName)name);
		} else {
		    throw new DesugarerError("Querying binding from TypeEnv with" +
		                " type " + name.getClass().toString() +
		                " is not supported.");
		}

		// The objExprTypeEnv contains things visible outside of the object
        // expression.  Since we have already passed type checking, we don't
        // need to worry about undefined variable.  If the binding is not
        // found, the reference must be declared / inherited within the
        // object expression.
		if( bindingOutside.isNone() ) {
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL,
					name, " is declared in object expr at ", objExprSpan);
			return true;
		}

        // If the binding is found, however, we still need to check for
        // name shadowing -- we can have object expression declares /
        // inherites something that shadows the binding outside.  In which
        // case, check if the same name exists within vars / methods /
        // functions declared within or inherited by object expr. (i.e.
        // shadowed in object expr), consider the name declared in the object expr.
        return isShadowedInNode(innerMostObjExpr, name);
	}

    /*
	private boolean isDeclaredInObjExpr(Node idOrOpOrEnclosing) {
        ObjectExpr innerMostObjExpr = objExprStack.peek();
        Span objExprSpan =  innerMostObjExpr.getSpan();
		TypeEnv objExprTypeEnv = typeCheckerOutput.getTypeEnv(innerMostObjExpr);

        if(objExprTypeEnv == null) {
            throw new DesugarerError( objExprSpan,
                "TypeEnv corresponding to Object Expr at source " +
                objExprSpan + " is not found!" );
        }

        // There is no sense checking for the static param binding,
        // because object expr can never declare static params of its own.
        // FIXME: check with Sukyoung - do I really need to check for Op or
        // Enclosing??
		Option<BindingLookup> bindingOutside = Option.<BindingLookup>none();
		if(idOrOpOrEnclosing instanceof Id) {
		    bindingOutside = objExprTypeEnv.binding( (Id)idOrOpOrEnclosing );
		} else if(idOrOpOrEnclosing instanceof Op) {
		    bindingOutside = objExprTypeEnv.binding( (Op)idOrOpOrEnclosing );
		} else if(idOrOpOrEnclosing instanceof Enclosing) {
            bindingOutside =
                objExprTypeEnv.binding( (Enclosing)idOrOpOrEnclosing );
		} else {
		    throw new DesugarerError("Querying binding from TypeEnv with" +
		                " type " + idOrOpOrEnclosing.getClass().toString() +
		                " is not supported.");
		}

		// The objExprTypeEnv contains things visible outside of the object
        // expression.  Since we have already passed type checking, we don't
        // need to worry about undefined variable.  If the binding is not
        // found, the reference must be declared / inherited within the
        // object expression.  If it's found, however, we need to be careful
        // still -- sometimes we can have object expression declares /
        // inherites something that shadows the binding outside.  In which
        // case, check for binding within the object expression.  If a
        // binding is found, then it's not free.
		if( bindingOutside.isNone() ) {
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL,
					idOrOpOrEnclosing, " is declared in ", innerMostObjExpr);
			return true;
		}

		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, idOrOpOrEnclosing,
			        " is NOT declared in ", innerMostObjExpr);
		return false;
	} */

	private boolean isDeclaredAtTopLevel(IdOrOpName idOrOpName) {
		// The first node in this stack must be declared at a top level
		// Its corresponding environment must be the top level environment
		Node topLevelNode = scopeStack.get(0);

		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "top level node is: ", topLevelNode);
		Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "its span is: ", topLevelNode.getSpan());

		TypeEnv topLevelEnv = typeCheckerOutput.getTypeEnv(topLevelNode);

		if(topLevelEnv == null) {
		    throw new DesugarerError("TypeEnv associated with "
		            + topLevelNode + " is not found when querying for " +
                    "type info for " + idOrOpName);
		}

	    Option<BindingLookup> binding = Option.<BindingLookup>none();
	    Option<StaticParam> staticParam = Option.<StaticParam>none();

	    if(idOrOpName instanceof Id) {
	        binding = topLevelEnv.binding( (Id)idOrOpName );
	        staticParam = topLevelEnv.staticParam( (Id)idOrOpName );
	    } else if(idOrOpName instanceof OpName) {
	        binding = topLevelEnv.binding( (OpName)idOrOpName );
	    } else {
	        throw new DesugarerError("Querying binding from TypeEnv with" +
	                    " type " + idOrOpName.getClass().toString() +
	                    " is not supported.");
	    }

		if( binding.isNone() && staticParam.isNone() ) {
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL,
			        idOrOpName, " is NOT declared in top level env.");
			return false;
		}

		Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL,
		            idOrOpName, " is declared in top level env.");

		return true;
	}

    private boolean isShadowedInNode(Node enclosing, IdOrOpName idOrOpName) {
        Id name = null;
        if(idOrOpName instanceof Id) {
            name = (Id) idOrOpName;
        } else { // FIXME: Need to support Op name later ...
            throw new DesugarerError("isShadowedInNode does not support " +
                "checking name shadowing where the name is not an Id type: " +
                idOrOpName);
        }

        DecledNamesCollector collector = new DecledNamesCollector(enclosing);
        enclosing.accept(collector);

        HashSet<Id> decledNames = collector.getDecledNames();
        HashSet<Id> extendedTypeNames = collector.getExtendedTypeNames();

        if( decledNames.contains(name) ) {
            Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, name,
                " is shadowed (decl found in decl list of node ",
                enclosing, ")");
            return true;
        } else {
            TraitIndex superTraitIndex;
            for(Id superType : extendedTypeNames) {
                superTraitIndex = getTraitIndexForName(superType, name.getSpan());
                if( superTraitIndex.getters().containsKey(name) ||
                    superTraitIndex.dottedMethods().containsFirst(name) ||
                    superTraitIndex.functionalMethods().containsFirst(name) ) {
                    Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, name,
                            " is shadowed (inherited from supertype ", superType,
                            " in node ", enclosing, ")");
                    return true;
                }
            }

            Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, name,
                        " is NOT shadowed in node ", enclosing);
            return false;
        }
    }

    private List<VarRef> filterFreeMutableVarRefs(ObjectExpr objExpr,
                                                  List<VarRef> freeVarRefs) {
        List<VarRef> freeMutableVarRefs = new LinkedList<VarRef>();
        TypeEnv typeEnv = typeCheckerOutput.getTypeEnv(objExpr);

		if(typeEnv == null) {
		    throw new DesugarerError("TypeEnv associated with" +
                    " object expression at span " + objExpr.getSpan() +
                    " is not found.");
		}

        for(VarRef var : freeVarRefs) {
            Option<Boolean> isMutable = typeEnv.mutable( var.getVar() );
            if( isMutable.isNone() ) {
                throw new DesugarerError("Binding for VarRef " +
                    var + " is not found!");
            } else if( isMutable.unwrap().booleanValue() ) {
                freeMutableVarRefs.add(var);
            }
        }

        return freeMutableVarRefs;
    }

    private TraitIndex getTraitIndexForName(Id traitId, Span spanForErrorMsg) {
        TraitIndex traitIndex;
        Option<TypeConsIndex> typeConsIndex = traitTable.typeCons(traitId);

		if(typeConsIndex.isNone()) {
			throw new DesugarerError(spanForErrorMsg,
			            "TypeConsIndex for " + traitId + " is not found.");
		} else if(typeConsIndex.unwrap() instanceof TraitIndex) {
			traitIndex = (TraitIndex) typeConsIndex.unwrap();
		} else {
			throw new DesugarerError(spanForErrorMsg,
        		"TypeConsIndex for " + traitId + " is not type TraitIndex.");
		}

        return traitIndex;
    }

    private class DecledNamesCollector extends NodeDepthFirstVisitor_void {
        private Node root;
        private HashSet<Id> decledNames;
        private HashSet<Id> extendedTypeNames;

        private DecledNamesCollector(Node root) {
            if( (root instanceof TraitDecl == false) &&
                (root instanceof ObjectDecl == false) &&
                (root instanceof ObjectExpr == false) ) {
                throw new DesugarerError(root.getSpan(),
                    "DecledNamesCollector does not accept node of type " +
                    root.getClass() + " as root.");
            }

            this.root = root;
            this.decledNames = new HashSet<Id>();
            this.extendedTypeNames = new HashSet<Id>();
        }

        public HashSet<Id> getDecledNames() {
            return decledNames;
        }

        public HashSet<Id> getExtendedTypeNames() {
            return extendedTypeNames;
        }

        @Override
        public void forTraitDecl(TraitDecl that) {
            // this TraitDecl must be the root; if not, it's an error
            if(root != that) {
                throw new DesugarerError(root + " != " + that +
                    " in DecledNamesCollector.");
            }
            super.recurOnListOfTraitTypeWhere(that.getExtendsClause());
            super.recurOnListOfDecl(that.getDecls());
        }

        @Override
        public void forObjectDecl(ObjectDecl that) {
            // this ObjectDecl must be the root; if not, it's an error
            if(root != that) {
                throw new DesugarerError(root + " != " + that +
                    " in DecledNamesCollector.");
            }
            super.recurOnListOfTraitTypeWhere(that.getExtendsClause());
            super.recurOnListOfDecl(that.getDecls());
        }

        @Override
        public void forObjectExpr(ObjectExpr that) {
            // we only want the names declared in the outer-most objExpr
            // so skip the inner ones
            if(root != that) return;
            super.recurOnListOfTraitTypeWhere(that.getExtendsClause());
            super.recurOnListOfDecl(that.getDecls());
        }

        @Override
        public void forTraitTypeWhere(TraitTypeWhere that) {
            BaseType baseType = that.getType();
            if( (baseType instanceof NamedType) == false ) {
                throw new DesugarerError(that.getSpan(),
                        "Unexpected type found for TraitTypeWhere " + that +
                        " when parsing extends clauses for object expr at " +
                        root.getSpan() );
            }
            Id typeName = ((NamedType) baseType).getName();
            extendedTypeNames.add(typeName);
        }

        @Override
        public void forVarDecl(VarDecl that) {
            recurOnListOfLValue(that.getLhs());
        }

        @Override
        public void forLValue(LValue that) {
            decledNames.add(that.getName());
        }

        @Override
        public void forFnDecl(FnDecl that) {
            IdOrOpOrAnonymousName name = that.getName();
            if(name instanceof Id) {
                decledNames.add((Id) name);
            } else {
                throw new DesugarerError(that.getSpan(), "Unexpected type " +
                        "for FnDecl name " + that.getName() + " when " +
                        "when parsing decls for object expr at " +
                        root.getSpan() );
            }
        }

        @Override
        public void forDimDecl(DimDecl that) {
            decledNames.add(that.getDim());
        }

        @Override
        public void forUnitDecl(UnitDecl that) {
            for(Id name : that.getUnits()) {
                decledNames.add(name);
            }
        }
    }

    /* Will no longer be needed once the getTypeEnv from
       TypeCheckerResult is in place
	private void DebugTypeEnvAtNode(Node nodeToLookFor) {
	    int i = 0;
		Span span = nodeToLookFor.getSpan();

	    Debug.debug(Debug.Type.COMPILER,
                    DEBUG_LEVEL, "Debuggging typeEnvAtNode ... ");
        Debug.debug(Debug.Type.COMPILER,
                DEBUG_LEVEL, "Looking for node: " + nodeToLookFor);

	    for(Pair<Node,Span> n : typeEnvAtNode.keySet()) {
			i++;
			Debug.debug(Debug.Type.COMPILER,
                        DEBUG_LEVEL, "key ", i, ": ", n.first());
			Debug.debug(Debug.Type.COMPILER,
                        DEBUG_LEVEL, "\t Its span is ", n.second());
			Debug.debug(Debug.Type.COMPILER,
                        DEBUG_LEVEL, "\t Are they equal? ",
                        n.first().getSpan().equals(n.second()));
			Debug.debug(Debug.Type.COMPILER,
                        DEBUG_LEVEL, "\t It's env contains: ",
                        typeEnvAtNode.get(n));
			if(n.second().equals(span)) {
			    Debug.debug(Debug.Type.COMPILER,
			                DEBUG_LEVEL, "Node in data struct: ", n.first());
			}
		}
	} */

}
