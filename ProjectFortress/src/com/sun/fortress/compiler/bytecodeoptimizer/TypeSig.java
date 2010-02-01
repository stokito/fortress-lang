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
import java.util.Vector;

class TypeSig {
  String str;
  int index;
  TypeState[] argTypes;
  TypeState[] resultType;

  public int parseNextToken() {
    int result = index;
    switch(str.charAt(result++)) {
    case RTC.SIGC_VOID:    
    case RTC.SIGC_BOOLEAN: 
    case RTC.SIGC_BYTE:    
    case RTC.SIGC_CHAR:    
    case RTC.SIGC_SHORT:   
    case RTC.SIGC_INT:     
    case RTC.SIGC_LONG:    
    case RTC.SIGC_FLOAT:   
    case RTC.SIGC_DOUBLE: return result;
    case RTC.SIGC_ARRAY: 
      {
	index++; 
	int res = parseNextToken(); 
	index--; 
	return res;
      }
    case RTC.SIGC_CLASS:
      {
	while (str.charAt(result++) != RTC.SIGC_ENDCLASS);
	return result;
      }
    }
    return -1;
  }
  
  public TypeState[] getNextType() {
    int start = index;
    index = parseNextToken();
    return stringToTypeState (str.substring(start, index));
  }
  public void parseTypes() {
    index = 1;
    Vector types = new Vector();
    while (str.charAt(index) != RTC.SIGC_ENDMETHOD) {
      TypeState result[] = getNextType();
      for (int i = 0; i < result.length; i++)
	types.addElement(result[i]);
    }
    argTypes = new TypeState[types.size()];
    types.copyInto(argTypes);
    index++;
    resultType = getNextType();
  }

  public static TypeState[] stringToTypeState(String str) {
    TypeState result[] = null;

    switch(str.charAt(0)) {
    case RTC.SIGC_VOID:    break;
    case RTC.SIGC_BOOLEAN:
    case RTC.SIGC_BYTE:   
    case RTC.SIGC_CHAR:   
    case RTC.SIGC_SHORT:  
    case RTC.SIGC_INT: 
      {
	result = new TypeState[1];
	result[0] = TypeState.typeInteger;
	break;
      }
    case RTC.SIGC_LONG: 
      {
	result = new TypeState[2];
	result[0] = TypeState.typeLong;
	result[1] = TypeState.typeLong2;
	break;
      }
    case RTC.SIGC_FLOAT:  {
	result = new TypeState[1];
	result[0] = TypeState.typeFloat;
	break;
    }
    case RTC.SIGC_DOUBLE:  
      {
	result = new TypeState[2];
	result[0] = TypeState.typeDouble;
	result[1] = TypeState.typeDouble2;
	break;
      }
    case RTC.SIGC_ARRAY: 
      {
	result = new TypeState[1];
	switch(str.charAt(1)) {
	case RTC.SIGC_BOOLEAN: result[0] = TypeState.typeBoolArray; break;
	case RTC.SIGC_BYTE:    result[0] = TypeState.typeByteArray; break;
	case RTC.SIGC_CHAR:    result[0] = TypeState.typeCharArray; break;
	case RTC.SIGC_SHORT:   result[0] = TypeState.typeShortArray; break;
	default: 
	  {
	    TypeState elems[] = stringToTypeState(str.substring(1));
	    result = new TypeState[1];
	    result[0] = TypeState.typeArrayRef(elems[0]);
	    break;
	  }
	}
	break;
      }
    case RTC.SIGC_CLASS:
      {
	int len = str.length();
	result = new TypeState[1];
	result[0] = TypeState.typeRef(str.substring(1,len-1), true);
	break;
      }
    }
    return result;
  }

  TypeSig(String s) {
    str = s;
    index = 0;
    parseTypes();
  }

  String asString() {
    String result = new String("(");
    int numArgs = argTypes.length;
    if (numArgs > 0)
      result = result + argTypes[0].asString();
    if (numArgs > 1) {
      for (int j = 1; j < numArgs; j++)
	result = result + " , " + argTypes[j].asString();
    }
    result = result + ")";
    if (resultType != null)
      result = result + resultType[0].asString();
    return result;
  }
}
