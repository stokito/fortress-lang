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
out=helloCompiled-output.txt
chk=helloCompiled-expected.txt
rm -f $out $chk
# ../../bin/fortress typecheck -compiler-lib ../helloCompiled.fss || exit 1
# ../../bin/fortress compile -compiler-lib ../helloCompiled.fss || exit 2
../../bin/fortress run -compiler-lib ../helloCompiled.fss > $out || exit 3
echo "Hello, World!" > $chk
cmp $out $chk || exit 4
rm -f $out $chk
