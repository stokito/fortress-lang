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
out=printing/test_output.txt
cmp=printing/test_oracle.txt
rm -f $out
../../bin/fortress run printing/ConcurrentPrinting.fss > $out
cmp $out $cmp
rm -f $out
