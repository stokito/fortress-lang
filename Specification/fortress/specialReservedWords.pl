#!/usr/bin/perl -w
use strict;
################################################################################
#    Copyright 2009 Sun Microsystems, Inc.,
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

# Generates fortress-specialReservedWords.tex

my $file = '../../SpecData/tables/fortress-specialReservedWords';
open IN, "<$file";
open OUT, '>>fortress-specialReservedWords.tex';

print OUT "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
print OUT "%   Copyright 2009 Sun Microsystems, Inc.,\n";
print OUT "%   4150 Network Circle, Santa Clara, California 95054, U.S.A.\n";
print OUT "%   All rights reserved.\n%\n";
print OUT "%   U.S. Government Rights - Commercial software.\n";
print OUT "%   Government users are subject to the Sun Microsystems, Inc. standard\n";
print OUT "%   license agreement and applicable provisions of the FAR and its supplements.\n%\n";
print OUT "%   Use is subject to license terms.\n%\n";
print OUT "%   This distribution may include materials developed by third parties.\n%\n";
print OUT "%   Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered\n";
print OUT "%   trademarks of Sun Microsystems, Inc. in the U.S. and other countries.\n";
print OUT "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n%\n";
print OUT "%\n";
print OUT "% WARNING: THIS IS A MACHINE-GENERATED FILE. DO NOT EDIT DIRECTLY.\n";
print OUT "%\n% REGENERATE WITH \"./ant genSource\".\n";
print OUT "%\n";

print OUT "\\begin{tabular}{lllllll}\n";

my $line;
my $nlines = 0;

while ( $line = <IN> ) {
    $nlines += 1;
    print OUT "\\KWD{";
    chomp $line;
    $line =~ s/_/\\_/;
    print OUT $line;
    print OUT "}";
    if ( $nlines % 7 == 0 ) {
        print OUT " \\\\\n";
    } else {
        print OUT " & ";
    }
}

print OUT "\n\\end{tabular}";

close OUT;
