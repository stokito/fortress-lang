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

    public static String dollar = "$";
    public static Character dot = '.';
    public static Character slash = '/';
    public static String openParen = "(";
    public static String closeParen = ")";
    public static String springBoard = "SpringBoard";
    public static String make = "make";
    public static String emptyString = "";

    public static String cache = ProjectProperties.BYTECODE_CACHE_DIR + slash;

    //Asm requires you to call visitMaxs for every method
    // but ignores the arguments.
    public static int ignore = 1;

    // Classes: internal names
    // (Section 2.1.2 in ASM 3.0: A Java bytecode engineering library)
    public static String internalFloat      = org.objectweb.asm.Type.getInternalName(float.class);
    public static String internalInt        = org.objectweb.asm.Type.getInternalName(int.class);
    public static String internalDouble     = org.objectweb.asm.Type.getInternalName(double.class);
    public static String internalLong       = org.objectweb.asm.Type.getInternalName(long.class);
    public static String internalBoolean    = org.objectweb.asm.Type.getInternalName(boolean.class);
    public static String internalChar       = org.objectweb.asm.Type.getInternalName(char.class);
    public static String internalObject     = org.objectweb.asm.Type.getInternalName(Object.class);
    public static String internalString     = org.objectweb.asm.Type.getInternalName(String.class);

    // Classes: type descriptors
    // (Section 2.1.3 in ASM 3.0: A Java bytecode engineering library)

    public static String internalToDesc(String type) {
        return "L" + type + ";";
    }
    public static String makeMethodDesc(String param, String result) {
        return "(" + param + ")" + result;
    }
    public static String makeMethodDesc(List<String> params, String result) {
        String desc ="(";
        for (String param : params) {
            desc = desc + param;
        }
        desc = desc + "(" + result;
        return desc;
    }
    public static String makeArrayDesc(String element) {
        return "[" + element;
    }

    public static String descFloat         = org.objectweb.asm.Type.getDescriptor(float.class);
    public static String descInt           = org.objectweb.asm.Type.getDescriptor(int.class);
    public static String descDouble        = org.objectweb.asm.Type.getDescriptor(double.class);
    public static String descLong          = org.objectweb.asm.Type.getDescriptor(long.class);
    public static String descBoolean       = org.objectweb.asm.Type.getDescriptor(boolean.class);
    public static String descChar          = org.objectweb.asm.Type.getDescriptor(char.class);
    public static String descString        = internalToDesc(internalString);
    public static String descVoid          = org.objectweb.asm.Type.getDescriptor(void.class);
    public static String stringArrayToVoid = makeMethodDesc(makeArrayDesc(descString), descVoid);
    public static String voidToVoid        = makeMethodDesc("", descVoid);

    // fortress types
    public static String fortressPackage = "fortress";
    public static String fortressAny = fortressPackage + slash + WellKnownNames.anyTypeLibrary() +
                         dollar + WellKnownNames.anyTypeName;

    // fortress interpreter types: internal names
    public static String makeFortressInternal(String type) {
        return "com/sun/fortress/compiler/runtimeValues/F" + type;
    }

    public static String internalFortressZZ32  = makeFortressInternal("ZZ32");
    public static String internalFortressZZ64  = makeFortressInternal("ZZ64");
    public static String internalFortressRR32  = makeFortressInternal("RR32");
    public static String internalFortressRR64  = makeFortressInternal("RR64");
    public static String internalFortressBool  = makeFortressInternal("Bool");
    public static String internalFortressChar  = makeFortressInternal("Char");
    public static String internalFortressString = makeFortressInternal("String");
    public static String internalFortressVoid   = makeFortressInternal("Void");

    // fortress interpreter types: type descriptors
    public static String descFortressZZ32  = internalToDesc(internalFortressZZ32);
    public static String descFortressZZ64  = internalToDesc(internalFortressZZ64);
    public static String descFortressRR32  = internalToDesc(internalFortressRR32);
    public static String descFortressRR64  = internalToDesc(internalFortressRR64);
    public static String descFortressBool  = internalToDesc(internalFortressBool);
    public static String descFortressChar  = internalToDesc(internalFortressChar);
    public static String descFortressString = internalToDesc(internalFortressString);
    public static String descFortressVoid   = internalToDesc(internalFortressVoid);

    public static String voidToFortressVoid = makeMethodDesc("", descFortressVoid);

    public static String emitFnDeclDesc(com.sun.fortress.nodes.Type domain,
                                  com.sun.fortress.nodes.Type range) {
        return makeMethodDesc(NodeUtil.isVoidType(domain) ? "" : emitDesc(domain),
                              emitDesc(range));
    }

    public static String emitDesc(com.sun.fortress.nodes.Type type) {
        return type.accept(new NodeAbstractVisitor<String>() {
            public void defaultCase(ASTNode x) {
                sayWhat( x );
            }
            public String forArrowType(ArrowType t) {
                if (NodeUtil.isVoidType(t.getDomain()))
                    return makeMethodDesc("", emitDesc(t.getRange()));
                else return makeMethodDesc(emitDesc(t.getDomain()),
                                      emitDesc(t.getRange()));
            }
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return descFortressVoid;
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
                if ( t.getName().getText().equals("String") )
                    return descFortressString;
                else if ( t.getName().getText().equals("ZZ32") )
                    return descFortressZZ32;
                else if ( t.getName().getText().equals("ZZ64") )
                    return descFortressZZ64;
                else if ( t.getName().getText().equals("RR32") )
                    return descFortressRR32;
                else if ( t.getName().getText().equals("RR64") )
                    return descFortressRR64;
                else if ( t.getName().getText().equals("Bool"))
                    return descFortressBool;
                else if ( t.getName().getText().equals("Char"))
                    return descFortressChar;
                else
                    return sayWhat( t );
            }
            });
    }

    public static String makeClassName(String packageName, String className, TraitObjectDecl t) {
        return packageName + className + Naming.dollar + NodeUtil.getName(t).getText();
    }

    public static String getJavaClassForSymbol(IdOrOp fnName) {
        Option<APIName> maybe_api = fnName.getApiName();
        String result = "fortress/";
        if (maybe_api.isSome()) {
            APIName apiName = maybe_api.unwrap();
            result = result + apiName.getText();
        }
        Debug.debug(Debug.Type.CODEGEN, 1,
                    "getJavaClassForSymbol(" + fnName +")=" + result);
        return result;
    }

    private static <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private static <T> T sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }


}
