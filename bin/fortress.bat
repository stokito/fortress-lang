@ECHO OFF
REM ################################################################################
REM #    Copyright 2010 Sun Microsystems, Inc.,
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
set SV=2.8.0.r21304
ECHO ON

if "%1"=="run" (java -Xms128m -Xmx512m -cp "%FORTRESS_HOME%/default_repository/caches/bytecode_cache;%FORTRESS_HOME%/default_repository/caches/bytecode_cache/*;%FORTRESS_HOME%/default_repository/caches/nativewrapper_cache;%FORTRESS_HOME%/ProjectFortress/build;%FORTRESS_HOME%/ProjectFortress/third_party/junit/junit.jar;%FORTRESS_HOME%/ProjectFortress/third_party/xtc/xtc.jar;%FORTRESS_HOME%/ProjectFortress/third_party/jsr166y/jsr166y.jar;%FORTRESS_HOME%/ProjectFortress/third_party/plt/plt.jar;%FORTRESS_HOME%/ProjectFortress/third_party/asm/asm-all-3.1.jar;%JAVA_HOME%/lib/tools.jar;%FORTRESS_HOME%/ProjectFortress/third_party/unsigned/unsigned.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-compiler-%SV%.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-library-%SV%.jar" com.sun.fortress.runtimeSystem.MainWrapper %2 %3 %4 %5 %6 %7 %8 %9) else (java -Xms128m -Xmx512m -cp "%FORTRESS_HOME%/ProjectFortress/build;%FORTRESS_HOME%/ProjectFortress/third_party/junit/junit.jar;%FORTRESS_HOME%/ProjectFortress/third_party/xtc/xtc.jar;%FORTRESS_HOME%/ProjectFortress/third_party/jsr166y/jsr166y.jar;%FORTRESS_HOME%/ProjectFortress/third_party/plt/plt.jar;%FORTRESS_HOME%/ProjectFortress/third_party/asm/asm-all-3.1.jar;%JAVA_HOME%/lib/tools.jar;%FORTRESS_HOME%/ProjectFortress/third_party/unsigned/unsigned.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-compiler-%SV%.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-library-%SV%.jar" com.sun.fortress.Shell %1 %2 %3 %4 %5 %6 %7 %8 %9)
