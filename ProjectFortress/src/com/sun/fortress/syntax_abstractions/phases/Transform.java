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
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NamedTransformerDef;
import com.sun.fortress.nodes.Transformer;
import com.sun.fortress.nodes.NodeTransformer;
import com.sun.fortress.nodes.CaseTransformer;
import com.sun.fortress.nodes.CaseTransformerClause;
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

import edu.rice.cs.plt.tuple.Option;

/* replaces syntax transformations with their bodies, replacing
 * template variables along the way
 */
public class Transform extends TemplateUpdateVisitor {
    private Map<String,Transformer> transformers;
    private Map<String,Object> variables;

    private Transform( Map<String,Transformer> transformers, Map<String,Object> variables ){
        this.transformers = transformers;
        this.variables = variables;
    }

    public static Node transform( GlobalEnvironment env, Node node ){
        return node.accept( new Transform( populateTransformers(env), 
					   new HashMap<String,Object>() ) );
    }

    private static Map<String,Transformer> populateTransformers( GlobalEnvironment env ){
        final Map<String,Transformer> map = new HashMap<String,Transformer>();
        for ( ApiIndex api : env.apis().values() ){
            api.ast().accept( new NodeDepthFirstVisitor_void(){
                @Override public void forNamedTransformerDef(NamedTransformerDef that) {
                    //Debug.debug(Debug.Type.SYNTAX, 3,
                    //            "transformer " + that.getName() + " =\n" + 
                    //            FortressAstToConcrete.astToString(that.getTransformer()));
                    map.put(that.getName(), that.getTransformer());
                }
		});
        }
        return map;
    }

    private Transformer lookupTransformer( String name ){
        if ( transformers.get( name ) == null ){
            throw new MacroError( "Cannot find transformer for " + name );
        }
        return transformers.get( name );
    }

    private Node curry( String original, Map<String,Object> vars, 
			List<String> parameters ){
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
                    throw new MacroError("Parameterized template gap is not bound " +
					 "to a _CurriedTransformer, instead bound to " + 
					 binding.getClass().getName());
                }

                CurriedTransformer curried = (CurriedTransformer) binding;
                Map<String,Object> vars = new HashMap<String,Object>( curried.getVariables() );
                if ( curried.getSyntaxParameters().size() != params.size() ){
                    throw new MacroError( "Passing " + params.size() +
					  " arguments to a nonterminal that accepts " + 
					  curried.getSyntaxParameters().size() + " arguments." );
                }

                Debug.debug( Debug.Type.SYNTAX, 3, 
			     "Template gap " + id.getText() + " has parameters " + params );
                for ( int i = 0; i < params.size(); i++ ){
                    Id parameter = params.get( i );
                    String name = curried.getSyntaxParameters().get( i );
                    Debug.debug( Debug.Type.SYNTAX, 3, "Adding parameter " + name );
                    vars.put( name, lookupVariable(parameter, new LinkedList<Id>()) );
                }
                // return curry((Node)binding, vars);
		Node newNode = 
		    new _SyntaxTransformationExpr(new Span(), curried.getSyntaxTransformer(), 
						  vars, new LinkedList<String>());
                return newNode.accept( this );
            }
        }
    }

    @Override public Node forTemplateGapOnly(TemplateGap that, Id gapId_result, 
					     List<Id> templateParams_result) {
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

    class TransformerEvaluator extends NodeDepthFirstVisitor<Node> {
	private Map<String,Transformer> transformers;
	private Map<String,Object> variables;

	private TransformerEvaluator(Map<String,Transformer> transformers, 
				     Map<String,Object> variables ){
	    this.transformers = transformers;
	    this.variables = variables;
	}

        private Object lookupVariable( Id name ){
            Object obj = variables.get( name.getText() );
            if ( obj == null ){
                throw new MacroError( "Can't find a binding for gap " + name );
            } else if ( obj instanceof CurriedTransformer ){
                throw new MacroError( name + " cannot accept parameters in a case expression" );
            }
            return obj;
        }

	@Override public Node forNodeTransformer(NodeTransformer that) {
	    return that.getNode().accept(new Transform(transformers, variables));
	}

	@Override public Node forCaseTransformer(CaseTransformer that) {
	    Id gapName = that.getGapName();
	    List<CaseTransformerClause> clauses = that.getClauses();

	    // Object toMatch = lookupVariable(gapName, new LinkedList<Id>());
	    Object toMatch = lookupVariable(gapName);
	    for (CaseTransformerClause clause : clauses) {
		Option<Node> result = matchClause(clause, toMatch);
		if (result.isSome()) {
		    return result.unwrap();
		}
		// else continue;
	    }
	    // FIXME
	    throw new RuntimeException("match failed");
	}

	private Option<Node> matchClause(CaseTransformerClause clause, Object toMatch) {
	    String constructor = clause.getConstructor().getText();
	    List<Id> parameters = clause.getParameters();
	    int parameterCount = parameters.size();
	    Transformer body = clause.getBody();

	    if (!(constructor.equals("Cons") && parameterCount == 2) &&
		!(constructor.equals("Empty") && parameterCount == 0)) {
		// Nothing else implemented yet
		throw new RuntimeException("bad case transformer constructor: " + constructor);
	    }

	    if (toMatch instanceof List) {
		List<?> list = (List<?>)toMatch;
		if (constructor.equals("Cons")) {
		    if (!list.isEmpty()) {
			Object first = list.get(0);
			Object rest = list.subList(1, list.size());
			String firstParam = parameters.get(0).getText();
			String restParam = parameters.get(1).getText();
			Map<String, Object> newEnv = new HashMap<String,Object>(variables);
			newEnv.put(firstParam, first);
			newEnv.put(restParam, rest);
			return Option.wrap(body.accept(new TransformerEvaluator(transformers, newEnv)));
		    }
		} else if (constructor.equals("Empty")) {
		    if (list.isEmpty()) {
			return Option.wrap(body.accept(this));
		    }
		}
	    }
	    return Option.<Node>none();
	}
    }

    private Object traverse( Object partial ){
        if ( partial instanceof List ){
            List<Object> all = new LinkedList<Object>();
            for ( Object o : (List<?>) partial ){
                all.add( traverse( o ) );
            }
            return all;
        } else if ( partial instanceof Node ){
            return ((Node) partial).accept( this );
        }
        throw new MacroError( "Unknown object type " + partial.getClass().getName() + " value: " + partial );
    }

            // Node argument = ((Node)var.getValue()).accept(this);

    @Override public Node defaultTransformationNodeCase(_SyntaxTransformation that) {
        if ( ! that.getSyntaxParameters().isEmpty() ){
	    /* needs parameters, curry it! */
            return curry( that.getSyntaxTransformer(), 
			  that.getVariables(), 
			  that.getSyntaxParameters() );
        }
        /*
	  Debug.debug(Debug.Type.SYNTAX, 1, 
	              "Run transformation on " + that + " is " + 
		      that.getSyntaxTransformer().invoke());
	  return that.getSyntaxTransformer().invoke().accept( this );
	*/
        Debug.debug( Debug.Type.SYNTAX, 1, "Run transformation " + that.getSyntaxTransformer() );
        Transformer transformer = lookupTransformer( that.getSyntaxTransformer() );
        //Debug.debug( Debug.Type.SYNTAX, 1, 
        //             "Transformation is " + FortressAstToConcrete.astToString(transformer));
        Map<String,Object> arguments = that.getVariables();
        Map<String,Object> evaluated = new HashMap<String,Object>();
        for ( Map.Entry<String,Object> var : arguments.entrySet() ){
            String varName = var.getKey();
            // Node argument = ((Node)var.getValue()).accept(this);
            Object argument = traverse( var.getValue() );
            // checkFullyTransformed(argument);
            evaluated.put(varName, argument);
            Debug.debug( Debug.Type.SYNTAX, 3,
			 "Argument " + varName + " is " + argument);
        }

        Debug.debug( Debug.Type.SYNTAX, "Invoking transformer " + 
		     that.getSyntaxTransformer() );
        Node transformed = 
	    transformer.accept(new TransformerEvaluator(this.transformers, evaluated) );
        checkFullyTransformed(transformed);
        return transformed;
    }


    private void checkFullyTransformed(Node n) {
        n.accept(new TemplateNodeDepthFirstVisitor_void() {
                @Override public void forTemplateGapOnly(TemplateGap that) {
                    throw new MacroError("Transformation left over template gap: " + 
					 that);
                }
                @Override public void for_SyntaxTransformationOnly(_SyntaxTransformation that) {
                    throw new MacroError("Transformation left over transformation application: "
                                               + that);
                }
            });
    }

    private class CurriedTransformer implements Node {
        private String original;
        private Map<String,Object> vars;
        private List<String> parameters;

        public CurriedTransformer( String original, Map<String,Object> vars, 
				   List<String> parameters ){
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

}
