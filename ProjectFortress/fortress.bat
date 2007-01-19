REM ################################################################################
REM #    Copyright 2007 Sun Microsystems, Inc.,
REM #    4150 Network Circle, Santa Clara, California 95054, U.S.A.
REM #    All rights reserved.
REM #
REM #    U.S. Government Rights - Commercial software.
REM #    Government users are subject to the Sun Microsystems, Inc. standard
REM #    license agreement and applicable provisions of the FAR and its supplements.
REM #
REM #    Use is subject to license terms.
REM #
REM #    This distribution may include materials developed by third parties.
REM #
REM #    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
REM #    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
REM ################################################################################

java -cp "build;third_party/junit/junit.jar;third_party/xtc/xtc.jar;third_party/FJ/concurrent.jar;third_party/dstm2/dstm2.jar;third_party/bcel/bcel-5.2.jar" com.sun.fortress.interpreter.drivers.fs %1 %2 %3 %4 %5 %6 %7 %8 %9
