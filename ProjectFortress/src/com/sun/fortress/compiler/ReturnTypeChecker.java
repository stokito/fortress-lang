package com.sun.fortress.compiler;

import java.util.ArrayList;
import java.util.List;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;

import edu.rice.cs.plt.collect.CollectUtil;


public class ReturnTypeChecker extends NodeDepthFirstVisitor_void {
  List<StaticError> errors;

  public List<StaticError>  getErrors(Node ast) {
      ast.accept(this);
      return errors;
  }
  
  public ReturnTypeChecker() {
      super();
      errors = new ArrayList<StaticError>();
  }

  @Override
  public void forFnDecl(FnDecl that) {
      if(that.getHeader().getReturnType().isNone()){
        errors.add(StaticError.make("The Scala Typechecker requires return types on function:" + that.getUnambiguousName(), that));
      }
  }
   

}
