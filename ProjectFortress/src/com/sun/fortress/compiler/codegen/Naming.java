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
package com.sun.fortress.compiler.codegen;

import java.util.*;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;
import org.objectweb.asm.*;

// I'm factoring this here in preperation for moving it to namingCzar.

public class Naming extends NodeAbstractVisitor<String> {

    public static String emitFnDeclDesc(com.sun.fortress.nodes.Type domain,
                                  com.sun.fortress.nodes.Type range) {
        return NamingCzar.makeMethodDesc(
                   NodeUtil.isVoidType(domain) ? "" : emitDesc(domain),
                   emitDesc(range));
    }

    public static String emitDesc(com.sun.fortress.nodes.Type type) {
        return type.accept(new NodeAbstractVisitor<String>() {
            public void defaultCase(ASTNode x) {
                sayWhat( x );
            }
            public String forArrowType(ArrowType t) {
                if (NodeUtil.isVoidType(t.getDomain()))
                    return NamingCzar.makeMethodDesc("", emitDesc(t.getRange()));
                else return NamingCzar.makeMethodDesc(emitDesc(t.getDomain()),
                                      emitDesc(t.getRange()));
            }
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return NamingCzar.descFortressVoid;
                else {
                    if (t.getVarargs().isSome())
                        sayWhat(t, "Can't compile VarArgs yet");
                    else if (!t.getKeywords().isEmpty())
                        sayWhat(t, "Can't compile Keyword args yet");
                    else {
                        List<com.sun.fortress.nodes.Type> elements = t.getElements();
                        Iterator<com.sun.fortress.nodes.Type> it = elements.iterator();
                        String res = "";
                        while (it.hasNext()) {
                            res = res + emitDesc(it.next());
                        }
                        return res;
                    }
                    return sayWhat( t );
                }
            }
            public String forTraitType(TraitType t) {
                String result;
                if ( t.getName().getText().equals("String") )
                    result = NamingCzar.descFortressString;
                else if ( t.getName().getText().equals("ZZ32") )
                    result = NamingCzar.descFortressZZ32;
                else if ( t.getName().getText().equals("ZZ64") )
                    result = NamingCzar.descFortressZZ64;
                else if ( t.getName().getText().equals("RR32") )
                    result = NamingCzar.descFortressRR32;
                else if ( t.getName().getText().equals("RR64") )
                    result = NamingCzar.descFortressRR64;
                else if ( t.getName().getText().equals("Boolean"))
                    result = NamingCzar.descFortressBoolean;
                else if ( t.getName().getText().equals("Char"))
                    result = NamingCzar.descFortressChar;
                else {
                    Id id = t.getName();
                    Option<APIName> maybeApi = id.getApiName();
                    String name = id.getText();
                    if (maybeApi.isSome()) {
                        APIName api = maybeApi.unwrap();
                        result = "L" + api.getText()  + "$" + name + ";";
                    } else {
                        return sayWhat(t);
                    }
                }
                Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t, " = ", result);

                return result;
            }
            });
    }

     public static String makeClassName(String packageAndClassName, TraitObjectDecl t) {
        return packageAndClassName + "$" + NodeUtil.getName(t).getText();
    }

    public static String getJavaClassForSymbol(IdOrOp fnName) {
        Option<APIName> maybe_api = fnName.getApiName();
        String result = "";
        if (maybe_api.isSome()) {
            APIName apiName = maybe_api.unwrap();
            if (WellKnownNames.exportsDefaultLibrary(apiName.getText()))
                result = result + "fortress/";
            result = result + apiName.getText();
        } 
        //        result = result + fnName.getText();
        
        Debug.debug(Debug.Type.CODEGEN, 1,
                    "getJavaClassForSymbol(" + fnName +")=" + result);
        return result;
    }


    public static String getDottedMethodDesc(IdOrOp opName) {
        return "(" + NamingCzar.descFortressZZ32 + ")" + NamingCzar.descFortressString;
    }

    public static String generateTypeDescriptor(FnDecl f) {
        FnHeader h = f.getHeader();
        IdOrOpOrAnonymousName xname = h.getName();
        IdOrOp name = (IdOrOp) xname;
        List<Param> params = h.getParams();
        Option<com.sun.fortress.nodes.Type> optionReturnType = h.getReturnType();
        String desc = "(";
        for (Param p : params) {
            Id paramName = p.getName();
            Option<com.sun.fortress.nodes.Type> optionType = p.getIdType();
            if (optionType.isNone())
                sayWhat(f);
            else {
                com.sun.fortress.nodes.Type t = optionType.unwrap();
                desc = desc + emitDesc(t);
            }
        }
        desc += ")";
        desc += optionReturnType.isNone()
                    ? NamingCzar.descFortressVoid
                    : emitDesc(optionReturnType.unwrap());
        Debug.debug(Debug.Type.CODEGEN, 1, "generateTypeDescriptor", f, " = ", desc);
        return desc;
    }

    private static <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private static <T> T sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }

    // This is definitely hacky, but the one in NamingCzar doesn't do what we need.
    //  [Actually the NamingCzar works just fine.  This code is obsolete. -JWM]
    // public static String mangle(String name) {
    //     if (name == "[ ]") 
    //         return "subscript";
    //     else if (name == "<")
    //         return "lessthan";
    //     else if (name == "<=")
    //         return "lessthanequals";
    //     else if (name == "+")
    //         return "plus";
    //     else if (name == "-")
    //         return "minus";
    //     else return name;
    // }

}
