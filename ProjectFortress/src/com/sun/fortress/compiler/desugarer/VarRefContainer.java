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

import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierSettable;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

public class VarRefContainer {
    private VarRef origVar;
    private Node origDeclNode;
    private String uniqueSuffix;

    private static final String CONTAINER_DECL_PREFIX = "Box";
    private static final String CONTAINER_FIELD_PREFIX = "box";

    public VarRefContainer(VarRef origVar,
                           Node origDeclNode, 
                           String uniqueSuffix) {
        this.origVar = origVar;
        this.origDeclNode = origDeclNode;
        this.uniqueSuffix = uniqueSuffix;
    }

    public Id containerDeclId() {
        String name = ObjectExpressionVisitor.MANGLE_CHAR +
            CONTAINER_DECL_PREFIX + "_" + uniqueSuffix +
            "_" + origVar.getVar().getText();
        return NodeFactory.makeId( origDeclNode.getSpan(), name );
    }

    public Id containerVarId() {
        String name = ObjectExpressionVisitor.MANGLE_CHAR +
            CONTAINER_FIELD_PREFIX + "_" + uniqueSuffix +
            "_" + origVar.getVar().getText();
        return NodeFactory.makeId( origDeclNode.getSpan(), name );
    }

    public Node origDeclNode() {
        return origDeclNode;
    }

    public Option<Type> containerType() {
        Option<Type> containerType = Option.<Type>some(
                         NodeFactory.makeTraitType(containerDeclId()) );
        return containerType;
    }

    public ObjectDecl containerDecl() {
        List<Param> params = new LinkedList<Param>();
        NormalParam param = makeVarParamFromVarRef( origVar,
                                origDeclNode.getSpan(), origVar.getExprType() );
        params.add(param);

        ObjectDecl container =
            new ObjectDecl( origDeclNode.getSpan(), containerDeclId(),
                            Collections.<StaticParam>emptyList(),
                            Collections.<TraitTypeWhere>emptyList(),
                            Option.<WhereClause>none(),
                            Option.<List<Param>>some(params),
                            Collections.<Decl>emptyList() );

        return container;
    }

    public NormalParam containerTypeParam() {
        return new NormalParam( origDeclNode.getSpan(),
                                containerVarId(), containerType() );
    }

    public VarDecl containerField() {
        List<LValueBind> lhs = new LinkedList<LValueBind>();
        // set the field to be immutable
        lhs.add( new LValueBind(origDeclNode.getSpan(), containerVarId(),
                                containerType(), false) );
        VarDecl field = new VarDecl( origDeclNode.getSpan(),
                                     lhs, makeCallToContainerObj() );

        return field;
    }

    public LocalVarDecl containerLocalVarDecl(List<Expr> bodyExprs) {
        List<LValue> lhs = new LinkedList<LValue>();
        // set the field to be immutable
        lhs.add( new LValueBind(origDeclNode.getSpan(), containerVarId(),
                                containerType(), false) );
        LocalVarDecl ret = ExprFactory.makeLocalVarDecl( origDeclNode.getSpan(),
                            lhs, makeCallToContainerObj(), bodyExprs );

        return ret;
    }

    public VarRef containerVarRef(Span varRefSpan) {
        return ExprFactory.makeVarRef( varRefSpan,
                                       containerVarId(),
                                       containerType() );
    }

    private NormalParam makeVarParamFromVarRef(VarRef var,
                                               Span paramSpan,
                                               Option<Type> typeOp) {
        List<Modifier> mods = new LinkedList<Modifier>();
        mods.add( new ModifierSettable(paramSpan) );
        NormalParam param = new NormalParam(paramSpan, mods,
                                            var.getVar(), typeOp);
        return param;
    }

    private TightJuxt makeCallToContainerObj() {
        List<Id> fns = new LinkedList<Id>();
        Span origSpan = origDeclNode.getSpan();

        fns.add( containerDeclId() );
        FnRef fnRefToDecl = ExprFactory.makeFnRef( origSpan, false,
                containerDeclId(), fns, Collections.<StaticArg>emptyList() );

        List<Expr> exprs = new LinkedList<Expr>();
        exprs.add(fnRefToDecl);
        exprs.add(origVar);

        return( ExprFactory.makeTightJuxt(origSpan, false, exprs) );
    }

}
