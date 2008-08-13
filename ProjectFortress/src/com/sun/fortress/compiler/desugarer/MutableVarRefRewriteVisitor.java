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

import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes_util.ExprFactory;


public class MutableVarRefRewriteVisitor extends NodeUpdateVisitor {
    private Node entryNode;
    private VarDecl containerField;
    private Id containerFieldId;

    public MutableVarRefRewriteVisitor(Node entryNode, 
                                       VarDecl containerField,
                                       Id containerFieldId) {
        this.entryNode = entryNode;
        this.containerField = containerField;
        this.containerFieldId = containerFieldId;
    }

    @Override
    public Node forObjectDecl(ObjectDecl that) {
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
        FieldRef newRef = null;
        VarRef containerFieldRef = ExprFactory.makeVarRef(containerFieldId);
        newRef = ExprFactory.makeFieldRef( that.getSpan(), 
                                           containerFieldRef, that.getVar() );
        
        return newRef;
    }
    
/*
    private VarDecl makeContainerField() {
        // FIXME: is this the right span to use?
        Span span = containerObjDecl.getSpan();
        Id fieldName = NodeFactory.makeId(span, containerFieldName);
        List<LValueBind> lhs = new LinkedList<LValueBind>(); 
        Option<Type> containerType = Option.<Type>some(
                NodeFactory.makeTraitType(containerObjDecl.getName()) );

        // set the field to be immutable 
        lhs.add( new LValueBind(span, fieldName, containerType, false) );
        VarDecl field = new VarDecl( span, lhs, makeCallToContainerObj() );

        return field;
    }

    private TightJuxt makeCallToContainerObj() {
        // FIXME: is this the right span to use?
        Span span = containerObjDecl.getSpan();
        Id containerName = containerObjDecl.getName();
        List<Id> fns = new LinkedList<Id>();
        fns.add(containerName);

        List<StaticArg> staticArgs = Collections.<StaticArg>emptyList();
        FnRef fnRefToConstructor = ExprFactory.makeFnRef(span, false,
                                        containerName, fns, staticArgs);
        
        List<Expr> exprs = new LinkedList<Expr>();
        // argsToContainerObj has size greater or equal to 1; never 0
        if( argsToContainerObj.size() == 1 ) {
            exprs.add( argsToContainerObj.get(0) );
        }
        else {
            TupleExpr tuple = ExprFactory.makeTuple(span, argsToContainerObj);
            exprs.add(tuple);
        }
        exprs.add(0, fnRefToConstructor);
        
        return( ExprFactory.makeTightJuxt(span, false, exprs) );
    } */


}


