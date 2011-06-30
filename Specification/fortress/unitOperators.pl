#!/usr/bin/perl -w
use strict;
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

# Generates fortress-unitOperators.tex

my $file = '../../SpecData/tables/fortress-unitOperators';
open IN, "<$file";
open OUT, '>>fortress-unitOperators.tex';

print OUT "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
print OUT "%   Copyright 2009, Oracle and/or its affiliates.\n";
print OUT "%   All rights reserved.\n%\n";
print OUT "%   Use is subject to license terms.\n%\n";
print OUT "%   This distribution may include materials developed by third parties.\n%\n";
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
    print OUT "\\TYP{";
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
