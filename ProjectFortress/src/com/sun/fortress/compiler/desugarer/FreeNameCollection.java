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
	private List<IntRef> freeIntRefs;
	private List<BoolRef> freeBoolRefs;
	private List<VarType> freeVarTypes; // type param
    private List<VarRef> freeMutableVarRefs;
	
	public final static FreeNameCollection EMPTY = new FreeNameCollection();
	private static final int DEBUG_LEVEL = 1;

	public FreeNameCollection() { }

	public FreeNameCollection composeResult(FreeNameCollection other) {
		this.freeVarRefs = composeLists(this.freeVarRefs, other.freeVarRefs);
		// this.freeFieldRefs = composeLists(this.freeFieldRefs, other.freeFieldRefs);
		this.freeFnRefs = composeLists(this.freeFnRefs, other.freeFnRefs);
		this.freeMethodRefs = composeLists(this.freeMethodRefs, 
		                                   other.freeMethodRefs);
		this.freeOpRefs = composeLists(this.freeOpRefs, other.freeOpRefs);
		this.freeDimRefs = composeLists(this.freeDimRefs, other.freeDimRefs);
		this.freeIntRefs = composeLists(this.freeIntRefs, other.freeIntRefs);
		this.freeBoolRefs = composeLists(this.freeBoolRefs, other.freeBoolRefs);
		this.freeVarTypes = composeLists(this.freeVarTypes, other.freeVarTypes);
		this.freeMutableVarRefs = composeLists(this.freeMutableVarRefs, 
		                                       other.freeMutableVarRefs);

		return this;
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
		if(other == null) return false;
		else {
			return( areEqualLists(freeVarRefs, other.freeVarRefs) &&
			    areEqualLists(freeMutableVarRefs, other.freeMutableVarRefs) &&
			    // areEqualLists(freeFieldRefs, other.freeFieldRefs) &&
				areEqualLists(freeFnRefs, other.freeFnRefs) &&
				areEqualLists(freeMethodRefs, other.freeMethodRefs) &&
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

	public FreeNameCollection add(VarRef n, boolean inAssignmentLhs) {
	    if(inAssignmentLhs) {
	        if(freeMutableVarRefs == null) {
	            freeMutableVarRefs = new LinkedList<VarRef>();
	            freeMutableVarRefs.add(n);
	        } else if(freeMutableVarRefs.contains(n) == false) {
	            freeMutableVarRefs.add(n);
	        }
	        if(freeVarRefs != null && freeVarRefs.contains(n)) {
	            freeVarRefs.remove(n);
	        }
	    } else {
	        if( freeMutableVarRefs == null || (freeMutableVarRefs != null && 
	                !freeMutableVarRefs.contains(n) == false) ) {	            
	            if(freeVarRefs == null) {
	                freeVarRefs = new LinkedList<VarRef>();
	                freeVarRefs.add(n);
	            } else if(freeVarRefs.contains(n) == false) {
	                freeVarRefs.add(n);
	            }	        
	        }
	    }
		return this;
	}


//	public FreeNameCollection add(FieldRef n) {
//		if(freeFieldRefs == null) {
//			freeFieldRefs = new LinkedList<FieldRef>();
//		}
//		freeFieldRefs.add(n);
//		return this;
//	}


	public FreeNameCollection add(FnRef n, boolean isDottedMethod) {
		if(isDottedMethod) {
			if(freeMethodRefs == null) {
				freeMethodRefs = new LinkedList<FnRef>();
				freeMethodRefs.add(n);				
			} else if(freeMethodRefs.contains(n) == false) {
			    freeMethodRefs.add(n);
		    }
		} else {
			if(freeFnRefs == null) {
				freeFnRefs = new LinkedList<FnRef>();
				freeFnRefs.add(n);
			} else if(freeFnRefs.contains(n) == false) {
				freeFnRefs.add(n);
			}			
		}
		
		return this;
	}

	public FreeNameCollection add(OpRef n) {
        if(freeOpRefs == null) {
        	freeOpRefs = new LinkedList<OpRef>();
		    freeOpRefs.add(n);
        } else if(freeOpRefs.contains(n) == false) {
		    freeOpRefs.add(n);
        }
		return this;
	}

	public FreeNameCollection add(DimRef n) {
	    if(freeDimRefs == null) {
	    	freeDimRefs = new LinkedList<DimRef>();
		    freeDimRefs.add(n);
	    } else if(freeDimRefs.contains(n) == false) {
		    freeDimRefs.add(n);
        }
		return this;
	}

	public FreeNameCollection add(IntRef n) {
		if(freeIntRefs == null) {
			freeIntRefs = new LinkedList<IntRef>();
		    freeIntRefs.add(n);
		} else if(freeIntRefs.contains(n) == false) {
		    freeIntRefs.add(n);
        }
		return this;
	}

	public FreeNameCollection add(BoolRef n) {
		if(freeBoolRefs == null) {
			freeBoolRefs = new LinkedList<BoolRef>();
		    freeBoolRefs.add(n);
		} else if(freeBoolRefs.contains(n) == false) {
		    freeBoolRefs.add(n);
        }
		return this;
	}

	public FreeNameCollection add(VarType n) {
		if(freeVarTypes == null) {
			freeVarTypes = new LinkedList<VarType>();
		    freeVarTypes.add(n);
		} else if(freeVarTypes.contains(n) == false) {
		    freeVarTypes.add(n);
        }
		return this;
	}

	private <T> List<T> composeLists(List<T> thisList, List<T> otherList) {
		if(thisList != null && otherList != null) {
		    for(T elt : otherList) {
		        if(thisList.contains(elt) == false) {
		           thisList.add(elt); 
		        }
		    }
			return thisList;
		} else {
		    return thisList == null ? otherList : thisList;
		}
	}
	
	private static <T> void debugList(List<T> list) {
		if(list == null) return;

		// For now just print it out.  Change it to use Debug later
		for(T elt : list) {
			Debug.debug(Debug.Type.COMPILER, DEBUG_LEVEL, elt);
		}
	}

	public static void printDebug(FreeNameCollection target) {
		debugList(target.freeVarRefs);
		debugList(target.freeMutableVarRefs);
		// debugList(target.freeFieldRefs);
		debugList(target.freeFnRefs);
		debugList(target.freeMethodRefs);
		debugList(target.freeOpRefs);
		debugList(target.freeDimRefs);
		debugList(target.freeIntRefs);
		debugList(target.freeBoolRefs);
		debugList(target.freeVarTypes);
	}
	
	public String toString() {
		String retS = "";
		if(freeVarRefs != null) 
		    retS += "freeVarRefs: " + freeVarRefs.toString() + "\n";
	    if(freeMutableVarRefs != null) 
	        retS += "freeMutableVarRefs: " + freeMutableVarRefs.toString() + "\n";
		// if(freeFieldRefs != null) 
		//     retS += "freeFieldRefs: " + freeFieldRefs.toString() + "\n";
		if(freeFnRefs != null) 
		    retS += "freeFnRefs: " + freeFnRefs.toString() + "\n";
		if(freeMethodRefs != null) 
		    retS += "freeMethodRefs: " + freeMethodRefs.toString() + "\n";
		if(freeOpRefs != null) 
		    retS += "freeOpRefs: " + freeOpRefs.toString() + "\n";
		if(freeDimRefs != null) 
		    retS += "freeDimRefs: " + freeDimRefs.toString() + "\n";
		if(freeIntRefs != null) 
		    retS += "freeIntRefs: " + freeIntRefs.toString() + "\n";
		if(freeBoolRefs != null) 
		    retS += "freeBoolRefs: " + freeBoolRefs.toString() + "\n";
		if(freeVarTypes != null) 
		    retS += "freeVarTypes: " + freeVarTypes.toString() + "\nA";
		
		return retS;
	}

}
