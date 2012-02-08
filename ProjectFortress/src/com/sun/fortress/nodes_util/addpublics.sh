#!/bin/bash -x

################################################################################
#    Copyright 2008, Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
################################################################################

for i in *.java ; do
  if grep -q "<T> T accept" ${i} ; then
     mv ${i} ${i}.old
     sed -e "s/<T> T accept/public <T> T accept/g" < ${i}.old > ${i}
  fi
done
