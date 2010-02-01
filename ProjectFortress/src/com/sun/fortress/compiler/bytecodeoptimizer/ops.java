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

class ops {
  static void binaryDoubleOpHelper(aiContext c){
    TypeState op1a = c.s.pop();
    TypeState op1b = c.s.pop();
    TypeState op2a = c.s.pop();
    TypeState op2b = c.s.pop();

    if ((op1a != TypeState.typeDouble2) |
	(op1b != TypeState.typeDouble) |
	(op2a != TypeState.typeDouble2) |
	(op2b != TypeState.typeDouble))
      throw new VerifyError("binaryDoubleOpHelper Error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }
  
  static void binaryLongOpHelper(aiContext c){
    TypeState op1a = c.s.pop();
    TypeState op1b = c.s.pop();
    TypeState op2a = c.s.pop();
    TypeState op2b = c.s.pop();

    if ((op1a != TypeState.typeLong2) |
	(op1b != TypeState.typeLong) |
	(op2a != TypeState.typeLong2) |
	(op2b != TypeState.typeLong))
      throw new VerifyError("binaryLongOpHelper Error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }
  
  static void binaryIntegerOpHelper(aiContext c) {
    TypeState op1 = c.s.pop();
    TypeState op2 = c.s.pop();
    if ((op1 != TypeState.typeInteger) |
	(op2 != TypeState.typeInteger))
      throw new VerifyError("binaryIntegerOpHelper Error");
    c.s.push(TypeState.typeInteger);
  }

  static void binaryFloatOpHelper(aiContext c) {
    TypeState op1 = c.s.pop();
    TypeState op2 = c.s.pop();
    if ((op1 != TypeState.typeFloat) |
	(op2 != TypeState.typeFloat))
      throw new VerifyError("binaryFloatOpHelper Error");
    c.s.push(TypeState.typeFloat);
  }

  static void opaaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	(!arrayref.isArrayReference()))
      throw new VerifyError("opaaload error");
    TypeState elem = arrayref.aref.elems;
    c.s.push(elem);
  }
  
  static void opaastore(aiContext c) {
    TypeState value = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((!value.isReference()) |
	(index != TypeState.typeInteger) |
	(!arrayref.isArrayReference()))
      throw new VerifyError("opaastore error");
  }

  static void opaconst_null(aiContext c) {
    c.s.push(TypeState.typeNull);
  }

  static void opaload(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState ref = c.l.read(index);
    if (!ref.isReference())
      throw new VerifyError("opaload error");
    c.s.push(ref);
  }

  static void opaload_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState ref = c.l.read(index);
    if (!ref.isReference())
      throw new VerifyError("opaload error");
    c.s.push(ref);
  }

  static void opaload_n(aiContext c, int index) {
    TypeState ref = c.l.read(index);
    if (!ref.isReference())
      throw new VerifyError("opaload_n error");
    c.s.push(ref);
  }

  static void opanewarray(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    TypeState count = c.s.pop();
    if (count != TypeState.typeInteger)
      throw new VerifyError("opanewarray error");
    TypeState refType = 
      TypeState.typeRef(c.cls.cp[index].getClassName(c.cls), true);
    c.s.push(TypeState.typeArrayRef(refType));
  }
  
  static void opareturn(aiContext c) {
    TypeState objectref = c.s.pop();
    if (!objectref.isReference())
      throw new VerifyError("opareturn error");
  }
  
  static void oparraylength(aiContext c) {
    TypeState arrayRef = c.s.pop();
    if (!arrayRef.isArrayReference())
      throw new VerifyError("oparraylength error");
    c.s.push(TypeState.typeInteger);
  }

  static void opastore(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState objectref = c.s.pop();
    if ((!objectref.isReference()) &
	(!objectref.isReturnAddr()))
      throw new VerifyError("opastore error");
    c.l.store(objectref, index);
  }

  static void opastore_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState objectref = c.s.pop();
    if (!objectref.isReference())
      throw new VerifyError("opastore error");
    c.l.store(objectref, index);
  }

  static void opastore_n(aiContext c, int i) {
    TypeState objectref = c.s.pop();
    if (!objectref.isReference())
      throw new VerifyError("opastore_n error");
    c.l.store(objectref, i);
  }
  
  static void opathrow(aiContext c) {
    TypeState objectref = c.s.pop();
    if (!objectref.isReference())
      throw new VerifyError("opathrow error");
  }

  static void opbaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	((arrayref != TypeState.typeBoolArray) &
	 ((arrayref != TypeState.typeByteArray) &
	  (arrayref != TypeState.typeNull))))
      throw new VerifyError("opbaload error");
    c.s.push(TypeState.typeInteger);
  }

  static void opbastore(aiContext c) {
    TypeState value = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value != TypeState.typeInteger) |
	(index != TypeState.typeInteger) |
	((arrayref != TypeState.typeBoolArray) &
	 (arrayref != TypeState.typeByteArray)))
      throw new VerifyError("opbastore error");
  }

  static void opcaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	((arrayref != TypeState.typeCharArray) &
	 (arrayref != TypeState.typeNull)))
      throw new VerifyError("opcaload error");
    c.s.push(TypeState.typeInteger);
  }

  static void opcastore(aiContext c) {
    TypeState value = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value != TypeState.typeInteger) |
	(index != TypeState.typeInteger) |
	(arrayref != TypeState.typeCharArray))
      throw new VerifyError("opcastore error");
  }

  static void opcheckcast(aiContext c) {
    TypeState oldType = c.s.pop();
    int index = util.get16BitValue(c.Code, c.pc+1);
    TypeState newType = c.cls.cp[index].getConstantClass(c.cls);
    c.s.push(newType);
  }

  static void opd2f(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t2 != TypeState.typeDouble2) |
	(t1 != TypeState.typeDouble))
      throw new VerifyError("opd2f error");
    c.s.push(TypeState.typeFloat);
  }

  static void opd2i(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opd2i error");
    c.s.push(TypeState.typeInteger);
  }

  static void opd2l(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opd2l error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void opdadd(aiContext c) {
    binaryDoubleOpHelper(c);
  }

  static void opdaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	((arrayref.aref.elems != TypeState.typeDouble) &
	 (arrayref != TypeState.typeNull)))
      throw new VerifyError("opdaload error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }

  static void opdastore(aiContext c) {
    TypeState value1 = c.s.pop();
    TypeState value2 = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value1 != TypeState.typeDouble2) |
	(value2 != TypeState.typeDouble) |
	(index != TypeState.typeInteger) |
	(arrayref.aref.elems != TypeState.typeDouble))
      throw new VerifyError("opdastore error");
  }

  static void opdcmp(aiContext c) {
    TypeState first1 = c.s.pop();
    TypeState first2 = c.s.pop();
    TypeState second1 = c.s.pop();
    TypeState second2 = c.s.pop();
    if ((first1 != TypeState.typeDouble2) |
	(first2 != TypeState.typeDouble) |
	(second1 != TypeState.typeDouble2) |
	(second2 != TypeState.typeDouble))
      throw new VerifyError("opdcmp error");
    c.s.push(TypeState.typeInteger);
  }

  static void opdconst(aiContext c) {
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }
  
  static void opddiv(aiContext c) {
    binaryDoubleOpHelper(c);
  }

  static void opdload(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState t1 = c.l.read(index);
    TypeState t2 = c.l.read(index+1);
    if ((t1 != TypeState.typeDouble) |
	(t2 != TypeState.typeDouble2))
      throw new VerifyError("opdload error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }

  static void opdload_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState t1 = c.l.read(index);
    TypeState t2 = c.l.read(index+1);
    if ((t1 != TypeState.typeDouble) |
	(t2 != TypeState.typeDouble2))
      throw new VerifyError("opdload error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }

  static void opdload_n(aiContext c, int index) {
    TypeState t1 = c.l.read(index);
    TypeState t2 = c.l.read(index+1);
    if ((t1 != TypeState.typeDouble) |
	(t2 != TypeState.typeDouble2))
      throw new VerifyError("opdload_n error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }
  
  static void opdmul(aiContext c) {
    binaryDoubleOpHelper(c);
  }
  
  static void opdneg(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opdneg error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }
  
  static void opdrem(aiContext c) {
    binaryDoubleOpHelper(c);
  }

  static void opdreturn(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opdreturn error");
  }
  
  static void opdstore(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opdstore error");
    c.l.store(TypeState.typeDouble, index);
    c.l.store(TypeState.typeDouble2, index+1);
  }
  static void opdstore_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opdstore error");
    c.l.store(TypeState.typeDouble, index);
    c.l.store(TypeState.typeDouble2, index+1);
  }

  static void opdstore_n(aiContext c, int index) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeDouble2) |
	(t2 != TypeState.typeDouble))
      throw new VerifyError("opdstore_n error");
    c.l.store(TypeState.typeDouble, index);
    c.l.store(TypeState.typeDouble2, index+1);
  }

  static void opdsub(aiContext c) {
    binaryDoubleOpHelper(c);
  }

  static void opdup(aiContext c){
    TypeState ts = c.s.pop();
    c.s.push(ts);
    c.s.push(ts);
  }

  static void opdup_x1(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    c.s.push(t1); c.s.push(t2); c.s.push(t1);
  }

  static void opdup_x2(aiContext c) {
   TypeState t1 = c.s.pop();
   TypeState t2 = c.s.pop();
   TypeState t3 = c.s.pop();
   c.s.push(t1); c.s.push(t3); c.s.push(t2); c.s.push(t1);
  }
  
  static void opdup2(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    c.s.push(t2); c.s.push(t1); c.s.push(t2); c.s.push(t1);
  }

  static void opdup2_x1(aiContext c)  {
   TypeState t1 = c.s.pop();
   TypeState t2 = c.s.pop();
   TypeState t3 = c.s.pop();
   c.s.push(t2); c.s.push(t1); c.s.push(t3); c.s.push(t2); c.s.push(t1);
  }

  static void opdup2_x2(aiContext c) {
   TypeState t1 = c.s.pop();
   TypeState t2 = c.s.pop();
   TypeState t3 = c.s.pop();
   TypeState t4 = c.s.pop();
   c.s.push(t2); 
   c.s.push(t1); c.s.push(t4); c.s.push(t3); c.s.push(t2); c.s.push(t1);
  }

  static void opf2d(aiContext c) {
    if (c.s.pop() != TypeState.typeFloat)
      throw new VerifyError("opf2d error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }

  static void opf2i(aiContext c) {
    if (c.s.pop() != TypeState.typeFloat)
      throw new VerifyError("opf2i error");
    c.s.push(TypeState.typeInteger);
  }
  
  static void opf2l(aiContext c) {
    if (c.s.pop() != TypeState.typeFloat)
      throw new VerifyError("opf2l error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }
  
  static void opfadd(aiContext c) {binaryFloatOpHelper(c);}

  static void opfaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	((arrayref.aref.elems != TypeState.typeFloat) &
	 (arrayref != TypeState.typeNull)))
      throw new VerifyError("opfaload error");
    c.s.push(TypeState.typeFloat);
  }
  
  static void opfastore(aiContext c) {
    TypeState value = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value != TypeState.typeFloat) |
	(index != TypeState.typeInteger) |
	(arrayref.aref.elems != TypeState.typeFloat))
      throw new VerifyError("opfastore error");
  }
  
  static void opfcmp(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();

    if ((t1 != TypeState.typeFloat) |
	(t2 != TypeState.typeFloat))
      throw new VerifyError("opfcmp Error");
    c.s.push(TypeState.typeInteger);
  }
  
  static void opfconst(aiContext c) {
    c.s.push(TypeState.typeFloat);
  }

  static void opfdiv(aiContext c) {
    binaryFloatOpHelper(c);
  }

  static void opfload(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState t1 = c.l.read(index);
    if (t1 != TypeState.typeFloat)
      throw new VerifyError("opfload error");
    c.s.push(t1);
  }
  static void opfload_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState t1 = c.l.read(index);
    if (t1 != TypeState.typeFloat)
      throw new VerifyError("opfload error");
    c.s.push(t1);
  }

  static void opfload_n(aiContext c, int index) {
    TypeState t1 = c.l.read(index);
    if (t1 != TypeState.typeFloat)
      throw new VerifyError("opfload error");
    c.s.push(t1);
  }
  
  static void opfmul(aiContext c) {
    binaryFloatOpHelper(c);
  }
  
  static void opfneg(aiContext c) {
    TypeState ts = c.s.pop();
    if (ts != TypeState.typeFloat)
      throw new VerifyError("opfneg error");
    c.s.push(ts);
  }

  static void opfrem(aiContext c) {
    binaryFloatOpHelper(c);
  }

  static void opfreturn(aiContext c) {
  TypeState ts = c.s.pop();
  if (ts != TypeState.typeFloat) 
    throw new VerifyError("opfreturn error");
  }

  static void opfstore(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState ts = c.s.pop();

    if (ts != TypeState.typeFloat)
      throw new VerifyError("opfstore error");
    c.l.store(ts, index);
  }

  static void opfstore_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState ts = c.s.pop();

    if (ts != TypeState.typeFloat)
      throw new VerifyError("opfstore error");
    c.l.store(ts, index);
  }

  static void opfstore_n(aiContext c, int index) {
    TypeState ts = c.s.pop();

    if (ts != TypeState.typeFloat)
      throw new VerifyError("opfstore_n error");
    c.l.store(ts, index);
  }

  static void opfsub(aiContext c) {
    binaryFloatOpHelper(c);
  }

  static void opgetfield(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    TypeState objectref = c.s.pop();
    if (!objectref.isReference())
      throw new VerifyError("opgetfield error");
    TypeState[] cons = c.cls.cp[index].getConstantField(c.cls);
    for (int i = 0; i < cons.length; i++)
      c.s.push(cons[i]);
  }

  static void opgetstatic(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    TypeState[] cons = c.cls.cp[index].getConstantField(c.cls);
    for (int i = 0; i < cons.length; i++)
      c.s.push(cons[i]);
  }

  static void opi2d(aiContext c) {
    if (c.s.pop() != TypeState.typeInteger)
      throw new VerifyError("opi2d error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }

  static void opi2f(aiContext c) {
    if (c.s.pop() != TypeState.typeInteger)
      throw new VerifyError("opi2f error");
    c.s.push(TypeState.typeFloat);
  }
  
  static void opi2l(aiContext c) {
    if (c.s.pop() != TypeState.typeInteger)
      throw new VerifyError("opi2l error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }
  
  static void opiadd(aiContext c) {
    binaryIntegerOpHelper(c);
  }
  
  static void opiaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	((arrayref.aref.elems != TypeState.typeInteger) &
	 (arrayref != TypeState.typeNull)))
      throw new VerifyError("opiaload error");
    c.s.push(TypeState.typeInteger);
  }
  
  static void opiand(aiContext c) {
    binaryIntegerOpHelper(c);
  }

  static void opiastore(aiContext c) {
    TypeState value = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value != TypeState.typeInteger) |
	(index != TypeState.typeInteger) |
	(arrayref.aref.elems != TypeState.typeInteger))
      throw new VerifyError("opiastore error");
  }

  static void opiconst(aiContext c) {
    c.s.push(TypeState.typeInteger);
  }

  static void opidiv(aiContext c) {
    binaryIntegerOpHelper(c);
  }

  static void opif_acmp(aiContext c) {
    TypeState value1 = c.s.pop();
    TypeState value2 = c.s.pop();
    if ((!value1.isReference()) |
	(!value2.isReference()))
      throw new VerifyError("opif_acmp Error");
  }

  static void opif_icmp(aiContext c) {
    TypeState value1 = c.s.pop();
    TypeState value2 = c.s.pop();
    if ((value1 != TypeState.typeInteger) |
	(value2 != TypeState.typeInteger)) 
      throw new VerifyError("opif_icmp error");
  }

  static void opif(aiContext c) {
    TypeState value = c.s.pop();
    if (value.state != TypeState.typeInteger.state)
      throw new VerifyError("opif error");
  }

  static void opifnonnull(aiContext c) {
    TypeState value = c.s.pop();
    if (!value.isReference())
      throw new VerifyError("opifnonnull error");
  }
  
  static void opifnull(aiContext c) {
    TypeState value = c.s.pop();
    if (!value.isReference())
      throw new VerifyError("opifnonnull error");
  }
  
  static void opiinc(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    if (c.l.read(index) != TypeState.typeInteger)
      throw new VerifyError("opiincOp Error");
  }

  static void opiinc_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    if (c.l.read(index) != TypeState.typeInteger)
      throw new VerifyError("opiincOp Error");
  }
  
  static void opiload(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState t1 = c.l.read(index);
    if (t1 != TypeState.typeInteger)
      throw new VerifyError("opiload error");
    c.s.push(t1);
  }
  static void opiload_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState t1 = c.l.read(index);
    if (t1 != TypeState.typeInteger)
      throw new VerifyError("opiload error");
    c.s.push(t1);
  }

  static void opiload_n(aiContext c, int index) {
    TypeState t1 = c.l.read(index);
    if (t1 != TypeState.typeInteger)
      throw new VerifyError("opiload_n error");
    c.s.push(t1);
  }
  
  static void opimul(aiContext c) {
    binaryIntegerOpHelper(c);
  }
  
  static void opineg(aiContext c) {
    TypeState ts = c.s.pop();
    if (ts != TypeState.typeInteger)
      throw new VerifyError("opineg error");
    c.s.push(ts);
  }
  
  static void opinstanceof(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    if (!c.s.pop().isReference())
      throw new VerifyError("opinstanceof error");
    c.s.push(TypeState.typeInteger);
  }
  
  static void opinvokeinterface(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    String intfMethSig = c.cls.cp[index].getIntfMethodSig(c.cls);
    String intfMethName = c.cls.cp[index].getIntfMethodName(c.cls);
    TypeSig sig = new TypeSig(intfMethSig);
    for (int i = sig.argTypes.length - 1; i >= 0 ; i--) {
      //System.out.println("argType " + i + " = " + sig.argTypes[i].asString());
      TypeState argType = c.s.pop();
      if (!TypeState.Same(argType, sig.argTypes[i]))
	throw new VerifyError("invokeHelper error");
    }
    if (!c.s.pop().isReference())
	throw new VerifyError("invokeHelper error");
    if (sig.resultType != null) {
      for (int i = 0; i < sig.resultType.length; i++)
        c.s.push(sig.resultType[i]);
    }
  }
  
  static void opinvokespecial(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    String methodSig = c.cls.cp[index].getMethodSig(c.cls);
    String methodName = c.cls.cp[index].getMethodName(c.cls);
    TypeSig sig = new TypeSig(methodSig);
    //System.out.println("Method Name = " + methodName +
    //	       " Type Signature = " + 
    //	       methodSig + 
    //	       " arg count = " + 
    //	       sig.argTypes.length);
    for (int i = sig.argTypes.length - 1; i >= 0 ; i--) {
      //System.out.println("argType " + i + " = " + sig.argTypes[i].asString());
      TypeState argType = c.s.pop();
      if (!TypeState.Same(argType,sig.argTypes[i]))
	throw new VerifyError("invokeHelper error");
    }
    TypeState objectref = c.s.pop();
    if (!objectref.isReference())
      throw new VerifyError("invokeHelper error");
    if (sig.resultType != null)
      c.s.push(sig.resultType[0]);

    if (methodName.equals("<init>")) {
      objectref.ref.initialize(c.pc);
      //System.out.println("Initialize objectref");
    }
  }

  static void opinvokestatic(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    //System.out.println("index = " + index);
    String methodSig = c.cls.cp[index].getMethodSig(c.cls);
    //System.out.println("methodSig = " + methodSig);
    String methodName = c.cls.cp[index].getMethodName(c.cls);
    //System.out.println("methodName = " + methodName);
    TypeSig sig = new TypeSig(methodSig);
    //System.out.println("Type Signature = " + 
    //		       methodSig + 
    //	       " arg count = " + 
    //	       sig.argTypes.length);
    for (int i = sig.argTypes.length - 1; i >= 0 ; i--) {
      //System.out.println("argType " + i + " = " + sig.argTypes[i].asString());
      TypeState argType = c.s.pop();
      if (!TypeState.Same(argType,sig.argTypes[i]))
	throw new VerifyError("invokeHelper error");
    }
    if (sig.resultType != null) {
      for (int i = 0; i < sig.resultType.length; i++)
	c.s.push(sig.resultType[i]);
    }
  }

  static void opinvokevirtual(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    String methodSig = c.cls.cp[index].getMethodSig(c.cls);
    String methodName = c.cls.cp[index].getMethodName(c.cls);
    TypeSig sig = new TypeSig(methodSig);
    //System.out.println("Type Signature = " + 
    //		       methodSig + 
    //	       " arg count = " + 
    //	       sig.argTypes.length);
    for (int i = sig.argTypes.length - 1; i >= 0 ; i--) {
      //System.out.println("argType " + i + " = " + sig.argTypes[i].asString());
      TypeState argType = c.s.pop();
      if (!TypeState.Same(argType,sig.argTypes[i]))
	throw new VerifyError("invokeHelper error");
    }
    if (!c.s.pop().isReference())
	throw new VerifyError("invokeHelper error");
    if (sig.resultType != null)
      for (int i = 0; i < sig.resultType.length; i++)
	c.s.push(sig.resultType[i]);
  }

  static void opior(aiContext c) {
    binaryIntegerOpHelper(c);
  }
  
  static void opirem(aiContext c) {
    binaryIntegerOpHelper(c);
  }

  static void opireturn(aiContext c) {
    TypeState ts = c.s.pop();
    if (ts != TypeState.typeInteger) 
      throw new VerifyError("opireturn error");
  }
  
  static void opishl(aiContext c) {
    TypeState tint = c.s.pop();
    TypeState ts = c.s.pop();
    if ((tint != TypeState.typeInteger) |
	(ts != TypeState.typeInteger))
      throw new VerifyError("opishl error");
    c.s.push(ts);
  }

  static void opishr(aiContext c) {
  TypeState tint = c.s.pop();
    TypeState ts = c.s.pop();
    if ((tint != TypeState.typeInteger) |
	(ts != TypeState.typeInteger))
      throw new VerifyError("opishr error");
    c.s.push(ts);
  }
  
  static void opistore(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState ts = c.s.pop();

    if (ts != TypeState.typeInteger)
      throw new VerifyError("opistore error");
    c.l.store(ts, index);
  }
  static void opistore_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState ts = c.s.pop();

    if (ts != TypeState.typeInteger)
      throw new VerifyError("opistore error");
    c.l.store(ts, index);
  }
  
  static void opistore_n(aiContext c, int index) {
    TypeState ts = c.s.pop();

    if (ts != TypeState.typeInteger)
      throw new VerifyError("opistore_n error");
    c.l.store(ts, index);
  }
  
  static void opisub(aiContext c) {
    binaryIntegerOpHelper(c);
  }
  
  static void opixor(aiContext c) {
    binaryIntegerOpHelper(c);
  }

  static void opjsr(aiContext c) {
    int ra = util.get16BitValue(c.Code, c.pc+1);
    c.s.push(TypeState.typeReturnAddr(ra));
  }
  static void opjsr_w(aiContext c) {
    int ra = util.get32BitValue(c.Code, c.pc+1);
    c.s.push(TypeState.typeReturnAddr(ra));
  }

  static void opl2d(aiContext c) {
    TypeState l1 = c.s.pop();
    TypeState l2 = c.s.pop();
    if ((l1 != TypeState.typeLong2) |
	(l2 != TypeState.typeLong))
      throw new VerifyError("opl2d error");
    c.s.push(TypeState.typeDouble);
    c.s.push(TypeState.typeDouble2);
  }
  
  static void opl2f(aiContext c) {
    TypeState l1 = c.s.pop();
    TypeState l2 = c.s.pop();
    if ((l1 != TypeState.typeLong2) |
	(l2 != TypeState.typeLong))
      throw new VerifyError("opl2f error");
    c.s.push(TypeState.typeFloat);
  }
  
  static void opl2i(aiContext c) {
    TypeState l1 = c.s.pop();
    TypeState l2 = c.s.pop();
    if ((l1 != TypeState.typeLong2) |
	(l2 != TypeState.typeLong))
      throw new VerifyError("opl2i error");
    c.s.push(TypeState.typeInteger);
  }

  static void opladd(aiContext c) {
    binaryLongOpHelper(c);
  }

  static void oplaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeLong) |
	((arrayref.aref.elems != TypeState.typeLong) &
	 (arrayref != TypeState.typeNull)))
      throw new VerifyError("oplaload error");
    c.s.push(TypeState.typeLong);
  }

  static void opland(aiContext c) {
    binaryLongOpHelper(c);
  }

  static void oplastore(aiContext c) {
    TypeState value1 = c.s.pop();
    TypeState value2 = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value1 != TypeState.typeLong2) |
	(value2 != TypeState.typeLong) |
	(index != TypeState.typeInteger) |
	(arrayref.aref.elems != TypeState.typeLong))
      throw new VerifyError("oplastore error");
  }

  static void oplcmp(aiContext c) {
    TypeState a1 = c.s.pop();
    TypeState a2 = c.s.pop();
    TypeState b1 = c.s.pop();
    TypeState b2 = c.s.pop();
    if ((a1 != TypeState.typeLong2) |
	(a2 != TypeState.typeLong) |
	(b1 != TypeState.typeLong2) |
	(b2 != TypeState.typeLong))
      throw new VerifyError("oplcmp error");
    c.s.push(TypeState.typeInteger);
  }

  static void oplconst(aiContext c) {
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void opldc(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    ConstantPoolInfo cpi = c.cls.cp[index];
    c.s.push(cpi.getConstantEntry());
  }
  
  static void opldc_w(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    ConstantPoolInfo cpi = c.cls.cp[index];
    c.s.push(cpi.getConstantEntry());
  }

  static void opldc2_w(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    ConstantPoolInfo cpi = c.cls.cp[index];
    TypeState val[] = cpi.getWideConstantEntry();
    c.s.push(val[0]);
    c.s.push(val[1]);
  }
	     
  
  static void opldiv(aiContext c) {
    binaryLongOpHelper(c);
  }

  static void oplload(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState t1 = c.l.read(index);
    TypeState t2 = c.l.read(index+1);
    if ((t1 != TypeState.typeLong) |
	(t2 != TypeState.typeLong2))
      throw new VerifyError("oplload error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void oplload_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState t1 = c.l.read(index);
    TypeState t2 = c.l.read(index+1);
    if ((t1 != TypeState.typeLong) |
	(t2 != TypeState.typeLong2))
      throw new VerifyError("oplload error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void oplload_n(aiContext c, int index) {
    TypeState t1 = c.l.read(index);
    TypeState t2 = c.l.read(index+1);
    if ((t1 != TypeState.typeLong) |
	(t2 != TypeState.typeLong2))
      throw new VerifyError("oplload_n error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void oplmul(aiContext c) {
    binaryLongOpHelper(c);
  }
  
  static void oplneg(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeLong2) |
	(t2 != TypeState.typeLong))
      throw new VerifyError("oplneg error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void oplookupswitch(aiContext c) {
    TypeState ts = c.s.pop();
    if (ts != TypeState.typeInteger)
      throw new VerifyError("oplookupswitch error");
  }
  
  static void oplor(aiContext c) {
    binaryLongOpHelper(c);
  }
  
  static void oplrem(aiContext c) {
    binaryLongOpHelper(c);
  }

  static void oplreturn(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeLong2) |
	(t2 != TypeState.typeLong))
      throw new VerifyError("oplreturn error");
  }

  static void oplshl(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState tl1= c.s.pop();
    TypeState tl2 = c.s.pop();    

    if ((t1 != TypeState.typeInteger) |
	(tl1 != TypeState.typeLong2) |
	(tl2 != TypeState.typeLong))
      throw new VerifyError("oplshl Error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }

  static void oplshr(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState tl1 = c.s.pop();
    TypeState tl2 = c.s.pop();    

    if ((t1 != TypeState.typeInteger) |
	(tl1 != TypeState.typeLong2) |
	(tl2 != TypeState.typeLong))
      throw new VerifyError("oplshr Error");
    c.s.push(TypeState.typeLong);
    c.s.push(TypeState.typeLong2);
  }
  
  static void oplstore(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeLong2) |
	(t2 != TypeState.typeLong))
      throw new VerifyError("oplstore error");
    c.l.store(TypeState.typeLong, index);
    c.l.store(TypeState.typeLong2, index+1);
  }

  static void oplstore_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeLong2) |
	(t2 != TypeState.typeLong))
      throw new VerifyError("oplstore error");
    c.l.store(TypeState.typeLong, index);
    c.l.store(TypeState.typeLong2, index+1);
  }

  static void oplstore_n(aiContext c, int index) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    if ((t1 != TypeState.typeLong2) |
	(t2 != TypeState.typeLong))
      throw new VerifyError("oplstore_n error");
    c.l.store(TypeState.typeLong, index);
    c.l.store(TypeState.typeLong2, index+1);
  }

  static void oplsub(aiContext c) {
    binaryLongOpHelper(c);
  }

  static void oplxor(aiContext c) {
    binaryLongOpHelper(c);
  }

  static void opmonitorenter(aiContext c) {
    c.s.pop();
  }

  static void opmonitorexit(aiContext c) {
    c.s.pop();
  }

  static void opmultianewarray(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    int dims =  util.get8BitValue(c.Code, c.pc + 3);
    TypeState arr = c.cls.cp[index].getConstantClass(c.cls);
    for (int i = 0; i < dims; i++) {
      c.s.pop();
    }
    c.s.push(arr);
  }
  
  static void opnew(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    String name = c.cls.cp[index].getClassName(c.cls);
    c.s.push(TypeState.typeRef(name, false, c.pc));
  }

  static void opnewarray(aiContext c) {
    int aType = util.get8BitValue(c.Code, c.pc+1);
    if (c.s.pop() != TypeState.typeInteger)
      throw new VerifyError("opnewarray error");
    switch(aType) {
    case RTC.T_BOOLEAN: c.s.push(TypeState.typeBoolArray); break;
    case RTC.T_CHAR:    c.s.push(TypeState.typeCharArray); break;
    case RTC.T_FLOAT:   c.s.push(TypeState.typeFloatArray); break;
    case RTC.T_DOUBLE:  c.s.push(TypeState.typeDoubleArray); break;
    case RTC.T_BYTE:    c.s.push(TypeState.typeByteArray); break;
    case RTC.T_SHORT:   c.s.push(TypeState.typeShortArray); break;
    case RTC.T_INT:     c.s.push(TypeState.typeIntArray); break;
    case RTC.T_LONG:    c.s.push(TypeState.typeLongArray); break;
    }
  }

  static void opnop(aiContext c) {
  }

  static void oppop(aiContext c){ 
    c.s.pop();
  }

  static void oppop2(aiContext c) {
    TypeState ts = c.s.pop(); 
    if ((ts == TypeState.typeLong) |
	(ts == TypeState.typeDouble))
      return;
    else 
      c.s.pop();
  }
  
  static void opputfield(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    TypeState[] fieldTypes = c.cls.cp[index].getConstantField(c.cls);
    for (int i = fieldTypes.length - 1; i >= 0; i--)
      if (!TypeState.Same(c.s.pop(),fieldTypes[i]))
	throw new VerifyError("opputfield error");
    if (!c.s.pop().isReference())
	throw new VerifyError("opputfield error");
  }

  static void opputstatic(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+1);
    TypeState[] fieldTypes = c.cls.cp[index].getConstantField(c.cls);
    for (int i = 0; i < fieldTypes.length; i++)
      if (c.s.pop().state != fieldTypes[i].state)
	throw new VerifyError("opputstatic error");	
  }

  static void opret(aiContext c) {
    int index = util.get8BitValue(c.Code, c.pc+1);
    TypeState ra = c.l.read(index);
    if (!ra.isReturnAddr())
      throw new VerifyError("opret error");
  }

  static void opret_wide(aiContext c) {
    int index = util.get16BitValue(c.Code, c.pc+2);
    TypeState ra = c.l.read(index);
    if (!ra.isReturnAddr())
      throw new VerifyError("opret error");
  }

  static void opsaload(aiContext c) {
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();
    if ((index != TypeState.typeInteger) |
	((arrayref != TypeState.typeShortArray) &
	 (arrayref != TypeState.typeNull)))	 
      throw new VerifyError("opsaload error");
    c.s.push(TypeState.typeInteger);
  }

  static void opsastore(aiContext c) {
    TypeState value = c.s.pop();
    TypeState index = c.s.pop();
    TypeState arrayref = c.s.pop();

    if ((value != TypeState.typeInteger) |
	(index != TypeState.typeInteger) |
	(arrayref != TypeState.typeShortArray))
      throw new VerifyError("opsastore error");
  }

  static void opswap(aiContext c) {
    TypeState t1 = c.s.pop();
    TypeState t2 = c.s.pop();
    c.s.push(t2); c.s.push(t1);
  }

  static void optableswitch(aiContext c) {
    TypeState ts = c.s.pop();
    if (ts != TypeState.typeInteger)
      throw new VerifyError("optableswitch error");
  }

  static void opwide(aiContext c) {
    switch(c.Code[c.pc+1]) {
    case RTC.opc_iload:  opiload_wide(c); break;
    case RTC.opc_fload:  opfload_wide(c); break;
    case RTC.opc_aload:  opaload_wide(c); break;
    case RTC.opc_lload:  oplload_wide(c); break;
    case RTC.opc_dload:  opdload_wide(c); break;
    case RTC.opc_istore: opistore_wide(c); break;
    case RTC.opc_fstore: opfstore_wide(c); break;
    case RTC.opc_astore: opastore_wide(c); break;
    case RTC.opc_lstore: oplstore_wide(c); break;
    case RTC.opc_dstore: opdstore_wide(c); break;
    case RTC.opc_ret:    opret_wide(c); break;
    case RTC.opc_iinc:   opiinc_wide(c); break;
    }
  }

  static void opbreakpoint(aiContext c) {
    throw new VerifyError("breakpoint NYI");
  }

  static void Interpret(aiContext c) {
    //System.out.println("Interpret: pc = " + c.pc + 
    //	       " Code[pc] = " + RTC.opcNames[c.Code[c.pc]]);
    switch(c.Code[c.pc]) {
    case RTC.opc_nop:             opnop(c); break;
    case RTC.opc_aconst_null:     opaconst_null(c); break;
    case RTC.opc_iconst_m1:   
    case RTC.opc_iconst_0:
    case RTC.opc_iconst_1:
    case RTC.opc_iconst_2:
    case RTC.opc_iconst_3:
    case RTC.opc_iconst_4:
    case RTC.opc_iconst_5:        opiconst(c); break;
    case RTC.opc_lconst_0:
    case RTC.opc_lconst_1:        oplconst(c); break;
    case RTC.opc_fconst_0:
    case RTC.opc_fconst_1:
    case RTC.opc_fconst_2:        opfconst(c); break;
    case RTC.opc_dconst_0:
    case RTC.opc_dconst_1:        opdconst(c); break;
    case RTC.opc_bipush:          opiconst(c); break;
    case RTC.opc_sipush:          opiconst(c); break;
    case RTC.opc_ldc:             opldc(c); break;
    case RTC.opc_ldc_w:           opldc_w(c); break;
    case RTC.opc_ldc2_w:          opldc2_w(c); break;
    case RTC.opc_iload:           opiload(c); break;
    case RTC.opc_lload:           oplload(c); break;
    case RTC.opc_fload:           opfload(c); break;
    case RTC.opc_dload:           opdload(c); break;
    case RTC.opc_aload:           opaload(c); break;
    case RTC.opc_iload_0:         opiload_n(c, 0); break;
    case RTC.opc_iload_1:         opiload_n(c, 1); break;
    case RTC.opc_iload_2:         opiload_n(c, 2); break;
    case RTC.opc_iload_3:         opiload_n(c, 3); break;
    case RTC.opc_fload_0:         opfload_n(c, 0); break;
    case RTC.opc_fload_1:         opfload_n(c, 1); break;
    case RTC.opc_fload_2:         opfload_n(c, 2); break;
    case RTC.opc_fload_3:         opfload_n(c, 3); break;
    case RTC.opc_dload_0:         opdload_n(c, 0); break;
    case RTC.opc_dload_1:         opdload_n(c, 1); break;
    case RTC.opc_dload_2:         opdload_n(c, 2); break;
    case RTC.opc_dload_3:         opdload_n(c, 3); break;
    case RTC.opc_lload_0:         oplload_n(c, 0); break;
    case RTC.opc_lload_1:         oplload_n(c, 1); break;
    case RTC.opc_lload_2:         oplload_n(c, 2); break;
    case RTC.opc_lload_3:         oplload_n(c, 3); break;
    case RTC.opc_aload_0:         opaload_n(c, 0); break;
    case RTC.opc_aload_1:         opaload_n(c, 1); break;
    case RTC.opc_aload_2:         opaload_n(c, 2); break;
    case RTC.opc_aload_3:         opaload_n(c, 3); break;
    case RTC.opc_iaload:          opiaload(c); break;
    case RTC.opc_laload:          oplaload(c); break;
    case RTC.opc_faload:          opfaload(c); break;
    case RTC.opc_daload:          opdaload(c); break;
    case RTC.opc_aaload:          opaaload(c); break;
    case RTC.opc_baload:          opbaload(c); break;
    case RTC.opc_caload:          opcaload(c); break;
    case RTC.opc_saload:          opsaload(c); break;
    case RTC.opc_istore:          opistore(c); break;
    case RTC.opc_lstore:          oplstore(c); break;
    case RTC.opc_fstore:          opfstore(c); break;
    case RTC.opc_dstore:          opdstore(c); break;
    case RTC.opc_astore:          opastore(c); break;
    case RTC.opc_istore_0:        opistore_n(c, 0); break;
    case RTC.opc_istore_1:        opistore_n(c, 1); break;
    case RTC.opc_istore_2:        opistore_n(c, 2); break;
    case RTC.opc_istore_3:        opistore_n(c, 3); break;
    case RTC.opc_lstore_0:        oplstore_n(c, 0); break;
    case RTC.opc_lstore_1:        oplstore_n(c, 1); break;
    case RTC.opc_lstore_2:        oplstore_n(c, 2); break;
    case RTC.opc_lstore_3:        oplstore_n(c, 3); break;
    case RTC.opc_fstore_0:        opfstore_n(c, 0); break;
    case RTC.opc_fstore_1:        opfstore_n(c, 1); break;
    case RTC.opc_fstore_2:        opfstore_n(c, 2); break;
    case RTC.opc_fstore_3:        opfstore_n(c, 3); break;
    case RTC.opc_dstore_0:        opdstore_n(c, 0); break;
    case RTC.opc_dstore_1:        opdstore_n(c, 1); break;
    case RTC.opc_dstore_2:        opdstore_n(c, 2); break;
    case RTC.opc_dstore_3:        opdstore_n(c, 3); break;
    case RTC.opc_astore_0:        opastore_n(c, 0); break;
    case RTC.opc_astore_1:        opastore_n(c, 1); break;
    case RTC.opc_astore_2:        opastore_n(c, 2); break;
    case RTC.opc_astore_3:        opastore_n(c, 3); break;
    case RTC.opc_iastore:         opiastore(c); break;
    case RTC.opc_lastore:         oplastore(c); break;
    case RTC.opc_fastore:         opfastore(c); break;
    case RTC.opc_dastore:         opdastore(c); break;
    case RTC.opc_aastore:         opaastore(c); break;
    case RTC.opc_bastore:         opbastore(c); break;
    case RTC.opc_castore:         opcastore(c); break;
    case RTC.opc_sastore:         opsastore(c); break;
    case RTC.opc_pop:             oppop(c); break;
    case RTC.opc_pop2:            oppop2(c); break;
    case RTC.opc_dup:             opdup(c); break;
    case RTC.opc_dup_x1:          opdup_x1(c); break;
    case RTC.opc_dup_x2:          opdup_x2(c); break;
    case RTC.opc_dup2:            opdup2(c); break;
    case RTC.opc_dup2_x1:         opdup2_x1(c); break;
    case RTC.opc_dup2_x2:         opdup2_x2(c); break;
    case RTC.opc_swap:            opswap(c); break;
    case RTC.opc_iadd:            opiadd(c); break;
    case RTC.opc_ladd:            opladd(c); break;
    case RTC.opc_fadd:            opfadd(c); break;
    case RTC.opc_dadd:            opdadd(c); break;
    case RTC.opc_isub:            opisub(c); break;
    case RTC.opc_lsub:            oplsub(c); break;
    case RTC.opc_fsub:            opfsub(c); break;
    case RTC.opc_dsub:            opdsub(c); break;
    case RTC.opc_imul:            opimul(c); break;
    case RTC.opc_lmul:            oplmul(c); break;
    case RTC.opc_fmul:            opfmul(c); break;
    case RTC.opc_dmul:            opdmul(c); break;
    case RTC.opc_idiv:            opidiv(c); break;
    case RTC.opc_ldiv:            opldiv(c); break;
    case RTC.opc_fdiv:            opfdiv(c); break;
    case RTC.opc_ddiv:            opddiv(c); break;
    case RTC.opc_irem:            opirem(c); break;
    case RTC.opc_lrem:            oplrem(c); break;
    case RTC.opc_frem:            opfrem(c); break;
    case RTC.opc_drem:            opdrem(c); break;
    case RTC.opc_ineg:            opineg(c); break;
    case RTC.opc_lneg:            oplneg(c); break;
    case RTC.opc_fneg:            opfneg(c); break;
    case RTC.opc_dneg:            opdneg(c); break;
    case RTC.opc_ishl:            opishl(c); break;
    case RTC.opc_lshl:            oplshl(c); break;
    case RTC.opc_ishr:            opishr(c); break;
    case RTC.opc_lshr:            oplshr(c); break;
    case RTC.opc_iushr:           opishr(c); break;
    case RTC.opc_lushr:           oplshr(c); break;
    case RTC.opc_iand:            opiand(c); break;
    case RTC.opc_land:            opland(c); break;
    case RTC.opc_ior:             opior(c); break;
    case RTC.opc_lor:             oplor(c); break;
    case RTC.opc_ixor:            opixor(c); break;
    case RTC.opc_lxor:            oplxor(c); break;
    case RTC.opc_iinc:            opiinc(c); break;
    case RTC.opc_i2l:             opi2l(c); break;
    case RTC.opc_i2f:             opi2f(c); break;
    case RTC.opc_i2d:             opi2d(c); break;
    case RTC.opc_l2i:             opl2i(c); break;
    case RTC.opc_l2f:             opl2f(c); break;
    case RTC.opc_l2d:             opl2d(c); break;
    case RTC.opc_f2i:             opf2i(c); break;
    case RTC.opc_f2l:             opf2l(c); break;
    case RTC.opc_f2d:             opf2d(c); break;
    case RTC.opc_d2i:             opd2i(c); break;
    case RTC.opc_d2l:             opd2l(c); break;
    case RTC.opc_d2f:             opd2f(c); break;
    case RTC.opc_i2b:             break;
    case RTC.opc_i2c:             break;
    case RTC.opc_i2s:             break;
    case RTC.opc_lcmp:            oplcmp(c); break;
    case RTC.opc_fcmpl:
    case RTC.opc_fcmpg:           opfcmp(c); break;
    case RTC.opc_dcmpl:    
    case RTC.opc_dcmpg:           opdcmp(c); break;
    case RTC.opc_ifeq: 
    case RTC.opc_ifne:
    case RTC.opc_iflt:
    case RTC.opc_ifge:
    case RTC.opc_ifgt:
    case RTC.opc_ifle:            opif(c); break;
    case RTC.opc_if_icmpeq: 
    case RTC.opc_if_icmpne:
    case RTC.opc_if_icmplt:
    case RTC.opc_if_icmpge:
    case RTC.opc_if_icmpgt:
    case RTC.opc_if_icmple:       opif_icmp(c); break;
    case RTC.opc_if_acmpeq:
    case RTC.opc_if_acmpne:       opif_acmp(c); break;
    case RTC.opc_goto:            break;
    case RTC.opc_jsr:             opjsr(c); break;
    case RTC.opc_ret:             opret(c); break;
    case RTC.opc_tableswitch:     optableswitch(c); break;
    case RTC.opc_lookupswitch:    oplookupswitch(c); break; 
    case RTC.opc_ireturn:         opireturn(c); break;
    case RTC.opc_lreturn:         oplreturn(c); break;
    case RTC.opc_freturn:         opfreturn(c); break;
    case RTC.opc_dreturn:         opdreturn(c); break;
    case RTC.opc_areturn:         opareturn(c); break;
    case RTC.opc_return:          break;
    case RTC.opc_getstatic:       opgetstatic(c); break;
    case RTC.opc_putstatic:       opputstatic(c); break;
    case RTC.opc_getfield:        opgetfield(c); break;
    case RTC.opc_putfield:        opputfield(c); break;
    case RTC.opc_invokevirtual:   opinvokevirtual(c); break;
    case RTC.opc_invokespecial:   opinvokespecial(c); break;
    case RTC.opc_invokestatic:    opinvokestatic(c); break;
    case RTC.opc_invokeinterface: opinvokeinterface(c); break;
    case RTC.opc_xxxunusedxxx:    break;
    case RTC.opc_new:             opnew(c); break;
    case RTC.opc_newarray:        opnewarray(c); break;
    case RTC.opc_anewarray:       opanewarray(c); break;
    case RTC.opc_arraylength:     oparraylength(c); break;
    case RTC.opc_athrow:          opathrow(c); break;
    case RTC.opc_checkcast:       opcheckcast(c); break;
    case RTC.opc_instanceof:      opinstanceof(c); break;
    case RTC.opc_monitorenter:    opmonitorenter(c); break;
    case RTC.opc_monitorexit:     opmonitorexit(c); break;
    case RTC.opc_wide:            opwide(c); break;
    case RTC.opc_multianewarray:  opmultianewarray(c); break;
    case RTC.opc_ifnull:          opifnull(c); break;
    case RTC.opc_ifnonnull:       opifnonnull(c); break;
    case RTC.opc_goto_w:          break;
    case RTC.opc_jsr_w:           opjsr_w(c); break;
    case RTC.opc_breakpoint:      opbreakpoint(c); break;
    }
    //c.print();
    c.incpc();
  }
}
    

	       
