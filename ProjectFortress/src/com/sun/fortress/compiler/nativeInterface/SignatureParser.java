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
    private List<String> arguments;
    private List<String> fortressArguments;
    private String result;
    private String fortressResult;
    private String signature;

    private final String prefix = "com/sun/fortress/interpreter/evaluator/values/";

        
    // For now only one dimensional arrays
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
            case 'C': 
                arguments.add("C"); 
                fortressArguments.add("L" + prefix + "FChar;");    
                index++; break;
            case 'F': 
                arguments.add("F"); 
                fortressArguments.add("L" + prefix + "FFloat;");  
                index++; 
                break;
            case 'I': 
                arguments.add("I"); 
                fortressArguments.add("L" + prefix + "FInt;");    
                index++; 
                break;
            case 'J': 
                arguments.add("J"); 
                fortressArguments.add("L" + prefix + "FLong;");    
                index++; 
                break;
            case 'Z': 
                arguments.add("Z"); 
                fortressArguments.add("L" + prefix + "FBool;"); 
                index++;
                break;
            case 'L': 
                int end = s.indexOf(';', index) + 1;
                String javaType = s.substring(index, end);
                arguments.add(javaType);
                if (javaType.equals("Ljava/lang/String;")) {
                    fortressArguments.add("L" + prefix + "FString;");
                    index = end;
                } else {
                    error(s);
                }
                break;
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
        case 'V': result = "V"; fortressResult = "L" + prefix + "FVoid;";    break;
        case 'C': result = "C"; fortressResult = "L" + prefix + "FChar;";    break;
        case 'F': result = "F"; fortressResult = "L" + prefix + "FFloat;";   break;
        case 'I': result = "I"; fortressResult = "L" + prefix + "FInt;";     break;
        case 'J': result = "J"; fortressResult = "L" + prefix + "FLong;";    break;
        case 'Z': result = "Z"; fortressResult = "L" + prefix + "FBool;";    break;
        case 'L': 
            int end = s.indexOf(';', index ) + 1;
            String javaType = s.substring(index, end);
            result = javaType;
            if (javaType.equals("Ljava/lang/String;")) {
                fortressResult = "L" + prefix + "FString;";
            } else {
                error(s);
            }
            break;
        default: error(s);
        }
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
