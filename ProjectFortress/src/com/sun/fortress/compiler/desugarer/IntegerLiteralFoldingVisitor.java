/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class IntegerLiteralFoldingVisitor extends NodeUpdateVisitor {
	
	public IntegerLiteralFoldingVisitor() { }
	
 @Override
   public Node forOpExprOnly(OpExpr that, ExprInfo info_result, FunctionalRef op_result, List<Expr> args_result) {
	 
	   if (that.getArgs().size()==1) 
		   if (args_result.get(0) instanceof IntLiteralExpr && op_result.toString().equals("prefix -")) 
		    	return ExprFactory.makeIntLiteralExpr(info_result, ((IntLiteralExpr)args_result.get(0)).getIntVal().negate());
	   
	   if (that.getInfo() == info_result && that.getOp() == op_result && that.getArgs() == args_result) return that;
       return new OpExpr(info_result, op_result, args_result);
   }
   
}