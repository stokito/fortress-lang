/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.asmbytecodeoptimizer;

public class Opcodes {

    /* Signature Characters */
    public static final char   SIGC_VOID                  = 'V';
    public static final String SIG_VOID                   = "V";
    public static final char   SIGC_BOOLEAN               = 'Z';
    public static final String SIG_BOOLEAN                = "Z";
    public static final char   SIGC_BYTE                  = 'B';
    public static final String SIG_BYTE                   = "B";
    public static final char   SIGC_CHAR                  = 'C';
    public static final String SIG_CHAR                   = "C";
    public static final char   SIGC_SHORT                 = 'S';
    public static final String SIG_SHORT                  = "S";
    public static final char   SIGC_INT                   = 'I';
    public static final String SIG_INT                    = "I";
    public static final char   SIGC_LONG                  = 'J';
    public static final String SIG_LONG                   = "J";
    public static final char   SIGC_FLOAT                 = 'F';
    public static final String SIG_FLOAT                  = "F";
    public static final char   SIGC_DOUBLE                = 'D';
    public static final String SIG_DOUBLE                 = "D";
    public static final char   SIGC_ARRAY                 = '[';
    public static final String SIG_ARRAY                  = "[";
    public static final char   SIGC_CLASS                 = 'L';
    public static final String SIG_CLASS                  = "L";
    public static final char   SIGC_METHOD                = '(';
    public static final String SIG_METHOD                 = "(";
    public static final char   SIGC_ENDCLASS              = ';';
    public static final String SIG_ENDCLASS               = ";";
    public static final char   SIGC_ENDMETHOD             = ')';
    public static final String SIG_ENDMETHOD              = ")";
    public static final char   SIGC_PACKAGE               = '/';
    public static final String SIG_PACKAGE                = "/";

    /* Class File Constants */
    public static final int JAVA_MAGIC                   = 0xcafebabe;
    public static final int JAVA_VERSION                 = 45;

    /* Works on java 2 too */
    public static final int JAVA_2_VERSION               = 46;
    public static final int JAVA_CURRENT_VERSION         = 49;
    public static final int JAVA_MINOR_VERSION           = 3;

    /* Class File Limitations */
    public static final int MAX_CONSTANT_POOL_COUNT      = 65535;
    public static final int MAX_METHOD_BYTES             = 65535;
    public static final int MAX_METHOD_LOCVARS           = 65535;
    public static final int MAX_METHOD_COUNT             = 65535;
    public static final int MAX_FIELD_COUNT              = 65535;
    public static final int MAX_OPERAND_STACK_WORDS      = 65535;
    public static final int MAX_ARRAY_DIMENSIONS         = 255;
    public static final int MAX_METHOD_ARG_WORDS         = 255;

    /* Constant table */
    public static final int CONSTANT_UTF8                = 1;
    public static final int CONSTANT_UNICODE             = 2;
    public static final int CONSTANT_INTEGER             = 3;
    public static final int CONSTANT_FLOAT               = 4;
    public static final int CONSTANT_LONG                = 5;
    public static final int CONSTANT_DOUBLE              = 6;
    public static final int CONSTANT_CLASS               = 7;
    public static final int CONSTANT_STRING              = 8;
    public static final int CONSTANT_FIELD               = 9;
    public static final int CONSTANT_METHOD              = 10;
    public static final int CONSTANT_INTERFACEMETHOD     = 11;
    public static final int CONSTANT_NAMEANDTYPE         = 12;

    /* Access Flags */
    public static final int ACC_PUBLIC                   = 0x00000001;
    public static final int ACC_PRIVATE                  = 0x00000002;
    public static final int ACC_PROTECTED                = 0x00000004;
    public static final int ACC_STATIC                   = 0x00000008;
    public static final int ACC_FINAL                    = 0x00000010;
    public static final int ACC_SYNCHRONIZED             = 0x00000020;
    public static final int ACC_VOLATILE                 = 0x00000040;
    public static final int ACC_TRANSIENT                = 0x00000080;
    public static final int ACC_NATIVE                   = 0x00000100;
    public static final int ACC_INTERFACE                = 0x00000200;
    public static final int ACC_ABSTRACT                 = 0x00000400;
    public static final int ACC_SUPER                    = 0x00000020;

    /* Frame codes */
    public static final int F_NEW                        = -1;
    public static final int F_FULL                       = 0;
    public static final int F_APPEND                     = 1;
    public static final int F_CHOP                       = 2;
    public static final int F_SAME                       = 3;
    public static final int F_SAME1                      = 4;

    /* Type codes */
    public static final int T_BOOLEAN                    = 0x00000004;
    public static final int T_CHAR                       = 0x00000005;
    public static final int T_FLOAT                      = 0x00000006;
    public static final int T_DOUBLE                     = 0x00000007;
    public static final int T_BYTE                       = 0x00000008;
    public static final int T_SHORT                      = 0x00000009;
    public static final int T_INT                        = 0x0000000a;
    public static final int T_LONG                       = 0x0000000b;

    /* Opcodes */
    public static final int TRY                      = -3;
    public static final int DEAD                     = -2;
    public static final int LABEL                    = -1;
    public static final int NOP                      = 0;
    public static final int ACONST_NULL              = 1;
    public static final int ICONST_M1                = 2;
    public static final int ICONST_0                 = 3;
    public static final int ICONST_1                 = 4;
    public static final int ICONST_2                 = 5;
    public static final int ICONST_3                 = 6;
    public static final int ICONST_4                 = 7;
    public static final int ICONST_5                 = 8;
    public static final int LCONST_0                 = 9;
    public static final int LCONST_1                 = 10;
    public static final int FCONST_0                 = 11;
    public static final int FCONST_1                 = 12;
    public static final int FCONST_2                 = 13;
    public static final int DCONST_0                 = 14;
    public static final int DCONST_1                 = 15;
    public static final int BIPUSH                   = 16;
    public static final int SIPUSH                   = 17;
    public static final int LDC                      = 18;
    public static final int LDC_W                    = 19;
    public static final int LDC2_W                   = 20;
    public static final int ILOAD                    = 21;
    public static final int LLOAD                    = 22;
    public static final int FLOAD                    = 23;
    public static final int DLOAD                    = 24;
    public static final int ALOAD                    = 25;
    public static final int ILOAD_0                  = 26;
    public static final int ILOAD_1                  = 27;
    public static final int ILOAD_2                  = 28;
    public static final int ILOAD_3                  = 29;
    public static final int LLOAD_0                  = 30;
    public static final int LLOAD_1                  = 31;
    public static final int LLOAD_2                  = 32;
    public static final int LLOAD_3                  = 33;
    public static final int FLOAD_0                  = 34;
    public static final int FLOAD_1                  = 35;
    public static final int FLOAD_2                  = 36;
    public static final int FLOAD_3                  = 37;
    public static final int DLOAD_0                  = 38;
    public static final int DLOAD_1                  = 39;
    public static final int DLOAD_2                  = 40;
    public static final int DLOAD_3                  = 41;
    public static final int ALOAD_0                  = 42;
    public static final int ALOAD_1                  = 43;
    public static final int ALOAD_2                  = 44;
    public static final int ALOAD_3                  = 45;
    public static final int IALOAD                   = 46;
    public static final int LALOAD                   = 47;
    public static final int FALOAD                   = 48;
    public static final int DALOAD                   = 49;
    public static final int AALOAD                   = 50;
    public static final int BALOAD                   = 51;
    public static final int CALOAD                   = 52;
    public static final int SALOAD                   = 53;
    public static final int ISTORE                   = 54;
    public static final int LSTORE                   = 55;
    public static final int FSTORE                   = 56;
    public static final int DSTORE                   = 57;
    public static final int ASTORE                   = 58;
    public static final int ISTORE_0                 = 59;
    public static final int ISTORE_1                 = 60;
    public static final int ISTORE_2                 = 61;
    public static final int ISTORE_3                 = 62;
    public static final int LSTORE_0                 = 63;
    public static final int LSTORE_1                 = 64;
    public static final int LSTORE_2                 = 65;
    public static final int LSTORE_3                 = 66;
    public static final int FSTORE_0                 = 67;
    public static final int FSTORE_1                 = 68;
    public static final int FSTORE_2                 = 69;
    public static final int FSTORE_3                 = 70;
    public static final int DSTORE_0                 = 71;
    public static final int DSTORE_1                 = 72;
    public static final int DSTORE_2                 = 73;
    public static final int DSTORE_3                 = 74;
    public static final int ASTORE_0                 = 75;
    public static final int ASTORE_1                 = 76;
    public static final int ASTORE_2                 = 77;
    public static final int ASTORE_3                 = 78;
    public static final int IASTORE                  = 79;
    public static final int LASTORE                  = 80;
    public static final int FASTORE                  = 81;
    public static final int DASTORE                  = 82;
    public static final int AASTORE                  = 83;
    public static final int BASTORE                  = 84;
    public static final int CASTORE                  = 85;
    public static final int SASTORE                  = 86;
    public static final int POP                      = 87;
    public static final int POP2                     = 88;
    public static final int DUP                      = 89;
    public static final int DUP_X1                   = 90;
    public static final int DUP_X2                   = 91;
    public static final int DUP2                     = 92;
    public static final int DUP2_X1                  = 93;
    public static final int DUP2_X2                  = 94;
    public static final int SWAP                     = 95;
    public static final int IADD                     = 96;
    public static final int LADD                     = 97;
    public static final int FADD                     = 98;
    public static final int DADD                     = 99;
    public static final int ISUB                     = 100;
    public static final int LSUB                     = 101;
    public static final int FSUB                     = 102;
    public static final int DSUB                     = 103;
    public static final int IMUL                     = 104;
    public static final int LMUL                     = 105;
    public static final int FMUL                     = 106;
    public static final int DMUL                     = 107;
    public static final int IDIV                     = 108;
    public static final int LDIV                     = 109;
    public static final int FDIV                     = 110;
    public static final int DDIV                     = 111;
    public static final int IREM                     = 112;
    public static final int LREM                     = 113;
    public static final int FREM                     = 114;
    public static final int DREM                     = 115;
    public static final int INEG                     = 116;
    public static final int LNEG                     = 117;
    public static final int FNEG                     = 118;
    public static final int DNEG                     = 119;
    public static final int ISHL                     = 120;
    public static final int LSHL                     = 121;
    public static final int ISHR                     = 122;
    public static final int LSHR                     = 123;
    public static final int IUSHR                    = 124;
    public static final int LUSHR                    = 125;
    public static final int IAND                     = 126;
    public static final int LAND                     = 127;
    public static final int IOR                      = 128;
    public static final int LOR                      = 129;
    public static final int IXOR                     = 130;
    public static final int LXOR                     = 131;
    public static final int IINC                     = 132;
    public static final int I2L                      = 133;
    public static final int I2F                      = 134;
    public static final int I2D                      = 135;
    public static final int L2I                      = 136;
    public static final int L2F                      = 137;
    public static final int L2D                      = 138;
    public static final int F2I                      = 139;
    public static final int F2L                      = 140;
    public static final int F2D                      = 141;
    public static final int D2I                      = 142;
    public static final int D2L                      = 143;
    public static final int D2F                      = 144;
    public static final int I2B                      = 145;
    public static final int I2C                      = 146;
    public static final int I2S                      = 147;
    public static final int LCMP                     = 148;
    public static final int FCMPL                    = 149;
    public static final int FCMPG                    = 150;
    public static final int DCMPL                    = 151;
    public static final int DCMPG                    = 152;
    public static final int IFEQ                     = 153;
    public static final int IFNE                     = 154;
    public static final int IFLT                     = 155;
    public static final int IFGE                     = 156;
    public static final int IFGT                     = 157;
    public static final int IFLE                     = 158;
    public static final int IF_ICMPEQ                = 159;
    public static final int IF_ICMPNE                = 160;
    public static final int IF_ICMPLT                = 161;
    public static final int IF_ICMPGE                = 162;
    public static final int IF_ICMPGT                = 163;
    public static final int IF_ICMPLE                = 164;
    public static final int IF_ACMPEQ                = 165;
    public static final int IF_ACMPNE                = 166;
    public static final int GOTO                     = 167;
    public static final int JSR                      = 168;
    public static final int RET                      = 169;
    public static final int TABLESWITCH              = 170;
    public static final int LOOKUPSWITCH             = 171;
    public static final int IRETURN                  = 172;
    public static final int LRETURN                  = 173;
    public static final int FRETURN                  = 174;
    public static final int DRETURN                  = 175;
    public static final int ARETURN                  = 176;
    public static final int RETURN                   = 177;
    public static final int GETSTATIC                = 178;
    public static final int PUTSTATIC                = 179;
    public static final int GETFIELD                 = 180;
    public static final int PUTFIELD                 = 181;
    public static final int INVOKEVIRTUAL            = 182;
    public static final int INVOKESPECIAL            = 183;
    public static final int INVOKESTATIC             = 184;
    public static final int INVOKEINTERFACE          = 185;
    public static final int XXXUNUSEDXXX             = 186;
    public static final int NEW                      = 187;
    public static final int NEWARRAY                 = 188;
    public static final int ANEWARRAY                = 189;
    public static final int ARRAYLENGTH              = 190;
    public static final int ATHROW                   = 191;
    public static final int CHECKCAST                = 192;
    public static final int INSTANCEOF               = 193;
    public static final int MONITORENTER             = 194;
    public static final int MONITOREXIT              = 195;
    public static final int WIDE                     = 196;
    public static final int MULTIANEWARRAY           = 197;
    public static final int IFNULL                   = 198;
    public static final int IFNONNULL                = 199;
    public static final int GOTO_W                  = 200;
    public static final int JSR_W                    = 201;
    public static final int BREAKPOINT               = 202;

    /* Opcode Names */
    public static final String opcNames[] = {
	"nop",
	"aconst_null",
	"iconst_m1",
	"iconst_0",
	"iconst_1",
	"iconst_2",
	"iconst_3",
	"iconst_4",
	"iconst_5",
	"lconst_0",
	"lconst_1",
	"fconst_0",
	"fconst_1",
	"fconst_2",
	"dconst_0",
	"dconst_1",
	"bipush",
	"sipush",
	"ldc",
	"ldc_w",
	"ldc2_w",
	"iload",
	"lload",
	"fload",
	"dload",
	"aload",
	"iload_0",
	"iload_1",
	"iload_2",
	"iload_3",
	"lload_0",
	"lload_1",
	"lload_2",
	"lload_3",
	"fload_0",
	"fload_1",
	"fload_2",
	"fload_3",
	"dload_0",
	"dload_1",
	"dload_2",
	"dload_3",
	"aload_0",
	"aload_1",
	"aload_2",
	"aload_3",
	"iaload",
	"laload",
	"faload",
	"daload",
	"aaload",
	"baload",
	"caload",
	"saload",
	"istore",
	"lstore",
	"fstore",
	"dstore",
	"astore",
	"istore_0",
	"istore_1",
	"istore_2",
	"istore_3",
	"lstore_0",
	"lstore_1",
	"lstore_2",
	"lstore_3",
	"fstore_0",
	"fstore_1",
	"fstore_2",
	"fstore_3",
	"dstore_0",
	"dstore_1",
	"dstore_2",
	"dstore_3",
	"astore_0",
	"astore_1",
	"astore_2",
	"astore_3",
	"iastore",
	"lastore",
	"fastore",
	"dastore",
	"aastore",
	"bastore",
	"castore",
	"sastore",
	"pop",
	"pop2",
	"dup",
	"dup_x1",
	"dup_x2",
	"dup2",
	"dup2_x1",
	"dup2_x2",
	"swap",
	"iadd",
	"ladd",
	"fadd",
	"dadd",
	"isub",
	"lsub",
	"fsub",
	"dsub",
	"imul",
	"lmul",
	"fmul",
	"dmul",
	"idiv",
	"ldiv",
	"fdiv",
	"ddiv",
	"irem",
	"lrem",
	"frem",
	"drem",
	"ineg",
	"lneg",
	"fneg",
	"dneg",
	"ishl",
	"lshl",
	"ishr",
	"lshr",
	"iushr",
	"lushr",
	"iand",
	"land",
	"ior",
	"lor",
	"ixor",
	"lxor",
	"iinc",
	"i2l",
	"i2f",
	"i2d",
	"l2i",
	"l2f",
	"l2d",
	"f2i",
	"f2l",
	"f2d",
	"d2i",
	"d2l",
	"d2f",
	"i2b",
	"i2c",
	"i2s",
	"lcmp",
	"fcmpl",
	"fcmpg",
	"dcmpl",
	"dcmpg",
	"ifeq",
	"ifne",
	"iflt",
	"ifge",
	"ifgt",
	"ifle",
	"if_icmpeq",
	"if_icmpne",
	"if_icmplt",
	"if_icmpge",
	"if_icmpgt",
	"if_icmple",
	"if_acmpeq",
	"if_acmpne",
	"goto",
	"jsr",
	"ret",
	"tableswitch",
	"lookupswitch",
	"ireturn",
	"lreturn",
	"freturn",
	"dreturn",
	"areturn",
	"return",
	"getstatic",
	"putstatic",
	"getfield",
	"putfield",
	"invokevirtual",
	"invokespecial",
	"invokestatic",
	"invokeinterface",
	"xxxunusedxxx",
	"new",
	"newarray",
	"anewarray",
	"arraylength",
	"athrow",
	"checkcast",
	"instanceof",
	"monitorenter",
	"monitorexit",
	"wide",
	"multianewarray",
	"ifnull",
	"ifnonnull",
	"goto_w",
	"jsr_w",
	"breakpoint"
    };

    /* Opcode Lengths */
    public static final int opcLengths[] = {
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	2,
	3,
	2,
	3,
	3,
	2,
	2,
	2,
	2,
	2,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	2,
	2,
	2,
	2,
	2,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	3,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	2,
	99,
	99,
	1,
	1,
	1,
	1,
	1,
	1,
	3,
	3,
	3,
	3,
	3,
	3,
	3,
	5,
	0,
	3,
	2,
	3,
	1,
	1,
	3,
	3,
	1,
	1,
	0,
	4,
	3,
	3,
	5,
	5,
	1
    };

}

