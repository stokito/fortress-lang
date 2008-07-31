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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;

import com.sun.fortress.exceptions.MacroError;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.TransformerNode;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes._SyntaxTransformation;
import com.sun.fortress.nodes._SyntaxTransformationExpr;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes.TemplateUpdateVisitor;
import com.sun.fortress.nodes.TemplateNodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeVisitor;
import com.sun.fortress.nodes.NodeVisitor_void;
import com.sun.fortress.nodes.TabPrintWriter;
import com.sun.fortress.useful.Debug;
// import com.sun.fortress.tools.FortressAstToConcrete;

/* replaces syntax transformations with their bodies, replacing
 * template variables along the way
 */
public class Transform extends TemplateUpdateVisitor {
    private Map<String,Node> transformers;
    private Map<String,Object> variables;

    private Transform( Map<String,Node> transformers, Map<String,Object> variables ){
        this.transformers = transformers;
        this.variables = variables;
    }

    private class CurriedTransformer implements Node {
        private String original;
        private Map<String,Object> vars;
        private List<String> parameters;

        public CurriedTransformer( String original, Map<String,Object> vars, List<String> parameters ){
            this.original = original;
            this.vars = vars;
            this.parameters = parameters;
        }

        public String getSyntaxTransformer(){
            return original;
        }

        public Map<String,Object> getVariables(){
            return vars;
        }

        public List<String> getSyntaxParameters(){
            return parameters;
        }

        private Object error(){
            throw new MacroError("Dont call this method");
        }

        public Span getSpan(){
            error();
            return null;
        }
        
        public String at(){
            error();
            return null;
        }

        public String stringName(){
            error();
            return null;
        }

        public <RetType> RetType accept(NodeVisitor<RetType> visitor){
            error();
            return null;
        }

        public void accept(NodeVisitor_void visitor){
            error();
        }

        public int generateHashCode(){
            error();
            return 0;
        }

        public java.lang.String serialize(){
            error();
            return null;
        }

        public void serialize(java.io.Writer writer){
            error();
        }

        public void outputHelp(TabPrintWriter writer, boolean lossless){
            error();
        }
    }
    
    public static Node transform( GlobalEnvironment env, Node node ){
        return node.accept( new Transform( populateTransformers(env), new HashMap<String,Object>() ) );
    }

    private static Map<String,Node> populateTransformers( GlobalEnvironment env ){
        final Map<String,Node> map = new HashMap<String,Node>();
        for ( ApiIndex api : env.apis().values() ){
            api.ast().accept( new NodeDepthFirstVisitor_void(){
                @Override public void forTransformerNode(TransformerNode that) {
                    //Debug.debug(Debug.Type.SYNTAX, 3,
                    //            "transformer " + that.getTransformer() + " =\n" + 
                    //            FortressAstToConcrete.astToString(that.getNode()));
                    map.put( that.getTransformer(), that.getNode() );
                }
            });
        }
        return map;
    }

    private Node lookupTransformer( String name ){
        if ( transformers.get( name ) == null ){
            throw new MacroError( "Cannot find transformer for " + name );
        }
        return transformers.get( name );
    }

    private Node curry( String original, Map<String,Object> vars, List<String> parameters ){
        return new CurriedTransformer(original, vars, parameters);
    }

    private Node lookupVariable(Id id, List<Id> params){
        String variable = id.getText();
        Object binding = this.variables.get(variable);
        if ( binding == null ){
                throw new MacroError( "Can't find a binding for gap " + id );
        } else {
            if ( params.isEmpty() ){
                return (Node)binding;
            } else {

                if ( ! (binding instanceof CurriedTransformer) ){
                    throw new MacroError( "Parameterized template gap is not bound to a _CurriedTransformer, instead bound to " + binding.getClass().getName() );
                }

                CurriedTransformer curried = (CurriedTransformer) binding;
                Map<String,Object> vars = new HashMap<String,Object>( curried.getVariables() );
                if ( curried.getSyntaxParameters().size() != params.size() ){
                    throw new MacroError( "Passing " + params.size() + " arguments to a nonterminal that accepts " + curried.getSyntaxParameters().size() + " arguments." );
                }

                Debug.debug( Debug.Type.SYNTAX, 3, "Template gap " + id.getText() + " has parameters " + params );
                for ( int i = 0; i < params.size(); i++ ){
                    Id parameter = params.get( i );
                    String name = curried.getSyntaxParameters().get( i );
                    Debug.debug( Debug.Type.SYNTAX, 3, "Adding parameter " + name );
                    vars.put( name, lookupVariable(parameter, new LinkedList<Id>()) );
                }
                // return curry((Node)binding, vars);
                return new _SyntaxTransformationExpr( new Span(), curried.getSyntaxTransformer(), vars, new LinkedList<String>() ).accept( this );
            }
        }
    }

    @Override public Node forTemplateGapOnly(TemplateGap that, Id gapId_result, List<Id> templateParams_result) {
        return lookupVariable(gapId_result, templateParams_result);
        
    }
    
    /*
    @Override public Node for_CurriedTransformer(_CurriedTransformer that) {
        AbstractNode node = that.getOriginal();
        if ( node instanceof _SyntaxTransformation ){
            _SyntaxTransformation syntax = (_SyntaxTransformation) node;
            Map<String,Object> newvars = new HashMap<String,Object>( syntax.getVariables() );
            List<String> vars = new LinkedList<String>();
            / * fill vars in the order that the transformation wants them * /

            for ( String var : vars ){
                newvars.put( var, that.getVariables().get( var ) );
            }
            return new _SyntaxTransformationExpr( new Span(), syntax.getSyntaxTransformer(), newvars ).accept( this );
        } else {
            return node;
        }
    }
    */

    @Override public Node defaultTransformationNodeCase(_SyntaxTransformation that) {
        /* needs parameters, curry it! */
        if ( ! that.getSyntaxParameters().isEmpty() ){
            return curry( that.getSyntaxTransformer(), that.getVariables(), that.getSyntaxParameters() );
        }
        /*
           Debug.debug( Debug.Type.SYNTAX, 1, "Run transformation on " + that + " is " + that.getSyntaxTransformer().invoke() );
           return that.getSyntaxTransformer().invoke().accept( this );
           */
        Debug.debug( Debug.Type.SYNTAX, 1, "Run transformation " + that.getSyntaxTransformer() );
        Node transformer = lookupTransformer( that.getSyntaxTransformer() );
        //Debug.debug( Debug.Type.SYNTAX, 1, 
        //             "Transformation is " + FortressAstToConcrete.astToString(transformer));
        Map<String,Object> arguments = that.getVariables();
        Map<String,Object> evaluated = new HashMap<String,Object>();
        for ( Map.Entry<String,Object> var : arguments.entrySet() ){
            String varName = var.getKey();
            Node argument = ((Node)var.getValue()).accept(this);
            // checkFullyTransformed(argument);
            evaluated.put(varName, argument);
            Debug.debug( Debug.Type.SYNTAX, 3, "Argument " + varName + " is " + argument);
        }

        Debug.debug( Debug.Type.SYNTAX, "Invoking transformer " + that.getSyntaxTransformer() );
        Node transformed = transformer.accept( new Transform(this.transformers, evaluated) );
        checkFullyTransformed(transformed);
        return transformed;
        // run this recursively??
        // return that.invoke().accept( this );
    }

    private void checkFullyTransformed(Node n) {
        n.accept(new TemplateNodeDepthFirstVisitor_void() {
                @Override public void forTemplateGapOnly(TemplateGap that) {
                    throw new MacroError("Transformation left over template gap: " + that);
                }
                @Override public void for_SyntaxTransformationOnly(_SyntaxTransformation that) {
                    throw new MacroError("Transformation left over transformation application: "
                                               + that);
                }
            });
    }

}
