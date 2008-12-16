#!/bin/sh

################################################################################
#    Copyright 2008 Sun Microsystems, Inc.,
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
