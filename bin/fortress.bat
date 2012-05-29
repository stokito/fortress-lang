@ECHO OFF
REM ################################################################################
REM #    Copyright 2008,2010, Oracle and/or its affiliates.
REM #    All rights reserved.
REM #
REM #
REM #    Use is subject to license terms.
REM #
REM #    This distribution may include materials developed by third parties.
REM #
REM ################################################################################
set SV=2.9.0
ECHO ON

if "%1"=="run" (java -Xms128m -Xmx512m -cp "%FORTRESS_HOME%/default_repository/caches/bytecode_cache;%FORTRESS_HOME%/default_repository/caches/bytecode_cache/*;%FORTRESS_HOME%/default_repository/caches/nativewrapper_cache;%FORTRESS_HOME%/ProjectFortress/build;%FORTRESS_HOME%/ProjectFortress/third_party/junit/junit.jar;%FORTRESS_HOME%/ProjectFortress/third_party/xtc/xtc.jar;%FORTRESS_HOME%/ProjectFortress/third_party/jsr166y/jsr166y.jar;%FORTRESS_HOME%/ProjectFortress/third_party/plt/plt.jar;%FORTRESS_HOME%/ProjectFortress/third_party/asm/asm-all-3.1.jar;%JAVA_HOME%/lib/tools.jar;%FORTRESS_HOME%/ProjectFortress/third_party/unsigned/unsigned.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-compiler-%SV%.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-library-%SV%.jar" com.sun.fortress.runtimeSystem.MainWrapper %2 %3 %4 %5 %6 %7 %8 %9) else (java -Xms128m -Xmx512m -cp "%FORTRESS_HOME%/ProjectFortress/build;%FORTRESS_HOME%/ProjectFortress/third_party/junit/junit.jar;%FORTRESS_HOME%/ProjectFortress/third_party/xtc/xtc.jar;%FORTRESS_HOME%/ProjectFortress/third_party/jsr166y/jsr166y.jar;%FORTRESS_HOME%/ProjectFortress/third_party/plt/plt.jar;%FORTRESS_HOME%/ProjectFortress/third_party/asm/asm-all-3.1.jar;%JAVA_HOME%/lib/tools.jar;%FORTRESS_HOME%/ProjectFortress/third_party/unsigned/unsigned.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-compiler-%SV%.jar;%FORTRESS_HOME%/ProjectFortress/third_party/scala/scala-library-%SV%.jar" com.sun.fortress.Shell %1 %2 %3 %4 %5 %6 %7 %8 %9)
