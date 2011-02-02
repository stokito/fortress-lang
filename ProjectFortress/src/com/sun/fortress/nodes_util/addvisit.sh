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
