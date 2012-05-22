#!/bin/sh

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

cd `dirname $0`
out=testDupInit/test_output.txt
rm -f $out
../../bin/fortress test testDupInit/testDupInit.fss > $out
if [ `grep -c '^' $out` -ne 1 ]; then
    echo "DUPLICATE INITIALIZATION"
    cat $out
    rm $out
    exit 1
fi
rm $out
exit 0
