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

package com.sun.fortress.interpreter.evaluator;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.exceptions.CircularDependenceError;
import com.sun.fortress.exceptions.RedefinitionError;
import com.sun.fortress.interpreter.env.IndirectionCell;
import com.sun.fortress.interpreter.env.ReferenceCell;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpName;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.StringArrayIterator;
import com.sun.fortress.useful.Visitor2;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

/**
 * A BaseEnv supplies (enforces!) some overloadings that
 * every environment must support.
 */

abstract public class BaseEnv implements Environment, Iterable<String> {

    public Environment getApi(APIName a) {
        String s = NodeUtil.nameString(a);
        return getApi(s);
    }

    public Environment getApi(List<Id> ids) {
        String s = com.sun.fortress.useful.Useful.<Id>dottedList(ids);
        return getApi(s);
    }

    public Environment getApi(String s) {
        Environment e = getApiNull(s);
        if (s == null) {
            return error(errorMsg("Missing api name ", s));
        }
        return e;
    }

    public void putApi(String apiName, Environment env) {
        /* Should override in the generated top level environment */
    }
    
    public Environment extend() {
        // TODO Auto-generated method stub
        return null;    	
    }
    
    public Environment extend(Environment additions) {
        // TODO Auto-generated method stub
        return null;   	
    }

    public Environment extendAt(HasAt x) {
        // TODO Auto-generated method stub
        return null;   	    	
    }

    public void visit(Visitor2<String, FType> vt,
            Visitor2<String, Number> vn,
            Visitor2<String, Number> vi,
            Visitor2<String, FValue> vv,
            Visitor2<String, Boolean> vb) {
        // TODO Auto-generated method stub    	
    }
    
    public void visit(Visitor2<String, Object> nameCollector) {
        // TODO Auto-generated method stub    	
    }

    /** Names noted for possible future overloading */
    private String[] namesPut;
    private int namesPutCount;

    private boolean blessed = false; /* until blessed, cannot be copied */
    private boolean topLevel = false;    
    
    public boolean debug = false;
    public boolean verboseDump = false;    

    /** Where created */
    protected HasAt within;
    
    public void debugPrint(String debugString) {
        if (debug)
            System.out.println(debugString);

    }

   static public String string(FValue f1) {
        return ((FString) f1).getString();
    }

   public void bless() {
       blessed = true;
   }

   public boolean getBlessed() {
       return blessed;
   }

   public void setTopLevel() {
       topLevel = true;
   }

   public boolean isTopLevel() {
       return topLevel;
   }   

   protected void augment(final Environment additions) {
       final Visitor2<String, FType> vt = new Visitor2<String, FType>() {
           public void visit(String s, FType o) {
               putTypeRaw(s, o);
           }
       };
       final Visitor2<String, Number> vn = new Visitor2<String, Number>() {
           public void visit(String s, Number o) {
               putNatRaw(s, o);
           }
       };
       final Visitor2<String, Number> vi = new Visitor2<String, Number>() {
           public void visit(String s, Number o) {
               putIntRaw(s, o);
           }
       };
       final Visitor2<String, FValue> vv = new Visitor2<String, FValue>() {
           public void visit(String s, FValue o) {
             
                   FType ft = additions.getVarTypeNull(s);
                   if (ft != null)
                      putValueRaw(s, o, ft);
                   else 
                      putValueRaw(s, o);
           }
       };
       final Visitor2<String, Boolean> vb = new Visitor2<String, Boolean>() {
           public void visit(String s, Boolean o) {
               putBoolRaw(s, o);
           }
       };
       
       visit(vt,vn,vi,vv,vb);
       
   }   

    abstract public  Appendable dump(Appendable a) throws IOException;

    protected final void putNoShadow(String index, FValue value, String what) {
        FValue fvo = getValueRaw(index);
        if (fvo == null) {
            putValueRaw(index, value);
        } else  if (fvo instanceof IndirectionCell) {
         // TODO Need to push the generic type Result into IndirectionCell, etc.
            // This yucky code is "correct" because IndirectionCell extends FValue,
            // and "it happens to be true" that this code will never be instantiated
            // above or below FValue in the type hierarchy.
            // Strictly speaking, this might be wrong if it permits
            // = redefinition of a mutable cell (doesn't seem to).
            IndirectionCell ic = (IndirectionCell) fvo;
            if (ic instanceof ReferenceCell) {
                throw new RedefinitionError("Mutable variable", index, fvo, value);
            } else {
                ic.storeValue((FValue) value);
            }
        } else {
            throw new RedefinitionError(what, index, fvo, value);
        }
    }

    protected final  void putFunction(String index, Fcn value, String what, boolean shadowIfDifferent, boolean overloadIsOK) {
        FValue fvo = getValueRaw(index);
        if (fvo == null) {
            putValueRaw(index, value);
            noteName(index);
        } else {
                  if (fvo instanceof IndirectionCell) {
                    // TODO Need to push the generic type Result into IndirectionCell, etc.
                    // This yucky code is "correct" because IndirectionCell extends FValue,
                    // and "it happens to be true" that this code will never be instantiated
                    // above or below FValue in the type hierarchy.
                    // Strictly speaking, this might be wrong if it permits
                    // = redefinition of a mutable cell (doesn't seem to).
                    IndirectionCell ic = (IndirectionCell) fvo;
                    if (ic instanceof ReferenceCell) {
                        throw new RedefinitionError("Mutable variable", index, fvo, value);
                    } else if (! ic.isInitialized()) {
                        ic.storeValue((FValue) value);
                        return;
                    } else {
                        // ic is an initialized value cell, not a true function.
                        // do not overload.
                        throw new RedefinitionError(what, index, fvo, value);
                    }
                }

                /* ic is a function, do an overloading on it.
                 * Because of wholesale symbol import via linking,
                 * it is possible to combine a pair of overloadings.
                 *
                 * This is all going to get simpler in the future,
                 * when overloading gets more complicated (allowing mixed
                 * generic and non-generic overloading).
                 */

                if (!(fvo instanceof Fcn))
                    System.err.println("Eek!");
                Fcn fcn_fvo = (Fcn) fvo;
                if (! shadowIfDifferent && // true for functional methods
                    fcn_fvo.getWithin() != value.getWithin() &&
                    ! (fcn_fvo.getWithin().isTopLevel() &&  value.getWithin().isTopLevel()) // for imports from another api
                    ) {
                    /*
                     * If defined in a different environment, shadow
                     * instead of overloading.
                     */
                    putValueRaw(index, value);
                    noteName(index);
                    return ;
                }

                /*
                 * Lots of overloading combinations
                 */
                OverloadedFunction ovl = null;
                if (fvo instanceof SingleFcn) {
                    SingleFcn gm = (SingleFcn)fvo;
                    ovl = new OverloadedFunction(gm.getFnName(), this);
                    ovl.addOverload(gm);
                    putValueRaw(index, ovl);
                    
                } else if (fvo instanceof OverloadedFunction) {
                    ovl = (OverloadedFunction)fvo;
                } else {
                    throw new RedefinitionError(what, index,
                                               fvo, value);
                }
                if (value instanceof SingleFcn) {
                    ovl.addOverload((SingleFcn) value, overloadIsOK);
                } else if (value instanceof OverloadedFunction) {
                    ovl.addOverloads((OverloadedFunction) value);
                } else {
                    error(errorMsg("Overload of ", ovl,
                                   " with inconsistent ", value));
                }
                /*
                 * The overloading occurs in the original table, unless a new overload
                 * was created (see returns of "table.add" above).
                 */
               
            } 
        
    }
 
    public void assignValue(HasAt loc, String str, FValue value) {
        FValue v = getValueRaw(str);
        if (v instanceof ReferenceCell) {
            ReferenceCell rc = (ReferenceCell) v;
            FType ft = rc.getType();
            if (ft != null) {
                if (!ft.typeMatch(value)) {
                    String m = errorMsg("Type mismatch assigning ", value, " (type ",
                                        value.type(), ") to ", str, " (type ", ft, ")");
                    error(loc, m);
                    return;
                }
            }
            rc.assignValue(value);
            return;
        }
        if (v == null)
            error(loc, this, "Cannot assign to unbound variable " + str);
        error(loc, this, "Cannot assign to immutable " + str);
    }
 
    public void storeType(HasAt loc, String str, FType f2) {
        FValue v = getValueRaw(str);
        if (v instanceof ReferenceCell) {
            ((ReferenceCell)v).storeType(f2);
            return;
        }
        if (v == null)
            error(loc, this, "Type stored to unbound variable " + str);
        error(loc, this, "Type stored to immutable variable " + str);

    }
       
    final public  Boolean getBool(String str)  {
        Boolean x = getBoolNull(str);
        if (x == null)
            return error(errorMsg("Missing boolean ", str));
        else
            return x;
    }

    abstract public  Boolean getBoolNull(String str) ;

    final public  Number getNat(String str) {
        Number x = getNatNull(str);
        if (x == null)
            return error(errorMsg("Missing nat ", str));
        else
            return x;
    }

    abstract public  Number getNatNull(String str);

    public Closure getRunClosure() {
        return (Closure) getValue("run");
    }

    final public  FType getType(NamedType q)  {
        FType x = getTypeNull(q.getName());
        if (x == null)
            {
                // System.err.println(this.toString());
                return error(errorMsg("Missing type ", q));
            }
        else
            return x;
    }
    
    final public  FType getType(Id q)  {
        FType x = getTypeNull(q);
        if (x == null)
            {
                // System.err.println(this.toString());
                return error(errorMsg("Missing type ", q));
            }
        else
            return x;
    }
    final public  FType getType(String str)  {
        FType x = getTypeNull(str);
        if (x == null)
            return error(errorMsg("Missing type ", str));
        else
            return x;
    }

    final public FType getTypeNull(Id name) {
        return getTypeNull(NodeUtil.nameString(name));
    }

    abstract public  FType getTypeNull(String str) ;    
    
//    final public  FValue getValue(FValue f1) {
//        return getValue(string(f1));
//    }

//    final public  FValue getValue(Id q)  {
//        FValue x = getValueNull(q);
//        if (x == null)
//            return error(errorMsg("Missing value ", q));
//        else
//            return x;
//    }

    final public  FValue getValue(String str) {
        FValue x = getValueNull(str);
        return getValueTail(str, x);
    }
    
    final public  FValue getValue(VarRef vr) {
        FValue x = getValueNull(vr);
        return getValueTail(vr, x);
    }
    
    final public  FValue getValue(OpRef vr) {
        FValue x = getValueNull(vr);
        return getValueTail(vr, x);
    }
    
    private FValue getValueTail(Object str, FValue x) {
        if (x == null) {
            return error(errorMsg("Missing value: ", str," in environment:\n",this));
        } else {
            return x;
        }
    }

    public FValue getValueNull(String s) {
        FValue v = getValueRaw(s);
        return getValueNullTail(s, v);
    }

    static private BASet<String> missedNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    
    final public  FValue getValueNull(VarRef vr) {
        Id name = vr.getVar();
        int l = vr.getLexicalDepth();
        return getValueNull(name, l);
    }

    final public  FValue getValueNull(OpRef vr) {
        OpName name = vr.getOriginalName();
        int l = vr.getLexicalDepth();
        return getValueNull(name, l);
    }

    final public FValue getValueNull(Id name, int l) throws CircularDependenceError {
        //String s = NodeUtil.nameString(name);
        String local = NodeUtil.nameSuffixString(name);
        Option<APIName> opt_api = name.getApi();
        return getValueNullTail(name, l, local, opt_api);
    }

      final public FValue getValueNull(OpName name, int l)
            throws CircularDependenceError {
        // String s = NodeUtil.nameString(name);
        String local = NodeUtil.nameSuffixString(name);
        Option<APIName> opt_api = name.getApi();
        return getValueNullTail(name, l, local, opt_api);

    }
    
      private FValue getValueNullTail(IdOrOpName name, int l, String local,
              Option<APIName> opt_api) throws OptionUnwrapException,
              CircularDependenceError {
          if (opt_api.isSome()) {
              if (l != TOP_LEVEL) {
                  bug("Non-top-level reference to imported " + name);
              }
              APIName api = opt_api.unwrap();
              // Circular dependence etc will be signalled in API.
              Environment api_e = getApi(api);
              return api_e.getValueNull(local);
          } else {
              FValue v = getValueRaw(local, l);
              return getValueNullTail(local, v);
          }
      }
      
    private FValue getValueNullTail(String s, FValue v)
        throws CircularDependenceError {
    if (v == null)
        return v;
    if (v instanceof IndirectionCell) {
        try {
            v = ((IndirectionCell) v).getValueNull();
        } catch (CircularDependenceError ce) {
            ce.addParticipant(s);
            throw ce;
        }
    }
    return v;
}

   final public  FType getVarType(String str) {
        FType x = getVarTypeNull(str);
        if (x == null)
            return error(errorMsg("Missing type of ", str));
        else
            return x;
    }

   public FType getVarTypeNull(String str) {
       FValue v = getValueRaw(str);
       if (v == null)
           return null;
       if (v instanceof ReferenceCell) {
           return ((ReferenceCell) v).getType();
       }
       return null;
   }
   
    public Environment installPrimitives() {
        Primitives.installPrimitives(this);
        return this;
   }
          
    public void putType(Id name, FType x) {
        putType(NodeUtil.nameString(name), x);
    }    
    
    
    final public void putValue(FValue f1, FValue f2) {
        putValue(string(f1), f2);
    }
    
    public void putValue(Id name, FValue x) {
        putValue(NodeUtil.nameString(name), x);
    }
    
    public Closure getClosure(String s) {
        return (Closure) getValue(s);
    }
    
    public void putVariable(String str, FValue f2) {
        putValue(str, new ReferenceCell(FTypeTop.ONLY, f2));
     }

    public void putVariablePlaceholder(String str) {
        putValue(str, new ReferenceCell());
     }

    public void putValueRaw(String str, FValue f2, FType ft) {
        putValueRaw(str, new ReferenceCell(ft, f2));
     }

    public void putVariable(String str, FValue f2, FType ft) {
        putValue(str, new ReferenceCell(ft, f2));
    }

    public void putVariable(String str, FType ft) {
        putValue(str, new ReferenceCell(ft));
    }

    public Environment genericLeafEnvHack(Environment genericEnv, HasAt loc) {
        return extend(genericEnv);
    }

    public void putBool(String str, Boolean f2) {
        putBoolRaw(str, f2);
        putValueRaw(str, FBool.make(f2));
   }

    public void putNat(String str, Number f2) {
        putNatRaw(str, f2);
        putValueRaw(str, FInt.make(f2.intValue()));
    }

    public void putInt(String str, Number f2) {
        putIntRaw(str, f2);
        putValueRaw(str, FInt.make(f2.intValue()));
    }

    public void putType(String str, FType f2) {
    	putTypeRaw(str,f2);
    }
    
    
    public void putValue(String str, FValue f2) {
        if (f2 instanceof Fcn)
            putFunction(str, (Fcn) f2, "Var/value", false, false);
        else
            // var_env = putNoShadow(var_env, str, f2, "Var/value");
            putNoShadow(str, f2, "Var/value");
        
     }

    public void putValueNoShadowFn(String str, FValue f2) {
        if (f2 instanceof Fcn)
            putFunction(str, (Fcn) f2, "Var/value", true, false);
        else
            // var_env = putNoShadow(var_env, str, f2, "Var/value");
            putNoShadow(str, f2, "Var/value");
     }
    
    /**
     *
     * @param str
     * @param f2
     */
    public void putFunctionalMethodInstance(String str, FValue f2) {
        if (f2 instanceof Fcn)
            putFunction(str, (Fcn) f2, "Var/value", true, true);
        else
            error(str + " must be a functional method instance ");
     }


    public void noteName(String s) {
        if (namesPutCount == 0)
            namesPut = new String[2];
        else if (namesPutCount == namesPut.length) {
            String[] next = new String[namesPutCount*2];
            System.arraycopy(namesPut, 0, next, 0, namesPut.length);
            namesPut = next;
        }
        namesPut[namesPutCount++] = s;
    }

    public Iterator<String> iterator() {
        if (namesPutCount > 0)
            return new StringArrayIterator(namesPut, namesPutCount);
       return Collections.<String>emptySet().iterator();
    }    
    
    // Slightly wrong -- returns all, not just the most recently bound.

    public HasAt getAt() {
        return within;
    }    
    
    /**
     * Level-tagged version of getTypeNull
     * 
     * @param name
     * @param level
     * @return
     */
    public FType getTypeNull(String name, int level) {
        return getTypeNull(name);
    }

    /**
     * Level-tagged version of getValueRaw
     * 
     * @param s
     * @param level
     * @return
     */
    public FValue getValueRaw(String s, int level) {
        return getValueRaw(s);
    }
 
    public Environment getApiNull(String apiName) {
    	return null;
    }
    
    public Iterable<String> youngestFrame() {
        return this;
    }

}
