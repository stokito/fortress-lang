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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.List;
import java.util.ArrayList;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes._EllipsesExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.OpRef;

import com.sun.fortress.useful.Debug;

/* implementation of macros-by-example
 * Wand, Kohlbecker[87], http://portal.acm.org/citation.cfm?id=41632
 *
 * This class implements the Tau function from that paper. The parser implements Beta and D.
 */
public class EllipsesVisitor extends NodeUpdateVisitor {

    private EllipsesEnvironment env;


    public EllipsesVisitor( EllipsesEnvironment env ){
        this.env = env;
    }

    @Override
    public Node forTemplateGapExpr(TemplateGapExpr that) {
        Debug.debug( Debug.Type.SYNTAX, 2, "Replace ", that.getGapId(), " with ", env.getValue( that.getGapId() ) );
        return (Node) env.getValue( that.getGapId() );
    }
    
    @Override
    public List<Expr> recurOnListOfExpr(List<Expr> that) {
        List<Expr> accum = new java.util.ArrayList<Expr>(that.size());
        for (Expr elt : that) {
            if ( elt instanceof _EllipsesExpr ){
                accum.addAll( handleEllipses((_EllipsesExpr) elt) );
            } else {
                accum.add((Expr) recur(elt));
            }
        }
        return accum;
    }

    /* find the first length of some repeated pattern variable */
    private int findRepeatedVar( EllipsesEnvironment env ){
        for ( Id var : env.getVars() ){
            if ( env.getLevel( var ) > 0 ){
                return ((List) env.getValue( var )).size();
            }
        }
        throw new MacroError( "No repeated variables!" );
    }

    /* convert an environment into a list of environments, one for each value in the list
     * of values
     */
    private List<EllipsesEnvironment> decompose( EllipsesEnvironment env, List<Id> freeVariables ){
        List<EllipsesEnvironment> all = new ArrayList<EllipsesEnvironment>();

        int repeats = findRepeatedVar( env );
        for ( int i = 0; i < repeats; i++){
            EllipsesEnvironment newEnv = new EllipsesEnvironment();

            for ( Id var : freeVariables ){
                int level = env.getLevel( var );
                Object value = env.getValue( var );
                if ( level == 0 ){
                    newEnv.add( var, level, value );
                } else {
                    List l = (List) value;
                    newEnv.add( var, level - 1, l.get( i ) );
                }
            }

            all.add( newEnv );
        }

        return all;
    }

    private List<Id> freeVariables( Node node ){
        final List<Id> vars = new ArrayList<Id>();

        node.accept( new NodeDepthFirstVisitor_void(){
            @Override
            public void forTemplateGapExpr( TemplateGapExpr gap ){
                vars.add( gap.getGapId() );
            }
        });

        return vars;
    }

    private boolean controllable( EllipsesEnvironment env, _EllipsesExpr that ){
        for ( Id var : freeVariables( that ) ){
            if ( env.contains( var ) && env.getLevel( var ) > 0 ){
                return true;
            }
        }
        return false;
    }

    private List<Expr> handleEllipses( _EllipsesExpr that ){
        if ( controllable( env, that ) ){
            List<Expr> nodes = new ArrayList<Expr>();
            for ( EllipsesEnvironment newEnv : decompose( env, freeVariables( that.getExpr() ) ) ){
                Debug.debug( Debug.Type.SYNTAX, 2, "Decomposed env ", newEnv );
                nodes.add( (Expr) that.getExpr().accept( new EllipsesVisitor( newEnv ) ) );
            }
            return nodes;
        } else {
            throw new MacroError( "Invalid ellipses expression: " + that );
        }
    }
    
    @Override
    public Node for_EllipsesExpr(_EllipsesExpr that) {
        throw new MacroError( "Should not see an ellipses expr" );
    }
}
