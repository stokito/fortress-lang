/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FunctionClosure;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

import java.io.IOException;
import java.util.List;

public class IndexedEnv implements Environment {

    public FType getTypeNull(VarType q) {
        // TODO Auto-generated method stub
        return null;
    }

    public void assignValue(HasAt loc, String str, FValue f2) {
        // TODO Auto-generated method stub

    }

    public void bless() {
        // TODO Auto-generated method stub

    }

    public void debugPrint(String debugString) {
        // TODO Auto-generated method stub

    }

    public Appendable dump(Appendable a) throws IOException {
        // TODO Auto-generated method stub
        return null;
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

    public Environment genericLeafEnvHack(Environment genericEnv) {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment getApi(APIName s) {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment getApi(List<Id> s) {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment getApi(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment getApiNull(String apiName) {
        // TODO Auto-generated method stub
        return null;
    }

    public HasAt getAt() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getBlessed() {
        // TODO Auto-generated method stub
        return false;
    }

    public Boolean getBool(String str) {
        // TODO Auto-generated method stub
        return Boolean.valueOf(false);
    }

    public Boolean getBoolNull(String s) {
        // TODO Auto-generated method stub
        return Boolean.valueOf(false);
    }

    public FunctionClosure getClosure(String toBeRun) {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment getHomeEnvironment(IdOrOpOrAnonymousName ioooan) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number getIntNull(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getLeafType(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getLeafTypeNull(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getLeafValue(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getLeafValueNull(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number getNat(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number getNatNull(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getRootType(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getRootTypeNull(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getRootValue(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getRootValueNull(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    public FunctionClosure getRunClosure() {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment getTopLevel() {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getType(Id d) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getType(VarType q) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getType(TraitType q) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getTypeNull(Id name) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getTypeNull(String name, int level) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getTypeNull(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValue(Id name, int l) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValue(Op name, int l) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValue(FunctionalRef vr) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValue(VarRef vr) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValueNull(IdOrOp name, int l) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValueNull(FunctionalRef vr) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValueNull(VarRef vr) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValueRaw(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    public FValue getValueRaw(String s, int level) {
        // TODO Auto-generated method stub
        return null;
    }

    public FType getVarTypeNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public Environment installPrimitives() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isTopLevel() {
        // TODO Auto-generated method stub
        return false;
    }

    public void noteName(String s) {
        // TODO Auto-generated method stub

    }

    public void putApi(String apiName, Environment env) {
        // TODO Auto-generated method stub

    }

    public void putBool(String str, Boolean f2) {
        // TODO Auto-generated method stub

    }

    public void putBoolRaw(String str, Boolean f2) {
        // TODO Auto-generated method stub

    }

    public void putFunctionalMethodInstance(String fndodname, FValue cl) {
        // TODO Auto-generated method stub

    }

    public void putInt(String add_as, Number cnnf) {
        // TODO Auto-generated method stub

    }

    public void putIntRaw(String str, Number f2) {
        // TODO Auto-generated method stub

    }

    public void putNat(String str, Number f2) {
        // TODO Auto-generated method stub

    }

    public void putNatRaw(String str, Number f2) {
        // TODO Auto-generated method stub

    }

    public void putType(Id d, FType x) {
        // TODO Auto-generated method stub

    }

    public void putType(String str, FType f2) {
        // TODO Auto-generated method stub

    }

    public void putTypeRaw(String str, FType f2) {
        // TODO Auto-generated method stub

    }

    public void putValue(FValue f1, FValue f2) {
        // TODO Auto-generated method stub

    }

    public void putValue(Id d, FValue x) {
        // TODO Auto-generated method stub

    }

    public void putValue(String str, FValue f2) {
        // TODO Auto-generated method stub

    }

    public void putValueNoShadowFn(String fndodname, FValue cl) {
        // TODO Auto-generated method stub

    }

    public void putValueRaw(String str, FValue f2) {
        // TODO Auto-generated method stub

    }

    public void putValueRaw(String name, FValue value, FType ft) {
        // TODO Auto-generated method stub

    }

    public void putVariable(String string, FType fvt) {
        // TODO Auto-generated method stub

    }

    public void putVariable(String s, FValue value) {
        // TODO Auto-generated method stub

    }

    public void putVariable(String str, FValue f2, FType ft) {
        // TODO Auto-generated method stub

    }

    public void putVariablePlaceholder(String sname) {
        // TODO Auto-generated method stub

    }

    public void removeType(String s) {
        // TODO Auto-generated method stub

    }

    public void removeVar(String name) {
        // TODO Auto-generated method stub

    }

    public void setTopLevel() {
        // TODO Auto-generated method stub

    }

    public void storeType(HasAt x, String sname, FType ft) {
        // TODO Auto-generated method stub

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

    public Iterable<String> youngestFrame() {
        // TODO Auto-generated method stub
        return null;
    }


}
