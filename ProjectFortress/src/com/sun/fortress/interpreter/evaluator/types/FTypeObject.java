/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildObjectEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.List;


public class FTypeObject extends FTraitOrObject {

    volatile BetterEnv declaredMembersOf;
    volatile protected boolean membersInitialized; // initially false

    // This is here because of a refactoring to make traits and objects more alike

    Environment methodEnv;


    // names of fields
    // including both the field declarations in the object declaration
    // and the non-transient value parameters of the object constructor
    List<Id> fields = new ArrayList<Id>();
    // names of methods
    // including coercions, getters, setters, operator methods,
    // functional methods, and dotted methods
    List<IdOrOpOrAnonymousName> methods = new ArrayList<IdOrOpOrAnonymousName>();

    public FTypeObject(String name,
                       Environment env,
                       HasAt at,
                       Option<List<Param>> params,
                       List<Decl> members,
                       AbstractNode def) {
        super(name, env, at, members, def);
        this.declaredMembersOf = new BetterEnv(at);
        for (Decl v : members) {
            if (v instanceof VarDecl) {
                for (LValue lhs : ((VarDecl) v).getLhs()) {
                    fields.add(lhs.getName());
                }
                if (params.isSome()) {
                    for (Param p : params.unwrap()) {
                        fields.add(p.getName());
                    }
                }
            } else if (v instanceof FnDecl) {
                methods.add(NodeUtil.getName((FnDecl) v));
            }
        }
        cannotBeExtended = true;
    }

    public List<Id> getFieldNames() {
        return fields;
    }

    public List<IdOrOpOrAnonymousName> getMethodNames() {
        return methods;
    }

    @Override
    protected void finishInitializing() {
        Environment interior = getWithin();
        methodEnv = interior.extend();
        methodEnv.bless();

    }

    public Environment getMethodExecutionEnv() {
        if (methodEnv == null) {
            bug("Internal error, get of unset methodEnv");
        }
        return methodEnv;
    }

    protected synchronized void initializeMembers() {

        if (membersInitialized) return;
        //BetterEnv into = getMembersInternal();
        List<Decl> defs = getASTmembers();

        /* The parameters to BuildObjectEnvironment are
         * myseriously backwards-looking
         */
        BuildObjectEnvironment inner = new BuildObjectEnvironment(methodEnv, getWithin(), this, null);

        // Wish we could say this, but it doesn't work yet.
        //  new BuildObjectEnvironment(into, methodEnv, this, null);
        //methodEnv.augment(into);

        inner.doDefs1234(defs);

        // This is a minor hack to deal with messed-up object environments.
        for (Decl v : members) {
            if (v instanceof FnDecl) {
                String s = NodeUtil.nameAsMethod((FnDecl) v);//.getName().stringName();
                declaredMembersOf.putValueRaw(s, methodEnv.getLeafValue(s));
            }
        }

        membersInitialized = true;
    }

    public BetterEnv getMembers() {
        if (!membersInitialized) {
            initializeMembers();
        }
        return declaredMembersOf;
    }

    protected BetterEnv getMembersInternal() {
        return declaredMembersOf;
    }


}
