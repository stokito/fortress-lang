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
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;

public final class FreeNameCollection {

	private List<VarRef> freeVarRefs;
	private List<FieldRef> freeFieldRefs;
	private List<FnRef> freeFnRefs;
	private List<OpRef> freeOpRefs;
	private List<DimRef> freeDimRefs;
	private List<IntRef> freeIntRefs;
	private List<BoolRef> freeBoolRefs;
	private List<VarType> freeVarTypes;

	public final static FreeNameCollection EMPTY = new FreeNameCollection();

	public FreeNameCollection() { }

	public FreeNameCollection composeResult(FreeNameCollection other) {
		this.freeVarRefs = composeLists(this.freeVarRefs, other.freeVarRefs);
		this.freeFieldRefs = composeLists(this.freeFieldRefs, other.freeFieldRefs);
		this.freeFnRefs = composeLists(this.freeFnRefs, other.freeFnRefs);
		this.freeOpRefs = composeLists(this.freeOpRefs, other.freeOpRefs);
		this.freeDimRefs = composeLists(this.freeDimRefs, other.freeDimRefs);
		this.freeIntRefs = composeLists(this.freeIntRefs, other.freeIntRefs);
		this.freeBoolRefs = composeLists(this.freeBoolRefs, other.freeBoolRefs);
		this.freeVarTypes = composeLists(this.freeVarTypes, other.freeVarTypes);

		return this;
	}

	public boolean equals(FreeNameCollection other) {
		if(other == null) return false;
		else {
			return( areEqualLists(freeVarRefs, other.freeVarRefs) &&
					areEqualLists(freeFieldRefs, other.freeFieldRefs) &&
					areEqualLists(freeFnRefs, other.freeFnRefs) &&
					areEqualLists(freeOpRefs, other.freeOpRefs) &&
					areEqualLists(freeDimRefs, other.freeDimRefs) &&
					areEqualLists(freeIntRefs, other.freeIntRefs) &&
					areEqualLists(freeBoolRefs, other.freeBoolRefs) &&
					areEqualLists(freeVarTypes, other.freeVarTypes) );
		}
	}

	private <T> boolean areEqualLists(List<T> thisList, List<T> otherList) {
		return thisList == null ?
				(otherList == null) : (thisList.equals(otherList));
	}

	public FreeNameCollection add(VarRef n) {
		if(freeVarRefs == null) {
			freeVarRefs = new LinkedList<VarRef>();
		}
		freeVarRefs.add(n);
		return this;
	}

	public FreeNameCollection add(FieldRef n) {
		if(freeFieldRefs == null) {
			freeFieldRefs = new LinkedList<FieldRef>();
		}
		freeFieldRefs.add(n);
		return this;
	}

	public FreeNameCollection add(FnRef n) {
		if(freeFnRefs == null) {
			freeFnRefs = new LinkedList<FnRef>();
		}
		freeFnRefs.add(n);
		return this;
	}

	public FreeNameCollection add(OpRef n) {
        if(freeOpRefs == null) {
        	freeOpRefs = new LinkedList<OpRef>();
        }
		freeOpRefs.add(n);
		return this;
	}

	public FreeNameCollection add(DimRef n) {
	    if(freeDimRefs == null) {
	    	freeDimRefs = new LinkedList<DimRef>();
	    }
		freeDimRefs.add(n);
		return this;
	}

	public FreeNameCollection add(IntRef n) {
		if(freeIntRefs == null) {
			freeIntRefs = new LinkedList<IntRef>();
		}
		freeIntRefs.add(n);
		return this;
	}

	public FreeNameCollection add(BoolRef n) {
		if(freeBoolRefs == null) {
			freeBoolRefs = new LinkedList<BoolRef>();
		}
		freeBoolRefs.add(n);
		return this;
	}

	public FreeNameCollection add(VarType n) {
		if(freeVarTypes == null) {
			freeVarTypes = new LinkedList<VarType>();
		}
		freeVarTypes.add(n);
		return this;
	}

	private <T> List<T> composeLists(List<T> thisList, List<T> otherList) {
		if(thisList != null && otherList != null) {
			thisList.addAll(otherList);
			return thisList;
		} else {
		    return thisList == null ? otherList : thisList;
		}
	}

	private static <T> void debugList(List<T> list) {
		if(list == null) return;

		// For now just print it out.  Change it to use Debug later
		for(T elt : list) {
			System.err.println(elt);
		}
	}

	public static void printDebug(FreeNameCollection target) {
		debugList(target.freeVarRefs);
		debugList(target.freeFieldRefs);
		debugList(target.freeFnRefs);
		debugList(target.freeOpRefs);
		debugList(target.freeDimRefs);
		debugList(target.freeIntRefs);
		debugList(target.freeBoolRefs);
		debugList(target.freeVarTypes);
	}

}
