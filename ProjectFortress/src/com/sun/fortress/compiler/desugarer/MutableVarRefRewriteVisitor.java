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

import java.util.List;
import java.util.Map;

import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Span;


public class MutableVarRefRewriteVisitor extends NodeUpdateVisitor {
    private Node entryNode;
    private Map<VarRef, VarRefContainer> varRefsToRewrite;

    public MutableVarRefRewriteVisitor(Node entryNode, 
                Map<VarRef, VarRefContainer> varRefsToRewrite) {
        this.entryNode = entryNode;
        this.varRefsToRewrite = varRefsToRewrite;
    }

    @Override
    public Node forObjectDecl(ObjectDecl that) {
        if( that.equals(entryNode) == false || 
            entryNode.getSpan().equals(that.getSpan()) == false ) {
            throw new DesugarerError("Wrong entry node for the rewriting " +
                "pass!  Expected: " + entryNode + " (" + entryNode.getSpan() + 
                "); " + "found: " + that + " (" + that.getSpan() + ").");
        }
        
        List<Decl> decls_result = recurOnListOfDecl( that.getDecls() );

        for( VarRefContainer container : varRefsToRewrite.values() ) {
            Node origDeclNode = container.origDeclNode();
            if( origDeclNode instanceof Param ) {
                decls_result.add( 0, container.containerField() );
            } else if( origDeclNode instanceof LValueBind ) {
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

/*
    @Override
    public Node forLocalVarDecl(LocalVarDecl that) {
        if( this.equals(entryNode) == false || 
            entryNode.getSpan().equals(that.getSpan()) == false ) {
            throw new DesugarerError("Wrong entry node for the rewriting " +
                "pass!  Expected: " + entryNode + " (" + entryNode.getSpan() + 
                "); " + "found: " + that + " (" + that.getSpan() + ").");
        }
        
        List<Decl> decls_result = recurOnListOfDecl( that.getDecls() );
        decls_result.add(containerField);

        return super.forObjectDeclOnly(that, that.getMods(), that.getName(),
                                       that.getStaticParams(),
                                       that.getExtendsClause(), that.getWhere(),
                                       that.getParams(), that.getThrowsClause(),
                                       that.getContract(), decls_result);
    } */

    @Override 
    public Node forVarRef(VarRef that) {
        Span origVarSpan = that.getSpan();
        VarRefContainer container = varRefsToRewrite.get(that);
        if( container != null ) {
            FieldRef newRef = ExprFactory.makeFieldRef( origVarSpan, 
                                    container.containerVarRef(origVarSpan), 
                                    that.getVar() );
            return newRef;
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
                List<LValueBind> lhs = cast.getLhs();
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


