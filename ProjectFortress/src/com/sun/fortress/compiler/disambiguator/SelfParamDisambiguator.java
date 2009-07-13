/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.disambiguator;

import java.util.List;

import static com.sun.fortress.compiler.IndexBuilder.SELF_NAME;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.compiler.typechecker.TypesUtil;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.ASTNodeInfo;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitTypeHeader;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

/**
 * Replaces implicitly-typed 'self' parameters of methods with explicitly-typed
 * ones. This is now a separate visitor because it needs to occur during
 * disambiguation but before the TypeDisambiguator pass.
 * At parse-time, methods that take the 'self' parameter may not have a
 * type for that parameter. However, at disambiguation time, we can give
 * it one.<br>
 * {@code trait Foo f(self) : () end}
 * becomes
 * {@code trait Foo f(self:Foo) : () end}
 * This method will only replace parameters "down" to the next
 * object or trait declaration. When the new trait or object is declared,
 * this method will be called again. This method is guaranteed to return
 * the type of node given.
 */
public class SelfParamDisambiguator extends NodeUpdateVisitor {
    @Override
    public Node forObjectDecl(ObjectDecl that) {
        // Add a type to self parameters of methods
        Type self_type = NodeFactory.makeTraitType(NodeUtil.getName(that),
                                                   TypeEnv.staticParamsToArgs(NodeUtil.getStaticParams(that)));
        ObjectDecl temp = (ObjectDecl)this.replaceSelfParamsWithType(that, self_type);
        ObjectDecl that_new = new ObjectDecl(temp.getInfo(), temp.getHeader(), temp.getParams(), Option.some(self_type));
        return super.forObjectDecl(that_new);
    }

    @Override
    public Node forTraitDecl(TraitDecl that) {
        // Add a type to self parameters of methods
        //        System.err.println("SelfParamDisambiguator " + that);
        Type self_type = NodeFactory.makeTraitType(NodeUtil.getName(that),
                                                   TypeEnv.staticParamsToArgs(NodeUtil.getStaticParams(that)));
        TraitDecl temp = (TraitDecl)this.replaceSelfParamsWithType(that, self_type);
        TraitDecl that_new = new TraitDecl(temp.getInfo(),temp.getHeader(), temp.getExcludesClause(), temp.getComprisesClause(), temp.isComprisesEllipses(), Option.some(self_type));
        return super.forTraitDecl(that_new);
    }



    @Override
    public Node forObjectExpr(ObjectExpr that) {
        // Add a type to self parameters of methods
        Type self_type = TypesUtil.getObjectExprType(that);
        ObjectExpr temp = (ObjectExpr)this.replaceSelfParamsWithType(that, self_type);
        ObjectExpr that_new = new ObjectExpr(temp.getInfo(),temp.getHeader(),Option.some(self_type));
        return super.forObjectExpr(that_new);
    }

    /**
     * Replaces Parameters whose name is 'self' with a parameter with
     * the explicit type given.
     *
     * @param thatNode
     * @param self_type
     */
    private Node replaceSelfParamsWithType(Node thatNode, final Type self_type) {

        NodeUpdateVisitor replacer = new NodeUpdateVisitor() {
            int traitNestingDepth = 0;
            @Override
                public Node forParamOnly(Param that, ASTNodeInfo info,
                                         Id name_result,
                                                 Option<Type> type_result,
                                                 Option<Expr> defaultExpr_result,
                                                 Option<Type> varargsType_result) {
                if ( ! NodeUtil.isVarargsParam(that) ) {
                    // my type is broken I need to qualify the type name
                    Option<Type> new_type;
                    if( name_result.equals(SELF_NAME) )
                        new_type = Option.some(self_type);
                    else
                        new_type = type_result;

                    return NodeFactory.makeParam(NodeUtil.getSpan(that),
                                                 that.getMods(),
                                                 that.getName(),
                                                 new_type,
                                                 that.getDefaultExpr(),
                                                 that.getVarargsType());
                } else
                    return that;
            }

            // end recurrance here
            @Override public Node forObjectDecl(ObjectDecl that) {
                return (++traitNestingDepth) > 1 ? that : super.forObjectDecl(that);
            }
            @Override public Node forTraitDecl(TraitDecl that) {
                return (++traitNestingDepth) > 1 ? that : super.forTraitDecl(that);
            }
            @Override public Node forObjectExpr(ObjectExpr that) {
                return (++traitNestingDepth) > 1 ? that : super.forObjectExpr(that);
            }
        };
        return thatNode.accept(replacer);
    }
}
