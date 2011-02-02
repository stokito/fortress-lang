################################################################################
#    Copyright 2009, Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
################################################################################

BEGIN {
 file="DontCount.names";
 while (getline name < file) {
   names[name] = 1;
 }
}
{if (! names[$1]) print $1;}
