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

# Generates fortress-specialReservedWords.tex

# Working directory must be SPEC_HOME

# Input file comes from SPEC_HOME/Data/fortress-specialReservedWords
# Output file is SPEC_HOME/Generated/Data/fortress-specialReservedWords.tex

my $infile = 'Data/fortress-specialReservedWords';
my $outfile = 'Generated/Data/fortress-specialReservedWords.tex';

open IN, "<$infile";
open OUT, ">$outfile";

print OUT "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
print OUT "%   Copyright 2012, Oracle and/or its affiliates.\n";
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
