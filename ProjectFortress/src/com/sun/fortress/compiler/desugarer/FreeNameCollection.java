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

import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.UnitRef;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.useful.Debug;

public final class FreeNameCollection {

    private List<VarRef> freeVarRefs; // variable references
    // Don't need to worry about FieldRef actually; in the case of x.y, we will
    // get x as VarRef, and .y as FieldRef, but we just need to pass in x
    // private List<FieldRef> freeFieldRefs; // field references (i.e. x.y)
    private List<FnRef> freeFnRefs; // function references
    private List<FnRef> freeMethodRefs; // dotted method references
    private List<OpRef> freeOpRefs; // operator references
    private List<DimRef> freeDimRefs;
    private List<UnitRef> freeUnitRefs;
    private List<IntRef> freeIntRefs;
    private List<BoolRef> freeBoolRefs;
    private List<VarType> freeVarTypes; // type param
    private List<VarRef> freeMutableVarRefs;

    public final static FreeNameCollection EMPTY = new FreeNameCollection();
    private static final int DEBUG_LEVEL = 1;

    public FreeNameCollection() {
        freeVarRefs     = new LinkedList<VarRef>();
        freeFnRefs      = new LinkedList<FnRef>();
        freeMethodRefs  = new LinkedList<FnRef>();
        freeOpRefs      = new LinkedList<OpRef>();
        freeUnitRefs    = new LinkedList<UnitRef>();
        freeIntRefs     = new LinkedList<IntRef>();
        freeBoolRefs    = new LinkedList<BoolRef>();
        freeVarTypes    = new LinkedList<VarType>();
        freeMutableVarRefs = new LinkedList<VarRef>();
    }

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

        return this;
    }

    /* Make copies of all the lists; only the lists themselves are
       deep copied, but not the elements they contain */
    public FreeNameCollection makeCopy() {
        FreeNameCollection copy = new FreeNameCollection();
        copy.composeResult(this);

        return copy;
    }

    public List<VarRef> getFreeVarRefs() {
        return freeVarRefs;
    }

    public List<VarRef> getFreeMutableVarRefs() {
        return freeMutableVarRefs;
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
                this.freeMutableVarRefs.equals( other.freeMutableVarRefs ) &&
                this.freeFnRefs.equals( other.freeFnRefs ) &&
                this.freeMethodRefs.equals( other.freeMethodRefs ) &&
                this.freeOpRefs.equals( other.freeOpRefs ) &&
                this.freeDimRefs.equals( other.freeDimRefs ) &&
                this.freeUnitRefs.equals( other.freeUnitRefs ) &&
                this.freeIntRefs.equals( other.freeIntRefs ) &&
                this.freeBoolRefs.equals( other.freeBoolRefs ) &&
                this.freeVarTypes.equals( other.freeVarTypes ) );
    }

    public FreeNameCollection add(VarRef n, boolean inAssignmentLhs) {
        if(inAssignmentLhs) {
            if( freeMutableVarRefs.contains(n) == false ) {
                freeMutableVarRefs.add(n);
    	        if( freeVarRefs.contains(n) ) {
    	            freeVarRefs.remove(n);
    	        }
            }
        } else {
            if( freeVarRefs.contains(n) == false &&
                freeMutableVarRefs.contains(n) == false ) {
                freeVarRefs.add(n);
            }
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
        debugList(target.freeMutableVarRefs);
        debugList(target.freeFnRefs);
        debugList(target.freeMethodRefs);
        debugList(target.freeOpRefs);
        debugList(target.freeDimRefs);
        debugList(target.freeUnitRefs);
        debugList(target.freeIntRefs);
        debugList(target.freeBoolRefs);
        debugList(target.freeVarTypes);
    }

    public String toString() {
        String retS = "";
        if( freeVarRefs.isEmpty() == false )
            retS += "freeVarRefs: " + freeVarRefs.toString() + "\n";
        if( freeMutableVarRefs.isEmpty() == false )
            retS += "freeMutableVarRefs: " +
                    freeMutableVarRefs.toString() + "\n";
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
            retS += "freeVarTypes: " + freeVarTypes.toString() + "\nA";

        return retS;
    }

    private static <T> void debugList(List<T> list) {
        if( list.isEmpty() ) return;

        for(T elt : list) {
            Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, elt);
        }
    }

}
