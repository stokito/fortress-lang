/*******************************************************************************
    Copyright 2009,2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

import com.sun.fortress.runtimeSystem.Naming;

/**
 * Generates the popular variants of the class name for a
 * Fortress trait or object.
 * 
 * @author dr2chase
 */
public class ClassNameBundle {

    
    /** The name of the class. */
    public final String className;
    
    /**
     * Descriptor form of the class name.  ( L ... ; )
     */
    public final String classDesc;

    /** Template file naming convention so 
     * generic expander can locate it.
     * Same as className for non-generic.
     */
    public final String fileName;
    
    /** No static parameters;
     * the ilk of the generic.
     */
    public final String stemClassName;
    
    public final boolean isGeneric;
    
    public ClassNameBundle(String stem_class_name, String sparams_part) {
        stemClassName = stem_class_name;

        className =
            Naming.combineStemAndSparams(stemClassName, sparams_part);
            
        fileName =
            Naming.combineStemAndSparams(stemClassName, Naming.makeTemplateSParams(sparams_part));
        
        classDesc = Naming.internalToDesc(className);
        
        isGeneric = sparams_part.length() > 0;
        
    }
}