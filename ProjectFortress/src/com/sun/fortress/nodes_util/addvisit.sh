################################################################################
#    Copyright 2007 Sun Microsystems, Inc.,
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
for i in `grep -v Modifier leafclasses` ; do
    FILE=${i}.java
    if grep -q "for${i}" $FILE ; then
      echo $FILE Already has visitor
    else
      echo $FILE Adding visitor
      java -cp .. SpliceCode ../SAFE/$FILE > ${FILE} <<EOF
        <T> T accept(NodeVisitor<T> v) {
            return v.for${i}(this);
        }
EOF
    fi
done
