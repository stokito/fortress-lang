#!/bin/bash

################################################################################
#    Copyright 2010, Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
################################################################################

# Minor special-case acceleration here.
export FORTRESS_HOME="`${0%BytecodeOptimizeEverything.sh}fortress_home`"

for i in "$FORTRESS_HOME"/default_repository/caches/bytecode_cache/*.jar ; do
   echo Optimizing "$i"
   "$FORTRESS_HOME"/bin/BytecodeOptimize "$i"
done
