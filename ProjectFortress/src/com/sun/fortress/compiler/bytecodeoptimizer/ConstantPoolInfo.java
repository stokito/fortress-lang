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

class ConstantPoolInfo {
    int tag;

    public static void readConstantPool(ClassToBeOptimized cls) {
        int count = cls.reader.read2Bytes();
        System.out.println("ReadConstantPool: count = " + count);
        cls.cp = new ConstantPoolInfo[count];
        for (int i = 1; i < count; i++) {
            int tag = cls.reader.read1Byte();
            System.out.println("Entry " + i + " has tag " + tag + " at pos " + cls.reader.index);
            switch(tag) {
            case 0 : System.out.println("ERROR: tag = " + tag + " Next 4 bytes = " + cls.reader.read4Bytes()); 
                throw new ClassFormatError();
            case RTC.CONSTANT_UTF8: cls.cp[i] = new ConstantUTF8Info(cls); break;
            case RTC.CONSTANT_UNICODE: cls.cp[i] = new ConstantPoolInfo(); break;
            case RTC.CONSTANT_INTEGER:
            case RTC.CONSTANT_FLOAT: 
                //cls.reader.read4Bytes();
                //cls.cp[i] = new ConstantPoolInfo(); 
                cls.cp[i] = new Constant4ByteInfo(cls); // FY 08/11/08
                break;
            case RTC.CONSTANT_LONG:
            case RTC.CONSTANT_DOUBLE: {
                // These take up two entries in the constant pool !! 
                //cls.reader.read4Bytes(); 
                //cls.reader.read4Bytes(); 
                //cls.cp[i] = new ConstantPoolInfo(); 
                cls.cp[i] = new Constant4ByteInfo(cls); // FY 08/11/08
                cls.cp[i].tag = tag;
                //cls.cp[i+1] = new ConstantPoolInfo(); 	
                cls.cp[i+1] = new Constant4ByteInfo(cls); // FY 08/11/08
                i = i + 1;
                break;
            }
            case RTC.CONSTANT_CLASS:  cls.cp[i] = new ConstantClassInfo(cls); break;
            case RTC.CONSTANT_STRING: cls.cp[i] = new ConstantStringInfo(cls); break;
            case RTC.CONSTANT_FIELD:  cls.cp[i] = new ConstantFieldInfo(cls); break;
            case RTC.CONSTANT_METHOD: cls.cp[i] = new ConstantMethodInfo(cls); break;
            case RTC.CONSTANT_INTERFACEMETHOD: 
                cls.cp[i] = new ConstantInterfaceMethodInfo(cls);
                break;
            case RTC.CONSTANT_NAMEANDTYPE: 
                cls.cp[i] = new ConstantNameAndTypeInfo(cls);
                break;
            default:
                throw new ClassFormatError();
            }
            cls.cp[i].tag = tag;
            cls.cp[i].print(cls);
        }
    }

    void print(ClassToBeOptimized cls) {
        String resType = new String();
        switch(tag) {
        case RTC.CONSTANT_UTF8: resType = "CONSTANT_UTF8"; break;
        case RTC.CONSTANT_UNICODE: resType = "CONSTANT_UNICODE"; break;
        case RTC.CONSTANT_INTEGER: resType = "CONSTANT_INTEGER"; break;
        case RTC.CONSTANT_FLOAT: resType = "CONSTANT_FLOAT"; break;
        case RTC.CONSTANT_LONG: resType = "CONSTANT_LONG"; break;
        case RTC.CONSTANT_DOUBLE: resType = "CONSTANT_DOUBLE"; break;
        case RTC.CONSTANT_CLASS:  resType = "CONSTANT_CLASS"; break;
        case RTC.CONSTANT_STRING: resType = "CONSTANT_STRING"; break;
        case RTC.CONSTANT_FIELD: resType = "CONSTANT_FIELD"; break;
        case RTC.CONSTANT_METHOD: resType = "CONSTANT_METHOD"; break;
        case RTC.CONSTANT_INTERFACEMETHOD: 
            resType = "CONSTANT_INTERFACEMETHOD"; break;
        case RTC.CONSTANT_NAMEANDTYPE: resType = "CONSTANT_NAMEANDTYPE"; break;
        }
        System.out.println("ConstantPoolInfo: " + resType);
    } 

    TypeState getConstantEntry() {
        switch (tag) 
            {
            case RTC.CONSTANT_INTEGER: return TypeState.typeInteger; 
            case RTC.CONSTANT_FLOAT:   return TypeState.typeFloat;
            case RTC.CONSTANT_STRING:  return TypeState.typeString;
            }
        return TypeState.typeTop;
    }

    TypeState[] getWideConstantEntry() {
        TypeState result[] = new TypeState[2];
        switch (tag) {
        case RTC.CONSTANT_DOUBLE:  
            {
                result[0] = TypeState.typeDouble;
                result[1] = TypeState.typeDouble2;
                break;
            }
        case RTC.CONSTANT_LONG: 
            {
                result[0] = TypeState.typeLong;
                result[1] = TypeState.typeLong2;
                break;
            }
        default:
            {
                result[0] = TypeState.typeTop;	
                result[0] = TypeState.typeTop;	
                break;
            }
        }
        return result;
    }

    TypeState[] getConstantField(ClassToBeOptimized cls) {
        ConstantFieldInfo cfi = (ConstantFieldInfo) this;
        ConstantNameAndTypeInfo cnti = 
            (ConstantNameAndTypeInfo)cls.cp[cfi.nameAndTypeIndex];
        ConstantUTF8Info cutfi = 
            (ConstantUTF8Info) cls.cp[cnti.type];
        String typeString = cutfi.ConstantString;
        TypeState result[] = TypeSig.stringToTypeState(typeString);
        return result;
    }

    String getUtf8String() {
        ConstantUTF8Info csi = (ConstantUTF8Info) this;
        return csi.ConstantString;
    }

    String getMethodName(ClassToBeOptimized cls) {
        ConstantMethodInfo cmi = (ConstantMethodInfo) this;
        ConstantNameAndTypeInfo cnti = 
            (ConstantNameAndTypeInfo) cls.cp[cmi.nameAndTypeIndex];
        ConstantUTF8Info nameString = (ConstantUTF8Info) cls.cp[cnti.name];
        return nameString.ConstantString;
    }

    String getMethodSig(ClassToBeOptimized cls) {
        ConstantMethodInfo cmi = (ConstantMethodInfo) this;
        ConstantNameAndTypeInfo cnti = 
            (ConstantNameAndTypeInfo) cls.cp[cmi.nameAndTypeIndex];
        ConstantUTF8Info typeString = (ConstantUTF8Info) cls.cp[cnti.type];
        return typeString.ConstantString;
    }

    String getIntfMethodName(ClassToBeOptimized cls) {
        ConstantInterfaceMethodInfo cmi = (ConstantInterfaceMethodInfo) this;
        ConstantNameAndTypeInfo cnti = 
            (ConstantNameAndTypeInfo) cls.cp[cmi.nameAndTypeIndex];
        ConstantUTF8Info nameString = (ConstantUTF8Info) cls.cp[cnti.name];
        return nameString.ConstantString;
    }

    String getIntfMethodSig(ClassToBeOptimized cls) {
        ConstantInterfaceMethodInfo cmi = (ConstantInterfaceMethodInfo) this;
        ConstantNameAndTypeInfo cnti = 
            (ConstantNameAndTypeInfo) cls.cp[cmi.nameAndTypeIndex];
        ConstantUTF8Info typeString = (ConstantUTF8Info) cls.cp[cnti.type];
        return typeString.ConstantString;
    }

    String getClassName(ClassToBeOptimized cls) {
        ConstantClassInfo cc = (ConstantClassInfo) this;
        return cc.getClassName(cls);
    }

    TypeState getConstantClass(ClassToBeOptimized cls) {
        String name = getClassName(cls);
        debug.println(2,"getConstantClass for " + name);
        switch(name.charAt(0)) {
        case RTC.SIGC_ARRAY: 
            {
                TypeState result[] = TypeSig.stringToTypeState(name);
                return result[0];
            }
        default: {
            TypeState result[] = TypeSig.stringToTypeState("L" + name + ";");
            return result[0];
        }
        }
    }
}



