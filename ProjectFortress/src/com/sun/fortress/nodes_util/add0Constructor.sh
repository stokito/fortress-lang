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

      echo $FILE Adding visitor
      java -cp .. SpliceCode ../SAFE/$FILE > ${FILE} <<EOF
        ${i}(Span span) {
            super(span);
        }
EOF
done
