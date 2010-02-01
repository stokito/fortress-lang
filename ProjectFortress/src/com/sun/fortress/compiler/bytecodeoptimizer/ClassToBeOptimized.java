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
import java.io.IOException;

class ClassToBeOptimized {
    int magicNumber;
    int minorVersion;
    int majorVersion;
    int accessFlags;
    int thisClass, superClass;
    ConstantPoolInfo cp[];
    InterfaceInfo interfaces[];
    FieldInfo fields[];
    MethodInfo methods[];
    AttributeInfo attributes[];
    ClassFileReader  reader;
    String name;

    ClassToBeOptimized(String name, byte buf[]) {
        reader = new ClassFileReader(buf);
        initialize();
    }

    ClassToBeOptimized(String fileName) {
        reader = new ClassFileReader (fileName);
        initialize();
        reader.close();
    }
  
    void initialize() {
        magicNumber =  reader.read4Bytes();
        minorVersion = reader.read2Bytes();
        majorVersion = reader.read2Bytes();
        ConstantPoolInfo.readConstantPool(this);
        accessFlags = reader.read2Bytes();
        thisClass = reader.read2Bytes();
        superClass = reader.read2Bytes();
        interfaces = InterfaceInfo.readInterfaces(this);
        fields = FieldInfo.readFields(this);
        methods = MethodInfo.readMethods(this);
        attributes = AttributeInfo.readAttributes(this);
        name = cp[thisClass].getClassName(this);
    }

    String getSuper() {
        debug.println(2,"Getting superClass of " + name);
        if (name.equals("java/lang/Object"))
            return name;
        else return cp[superClass].getClassName(this);
    }

    private static boolean isKeyword(String str, int startIndex, int count) {
        switch (count) {
        case 2:
            if (str.regionMatches(startIndex,"do",0,count) ||       
                str.regionMatches(startIndex,"if",0,count)){
                return true;
            }
            break;
        case 3: 
            if (str.regionMatches(startIndex,"for",0,count) || 
                str.regionMatches(startIndex,"int",0,count) || 
                str.regionMatches(startIndex,"new",0,count) || 
                str.regionMatches(startIndex,"try",0,count)){
                return true;
            }
            break;
        case 4:
            if (str.regionMatches(startIndex,"byte",0,count) ||       
                str.regionMatches(startIndex,"case",0,count) || 
                str.regionMatches(startIndex,"char",0,count) || 
                str.regionMatches(startIndex,"else",0,count) ||       
                str.regionMatches(startIndex,"goto",0,count) ||       
                str.regionMatches(startIndex,"long",0,count) || 
                str.regionMatches(startIndex,"this",0,count) ||
                str.regionMatches(startIndex,"void",0,count) || 
                str.regionMatches(startIndex,"true",0,count) ||       
                str.regionMatches(startIndex,"false",0,count)) {
                return true;
            }
            break; 
        case 5:
            if (str.regionMatches(startIndex,"break",0,count) || 
                str.regionMatches(startIndex,"catch",0,count) ||       
                str.regionMatches(startIndex,"class",0,count) ||       
                str.regionMatches(startIndex,"const",0,count) || 
                str.regionMatches(startIndex,"final",0,count) ||       
                str.regionMatches(startIndex,"float",0,count) ||       
                str.regionMatches(startIndex,"import",0,count) || 
                str.regionMatches(startIndex,"native",0,count) ||       
                str.regionMatches(startIndex,"short",0,count) || 
                str.regionMatches(startIndex,"super",0,count) || 
                str.regionMatches(startIndex,"throw",0,count) || 
                str.regionMatches(startIndex,"while",0,count)) {
                return true;
            }
            break; 
        case 6:
            if (str.regionMatches(startIndex,"double",0,count) || 
                str.regionMatches(startIndex,"public",0,count) || 
                str.regionMatches(startIndex,"return",0,count) ||       
                str.regionMatches(startIndex,"static",0,count) ||       
                str.regionMatches(startIndex,"switch",0,count) ||       
                str.regionMatches(startIndex,"throws",0,count)) {
                return true;
            }
            break;
        case 7:       
            if (str.regionMatches(startIndex,"boolean",0,count) ||
                str.regionMatches(startIndex,"default",0,count) || 
                str.regionMatches(startIndex,"extends",0,count) || 
                str.regionMatches(startIndex,"finally",0,count) || 
                str.regionMatches(startIndex,"package",0,count) ||       
                str.regionMatches(startIndex,"private",0,count)) {
                return true;
            }
            break;
        case 8:
            if (str.regionMatches(startIndex,"abstract",0,count) ||
                str.regionMatches(startIndex,"continue",0,count) ||
                str.regionMatches(startIndex,"volatile",0,count)) {
                return true;
            }
            break; 
        case 9:
            if (str.regionMatches(startIndex,"interface",0,count) ||       
                str.regionMatches(startIndex,"protected",0,count) ||
                str.regionMatches(startIndex,"transient",0,count)) {
                return true;
            }
            break; 
        case 10:     
            if (str.regionMatches(startIndex,"implements",0,count) ||       
                str.regionMatches(startIndex,"instanceof",0,count)){
                return true;
            }
            break;
        case 11:
            if (str.regionMatches(startIndex,"synchronized",0,count)) {
                return true;
            }
            break;
        default:
            return false; 
        }
        return false;     
    }
    private static void checkIdentifier(String str, int offset, int count){
        int i=0;
        if (!Character.isJavaLetter(str.charAt(i+offset))) {
            System.out.println("Verification Pass 2: Bad identifier (1)");
            throw new VerifyError(); 
        }
        for (i=1; i<count; i++) {
            if (!Character.isJavaLetterOrDigit(str.charAt(i+offset))) {
                System.out.println("Verification Pass 2: Bad identifier (2)");
                throw new VerifyError(); 
            }
        }
        if (isKeyword(str,offset,count)) { 
            System.out.println("Verification Pass 2: Bad identifier (3)");
            throw new VerifyError();
        }
    } 
    private static void checkClassName(String str) {
        int i = 0;
        if (str.charAt(0) == RTC.SIGC_ARRAY) { /* Array Class */
            checkFieldDescriptor(str,0,str.length());
        }
        else {
            int startIndex; /* marker for start of a package/class name */
            startIndex = i;
            if (!Character.isJavaLetter(str.charAt(i++))) {
                System.out.println("Verification Pass 2: Bad class name (1)");
                throw new VerifyError();
            }
            while (i!=str.length()){
                char c = str.charAt(i);
                if((!Character.isJavaLetterOrDigit(c) && (c != RTC.SIGC_PACKAGE)) || 
                   ((c == RTC.SIGC_PACKAGE) && (str.charAt(i-1) == c))){
                    System.out.println("Verification Pass 2: Bad class name (2)");
                    throw new VerifyError();
                }
                if (c == RTC.SIGC_PACKAGE) {
                    if (isKeyword(str, startIndex, i-startIndex)) {
                        System.out.println("Verification Pass 2: Bad class name (3)");
                        throw new VerifyError();
                    }
                    startIndex = i+1;
                }
                else if ((i+1)==str.length()) {
                    if (isKeyword(str, startIndex, i-startIndex+1)) {
                        System.out.println("Verification Pass 2: Bad class name (4)");
                        throw new VerifyError();
                    }
                }
                i++;
            }

            if(!ClassFileReader .exists(str+".class")) {
                System.out.println("Verification Pass 2: Reference to non-existent class (1) "+str);
                throw new VerifyError();
            }
        }  
    }
    private static void checkFieldDescriptor(String str, int offset, int count) {
        int i = offset;
        while(str.charAt(i)==RTC.SIGC_ARRAY) {i++;}
        if (i>(RTC.MAX_ARRAY_DIMENSIONS+offset)) {
            System.out.println("Verification Pass 2: Bad field descriptor (1)");
            throw new VerifyError();    
        }
        switch(str.charAt(i++)) {
        case RTC.SIGC_BOOLEAN: 
        case RTC.SIGC_BYTE:    
        case RTC.SIGC_CHAR:    
        case RTC.SIGC_SHORT:   
        case RTC.SIGC_INT:     
        case RTC.SIGC_LONG:    
        case RTC.SIGC_FLOAT:   
        case RTC.SIGC_DOUBLE: {
            if(i != (offset+count)) {
                System.out.println("Verification Pass 2: Bad field descriptor (2)");
                throw new VerifyError();
            }
            return;
        }
        case RTC.SIGC_CLASS:{
            int startIndex; /* marker for start of a package/class name */
            startIndex = i;
            if (!Character.isJavaLetter(str.charAt(i++))) {
                System.out.println("Verification Pass 2: Bad field descriptor (3)");
                throw new VerifyError();
            }
            char c = str.charAt(i); 
            while (c!=RTC.SIGC_ENDCLASS){
                if ((!Character.isJavaLetterOrDigit(c) && (c!=RTC.SIGC_PACKAGE)) || 
                    ((c == RTC.SIGC_PACKAGE) && (str.charAt(i-1) == c))) {
                    System.out.println("Verification Pass 2: Bad field descriptor (4)");
                    throw new VerifyError();
                }
                if (c == RTC.SIGC_PACKAGE) {
                    if(isKeyword(str, startIndex, i-startIndex)) {
                        System.out.println("Verification Pass 2: Bad field descriptor (5)");
                        throw new VerifyError();
                    }
                    if (str.charAt(i+1)==RTC.SIGC_ENDCLASS) {
                        System.out.println("Verification Pass 2: Bad field descriptor (6)");
                        throw new VerifyError();
                    }
                    startIndex = i+1;
                }
                else if (str.charAt(i+1) == RTC.SIGC_ENDCLASS) {
                    if(isKeyword(str, startIndex, i-startIndex+1)) {
                        System.out.println("Verification Pass 2: Bad field descriptor (7)");
                        throw new VerifyError();
                    }
                }
                c = str.charAt(++i);
            }
            if(++i != (offset+count)) {
                System.out.println("Verification Pass 2: Bad field descriptor (8)");
                throw new VerifyError();
            }
            return;        
        }        
        default:
            System.out.println("Verification Pass 2: Bad field descriptor (9)");
            throw new VerifyError();
        }
    }

    /* Check the descriptor of a method. Meaning of return value depends
       on the mode selected. In mode ZERO returns the number of return 
       value words. In mode ONE returns the number of argument words. 
       Otherwise returns -1. */
    private static int checkMethodDescriptor(String str, int mode) {
        int i = 0;
        int numArgWords = 0, numRetWords = 0;
        char c;

        if (str.charAt(i++) != RTC.SIGC_METHOD) {
            throw new VerifyError();
        }
        c = str.charAt(i);
        while (c != RTC.SIGC_ENDMETHOD) {
            switch(c) {
            case RTC.SIGC_BOOLEAN: 
            case RTC.SIGC_BYTE:    
            case RTC.SIGC_CHAR:    
            case RTC.SIGC_SHORT:   
            case RTC.SIGC_INT:     
            case RTC.SIGC_FLOAT: {
                c = str.charAt(++i);
                numArgWords++;
                break; 
            }
            case RTC.SIGC_LONG:    
            case RTC.SIGC_DOUBLE: {
                c = str.charAt(++i);
                numArgWords += 2;
                break; 
            }
            case RTC.SIGC_CLASS: {
                int startIndex = i; /* pointer to start of a field descriptor */
                while(c != RTC.SIGC_ENDCLASS) {
                    c = str.charAt(++i);
                }
                checkFieldDescriptor(str, startIndex, i-startIndex+1);
                c = str.charAt(++i);
                numArgWords++;
                break;  
            }
            case RTC.SIGC_ARRAY: {
                int startIndex = i; /* pointer to start of a field descriptor */
                while (c == RTC.SIGC_ARRAY) {
                    c = str.charAt(++i);
                }
                if (c == RTC.SIGC_CLASS) {
                    while (c != RTC.SIGC_ENDCLASS) {
                        c = str.charAt(++i);
                    }
                }
                checkFieldDescriptor(str, startIndex, i-startIndex+1);
                c = str.charAt(++i);
                numArgWords++;
                break;
            }
            default:
                System.out.println("Verification Pass 2: checkMethodDescriptor (1)");
                throw new VerifyError();      
            }
        }
    
        c = str.charAt(++i);
        switch (c) {
        case RTC.SIGC_BOOLEAN: 
        case RTC.SIGC_BYTE:    
        case RTC.SIGC_CHAR:    
        case RTC.SIGC_SHORT:   
        case RTC.SIGC_INT:     
        case RTC.SIGC_FLOAT: {   
            if ((i+1) != str.length()) {
                System.out.println("Verification Pass 2: checkMethodDescriptor (2)");
                throw new VerifyError();
            }
            numRetWords = 1;
            break;
        }
        case RTC.SIGC_LONG:    
        case RTC.SIGC_DOUBLE: {
            if ((i+1) != str.length()) {
                System.out.println("Verification Pass 2: checkMethodDescriptor (2)");
                throw new VerifyError();
            }
            numRetWords = 2;
            break;
        }
        case RTC.SIGC_VOID: {
            if ((i+1) != str.length()) {
                System.out.println("Verification Pass 2: checkMethodDescriptor (3)");
                throw new VerifyError();
            }
            numRetWords = 0;
            break;
        }     
        case RTC.SIGC_CLASS: {
            int startIndex = i; /* pointer to start of a field descriptor */
            while(c != RTC.SIGC_ENDCLASS) {
                c = str.charAt(++i);
            }
            checkFieldDescriptor(str, startIndex, i-startIndex+1);
            if ((i+1) != str.length()) {
                System.out.println("Verification Pass 2: checkMethodDescriptor (4)");
                throw new VerifyError();
            }
            numRetWords = 1;
            break;  
        }
        case RTC.SIGC_ARRAY: {
            int startIndex = i; /* pointer to start of a field descriptor */
            while (c == RTC.SIGC_ARRAY) {
                c = str.charAt(++i);
            }
            if (c == RTC.SIGC_CLASS) {
                while (c != RTC.SIGC_ENDCLASS) {
                    c = str.charAt(++i);
                }
            }
            checkFieldDescriptor(str, startIndex, i-startIndex+1);
            if ((i+1) != str.length()) {
                System.out.println("Verification Pass 2: checkMethodDescriptor (5)");
                throw new VerifyError();
            }
            numRetWords = 1;
            break;  
        }
        default:
            System.out.println("Verification Pass 2: checkMethodDescriptor (6)");
            throw new VerifyError();      
        }        

        if (mode == 0) {
            return numRetWords;
        }
        else if (mode == 1) {
            return numArgWords;
        }
        else {
            return (-1);
        }
    }

    private void checkConstantPool() {
        int count = cp.length;

        for (int i = 1; i < count; i++) {
            ConstantPoolInfo cpInfo = cp[i];
            switch(cpInfo.tag) {
            case RTC.CONSTANT_UTF8: {
                String str = ((ConstantUTF8Info)cpInfo).ConstantString;
                for (int j=0; j<str.length(); j++) {
                    char c = str.charAt(j);
                    if(((byte)c == (byte)0) ||
                       (((byte)c >= (byte)0xf0) && 
                        ((byte)c <= (byte)0xff))) {
                        System.out.println("Verification Pass 2: checkConstantPool (1)");
                        throw new VerifyError();
                    }
                }
                break;
            }
            case RTC.CONSTANT_UNICODE: 
            case RTC.CONSTANT_INTEGER:
            case RTC.CONSTANT_FLOAT: 
            case RTC.CONSTANT_LONG:
            case RTC.CONSTANT_DOUBLE:
                break;
            case RTC.CONSTANT_CLASS: {
                ConstantUTF8Info utf8Info = 
                    (ConstantUTF8Info)cp[((ConstantClassInfo)cpInfo).nameIndex];
                checkClassName(utf8Info.ConstantString);
                break;
            }
            case RTC.CONSTANT_STRING: {
                if (cp[((ConstantStringInfo)cpInfo).stringIndex].tag
                    != RTC.CONSTANT_UTF8) { 
                    System.out.println("Verification Pass 2: checkConstantPool (2)");
                    throw new VerifyError();
                }
                break;        
            }
            case RTC.CONSTANT_FIELD: {
                if (cp[((ConstantFieldInfo)cpInfo).classIndex].tag
                    != RTC.CONSTANT_CLASS) { 
                    System.out.println("Verification Pass 2: checkConstantPool (3)");
                    throw new VerifyError();
                }
                ConstantNameAndTypeInfo nameTypeInfo = (ConstantNameAndTypeInfo)
                    cp[((ConstantFieldInfo)cpInfo).nameAndTypeIndex];
                ConstantUTF8Info type = (ConstantUTF8Info)cp[nameTypeInfo.type];
                ConstantUTF8Info name = (ConstantUTF8Info)cp[nameTypeInfo.name];
                checkFieldDescriptor(type.ConstantString,0,
                                     type.ConstantString.length());
                checkIdentifier(name.ConstantString, 0, name.ConstantString.length());
                break;
            }
            case RTC.CONSTANT_METHOD: {
                if (cp[((ConstantMethodInfo)cpInfo).classIndex].tag
                    != RTC.CONSTANT_CLASS) { 
                    throw new VerifyError();
                }
                ConstantNameAndTypeInfo nameTypeInfo = (ConstantNameAndTypeInfo)
                    cp[((ConstantMethodInfo)cpInfo).nameAndTypeIndex];
                ConstantUTF8Info type = (ConstantUTF8Info)cp[nameTypeInfo.type];
                ConstantUTF8Info name = (ConstantUTF8Info)cp[nameTypeInfo.name];
                int retWords = checkMethodDescriptor(type.ConstantString, 0);
                if((name.ConstantString.compareTo("<init>")==0) ||
                   (name.ConstantString.compareTo("<clinit>")==0)) {
                    if(retWords > 0) {
                        System.out.println("Verification Pass 2: checkConstantPool (4)");
                        throw new VerifyError();
                    }
                    break;
                }
                checkIdentifier(name.ConstantString, 0, name.ConstantString.length());
                break;
            }
            case RTC.CONSTANT_INTERFACEMETHOD: {
                if (cp[((ConstantInterfaceMethodInfo)cpInfo).classIndex].tag
                    != RTC.CONSTANT_CLASS) { 
                    System.out.println("Verification Pass 2: checkConstantPool (5)");
                    throw new VerifyError();
                }
                ConstantNameAndTypeInfo nameTypeInfo = (ConstantNameAndTypeInfo)
                    cp[((ConstantInterfaceMethodInfo)cpInfo).nameAndTypeIndex];
                ConstantUTF8Info type = (ConstantUTF8Info)cp[nameTypeInfo.type];
                ConstantUTF8Info name = (ConstantUTF8Info)cp[nameTypeInfo.name];
                checkMethodDescriptor(type.ConstantString, 0);
                checkIdentifier(name.ConstantString, 0, name.ConstantString.length());
                break;
            }
            case RTC.CONSTANT_NAMEANDTYPE: 
                break;
            default: 
                System.out.println("Verification Pass 2: checkConstantPool (6)");
                throw new VerifyError();
            }
        }
    }

    public void checkFields() {
        int count = fields.length;
        for (int i = 0; i < count; i++) {
            FieldInfo field = fields[i];
            int fieldAccess = field.accessFlags;
            String fieldDescriptor;

            /* access flag checks */
            fieldAccess &= (RTC.ACC_PUBLIC|RTC.ACC_PRIVATE|RTC.ACC_PROTECTED|
                            RTC.ACC_STATIC|RTC.ACC_FINAL|RTC.ACC_VOLATILE|
                            RTC.ACC_TRANSIENT);

            if ((accessFlags & RTC.ACC_INTERFACE) != 0) {
                if ((fieldAccess & (RTC.ACC_PRIVATE|RTC.ACC_PROTECTED|RTC.ACC_VOLATILE|
                                    RTC.ACC_TRANSIENT)) != 0) {
                    System.out.println("Verification Pass 2: Bad field access flag (1)");
                    throw new VerifyError();
                }
                if (((fieldAccess & RTC.ACC_STATIC)==0) || 
                    ((fieldAccess & RTC.ACC_FINAL)==0) ||
                    ((fieldAccess & RTC.ACC_PUBLIC)==0)) {
                    System.out.println("Verification Pass 2: Bad field access flag (2)");
                    throw new VerifyError();
                }
            }
            else {
                if ((((fieldAccess & RTC.ACC_PRIVATE)!=0)&&
                     ((fieldAccess & RTC.ACC_PROTECTED)!=0)) ||
                    (((fieldAccess & RTC.ACC_PRIVATE)!=0)&&
                     ((fieldAccess & RTC.ACC_PUBLIC)!=0)) ||
                    (((fieldAccess & RTC.ACC_PUBLIC)!=0)&&
                     ((fieldAccess & RTC.ACC_PROTECTED)!=0)) ||
                    (((fieldAccess & RTC.ACC_FINAL)!=0)&&
                     ((fieldAccess & RTC.ACC_VOLATILE)!=0))) {
                    System.out.println("Verification Pass 2: Bad field access flag (3)");
                    throw new VerifyError();
                }      
            }

            /* name & descriptor check */      
            if ((cp[field.nameIndex].tag != RTC.CONSTANT_UTF8) ||
                (cp[field.descriptorIndex].tag != RTC.CONSTANT_UTF8)) {
                System.out.println("Verification Pass 2: Bad field name or desc (1)");
                throw new VerifyError();
            }
      
            /* attribute check */
            fieldDescriptor = 
                ((ConstantUTF8Info)cp[field.descriptorIndex]).ConstantString;
            int constVarAttrCount = 0, type;
            AttributeInfo[] attributes = field.attributes;
            int attrCount = attributes.length;

            for (int j = 0; j < attrCount; j++) {
                AttributeInfo attr = attributes[j];
                if (attr.attributeName.equals("ConstantValue")) {
                    constVarAttrCount++;
                    if ((constVarAttrCount > 1) || 
                        // atr.atrcvl001: ((fieldAccess & RTC.ACC_STATIC)==0) ||
                        (attr.attributeLength != 2)) {
                        System.out.println("Verification Pass 2: Bad field attribute");
                        throw new VerifyError();
                    }
                    type = cp[((ConstantValueAttributeInfo)attr).constantValueIndex].tag;
          
                    switch(type) {
                    case RTC.CONSTANT_INTEGER:
                        if(!(fieldDescriptor.equals(RTC.SIG_INT)) &&
                           !(fieldDescriptor.equals(RTC.SIG_SHORT)) &&
                           !(fieldDescriptor.equals(RTC.SIG_CHAR)) &&
                           !(fieldDescriptor.equals(RTC.SIG_BYTE)) &&
                           !(fieldDescriptor.equals(RTC.SIG_BOOLEAN))) {
                            System.out.println("Verification Pass 2: Bad field const type (1)");
                            throw new VerifyError();
                        }
                        break;
                    case RTC.CONSTANT_FLOAT:
                        if(!(fieldDescriptor.equals(RTC.SIG_FLOAT))) {
                            System.out.println("Verification Pass 2: Bad field const type (2)");
                            throw new VerifyError();
                        }
                        break; 
                    case RTC.CONSTANT_LONG:
                        if(!(fieldDescriptor.equals(RTC.SIG_LONG))) {
                            System.out.println("Verification Pass 2: Bad field const type (3)");
                            throw new VerifyError();
                        }
                        break;
                    case RTC.CONSTANT_DOUBLE:
                        if(!(fieldDescriptor.equals(RTC.SIG_DOUBLE))) {
                            System.out.println("Verification Pass 2: Bad field const type (4)");
                            throw new VerifyError();
                        }
                        break;
                    case RTC.CONSTANT_STRING:
                        if(!(fieldDescriptor.equals("Ljava/lang/String;"))) {
                            System.out.println("Verification Pass 2: Bad field const type (5)");
                            throw new VerifyError();
                        }
                        break;
                    default:
                        System.out.println("Verification Pass 2: Bad field const type (6)");
                        throw new VerifyError();
                    }
                }
            }

        }

        // check for same name & descriptor
        for (int i = 0; i < count; i++) {
            FieldInfo field = fields[i];
            String fieldName, fieldDesc;

            fieldName =((ConstantUTF8Info)cp[field.nameIndex]).ConstantString;
            fieldDesc =((ConstantUTF8Info)cp[field.descriptorIndex]).ConstantString;

            for (int j = 0; j < count; j++) {
                FieldInfo f = fields[j];
                String name, desc;
                name =((ConstantUTF8Info)cp[f.nameIndex]).ConstantString;
                desc =((ConstantUTF8Info)cp[f.descriptorIndex]).ConstantString;
                if ((i != j) && fieldName.equals(name) && fieldDesc.equals(desc)) {
                    System.out.println("Verification Pass 2: Fields w/ same name or desc (1)");
                    throw new VerifyError();
                }
            }
        }
    }

    public void checkCodeAttribute(CodeAttributeInfo codeAttr, String methodDesc,
                                   int methodAccess) {
        if (codeAttr == null) return;

        ExceptionTableInfo[] exceptions = codeAttr.exceptions;
        int codeLength = codeAttr.code.length;
        int numEntries = exceptions.length;

        // Check exception table
        for (int j = 0; j < numEntries; j++) {
            ExceptionTableInfo e = exceptions[j];
            if ((e.start_pc < 0) || (e.start_pc >= codeLength) ||
                (e.end_pc < 0) || (e.end_pc > codeLength) ||
                (e.handler_pc < 0) || (e.handler_pc >= codeLength) || 
                (e.start_pc >= e.end_pc)) {
                System.out.println("Verification Pass 2: Bad except. tbl entry (1)");
                throw new VerifyError();          
            }

            if (e.catch_type != 0) {
                try {
                    ConstantClassInfo cpInfo = (ConstantClassInfo)cp[e.catch_type];
                    String str = ((ConstantUTF8Info)cp[cpInfo.nameIndex]).ConstantString;
                    if (!str.equals("java/lang/Throwable")) {
                        String className = str.replace('/', '.');
                        Class cls = Class.forName(className);
                        while (true) {
                            cls = cls.getSuperclass();
                            if (cls == null) {
                                System.out.println("Verification Pass 2: Bad except. cls (1)");
                                throw new VerifyError();
                            }
                            if (cls.getName().equals("java.lang.Throwable")) {
                                break;
                            }
                        } 
                    }
                } catch (Exception x) {
                    System.out.println("Verification Pass 2: Bad except. tbl entry (2)");
                    throw new VerifyError();                    
                }	    
            }
        }

        // Check max_locals
        TypeSig sig = new TypeSig(methodDesc);
        if ( (methodAccess & RTC.ACC_STATIC) != 0) {
            if (sig.argTypes.length > codeAttr.maxLocals) {
                System.out.println("Verification Pass 2: Bad maxLocals (1)");
                throw new VerifyError();                                
            }
        }
        else {
            if ((sig.argTypes.length+1) > codeAttr.maxLocals) {
                System.out.println("Verification Pass 2: Bad maxLocals (2)");
                throw new VerifyError();                                
            }
        }

        // Check attributes
        AttributeInfo[] attributes = codeAttr.attributes;
        int attrCount = attributes.length;
        int[] locVarAttrCount = new int[codeAttr.maxLocals];

        for (int j = 0; j < attrCount; j++) {
            AttributeInfo attr = attributes[j];
            if (attr.attributeName.equals("LineNumberTable")) {
                LineNumberTableAttributeInfo lnta = (LineNumberTableAttributeInfo)attr;
                int length = lnta.attributeLength;
                int lntcount = lnta.lineNumberTableLength;
                LineNumberTableInfo [] lntable = lnta.lineNumberTable;

                if (length != (lntcount*4 + 2)) {
                    System.out.println("Verification Pass 2: Bad lnt attr (1)");
                    throw new VerifyError();            
                }
                for (int z = 0; z < lntcount; z++) {
                    if (lntable[z].start_pc >= codeLength) {
                        System.out.println("Verification Pass 2: Bad lnt attr (2)");
                        throw new VerifyError();                         
                    }
                }
            }
            else if (attr.attributeName.equals("LocalVariableTable")) {
                LocalVariableTableAttributeInfo lvta = 
                    (LocalVariableTableAttributeInfo)attr;
                int length = lvta.attributeLength;
                int lvtcount = lvta.localVariableTableLength;
                LocalVariableTableInfo [] lvtable = lvta.localVariableTable;

                if (length != (lvtcount*10 + 2)) {
                    System.out.println("Verification Pass 2: Bad lvt attr (1)");
                    throw new VerifyError();            
                }
                for (int z = 0; z < lvtcount; z++) {
                    int start_pc = lvtable[z].start_pc;
                    int len = lvtable[z].length;
                    int nameIndex = lvtable[z].nameIndex;
                    int descriptorIndex = lvtable[z].descriptorIndex;
                    String name, desc;

                    if ((start_pc >= codeLength) || (start_pc < 0)) {
                        System.out.println("Verification Pass 2: Bad lvt attr (2)");
                        throw new VerifyError();                         
                    }
                    if ( ((start_pc+len) > codeLength) || ((start_pc+len) < 0) ) {
                        System.out.println("Verification Pass 3: Bad lvt attr (3)");
                        throw new VerifyError();                         
                    }

                    /* name & descriptor check */      
                    if ((cp[nameIndex].tag != RTC.CONSTANT_UTF8) ||
                        (cp[descriptorIndex].tag != RTC.CONSTANT_UTF8)) {
                        System.out.println("Verification Pass 2: Bad lvt attr (4)");
                        throw new VerifyError();
                    }
          
                    name =((ConstantUTF8Info)cp[nameIndex]).ConstantString;
                    desc =((ConstantUTF8Info)cp[descriptorIndex]).ConstantString;
          
                    if (!name.equals("this")) {
                        try {
                            checkIdentifier(name, 0, name.length());
                        }
                        catch (Throwable t) {
                            System.out.println("Verification Pass 2: Bad lvt attr (5)");
                            throw new VerifyError();            
                        }
                    }

                    try {
                        checkFieldDescriptor(desc, 0, desc.length());
                    }
                    catch (Throwable t) {
                        System.out.println("Verification Pass 2: Bad lvt attr (6)");
                        throw new VerifyError();            
                    }

                    try {
                        locVarAttrCount[lvtable[z].index]++;
                    }
                    catch (Exception e) {
                        System.out.println("Verification Pass 2: Bad lvt attr (7)");
                        throw new VerifyError();            
                    }

                    if (locVarAttrCount[lvtable[z].index] > 1) {
                        System.out.println("Verification Pass 2: Bad lvt attr (8)");
                        throw new VerifyError();            
                    }

                }
            }
        }
    }

    public void checkMethods() {
        int count = methods.length;
        for (int i = 0; i < count; i++) {
            MethodInfo method = methods[i];
            int methodAccess = method.accessFlags;
            String methodName, methodDesc;

            /* name & descriptor check */      
            if ((cp[method.nameIndex].tag != RTC.CONSTANT_UTF8) ||
                (cp[method.descriptorIndex].tag != RTC.CONSTANT_UTF8)) {
                System.out.println("Verification Pass 2: Bad method name or desc (1)");
                throw new VerifyError();
            }
      
            methodName = ((ConstantUTF8Info)cp[method.nameIndex]).ConstantString;

            /* access flag checks */
            methodAccess &= (RTC.ACC_PUBLIC|RTC.ACC_PRIVATE|RTC.ACC_PROTECTED|
                             RTC.ACC_STATIC|RTC.ACC_FINAL|RTC.ACC_SYNCHRONIZED|
                             RTC.ACC_NATIVE|RTC.ACC_ABSTRACT);

            if (methodName.equals("<clinit>")) { }
            else if ((accessFlags & RTC.ACC_INTERFACE) != 0) {
                if ((methodAccess & (RTC.ACC_PRIVATE|RTC.ACC_PROTECTED|RTC.ACC_STATIC|
                                     RTC.ACC_FINAL|RTC.ACC_SYNCHRONIZED|RTC.ACC_NATIVE)) != 0) {
                    System.out.println("Verification Pass 2: Bad method access flag (1)");
                    throw new VerifyError();
                }
                if (((methodAccess & RTC.ACC_ABSTRACT)==0) || 
                    ((methodAccess & RTC.ACC_PUBLIC)==0)) {
                    System.out.println("Verification Pass 2: Bad method access flag (2)");
                    throw new VerifyError();
                }
            }
            else {
                if ( ((methodAccess & RTC.ACC_ABSTRACT)!=0)&&
                     (((methodAccess & RTC.ACC_FINAL)!=0) ||
                      ((methodAccess & RTC.ACC_NATIVE)!=0) ||
                      ((methodAccess & RTC.ACC_SYNCHRONIZED)!=0) ||
                      ((methodAccess & RTC.ACC_PRIVATE)!=0) ||
                      ((methodAccess & RTC.ACC_STATIC)!=0)) ) {
                    System.out.println("Verification Pass 2: Bad method access flag (3)");
                    throw new VerifyError();
                }        
                if (methodName.equals("<init>")) {
                    if ((methodAccess & (RTC.ACC_STATIC|RTC.ACC_FINAL|
                                         RTC.ACC_SYNCHRONIZED|RTC.ACC_NATIVE|RTC.ACC_ABSTRACT)) != 0) {
                        System.out.println("Verification Pass 2: Bad method access flag (4)");
                        throw new VerifyError();
                    }      
                }
            }

            if (methodName.equals("<clinit>")) { }
            else if ((((methodAccess & RTC.ACC_PRIVATE)!=0)&&
                      ((methodAccess & RTC.ACC_PROTECTED)!=0)) ||
                     (((methodAccess & RTC.ACC_PRIVATE)!=0)&&
                      ((methodAccess & RTC.ACC_PUBLIC)!=0)) ||
                     (((methodAccess & RTC.ACC_PUBLIC)!=0)&&
                      ((methodAccess & RTC.ACC_PROTECTED)!=0))) {
                System.out.println("Verification Pass 2: Bad method access flag (5)");
                throw new VerifyError();
            }      

            // Check attributes
            int codeAttrCount = 0, exceptionAttrCount = 0;
            CodeAttributeInfo codeAttr = null;
            AttributeInfo[] attributes = method.attributes;
            int attrCount = attributes.length;

            for (int j = 0; j < attrCount; j++) {
                AttributeInfo attr = attributes[j];
                if (attr.attributeName.equals("Code")) {
                    codeAttr = ((CodeAttributeInfo)attr);
                    codeAttrCount++;
                    if (codeAttrCount > 1) {
                        System.out.println("Verification Pass 2: > 1 code attr (1)");
                        throw new VerifyError();
                    }
                }
                else if (attr.attributeName.equals("Exceptions")) {
                    int length = attr.attributeLength;
                    int excount = ((ExceptionAttributeInfo)attr).numberOfExceptions;
                    int [] extable = ((ExceptionAttributeInfo)attr).exceptionTableIndex;
                    if (length != (excount*2 + 2)) {
                        System.out.println("Verification Pass 2: Bad except. attr (1)");
                        throw new VerifyError();            
                    }
                    for (int z = 0; z < excount; z++) {
                        try {
                            ConstantClassInfo info = (ConstantClassInfo)cp[extable[z]];
                        }
                        catch (Exception e) {
                            System.out.println("Verification Pass 2: Bad except. attr (2)");
                            throw new VerifyError();                         
                        } 
                    }
          
                    exceptionAttrCount++;
                    if (exceptionAttrCount > 1) {
                        System.out.println("Verification Pass 2: > 1 except. attr (1)");
                        throw new VerifyError();
                    }
                }
            }

            if   (((methodAccess & (RTC.ACC_NATIVE|RTC.ACC_ABSTRACT)) == 0) ||
                  (methodName.equals("<clinit>"))) {
                if (codeAttrCount != 1) {
                    System.out.println("Verification Pass 2: No code attr found (1)");
                    throw new VerifyError();
                }
            }
            else {
                if (codeAttrCount != 0) {
                    System.out.println("Verification Pass 2: Code attr found (1)");
                    throw new VerifyError();
                }
            }

            methodDesc=((ConstantUTF8Info)cp[method.descriptorIndex]).ConstantString;

            /* exception table check */
            checkCodeAttribute(codeAttr, methodDesc, methodAccess);

            /* check for max no. of arg words */
            int argWords = checkMethodDescriptor(methodDesc, 1);
            if ((methodAccess & RTC.ACC_STATIC) != 0) {
                if (argWords > RTC.MAX_METHOD_ARG_WORDS) {
                    System.out.println("Verification Pass 2: Max. arg words limit (1)");
                    throw new VerifyError();
                }
            }
            else {
                if (argWords > (RTC.MAX_METHOD_ARG_WORDS - 1)) {
                    System.out.println("Verification Pass 2: Max. arg words limit (2)");
                    throw new VerifyError();
                }
            }
        }

        // check for same name & descriptor
        for (int i = 0; i < count; i++) {
            MethodInfo method = methods[i];
            String methodName, methodDesc;

            methodName=((ConstantUTF8Info)cp[method.nameIndex]).ConstantString;
            methodDesc=((ConstantUTF8Info)cp[method.descriptorIndex]).ConstantString;

            for (int j = 0; j < count; j++) {
                MethodInfo m = methods[j];
                String name, desc;
                name =((ConstantUTF8Info)cp[m.nameIndex]).ConstantString;
                desc =((ConstantUTF8Info)cp[m.descriptorIndex]).ConstantString;
                if ((i!=j) && methodName.equals(name) && methodDesc.equals(desc)) {
                    System.out.println("Verification Pass 2: Methods w/ same name or desc (1)");
                    throw new VerifyError();
                }
            }
        }     
    }

    public void Print() {
        System.out.println("Class Magic Number = " + 
                           Integer.toHexString(magicNumber));
        System.out.println("Class Minor Version = " + minorVersion);
        System.out.println("Class Major Version = " + majorVersion);
        System.out.println(cp.length + " entries in the constant pool");
        for (int i = 1; i < cp.length; i++) {
            System.out.print("Entry " + i + ":");
            cp[i].print(this);
        }
        System.out.println("accessFlags = " + accessFlags);
        System.out.println("thisClass = " + thisClass);
        System.out.println("SuperClass = " + superClass);
        InterfaceInfo.printInterfaces(interfaces);
        FieldInfo.printFields(fields);
        MethodInfo.printMethods(methods);
        AttributeInfo.printAttributes(attributes);
    }

    /* Checks for Verification Pass 2 */
    public void Verify2() throws Exception {
        System.out.println("Verification Pass 2");

        /* check version numbers */
        if (minorVersion > RTC.JAVA_MINOR_VERSION) {
            System.out.println("Verification Pass 2: Bad minor version (1)");
            throw new VerifyError();
        }

        // Temporary hack so that java verifier can verify itself 
        if ((majorVersion < RTC.JAVA_VERSION) || (majorVersion > RTC.JAVA_CURRENT_VERSION)) {
            System.out.println("Verification Pass 2: Bad major version (1)");
            throw new VerifyError();
        }

        checkConstantPool();

        /* check access flags */
        accessFlags &= (RTC.ACC_PUBLIC|RTC.ACC_FINAL|RTC.ACC_SUPER|
                        RTC.ACC_INTERFACE|RTC.ACC_ABSTRACT);

        if ( (accessFlags & RTC.ACC_INTERFACE) != 0) {
            if ( ((accessFlags & RTC.ACC_ABSTRACT) == 0) ||
                 ((accessFlags & RTC.ACC_FINAL) != 0) ) {
                System.out.println("Verification Pass 2: Invalid access flag for class or interface (1)");     
                throw new VerifyError();
            }
        }
        if ( (accessFlags & RTC.ACC_INTERFACE) == 0) {
            if ( ((accessFlags & RTC.ACC_ABSTRACT) != 0) &&
                 ((accessFlags & RTC.ACC_FINAL) != 0) ) {
                System.out.println("Verification Pass 2: Invalid access flag for class or interface (2)");     
                throw new VerifyError();
            }
        }
  
        /* check thisClass & superClass */
        if ((thisClass <= 0) || (thisClass > (cp.length - 1))) {
            System.out.println("Verification Pass 2: Bad class index (1)");     
            throw new VerifyError();
        }
        if (cp[thisClass].tag != RTC.CONSTANT_CLASS) {
            System.out.println("Verification Pass 2: Bad class index (2)");     
            throw new VerifyError();
        }
        if ((superClass < 0) || (superClass > (cp.length - 1))) {
            System.out.println("Verification Pass 2: Bad superclass index (1)");    
            throw new VerifyError();
        }
        if (superClass != 0) {
            if (cp[superClass].tag != RTC.CONSTANT_CLASS) {
                System.out.println("Verification Pass 2: Bad superclass index (2)"); 
                throw new VerifyError();
            }
            ConstantUTF8Info cpInfo = (ConstantUTF8Info)cp[((ConstantClassInfo)
                                                            cp[superClass]).nameIndex];

            String className = cpInfo.ConstantString.replace('/', '.');
            if ( (Class.forName(className).getModifiers()
                  & RTC.ACC_FINAL) != 0 ) {
                System.out.println("Verification Pass 2: Bad superclass (1)");
                throw new VerifyError();
            }
        }
        else {
            ConstantUTF8Info cpInfo = (ConstantUTF8Info)cp[((ConstantClassInfo)
                                                            cp[thisClass]).nameIndex];

            if(cpInfo.ConstantString.compareTo("java/lang/Object") != 0) {
                System.out.println("Verification Pass 2: Bad superclass (2)");     
                throw new VerifyError();
            }
        }
        if ( (accessFlags & RTC.ACC_INTERFACE) != 0 ) {
            if ((superClass <= 0) || (superClass > (cp.length - 1))) {
                System.out.println("Verification Pass 2: Bad superclass index (3)"); 
                throw new VerifyError();
            }
            ConstantUTF8Info cpInfo = (ConstantUTF8Info)cp[((ConstantClassInfo)
                                                            cp[superClass]).nameIndex];
            if(cpInfo.ConstantString.compareTo("java/lang/Object") != 0) {
                System.out.println("Verification Pass 2: Bad super class (3)");     
                throw new VerifyError();
            }
        }
   
        /* check interfaces */
        for (int i = 0; i < interfaces.length; i++) {
            int index = interfaces[i].info;
            if ((index <= 0) || (index > (cp.length - 1))) {
                System.out.println("Verification Pass 2: Bad interface index (1)");  
                throw new VerifyError();
            }
            if (cp[index].tag != RTC.CONSTANT_CLASS) {
                System.out.println("Verification Pass 2: Bad interface index (2)");  
                throw new VerifyError();
            }
        }

        checkFields();
    
        checkMethods();

        /* check attributes */
        int sourceFileAttrCount = 0;
        int attrCount = attributes.length;

        for (int j = 0; j < attrCount; j++) {
            AttributeInfo attr = attributes[j];
            if (attr.attributeName.equals("SourceFile")) {
                sourceFileAttrCount++;
                if (sourceFileAttrCount > 1) {
                    System.out.println("Verification Pass 2: Bad SourceFileAttribute count (1)");     
                    throw new VerifyError();
                }
                if (attr.attributeLength != 2) {
                    System.out.println("Verification Pass 2: Bad SourceFileAttribute length (1)");     
                    throw new VerifyError();
                }
                String sourceFileName = ((SourceFileAttributeInfo)attr).sourceFileName;
                int dotIndex = sourceFileName.indexOf('.');
        
                // Disable this check for now.
                // checkIdentifier(sourceFileName, 0, dotIndex);
                // String suffix = sourceFileName.substring(dotIndex);
                // if (!suffix.equals(".java")) {
                //  System.out.println("Verification Pass 2: Bad source file name (1)"); 
                //throw new VerifyError();
                //}
            }
        }
        System.out.println("Pass 2 Done");
    }

    /* Checks for Verification Pass 3 */
    public void Verify3() throws Exception  {
        System.out.println("Verification Pass 3");
        for (int i = 0; i < methods.length; i++)
            methods[i].Verify();
        System.out.println("Pass 3 Done");
    }
    public void Verify() throws Exception {
        //    Verify2();
        Verify3();
    }

    public void Optimize() {
        try {
            for (int i = 0; i < methods.length; i++)
                methods[i].Verify();
        } catch (Exception e)  {
            System.out.println("Foo");
        }
 
        System.out.println("Pass 3 Done");
    }
}

