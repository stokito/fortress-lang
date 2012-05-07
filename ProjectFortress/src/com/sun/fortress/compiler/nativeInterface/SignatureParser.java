/*******************************************************************************
    Copyright 2009,2010,2011 Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.nativeInterface;

import com.sun.fortress.runtimeSystem.Naming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.objectweb.asm.Opcodes;

public class SignatureParser implements Opcodes {
    /**
     * Tokenization of input signature into its single Java type elements.
     */
    private List<String> arguments;
   
    /**
     * result Java type from signature
     */
    private String result;
   
    /**
     * Input signature to constructor.
     */
    private final String signature;

    public String toString() {
        return arguments + "->" + result;
    }
    
    // private final String prefix = "com/sun/fortress/interpreter/evaluator/values/";

    public static boolean unsayable(String s) {
        if (s.charAt(0) != '(') error(s);
        int index = 1;
        int ch = s.charAt(index);

        while (ch != ')') {
            switch(ch) {
                // We can't represent bytes, shorts or arrays yet.
            case 'B': 
            case 'S': 
            case '[': return true;
            case 'F': 
            case 'D': 
            case 'C': 
            case 'I': 
            case 'J': 
            case 'Z': index++; break;
            case 'L': index = s.indexOf(';', index) + 1; break;
            default: error(s);
            }
            ch = s.charAt(index);
        }

        index++;
        ch = s.charAt(index);

        switch(ch) {
        case 'B': 
        case 'S': 
        case '[': return true;
        case 'F': 
        case 'D': 
        case 'V': 
        case 'C':
        case 'I':
        case 'J':
        case 'Z': 
        case 'L': break;
        default: return true;
        }
        return false;
    }

    /**
     * Converts s, the signature of an existing Java method, into
     * a lsit of parameter types and a return type.
     */
    public SignatureParser(String s) {
        arguments = new ArrayList<String>();
        signature = s;
        
        s = Naming.mangleMethodSignature(s);

        if (s.charAt(0) != '(') error(s);
        int index = 1;
        int ch = s.charAt(index);

        while (ch != ')') {
            switch(ch) {
            case 'B':
            case 'S': error(s);
            break; // Superfluous, but just in case you thought error returned.
            case '[': error(s);
            break; // Superfluous, but just in case you thought error returned.
            case 'C': 
            case 'D': 
            case 'F':
            case 'I': 
            case 'J': 
            case 'Z': 
            {
                String arg_desc = String.valueOf((char)ch);
                arguments.add(arg_desc);
                 
                index++;
            } break;
                 
            case 'L': 
            {
                int end = s.indexOf(';', index) + 1;
                String javaType = s.substring(index, end);
                arguments.add(Naming.demangleFortressDescriptor(javaType));
                    
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
        break; // Superfluous, but just in case you thought error returned.
        case 'S': error(s);
        break; // Superfluous, but just in case you thought error returned.
        case '[': error(s);
        break; // Superfluous, but just in case you thought error returned.
        case 'C':
        case 'D': 
        case 'F':
        case 'I':
        case 'J':
        case 'V': 
        case 'Z':   
        {
            String arg_desc = String.valueOf((char)ch);
            result = arg_desc;
        }
        break;
        
        case 'L': 
            {
                int end = s.indexOf(';', index) + 1;
                String javaType = s.substring(index, end);
                result = Naming.demangleFortressDescriptor(javaType);
            } 
            
            break;
        default: error(s);
        }
    }

  

    public List<String> getJVMArguments() { return Collections.unmodifiableList(arguments);}
    public String getJVMResult() {return result;}
  
    String getSignature() {
        return signature;
    }

    public String removeNthParameter(int i) {
        StringBuilder ret = new StringBuilder("(");
        for( int j = 0; j < this.arguments.size(); j++) 
            if (i != j) ret.append(this.arguments.get(j));
        ret.append(")");
        ret.append(this.result);
        return ret.toString();
    }
    
    public String replaceNthParameter(int i, String newDesc) {
        StringBuilder ret = new StringBuilder("(");
        for( int j = 0; j < this.arguments.size(); j++) 
            if (i != j) ret.append(this.arguments.get(j));
            else ret.append(newDesc);
        ret.append(")");
        ret.append(this.result);
        return ret.toString();
    }
    
    static void error(String s) {throw new RuntimeException("Bad Signature " + s);}

    public static int width(String oneParam) {
        char ch = oneParam.charAt(0);
        if (ch == 'D' || ch == 'J')
            return 2;
        return 1;
    }
    
    public static int asm_loadop(String oneParam) {
        char ch = oneParam.charAt(0);
        switch (ch) {
        case 'J' :
            return LLOAD;
            
        case 'F' :
            return FLOAD;

        case 'L' :
            return ALOAD;
       
        case 'D' :
            return DLOAD;
           
        default:
            return ILOAD;
        }
        
    }

    public int paramCount() {
        return arguments.size();
    }
    
}
