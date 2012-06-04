/*
 * Created on Aug 1, 2011
 *
 */
package com.sun.fortress.runtimeSystem;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.compiler.runtimeValues.RTTI;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.Useful;

public class RTHelpers {   
    static public Class getRTTIclass(String stem, RTTI[] params) {
        int lh_angle_index = stem.indexOf(Naming.LEFT_HEAVY_ANGLE);
        List<String> opr_params = null;
        if (lh_angle_index != -1 ) {
            // Not 100% sure of the mangling/dot status here. 
            // might be none because we are dealing with opr parameters.
            String dename = Naming.dotToSep(stem);
            dename = Naming.demangleFortressIdentifier(dename);
            int left = dename.indexOf(Naming.LEFT_HEAVY_ANGLE);
            int right = dename.lastIndexOf(Naming.RIGHT_HEAVY_ANGLE);
            stem = dename.substring(0,lh_angle_index);
            opr_params = RTHelpers.extractStringParameters(dename, left, right);
        }
        StringBuilder classNameBuf = new StringBuilder(stem + Naming.LEFT_OXFORD);
        int j = 0;
        for (int i = 0; i < params.length - 1; i++) {
            String pcl = params[i] == null ?
                    opr_params.get(j++) : params[i].className();
            classNameBuf.append(pcl + Naming.GENERIC_SEPARATOR);
        }
        {
        int i = params.length-1;
        String pcl = params[i] == null ?
                opr_params.get(j++) : params[i].className();
        classNameBuf.append(pcl + Naming.RIGHT_OXFORD);
        }
        
        String mangledClassName = Naming.mangleFortressIdentifier(classNameBuf.toString());
        String mangledDots = Naming.sepToDot(mangledClassName);
        
        try {
            return MainWrapper.icl.loadClass(mangledDots); //ONLY.loadClass(Naming.sepToDot(mangledClassName), false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("class " + mangledClassName.toString() + " failed to load");
        }
    }
    
    static public Class getRTTIclass(String stem, RTTI param1) {
        RTTI[] params = { param1 };
        return getRTTIclass(stem, params);
    }
    
    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2) {
        RTTI[] params = { param1, param2 };
        return getRTTIclass(stem, params);
    }
    
    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3) {
        RTTI[] params = { param1, param2, param3 };
        return getRTTIclass(stem, params);
    }
    
    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4) {
        RTTI[] params = { param1, param2, param3, param4 };
        return getRTTIclass(stem, params);
    }
    
    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4, RTTI param5) {
        RTTI[] params = { param1, param2, param3, param4, param5 };
        return getRTTIclass(stem, params);
    }

    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4, RTTI param5, RTTI param6) {
        RTTI[] params = { param1, param2, param3, param4, param5, param6 };
        return getRTTIclass(stem, params);
    }

    //generic method in non-generic trait
    static public Object findGenericMethodClosure(long l, BAlongTree t, String tcn, String sig) {
        if (InstantiatingClassloader.LOG_LOADS)
            System.err.println("findGenericMethodClosure("+l+", t, " + tcn +", " + sig +")");
    
        int up_index = tcn.indexOf(Naming.UP_INDEX);
        int envelope = tcn.indexOf(Naming.ENVELOPE); // Preceding char is RIGHT_OXFORD;
        int begin_static_params = tcn.indexOf(Naming.LEFT_OXFORD, up_index);
        // int gear_index = tcn.indexOf(Naming.GEAR);
        // String self_class = tcn.substring(0,gear_index) + tcn.substring(gear_index+1,up_index);
        
        String class_we_want = tcn.substring(0,begin_static_params+1) + // self_class + ";" +
            sig.substring(1) + tcn.substring(envelope);
        class_we_want = Naming.mangleFortressIdentifier(class_we_want);
        return RTHelpers.loadClosureClass(l, t, class_we_want);
    }

    //generic method in generic trait
    static public Object findGenericMethodClosure(long l, BAlongTree t,
            String tcn, String sig, String trait_sig) {
        if (InstantiatingClassloader.LOG_LOADS)
            System.err.println("findGenericMethodClosure("+l+", t, " + tcn +
                    ", " + sig +", " + trait_sig + ")");
    
        int up_index = tcn.indexOf(Naming.UP_INDEX);
        int envelope = tcn.indexOf(Naming.ENVELOPE); // Preceding char is RIGHT_OXFORD;
        int begin_static_params = tcn.indexOf(Naming.LEFT_OXFORD, up_index);
        // int gear_index = tcn.indexOf(Naming.GEAR);
        // String self_class = tcn.substring(0,gear_index) + tcn.substring(gear_index+1,up_index);
        
        String class_we_want = tcn.substring(0,begin_static_params+1) + // self_class + ";" +
            Useful.substring(sig,1,-1) + Naming.GENERIC_SEPARATOR + trait_sig.substring(1) + tcn.substring(envelope);
        class_we_want = Naming.mangleFortressIdentifier(class_we_want);
        return RTHelpers.loadClosureClass(l, t, class_we_want);
    }

    /**
     * @param l
     * @param t
     * @param class_we_want
     * @throws Error
     */
    static Object loadClosureClass(long l, BAlongTree t,
            String class_we_want) throws Error {
        Class cl;
        try {
            ClassLoader icl = MainWrapper.icl;
            cl = icl.loadClass(Naming.sepToDot(class_we_want));
            // cl = Class.forName(class_we_want, true, icl);
            synchronized (t) {
                Object o = t.get(l);
                if (o == null) {
                    o = cl.newInstance();
                    t.put(l,o);
                }
                return o;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + class_we_want + " failed to load (class not found).");
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Class " + class_we_want + " failed to load (instantiation exception).");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.err.println("Class " + class_we_want + " failed to load (illegal access).");
            e.printStackTrace();
        }
        
        throw new Error("Not supposed to happen; some template class must be missing.");
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI[] params) {
        //compute hashed key from RTTIs
        long hash = 0;
        for(int i = 0; i < params.length; i++) {
            hash = hash + params[i].getSN()*MagicNumbers.a(i);
        }
        
        //look in tree
        Object o = t.get(hash);
        if (o != null) 
            return o;
        
        //otherwise, get new instance by building class to get
        StringBuilder paramList = new StringBuilder(Naming.LEFT_OXFORD);
        for (int i = 0; i < params.length-1; i++) paramList.append(params[i].className() + Naming.GENERIC_SEPARATOR);
        paramList.append(params[params.length-1].className() + Naming.RIGHT_OXFORD);
        int insertLoc = stem.indexOf(Naming.LEFT_OXFORD + Naming.RIGHT_OXFORD);
        String className = stem.substring(0,insertLoc) + paramList.toString() + stem.substring(insertLoc+2);
        String class_we_want = Naming.sepToDot(Naming.mangleFortressIdentifier(className));
        
        //load closure class and save into table
        return loadClosureClass(hash,t,class_we_want);   
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI param1) {
        RTTI[] params = { param1 };
        Object cclass = loadClosureClass(t, stem, params); 
        return cclass;
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI param1, RTTI param2) {
        RTTI[] params = { param1, param2 };
        return loadClosureClass(t, stem, params);
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI param1, RTTI param2, RTTI param3) {
        RTTI[] params = { param1, param2, param3 };
        return loadClosureClass(t, stem, params);
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4) {
        RTTI[] params = { param1, param2, param3, param4 };
        return loadClosureClass(t, stem, params);
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4, RTTI param5) {
        RTTI[] params = { param1, param2, param3, param4, param5 };
        return loadClosureClass(t, stem, params);
    }

    public static Object loadClosureClass(BAlongTree t, String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4, RTTI param5, RTTI param6) {
        RTTI[] params = { param1, param2, param3, param4, param5, param6 };
        return loadClosureClass(t, stem, params);
    }

    /**
     * @param s
     * @param leftBracket
     * @param rightBracket
     * @return
     */
    static List<String> extractStringParameters(String s,
                                                             int leftBracket, int rightBracket) {
        
        ArrayList<String> parameters = new ArrayList<String>();
        return InstantiationMap.extractStringParameters(s, leftBracket, rightBracket, parameters);
    }

    public static List<String> extractStringParameters(String s) {
        int leftBracket = s.indexOf(Naming.LEFT_OXFORD);
        int rightBracket = InstantiationMap.templateClosingRightOxford(s);
        return extractStringParameters(s, leftBracket, rightBracket);
    }

    /**
     * @param string_constant
     */
    public static void symbolicLdc( MethodVisitor mv, String string_constant) {
        String loadString = Naming.opForString(Naming.stringMethod, string_constant);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.magicInterpClass, loadString, "()Ljava/lang/String;");
    }
}
