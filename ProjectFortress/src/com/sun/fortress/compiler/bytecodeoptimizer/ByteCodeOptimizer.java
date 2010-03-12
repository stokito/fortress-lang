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

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.ByteCodeWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.*;


class ByteCodeOptimizer {

    List<ClassToBeOptimized> classes = new ArrayList<ClassToBeOptimized>();    
    
    void readInClasses(String jarFile) {
        try {
            JarInputStream jario = new JarInputStream(new FileInputStream(jarFile));
            // How big can classes get?  10,000 is our limit for now.
            int bufsize = 10000;
            int bufchunk = 1000;
            JarEntry entry;
            int bytesread;

            while ((entry = jario.getNextJarEntry()) != null) {
                byte buf[] = new byte[bufsize];
                byte tempbuf[] = new byte[bufchunk];
                int bytepos = 0;
                int lastread = 0;

                while ((bytesread = jario.read(tempbuf, lastread, bufchunk)) > 0) {
                    System.arraycopy(tempbuf, 0, buf, bytepos, bytesread);
                    bytepos = bytepos + bytesread;
                }

                ClassToBeOptimized cls = new ClassToBeOptimized(entry.getName(), buf);
                cls.Print();
                classes.add(cls);
            }
        } catch (Exception e) {
            System.out.println("ClassFormatError:"  + e );
            e.printStackTrace();
        }

    }

    void NYI() {
        throw new RuntimeException("Not Yet Implemented");
    }


    void generateOpcode(MethodVisitor mv, ClassToBeOptimized c, int code[], int opcode, int len, int pc) {

        System.out.println("generateOpcode, pc = " + pc + "code[pc] = " + code[pc]);
        switch(opcode) {
        case 0: /* nop */ mv.visitInsn(opcode); break;
        case 1: /* aconst_null */ mv.visitInsn(opcode); break;
        case 2: /* iconst_m1 */mv.visitInsn(opcode); break;
        case 3: /* iconst_0 */ mv.visitInsn(opcode); break;
        case 4: /* iconst_1 */ mv.visitInsn(opcode); break;
        case 5: /* iconst_2 */ mv.visitInsn(opcode); break;
        case 6: /* iconst_3 */ mv.visitInsn(opcode); break;
        case 7: /* iconst_4 */ mv.visitInsn(opcode); break;
        case 8: /* iconst_5 */ mv.visitInsn(opcode); break;
        case 9: /* lconst_0 */ mv.visitInsn(opcode); break;
        case 10: /* lconst_1 */ mv.visitInsn(opcode); break;
        case 11: /* fconst_0 */ mv.visitInsn(opcode); break;
        case 12: /* fconst_1 */ mv.visitInsn(opcode); break;
        case 13: /* fconst_2 */ mv.visitInsn(opcode); break;
        case 14: /* dconst_0 */ mv.visitInsn(opcode); break;
        case 15: /* dconst_1 */ mv.visitInsn(opcode); break;
        case 16: /* bipush */ mv.visitIntInsn(opcode, code[pc++]); break;
        case 17: /* sipush */ mv.visitIntInsn(opcode, code[pc++]); break;
        case 18: /* ldc */ 
        case 19: /* ldc_w */ 
        case 20: /* ldc2_w */ NYI(); break;
        case 21: /* iload */ NYI(); break;
        case 22: /* lload */ NYI(); break;
        case 23: /* fload */ NYI(); break;
        case 24: /* dload */ NYI(); break;
        case 25: /* aload */ NYI(); break;
        case 26: /* iload_0 */ mv.visitVarInsn(opcode, 0); break;
        case 27: /* iload_1 */ mv.visitVarInsn(opcode, 1); break;
        case 28: /* iload_2 */ mv.visitVarInsn(opcode, 2); break;
        case 29: /* iload_3 */ mv.visitVarInsn(opcode, 3); break;
        case 30: /* lload_0 */ NYI(); break;
        case 31: /* lload_1 */ NYI(); break;
        case 32: /* lload_2 */ NYI(); break;
        case 33: /* lload_3 */ NYI(); break;
        case 34: /* fload_0 */ NYI(); break;
        case 35: /* fload_1 */ NYI(); break;
        case 36: /* fload_2 */ NYI(); break;
        case 37: /* fload_3 */ NYI(); break;
        case 38: /* dload_0 */ NYI(); break;
        case 39: /* dload_1 */ NYI(); break;
        case 40: /* dload_2 */ NYI(); break;
        case 41: /* dload_3 */ NYI(); break;
        case 42: /* aload_0 */ mv.visitVarInsn(Opcodes.ALOAD, 0); break;
        case 43: /* aload_1 */ mv.visitVarInsn(Opcodes.ALOAD, 1); break;
        case 44: /* aload_2 */ mv.visitVarInsn(Opcodes.ALOAD, 2); break;
        case 45: /* aload_3 */ mv.visitVarInsn(Opcodes.ALOAD, 3); break;
        case 46: /* iaload */ NYI(); break;
        case 47: /* laload */ NYI(); break;
        case 48: /* faload */ NYI(); break;
        case 49: /* daload */ NYI(); break;
        case 50: /* aaload */ NYI(); break;
        case 51: /* baload */ NYI(); break;
        case 52: /* caload */ NYI(); break;
        case 53: /* saload */ NYI(); break;
        case 54: /* istore */ NYI(); break;
        case 55: /* lstore */ NYI(); break;
        case 56: /* fstore */ NYI(); break;
        case 57: /* dstore */ NYI(); break;
        case 58: /* astore */ NYI(); break;
        case 59: /* istore_0 */ NYI(); break;
        case 60: /* istore_1 */ NYI(); break;
        case 61: /* istore_2 */ NYI(); break;
        case 62: /* istore_3 */ NYI(); break;
        case 63: /* lstore_0 */ NYI(); break;
        case 64: /* lstore_1 */ NYI(); break;
        case 65: /* lstore_2 */ NYI(); break;
        case 66: /* lstore_3 */ NYI(); break;
        case 67: /* fstore_0 */ NYI(); break;
        case 68: /* fstore_1 */ NYI(); break;
        case 69: /* fstore_2 */ NYI(); break;
        case 70: /* fstore_3 */ NYI(); break;
        case 71: /* dstore_0 */ NYI(); break;
        case 72: /* dstore_1 */ NYI(); break;
        case 73: /* dstore_2 */ NYI(); break;
        case 74: /* dstore_3 */ NYI(); break;
        case 75: /* astore_0 */ NYI(); break;
        case 76: /* astore_1 */ NYI(); break;
        case 77: /* astore_2 */ NYI(); break;
        case 78: /* astore_3 */ NYI(); break;
        case 79: /* iastore */ NYI(); break;
        case 80: /* lastore */ NYI(); break;
        case 81: /* fastore */ NYI(); break;
        case 82: /* dastore */ NYI(); break;
        case 83: /* aastore */ NYI(); break;
        case 84: /* bastore */ NYI(); break;
        case 85: /* castore */ NYI(); break;
        case 86: /* sastore */ NYI(); break;
        case 87: /* pop */ mv.visitInsn(opcode); break;
        case 88: /* pop2 */ mv.visitInsn(opcode); break;
        case 89: /* dup */ mv.visitInsn(opcode); break;
        case 90: /* dup_x1 */ NYI(); break;
        case 91: /* dup_x2 */ NYI(); break;
        case 92: /* dup2 */ NYI(); break;
        case 93: /* dup2_x1 */ NYI(); break;
        case 94: /* dup2_x2 */ NYI(); break;
        case 95: /* swap */ NYI(); break;
        case 96: /* iadd */ NYI(); break;
        case 97: /* ladd */ NYI(); break;
        case 98: /* fadd */ NYI(); break;
        case 99: /* dadd */ NYI(); break;
        case 100: /* isub */ NYI(); break;
        case 101: /* lsub */ NYI(); break;
        case 102: /* fsub */ NYI(); break;
        case 103: /* dsub */ NYI(); break;
        case 104: /* imul */ NYI(); break;
        case 105: /* lmul */ NYI(); break;
        case 106: /* fmul */ NYI(); break;
        case 107: /* dmul */ NYI(); break;
        case 108: /* idiv */ NYI(); break;
        case 109: /* ldiv */ NYI(); break;
        case 110: /* fdiv */ NYI(); break;
        case 111: /* ddiv */ NYI(); break;
        case 112: /* irem */ NYI(); break;
        case 113: /* lrem */ NYI(); break;
        case 114: /* frem */ NYI(); break;
        case 115: /* drem */ NYI(); break;
        case 116: /* ineg */ NYI(); break;
        case 117: /* lneg */ NYI(); break;
        case 118: /* fneg */ NYI(); break;
        case 119: /* dneg */ NYI(); break;
        case 120: /* ishl */ NYI(); break;
        case 121: /* lshl */ NYI(); break;
        case 122: /* ishr */ NYI(); break;
        case 123: /* lshr */ NYI(); break;
        case 124: /* iushr */ NYI(); break;
        case 125: /* lushr */ NYI(); break;
        case 126: /* iand */ NYI(); break;
        case 127: /* land */ NYI(); break;
        case 128: /* ior */ NYI(); break;
        case 129: /* lor */ NYI(); break;
        case 130: /* ixor */ NYI(); break;
        case 131: /* lxor */ NYI(); break;
        case 132: /* iinc */ NYI(); break;
        case 133: /* i2l */ NYI(); break;
        case 134: /* i2f */ NYI(); break;
        case 135: /* i2d */ NYI(); break;
        case 136: /* l2i */ NYI(); break;
        case 137: /* l2f */ NYI(); break;
        case 138: /* l2d */ NYI(); break;
        case 139: /* f2i */ NYI(); break;
        case 140: /* f2l */ NYI(); break;
        case 141: /* f2d */ NYI(); break;
        case 142: /* d2i */ NYI(); break;
        case 143: /* d2l */ NYI(); break;
        case 144: /* d2f */ NYI(); break;
        case 145: /* i2b */ NYI(); break;
        case 146: /* i2c */ NYI(); break;
        case 147: /* i2s */ NYI(); break;
        case 148: /* lcmp */ NYI(); break;
        case 149: /* fcmpl */ NYI(); break;
        case 150: /* fcmpg */ NYI(); break;
        case 151: /* dcmpl */ NYI(); break;
        case 152: /* dcmpg */ NYI(); break;
        case 153: /* ifeq */ System.out.println("ifeq" + code[pc+1] + "   " + code[pc+2]); NYI(); break;
        case 154: /* ifne */ NYI(); break;
        case 155: /* iflt */ NYI(); break;
        case 156: /* ifge */ NYI(); break;
        case 157: /* ifgt */ NYI(); break;
        case 158: /* ifle */ NYI(); break;
        case 159: /* if_icmpeq */ NYI(); break;
        case 160: /* if_icmpne */ NYI(); break;
        case 161: /* if_icmplt */ NYI(); break;
        case 162: /* if_icmpge */ NYI(); break;
        case 163: /* if_icmpgt */ NYI(); break;
        case 164: /* if_icmple */ NYI(); break;
        case 165: /* if_acmpeq */ NYI(); break;
        case 166: /* if_acmpne */ NYI(); break;
        case 167: /* goto */ NYI(); break;
        case 168: /* jsr */ NYI(); break;
        case 169: /* ret */ NYI(); break;
        case 170: /* tableswitch */ NYI(); break;
        case 171: /* lookupswitch */ NYI(); break;
        case 172: /* ireturn */ NYI(); break;
        case 173: /* lreturn */ NYI(); break;
        case 174: /* freturn */ NYI(); break;
        case 175: /* dreturn */ NYI(); break;
        case 176: /* areturn */ NYI(); break;
        case 177: /* return */  mv.visitInsn(Opcodes.RETURN); break;
        case 178: /* getstatic */ NYI(); break;
        case 179: /* putstatic */ NYI(); break;
        case 180: /* getfield */ generateGetField(mv, c, code[pc+2]); break;
        case 181: /* putfield */ generatePutField(mv, c, code[pc+2]); break;
        case 182: /* invokevirtual */ generateInvokeVirtual(mv, c, code[pc+2]); break;
        case 183: /* invokespecial */ generateInvokeSpecial(mv, c, code[pc+2]); break;
        case 184: /* invokestatic */ generateInvokeStatic(mv, c, code[pc+2]); break;
        case 185: /* invokeinterface */ NYI(); break;
        case 186: /* xxxunusedxxx */ NYI(); break;
        case 187: /* new */ generateNew(mv, c, code[pc+2]); break;
        case 188: /* newarray */ NYI(); break;
        case 189: /* anewarray */ NYI(); break;
        case 190: /* arraylength */ NYI(); break;
        case 191: /* athrow */ NYI(); break;
        case 192: /* checkcast */ NYI(); break;
        case 193: /* instanceof */ NYI(); break;
        case 194: /* monitorenter */ NYI(); break;
        case 195: /* monitorexit */ NYI(); break;
        case 196: /* wide */ NYI(); break;
        case 197: /* multianewarray */ NYI(); break;
        case 198: /* ifnull */ NYI(); break;
        case 199: /* ifnonnull */ NYI(); break;
        case 200: /* goto_w */ NYI(); break;
        case 201: /* jsr_w */ NYI(); break;
        case 202: /* breakpoint */ NYI(); break;
        default: throw new ClassFormatError();
        }
    }

    void generateNew(MethodVisitor mv, ClassToBeOptimized c, int index) {   
        ConstantClassInfo cci = (ConstantClassInfo) c.cp[index];
        ConstantUTF8Info className = (ConstantUTF8Info) c.cp[cci.nameIndex];
        mv.visitTypeInsn(Opcodes.NEW, className.ConstantString);
    }

    void generateGetField(MethodVisitor mv, ClassToBeOptimized c, int index) {   
        ConstantFieldInfo cfi = (ConstantFieldInfo) c.cp[index];
        ConstantClassInfo cci = (ConstantClassInfo) c.cp[cfi.classIndex];
        ConstantUTF8Info className = (ConstantUTF8Info) c.cp[cci.nameIndex];
        ConstantNameAndTypeInfo cnti = (ConstantNameAndTypeInfo) c.cp[cfi.nameAndTypeIndex];
        ConstantUTF8Info name = (ConstantUTF8Info) c.cp[cnti.name];
        ConstantUTF8Info type = (ConstantUTF8Info) c.cp[cnti.type];            
            
        mv.visitFieldInsn(Opcodes.GETFIELD, 
                          className.ConstantString, 
                          name.ConstantString, 
                          type.ConstantString);
    }

    void generatePutField(MethodVisitor mv, ClassToBeOptimized c, int index) {   
        ConstantFieldInfo cfi = (ConstantFieldInfo) c.cp[index];
        ConstantClassInfo cci = (ConstantClassInfo) c.cp[cfi.classIndex];
        ConstantUTF8Info className = (ConstantUTF8Info) c.cp[cci.nameIndex];
        ConstantNameAndTypeInfo cnti = (ConstantNameAndTypeInfo) c.cp[cfi.nameAndTypeIndex];
        ConstantUTF8Info name = (ConstantUTF8Info) c.cp[cnti.name];
        ConstantUTF8Info type = (ConstantUTF8Info) c.cp[cnti.type];            
            
        mv.visitFieldInsn(Opcodes.PUTFIELD, 
                          className.ConstantString, 
                          name.ConstantString, 
                          type.ConstantString);
    }

    void generateInvokeStatic(MethodVisitor mv, ClassToBeOptimized c, int index) {
            ConstantMethodInfo cmi = (ConstantMethodInfo) c.cp[index];
            ConstantClassInfo cci = (ConstantClassInfo) c.cp[cmi.classIndex];
            ConstantUTF8Info className = (ConstantUTF8Info) c.cp[cci.nameIndex];
            ConstantNameAndTypeInfo cnti = (ConstantNameAndTypeInfo) c.cp[cmi.nameAndTypeIndex];
            ConstantUTF8Info name = (ConstantUTF8Info) c.cp[cnti.name];
            ConstantUTF8Info type = (ConstantUTF8Info) c.cp[cnti.type];            
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, className.ConstantString,
                               name.ConstantString,
                               type.ConstantString);
    }

    void generateInvokeSpecial(MethodVisitor mv, ClassToBeOptimized c, int index) {
            ConstantMethodInfo cmi = (ConstantMethodInfo) c.cp[index];
            ConstantClassInfo cci = (ConstantClassInfo) c.cp[cmi.classIndex];
            ConstantUTF8Info className = (ConstantUTF8Info) c.cp[cci.nameIndex];
            ConstantNameAndTypeInfo cnti = (ConstantNameAndTypeInfo) c.cp[cmi.nameAndTypeIndex];
            ConstantUTF8Info name = (ConstantUTF8Info) c.cp[cnti.name];
            ConstantUTF8Info type = (ConstantUTF8Info) c.cp[cnti.type];            
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className.ConstantString,
                               name.ConstantString,
                               type.ConstantString);
    }

    void generateInvokeVirtual(MethodVisitor mv, ClassToBeOptimized c, int index) {
            ConstantMethodInfo cmi = (ConstantMethodInfo) c.cp[index];
            ConstantClassInfo cci = (ConstantClassInfo) c.cp[cmi.classIndex];
            ConstantUTF8Info className = (ConstantUTF8Info) c.cp[cci.nameIndex];
            ConstantNameAndTypeInfo cnti = (ConstantNameAndTypeInfo) c.cp[cmi.nameAndTypeIndex];
            ConstantUTF8Info name = (ConstantUTF8Info) c.cp[cnti.name];
            ConstantUTF8Info type = (ConstantUTF8Info) c.cp[cnti.type];            
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className.ConstantString,
                               name.ConstantString,
                               type.ConstantString);
    }

    void writeOutClasses(String jarFile) {
        try {
            JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(NamingCzar.optimizedcache + jarFile)));
 
            for (ClassToBeOptimized c : classes) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                cw.visitSource(c.sourceFileName, null);
                System.out.println("Class to be optimized = " + c.name);
                System.out.println("AccessFlags = " + c.accessFlags);
                System.out.println("superclass = " + c.cp[c.superClass].getClassName(c));

                cw.visit(Opcodes.V1_5, c.accessFlags, c.name, null,
                         c.cp[c.superClass].getClassName(c), 
                         null);

                for (FieldInfo f : c.fields) {
                    System.out.println("Field " + f);
                    System.out.println("AccessFlags " + c.accessFlags);
                    System.out.println("Name " + c.cp[f.nameIndex].getUtf8String());
                    System.out.println("Type Descriptor " + c.cp[f.descriptorIndex].getUtf8String());

                    cw.visitField(c.accessFlags,
                                  c.cp[f.nameIndex].getUtf8String(),
                                  c.cp[f.descriptorIndex].getUtf8String(),
                                  null, null);
                }

                for (MethodInfo m : c.methods) {
                    System.out.println("Method " + m);
                    System.out.println("AccessFlags " + c.accessFlags);
                    System.out.println("Class Name " + c.cp[m.nameIndex].getUtf8String());
                    System.out.println("Sig = " + c.cp[m.descriptorIndex].getUtf8String());

                    MethodVisitor mv = cw.visitMethod(c.accessFlags,
                                   c.cp[m.nameIndex].getUtf8String(),
                                   c.cp[m.descriptorIndex].getUtf8String(), 
                                   null,
                                   null);
                    mv.visitCode();
                    CodeAttributeInfo cai = null;
                    for (int i = 0; i < m.attributes.length; i++)
                        if (m.attributes[i] instanceof CodeAttributeInfo) {                        
                            cai = (CodeAttributeInfo) m.attributes[i];
                        }
                    if (cai == null) break;
                    int pc = 0;
                    while (pc < cai.codeLength) {
                        int opcode = cai.code[pc];
                        int len = util.getOpCodeLength(cai.code, pc);
                        generateOpcode(mv, c, cai.code, opcode, len, pc);
                        pc = pc + len;
                    }

                    mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
                    mv.visitEnd();                    
                }
                cw.visitEnd();
                System.out.println("jarfile = " + jarFile);
                ByteCodeWriter.writeJarredClass(jos, c.name, cw.toByteArray());
            }
            jos.close();

        } catch (Exception e) {
            System.out.println("RuhRohRaggy " + e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) 
    {
        ByteCodeOptimizer bco = new ByteCodeOptimizer();
        bco.readInClasses(args[0]);
        bco.writeOutClasses(args[0]);
    }

}

	
