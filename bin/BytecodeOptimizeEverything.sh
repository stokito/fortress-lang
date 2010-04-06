#!/bin/bash

################################################################################
#    Copyright 2010 Sun Microsystems, Inc.,
#    4150 Network Circle, Santa Clara, California 95054, U.S.A.
#    All rights reserved.
#
#    U.S. Government Rights - Commercial software.
#    Government users are subject to the Sun Microsystems, Inc. standard
#    license agreement and applicable provisions of the FAR and its supplements.
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
#    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
#    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
################################################################################

# Minor special-case acceleration here.
export FORTRESS_HOME="`${0%BytecodeOptimizeEverything.sh}fortress_home`"

for i in "$FORTRESS_HOME"/default_repository/caches/bytecode_cache/*.jar ; do
   echo Optimizing "$i"
   "$FORTRESS_HOME"/bin/BytecodeOptimize "$i"
done
