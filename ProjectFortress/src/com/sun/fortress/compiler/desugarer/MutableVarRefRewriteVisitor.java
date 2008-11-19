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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Span;


public class MutableVarRefRewriteVisitor extends NodeUpdateVisitor {
    // The root of the subtree in AST which we are trying to rewrite
    // This root can only be either ObjectDecl or LocalVarDecl, which can
    // declare mutable variables captured by ObjectExpr
    private Node entryNode;
    // A map mapping from VarRefs to its corresponding boxed type
    private Map<VarRef, VarRefContainer> mutableVarRefContainerMap;
    // A list of varRefs that are declared directly under entryNode and
    // need to be rewriten into its corresponding boxed type; note that
    // mutableVarRefContainerMap.keySet() is a superset of varRefsToRewrite
    private List<VarRef> varRefsToRewrite;

    public MutableVarRefRewriteVisitor(Node entryNode,
                Map<VarRef, VarRefContainer> mutableVarRefContainerMap,
                List<VarRef> varRefsToRewrite) {
        this.entryNode = entryNode;
        this.mutableVarRefContainerMap = mutableVarRefContainerMap;
        this.varRefsToRewrite = varRefsToRewrite;
    }

    @Override
    public Node forObjectDecl(ObjectDecl that) {
        if( entryNode instanceof ObjectDecl &&
            (that.equals(entryNode) == false ||
             entryNode.getSpan().equals(that.getSpan()) == false) ) {
            throw new DesugarerError("Wrong entry node for the rewriting " +
                "pass!  Expected: " + entryNode + " (" + entryNode.getSpan() +
                "); " + "found: " + that + " (" + that.getSpan() + ").");
        }

        List<Decl> decls_result = recurOnListOfDecl( that.getDecls() );

        for( VarRef var : varRefsToRewrite ) {
            VarRefContainer container = mutableVarRefContainerMap.get(var);
            if( container == null ) {
                throw new DesugarerError(var.getSpan(),
                        "VarRefContainer for " + var +
                        " is not found in map while rewriting " + that);
            }

            Node origDeclNode = container.origDeclNode();
            if( origDeclNode instanceof Param ) {
                decls_result.add( 0, container.containerField() );
            } else if( origDeclNode instanceof LValue ) {
                insertAfterOrigDecl(decls_result, container);
            } else {
                throw new DesugarerError("Unexpected type of origDeclNode!");
            }
        }

        return super.forObjectDeclOnly(that, that.getMods(), that.getName(),
                                       that.getStaticParams(),
                                       that.getExtendsClause(), that.getWhere(),
                                       that.getParams(), that.getThrowsClause(),
                                       that.getContract(), decls_result);
    }

    @Override
    public Node forLocalVarDecl(LocalVarDecl that) {
        // This is not the node that we are trying to rewrite
        if( that.equals(entryNode) == false ||
            entryNode.getSpan().equals(that.getSpan()) == false ) {
            return super.forLocalVarDecl(that);
        }

        LocalVarDecl newLocalVarDecl = null;
        List<Expr> body_result = recurOnListOfExpr( that.getBody() );

        for( VarRef var : varRefsToRewrite ) {
            VarRefContainer container = mutableVarRefContainerMap.get(var);
            if( container == null ) {
                throw new DesugarerError(var.getSpan(),
                        "VarRefContainer for " + var +
                        " is not found in map while rewriting " + that);
            }

// Why does this sanity check fail?
// It fails because this LocalVarDecl may have been rewritten if it refers
// to any var declared in ObjectDecl which is also captured by the object
// Expr it encloses - such references have been rewritten into FieldRef of
// its container type.
//            Node origDeclNode = container.origDeclNode();
//            if( origDeclNode.equals(that) == false ) {
//                throw new DesugarerError(that.getSpan(),
//                    "Unexpected node in rewriteList when rewriting " + that);
//            } else {
                if( newLocalVarDecl == null ) {
                    newLocalVarDecl =
                        container.containerLocalVarDecl(body_result);
                } else {
                    List<Expr> newBody = new ArrayList<Expr>(1);
                    newBody.add(newLocalVarDecl);
                    newLocalVarDecl =
                        container.containerLocalVarDecl(newBody);
                }
//            }
        }

        List<Expr> new_body_result = new ArrayList<Expr>(1);
        new_body_result.add(newLocalVarDecl);

        return super.forLocalVarDeclOnly( that, that.getExprType(),
                            new_body_result, that.getLhs(), that.getRhs() );
    }

    @Override
    public Node forVarRef(VarRef that) {
        if( varRefsToRewrite.contains(that) ) {
            VarRefContainer container = mutableVarRefContainerMap.get(that);
            return container.containerFieldRef( that.getSpan() );
        } else {
            return super.forVarRef(that);
        }
    }

    private void insertAfterOrigDecl(List<Decl> decl_result,
                                     VarRefContainer container) {
        int indexToInsert = 0;
        for( Decl decl : decl_result ) {
            indexToInsert = indexToInsert + 1;
            if(decl instanceof VarDecl) {
                VarDecl cast = (VarDecl) decl;
                List<LValue> lhs = cast.getLhs();
                if( lhs.contains(container.origDeclNode()) ) {
                    decl_result.add(indexToInsert, container.containerField());
                    break;
                }
            } else {
                // The decls should be in order, i.e. fields come before
                // method / function decls, so we never get to this point.
                throw new DesugarerError( "Can't find place to insert " +
                    " container decl for " + container.origDeclNode() );
            }
        }
    }


}
