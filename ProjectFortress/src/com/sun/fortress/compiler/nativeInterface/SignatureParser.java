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
package com.sun.fortress.compiler.nativeInterface;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FChar;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FBool;

/* This only handles some base types and strings.  We need to beef it up. */
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SignatureParser {
    /**
     * Tokenization of input signature into its single Java type elements.
     */
    private List<String> arguments;
    /**
     * Tokenization and double translation of input signature into the
     * Java types implementing the Fortress types corresponding to the
     * Java types in the original signature.
     */
    private List<String> fortressArguments;
    /**
     * result Java type from signature
     */
    private String result;
    /**
     * Double translation of foreign Java type to corresponding Fortress type
     * and then into imlementing Java type. 
     */
    private String fortressResult;
    /**
     * Input signature to constructor.
     */
    private final String signature;

    // private final String prefix = "com/sun/fortress/interpreter/evaluator/values/";

            
    // For now only one dimensional arrays
    /**
     * Converts s, the signature of an existing Java method, into
     * the 
     */
    SignatureParser(String s) {
        arguments = new ArrayList<String>();
        fortressArguments = new ArrayList<String>();
        signature = s;

        if (s.charAt(0) != '(') error(s);
        int index = 1;
        int ch = s.charAt(index);

        while (ch != ')') {
            switch(ch) {
            case 'B': error(s);
            case 'S': error(s);
            case '[': error(s);
            case 'D': error(s);
            break; // Superfluous, but just in case you thought error returned.
            
            case 'C': 
            case 'F': 
            case 'I': 
            case 'J': 
            case 'Z': 
            {
                String arg_desc = String.valueOf((char)ch);
                arguments.add(arg_desc);
                String desc = toImplFFFF(arg_desc);
                fortressArguments.add(desc);    
                index++;
            } break;
                 
            case 'L': 
            {
                int end = s.indexOf(';', index) + 1;
                String javaType = s.substring(index, end);
                arguments.add(javaType);
                String desc = toImplFFFF(javaType);
                fortressArguments.add(desc);    
                index = end;
            } break;
            
            default: error(s);
            }
            ch = s.charAt(index);
        }

        index++;
        ch = s.charAt(index);

        switch(ch) {
        case 'B': error(s);
        case 'S': error(s);
        case '[': error(s);
        case 'D': error(s);
        break; // Superfluous, but just in case you thought error returned.

        case 'V': 
        case 'C':
        case 'F': 
        case 'I':
        case 'J':
        case 'Z':   
        {
            String arg_desc = String.valueOf((char)ch);
            result = arg_desc;
            fortressResult = toImplFFFF(arg_desc);
        }
        break;
        
        case 'L': 
            {
                int end = s.indexOf(';', index) + 1;
                String javaType = s.substring(index, end);
                result = javaType;
                String desc = toImplFFFF(javaType);
                fortressResult = desc;    
            } 
            
            break;
        default: error(s);
        }
    }

    /**
     * converts an input, foreign, Java type descriptor into the Java type
     * descriptor for the implementation of the corresponding Fortress type.
     * 
     * The name is an abbreviation for toImplForFortressForForeign.
     * 
     * @param arg_desc
     * @return
     */
    private String toImplFFFF(String arg_desc) {
        com.sun.fortress.nodes.Type ftype = NamingCzar.only.fortressTypeForForeignJavaType(arg_desc);
        if (ftype == null)
            error("No Fortress type (yet) for foreign Java type descriptor '" + arg_desc + "'");
        String desc = NamingCzar.only.boxedImplDesc(ftype);
        if (desc == null)
            error("No Java impl type (yet) for Fortress type " + ftype + " for foreign descriptor '" + arg_desc + "'");
        return desc;
    }

    List<String> getArguments() { return arguments;}
    List<String> getFortressArguments() { return fortressArguments;}
    String getResult() {return result;}
    String getFortressResult() {return fortressResult;}

    String getFortressifiedSignature() {
        String result = "(";
        // Don't forget the commas"
        for (String s : fortressArguments) 
            result = result + s;
        result = result + ")" + fortressResult;
        return result;
    }
        
    String getSignature() {
        return signature;
    }

    void error(String s) {throw new RuntimeException("Bad Signature " + s);}

}
