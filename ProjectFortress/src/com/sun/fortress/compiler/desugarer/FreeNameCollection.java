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

package com.sun.fortress.compiler.desugarer;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.UnitRef;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;

public final class FreeNameCollection {

    // variable references
    private List<VarRef> freeVarRefs = new LinkedList<VarRef>();
    // function references
    private List<FnRef> freeFnRefs = new LinkedList<FnRef>();
    // dotted method references
    private List<FnRef> freeMethodRefs = new LinkedList<FnRef>();
    // Functional method references are not captured
    // because they are available at top level.
    // operator references
    private List<OpRef> freeOpRefs = new LinkedList<OpRef>();
    private List<DimRef> freeDimRefs = new LinkedList<DimRef>();
    private List<UnitRef> freeUnitRefs = new LinkedList<UnitRef>();
    private List<IntRef> freeIntRefs = new LinkedList<IntRef>();
    private List<BoolRef> freeBoolRefs = new LinkedList<BoolRef>();
    // type param
    private List<VarType> freeVarTypes = new LinkedList<VarType>();

    // free variable references that are mutable - subset of freeVarRefs
    // This is set later via a call made by FreeNameCollector
    private List<VarRef> freeMutableVarRefs = new LinkedList<VarRef>();

    // This is only set via a call made by FreeNameCollector;
    // The enclosingSelfType only exists if the corresponding objectExpr is
    // enclosed by an ObjectDecl
    private Option<Type> enclosingSelfType = Option.<Type>none();

    private static final int DEBUG_LEVEL = 1;

    public final static FreeNameCollection EMPTY = new FreeNameCollection();

    /* side effect on _this_ list but not the other list */
    public FreeNameCollection composeResult(FreeNameCollection other) {
        this.freeVarRefs.addAll(other.freeVarRefs);
        this.freeFnRefs.addAll(other.freeFnRefs);
        this.freeMethodRefs.addAll(other.freeMethodRefs);
        this.freeOpRefs.addAll(other.freeOpRefs);
        this.freeDimRefs.addAll(other.freeDimRefs);
        this.freeUnitRefs.addAll(other.freeUnitRefs);
        this.freeIntRefs.addAll(other.freeIntRefs);
        this.freeBoolRefs.addAll(other.freeBoolRefs);
        this.freeVarTypes.addAll(other.freeVarTypes);

        this.freeMutableVarRefs.addAll(other.freeMutableVarRefs);
        
        if( other.enclosingSelfType.isSome() ) {
            this.enclosingSelfType = 
                Option.<Type>some( other.enclosingSelfType.unwrap() );
        }

        return this;
    }

    public boolean isMutable(VarRef var) {
        if( freeMutableVarRefs == null ) {
            throw new DesugarerError("The list freeMutableVarRefs is not set!");
        }
        return freeMutableVarRefs.contains(var); 
    } 

    public Option<Type> getEnclosingSelfType() {
        return enclosingSelfType;
    }

    public void setEnclosingSelfType(Option<Type> enclosingSelfType) {
        this.enclosingSelfType = enclosingSelfType;
    }

    /* 
     * Sometimes static params can be used in a expr context, in which
     * case, they are parsed as a VarRef.  As a result, redundant free
     * references are captured in the freeVarRefs list when they are already
     * present in the freeBoolRefs / freeIntRefs list.  Since we are 
     * already passing the static params with the StaticParam list in the 
     * lifted ObjectDecl, we don't want to repeat them in its Param list.  
     * Hence, remove these redundant references from the freeVarRefs.
     */
    public void removeStaticRefsFromFreeVarRefs(TypeEnv typeEnv) {
        // need to use a new list; can't just remove the redundant var from
        // the old list, otherwise we get a ConcurrentModificationException.
        List<VarRef> newFreeVarRefs = new LinkedList<VarRef>();

        for(VarRef var : freeVarRefs) {
            Option<StaticParam> spOp = typeEnv.staticParam( var.getVar() );
            if( spOp.isNone() ) { // it's not a static param
                newFreeVarRefs.add(var);
            } else if( spOp.unwrap() instanceof BoolParam ) {
                this.add( new BoolRef(var.getSpan(), var.getVar()) ); 
            } else if( spOp.unwrap() instanceof IntParam ) {
                this.add( new IntRef(var.getSpan(), var.getVar()) ); 
            } else if( spOp.unwrap() instanceof NatParam ) {
                this.add( new IntRef(var.getSpan(), var.getVar()) ); 
            } else {
                throw new DesugarerError( "Unexpected Static Param type " +
                    "found: " + spOp.unwrap() );
            }
        }

        this.freeVarRefs = newFreeVarRefs;
    }

    public void setFreeMutableVarRefs(List<VarRef> freeMutableVarRefs) {
        this.freeMutableVarRefs = freeMutableVarRefs;
    }

    /* 
     * Make copies of all the lists; only the lists themselves are
     *  deep copied, but not the elements they contain 
     */
    public FreeNameCollection makeCopy() {
        FreeNameCollection copy = new FreeNameCollection();
        copy.composeResult(this);

        return copy;
    }

    public List<VarRef> getFreeVarRefs() {
        return freeVarRefs;
    }

    public List<FnRef> getFreeFnRefs() {
        return freeFnRefs;
    }

    public List<FnRef> getFreeMethodRefs() {
        return freeMethodRefs;
    }

    public List<OpRef> getFreeOpRefs() {
        return freeOpRefs;
    }

    public List<DimRef> getFreeDimRefs() {
        return freeDimRefs;
    }

    public List<UnitRef> getFreeUnitRefs() {
        return freeUnitRefs;
    }

    public List<IntRef> getFreeIntRefs() {
        return freeIntRefs;
    }

    public List<BoolRef> getFreeBoolRefs() {
        return freeBoolRefs;
    }

    public List<VarType> getFreeVarTypes() {
        return freeVarTypes;
    }

    public boolean equals(FreeNameCollection other) {
        return( this.freeVarRefs.equals( other.freeVarRefs ) &&
                this.freeFnRefs.equals( other.freeFnRefs ) &&
                this.freeMethodRefs.equals( other.freeMethodRefs ) &&
                this.freeOpRefs.equals( other.freeOpRefs ) &&
                this.freeDimRefs.equals( other.freeDimRefs ) &&
                this.freeUnitRefs.equals( other.freeUnitRefs ) &&
                this.freeIntRefs.equals( other.freeIntRefs ) &&
                this.freeBoolRefs.equals( other.freeBoolRefs ) &&
                this.freeVarTypes.equals( other.freeVarTypes ) && 
                this.freeMutableVarRefs.equals( other.freeMutableVarRefs ) ); 
    }

    public FreeNameCollection add(VarRef n) {
        if( freeVarRefs.contains(n) == false ) {
            freeVarRefs.add(n);
        }

        return this;
    }

    public FreeNameCollection add(FnRef n, boolean isDottedMethod) {
        if(isDottedMethod) {
            if(freeMethodRefs.contains(n) == false) {
                freeMethodRefs.add(n);
            }
        } else {
            if(freeFnRefs.contains(n) == false) {
                freeFnRefs.add(n);
            }
        }

        return this;
    }

    public FreeNameCollection add(OpRef n) {
        if(freeOpRefs.contains(n) == false) {
            freeOpRefs.add(n);
        }
        return this;
    }

    public FreeNameCollection add(DimRef n) {
        if(freeDimRefs.contains(n) == false) {
            freeDimRefs.add(n);
        }
        return this;
    }

    public FreeNameCollection add(UnitRef n) {
        if(freeUnitRefs.contains(n) == false) {
            freeUnitRefs.add(n);
        }
        return this;
    }

    public FreeNameCollection add(IntRef n) {
        if(freeIntRefs.contains(n) == false) {
            freeIntRefs.add(n);
        }
        return this;
    }

    public FreeNameCollection add(BoolRef n) {
        if(freeBoolRefs.contains(n) == false) {
            freeBoolRefs.add(n);
        }
        return this;
    }

    public FreeNameCollection add(VarType n) {
        if(freeVarTypes.contains(n) == false) {
            freeVarTypes.add(n);
        }
        return this;
    }

    public static void printDebug(FreeNameCollection target) {
        debugList(target.freeVarRefs);
        debugList(target.freeFnRefs);
        debugList(target.freeMethodRefs);
        debugList(target.freeOpRefs);
        debugList(target.freeDimRefs);
        debugList(target.freeUnitRefs);
        debugList(target.freeIntRefs);
        debugList(target.freeBoolRefs);
        debugList(target.freeVarTypes);
        debugList(target.freeMutableVarRefs);
    }

    public String toString() {
        String retS = "";
        if( freeVarRefs.isEmpty() == false )
            retS += "freeVarRefs: " + freeVarRefs.toString() + "\n";
        if( freeFnRefs.isEmpty() == false )
            retS += "freeFnRefs: " + freeFnRefs.toString() + "\n";
        if( freeMethodRefs.isEmpty() == false )
            retS += "freeMethodRefs: " + freeMethodRefs.toString() + "\n";
        if( freeOpRefs.isEmpty() == false )
            retS += "freeOpRefs: " + freeOpRefs.toString() + "\n";
        if( freeDimRefs.isEmpty() == false )
            retS += "freeDimRefs: " + freeDimRefs.toString() + "\n";
        if( freeUnitRefs.isEmpty() == false )
            retS += "freeUnitRefs: " + freeUnitRefs.toString() + "\n";
        if( freeIntRefs.isEmpty() == false )
            retS += "freeIntRefs: " + freeIntRefs.toString() + "\n";
        if( freeBoolRefs.isEmpty() == false )
            retS += "freeBoolRefs: " + freeBoolRefs.toString() + "\n";
        if( freeVarTypes.isEmpty() == false )
            retS += "freeVarTypes: " + freeVarTypes.toString() + "\n";
        if( freeMutableVarRefs.isEmpty() == false )
            retS += "freeMutableVarRefs: " + freeMutableVarRefs.toString();

        return retS;
    }

    private static <T> void debugList(List<T> list) {
        if( list.isEmpty() ) return;
        for(T elt : list) {
            Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, elt);
        }
    }

}

