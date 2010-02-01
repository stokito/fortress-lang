/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.bytecodeoptimizer;

class Reference {
  String refName;
  int createPC;
  int initializePC;
  boolean init;
  static int count = 0;
  int id;

  Reference(String s, boolean b) {
    refName = s;
    init = b;
    createPC = 0;
    initializePC = 0;
    id = count++;
  }

  Reference(String s, boolean b, int create) {
    refName = s;
    init = b;
    createPC = create;
    initializePC = 0;
    id = count++;
  }

  Reference(String s, boolean b, int create, int initialize) {
    refName = s;
    init = b;
    createPC = create;
    initializePC = initialize;
    id = count++;
  }
  
  void initialize(int pc) {init = true; initializePC = pc;}

  String asString() {
    if (init == true) 
      return new String(refName + " created " + createPC + 
			" inited " + initializePC +
			" id = " + id);
    else return new String("Uninit " + refName + " id = " + id);
  }
}

class ArrayReference {
  TypeState elems;
  ArrayReference(TypeState ts) { elems = ts;}
  String asString() {
    return new String("Array of " + elems.asString());
  }
}

class TypeState {
  int state;
  Reference ref;
  ArrayReference aref;
  int returnAddress;

  static TypeState Merge(TypeState t1, TypeState t2, int pc) {
    switch(t1.state) {
    case tBottom: return MergeBottom(t2); 
    case tNull: return MergeNull(t2);
    case tUninit: return MergeUninit(t2); 
    case tInteger: return MergeInteger(t2);
    case tFloat: return MergeFloat(t2); 
    case tLong: return MergeLong(t2); 
    case tLong2: return MergeLong2(t2);
    case tDouble: return MergeDouble(t2);
    case tDouble2: return MergeDouble2(t2);
    case tReference: return MergeReference(t1,t2, pc);
    case tArrayRef: return MergeRefArray(t1,t2);
    case tReturnAddr: return MergeReturnAddr(t1,t2);
    case tTop: return MergeTop(t2); 
    }
    return typeTop;
  }

  static TypeState MergeBottom(TypeState t2) { return t2;}

  static TypeState MergeNull(TypeState t2) {
    switch (t2.state) {
    case tBottom:
    case tNull: return typeNull; 
    case tReference: 
    case tArrayRef: return t2; 
    default: return typeTop;
    }
  }
      
  static TypeState MergeUninit(TypeState t2) {
    switch(t2.state) {
    case tBottom: 
    case tUninit: return typeUninit; 
    }
    return typeTop;
  }
  
  static TypeState MergeInteger(TypeState t2) {
    switch(t2.state) {
    case tBottom:
    case tInteger: return typeInteger;
    }
    return typeTop; 
  }

  static TypeState MergeFloat(TypeState t2) {
  switch(t2.state) {
  case tBottom:
    case tFloat: return typeFloat; 
  }
  return typeTop;
  }
  static TypeState MergeLong(TypeState t2) {
  switch(t2.state) {
  case tBottom:
    case tLong: return typeLong; 
  }
  return typeTop;
  }
  static TypeState MergeLong2(TypeState t2) {
  switch(t2.state) {
  case tBottom:
    case tLong2: return typeLong2; 
  }
  return typeTop;
  }
  static TypeState MergeDouble(TypeState t2) {
  switch(t2.state) {
  case tBottom:
    case tDouble: return typeDouble; 
  }
  return typeTop;
  }
  static TypeState MergeDouble2(TypeState t2) {
    switch(t2.state) {
    case tBottom:
    case tDouble2: return typeDouble2; 
    }
    return typeTop;
  }
  static TypeState MergeReference(TypeState t1, TypeState t2, int pc) {
    switch(t2.state) {
    case tBottom:
    case tNull: return t1; 
    case tReference: {
      String clName = FindCommon(t1.ref.refName, t2.ref.refName);     
      if (t1.ref.init) {
	if (t2.ref.init) {
	  // Both are initialized
	  return t2;
	} else {
	  // t1 is initialized, t2 is not
	  if (t1.ref.initializePC >= pc)
	    return t2;
	  else return TypeState.typeTop;
	}
      } else if (t2.ref.init) {
	// t2 is initialized, t1 is not
	if (t2.ref.initializePC >= pc)
	    return t2;
	  else return TypeState.typeTop;	
      } else {
	// Both are uninitialized
	t2.ref.refName = clName;
	return t2;
      }
    }
    case tArrayRef:
      if (t1.ref.refName.equals("java/lang/Object"))
	return t2;
    }
    return typeTop;
  }

  static TypeState MergeRefArray(TypeState t1, TypeState t2) {
    switch(t2.state) {
    case tBottom:
    case tNull: return t1; 
    case tArrayRef: 
      {
	String arrayType = t1.asString().substring(1);
	switch (arrayType.charAt(0)) {
	case RTC.SIGC_BOOLEAN: return TypeState.typeBoolArray;
	case RTC.SIGC_BYTE:    return TypeState.typeByteArray;
	case RTC.SIGC_CHAR:    return TypeState.typeCharArray;
	case RTC.SIGC_SHORT:   return TypeState.typeShortArray;
	default: return typeArrayRef(Merge(t1.aref.elems, t2.aref.elems, 0)); 
	}
      }
    default: return typeTop;
    }
  }

  static TypeState MergeReturnAddr(TypeState t1, TypeState t2) {
    switch(t2.state) {
    case tBottom: return t1;
    case tReturnAddr: return t2;
    default: return typeTop;
    }
  }
   
  static TypeState MergeTop(TypeState t2) {
    return typeTop;
  }

  static boolean Same(TypeState t1, TypeState t2) {
    boolean result = false;
    switch(t1.state) {
    case tBottom: result = false; break;
    case tInteger: 
    case tFloat: 
    case tLong: 
    case tLong2: 
    case tDouble: 
    case tDouble2: 
    case tReturnAddr: 
    case tUninit: if (t2.state == t1.state) result = true; break;
    case tNull: 
    case tArrayRef: 
    case tReference: result = t2.isReference(); break;
    case tTop: result = false; break;
    }
    return result;
  }
  
  String asString() {
    switch(state) {
    case tBottom: return new String("Bottom"); 
    case tUninit: return new String("Uninit"); 
    case tInteger: return new String("Integer");
    case tFloat: return new String("Float"); 
    case tLong: return new String("Long"); 
    case tLong2: return new String("Long2"); 
    case tDouble: return new String("Double"); 
    case tDouble2: return new String("Double2"); 
    case tReference: return ref.asString(); 
    case tArrayRef: 
      {
	if (this == typeByteArray) return new String("[B");
	else if (this == typeCharArray) return new String("[C");
	else if (this == typeBoolArray) return new String("[Z");
	else if (this == typeShortArray) return new String("[S");
	else return aref.asString();
      }
    case tReturnAddr: return new String("ReturnAddr " + returnAddress); 
    case tNull: return new String("Null"); 
    case tTop: return new String("Top"); 
    default: return new String("Error"); 
    }
  }

  private static final int tBottom        = 1;
  private static final int tUninit        = 2;
  private static final int tInteger       = 3;
  private static final int tFloat         = 4;
  private static final int tLong          = 5;
  private static final int tLong2         = 6;
  private static final int tDouble        = 7;
  private static final int tDouble2       = 8;
  private static final int tReference     = 9;
  private static final int tArrayRef      = 10;
  private static final int tReturnAddr    = 11;
  private static final int tTop           = 20;
  private static final int tNull          = 21;

  boolean isReference() {
    if ((state == tReference) ||
	(state == tArrayRef) ||
	(state == tNull))
      return true;
    else return false;
  }
  
  boolean isArrayReference() {
    if ((state == tArrayRef) || (state == tNull))
      return true;
    else return false;
  }

  boolean isReturnAddr() {
    if (state == tReturnAddr)
      return true;
    else return false;
  }

  public static TypeState typeBottom = new TypeState(tBottom);
  public static TypeState typeUninit = new TypeState(tUninit);
  public static TypeState typeInteger = new TypeState(tInteger);
  public static TypeState typeFloat = new TypeState(tFloat);
  public static TypeState typeDouble = new TypeState(tDouble);
  public static TypeState typeDouble2 = new TypeState(tDouble2);
  public static TypeState typeLong = new TypeState(tLong);
  public static TypeState typeLong2 = new TypeState(tLong2);
  public static TypeState typeIntArray = typeArrayRef(typeInteger);
  public static TypeState typeCharArray = typeArrayRef(typeInteger);
  public static TypeState typeBoolArray = typeArrayRef(typeInteger);
  public static TypeState typeByteArray = typeArrayRef(typeInteger);
  public static TypeState typeFloatArray = typeArrayRef(typeFloat);
  public static TypeState typeDoubleArray = typeArrayRef(typeDouble);
  public static TypeState typeLongArray = typeArrayRef(typeLong);
  public static TypeState typeShortArray = typeArrayRef(typeInteger);
  public static TypeState typeTop = new TypeState(tTop);
  public static TypeState typeNull = new TypeState(tNull);

  public static TypeState typeString = typeRef("java/lang/String", true);
  TypeState(int s) {state = s;}

  static TypeState typeRef(String r, boolean b) {
    TypeState res = new TypeState(tReference);
    res.ref = new Reference(r, b);
    return res;
  }
  
  static TypeState typeRef(String r, boolean b, int start) {
    TypeState res = new TypeState(tReference);
    res.ref = new Reference(r,b,start);
    return res;
  }

  static TypeState typeRef(String r, boolean b, int start, int init) {
    TypeState res = new TypeState(tReference);
    res.ref = new Reference(r,b,start, init);
    return res;
  }
    
  static TypeState typeArrayRef(TypeState ts) {
    TypeState res = new TypeState(tArrayRef);
    res.aref = new ArrayReference(ts);
    return res;
  }

  static TypeState typeReturnAddr(int i) {
    TypeState res = new TypeState(tReturnAddr);
    res.returnAddress = i;
    return res;
  }

  static String FindCommon(String s1, String s2) {
    if (s1.equals(s2)) return s1;
    ClassToBeOptimized c1 = new ClassToBeOptimized(s1 + ".class");
    ClassToBeOptimized c2 = new ClassToBeOptimized(s2 + ".class");
    int count1 = 0;
    int count2 = 0;
    int count = 0;

    while (!c1.getSuper().equals("java/lang/Object")) {
      c1 = new ClassToBeOptimized(c1.getSuper() + ".class");
      count1++;
    }

    System.out.println("Class " + s1 + " is at depth " + count1);
    while (!c2.getSuper().equals("java/lang/Object")) {
      c2 = new ClassToBeOptimized(c2.getSuper() + ".class");
      count2++;
    }
    System.out.println("Class " + s2 + " is at depth " + count2);
    c1 = new ClassToBeOptimized(s1 + ".class");
    while (count1 > count2) {
      c1 = new ClassToBeOptimized(c1.getSuper() + ".class");
      count1-- ;
    }
    c2 = new ClassToBeOptimized(s2 + ".class");
    while (count2 > count1) {
      c2 = new ClassToBeOptimized(c2.getSuper() + ".class");
      count2--;
    }
    System.out.println("name 1 = " + c1.name);
    System.out.println("name 2 = " + c2.name);
    while (!c1.name.equals(c2.name) &
	   (!c1.name.equals("java/lang/Object"))) {
      c1 = new ClassToBeOptimized(c1.getSuper() + ".class");
      c2 = new ClassToBeOptimized(c2.getSuper() + ".class");
    }      
    return c1.name;
  }
}

    


