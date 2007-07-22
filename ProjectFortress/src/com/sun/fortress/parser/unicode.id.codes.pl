#!/usr/bin/perl -w
use strict;
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

# Generates Unicode.rats.
# http://www.unicode.org/Public/UNIDATA/UnicodeData.txt

my $file = '../../../../../../third_party/unicode/UnicodeData.500.txt';

open IN, "<$file";

my $line;
my @codesstart1 = ();
my @codesstart2 = ();
my @codesrest1 = ();
my @codesrest2 = ();
while ( $line = <IN> ) {
  $line =~ /^([0-9A-F]+);([^;]+);([^;]+);/;
  my ($code,$name,$category) = ($1,$2,$3);
  if ( $category =~ /L.|Nl/ ) {
    my $hex = hex $code;
    if ( $hex > 0xFFFF ) {
      push @codesstart2, $hex;
    } else {
      push @codesstart1, $hex;
    }
  } elsif ( $category =~ /Mn|Mc|Nd|Pc|Cf/ ) {
    my $hex = hex $code;
    if ( $hex > 0xFFFF ) {
      push @codesrest2, $hex;
    } else {
      push @codesrest1, $hex;
    }
  }
}

sub ranges1 {

  my @codes = sort { $a <=> $b } @_;
  my @output = ();

  while ( @codes ) {
    my $first = shift @codes;
    my $last = $first;
    while ( @codes && $codes[0] == $last + 1 ) {
      $last = shift @codes;
    }
    if ( $first == $last ) {
      push @output, sprintf("\\u%04x", $first);
    } else {
      push @output, sprintf("\\u%04x-\\u%04x", $first, $last);
    }
  }

  return @output;
}

sub front {
  return 0xD7C0 + ($_[0] >> 10);
}

sub back {
  return 0xDC00 | $_[0] & 0x3FF;
}

sub ranges2 {

  my @codes = sort { $a <=> $b } @_;
  my $header = 0;

  while ( @codes ) {
    my $first = shift @codes;
    my $last = $first;
    while ( @codes && $codes[0] == $last + 1 &&
	    front($codes[0]) == front($last + 1) ) {
      $last = shift @codes;
    }
    my $firstfront = front($first);
    my $firstback  = back($first);
    my $lastfront  = front($last);
    my $lastback   = back($last);
    if ( $first == $last ) {
      if ( $firstfront == $header ) {
        printf("\\u%04x", $firstback);
      } else {
        printf("]\n  / '\\u%04x'[\\u%04x", $firstfront, $firstback);
	$header = $firstfront;
      }
    } else {
      if ( $firstfront == $header ) {
        printf("\\u%04x-\\u%04x", $firstback, $lastback);
      } else { # $firstfront != $header
        printf("]\n  / '\\u%04x'[\\u%04x-\\u%04x",
	       $firstfront, $firstback, $lastback);
	$header = $firstfront;
      }
    }
  }
  print "];\n";
}

print "/*******************************************************************************\n";
print "    Copyright 2007 Sun Microsystems, Inc.,\n";
print "    4150 Network Circle, Santa Clara, California 95054, U.S.A.\n";
print "    All rights reserved.\n\n";
print "    U.S. Government Rights - Commercial software.\n";
print "    Government users are subject to the Sun Microsystems, Inc. standard\n";
print "    license agreement and applicable provisions of the FAR and its supplements.\n\n";
print "    Use is subject to license terms.\n\n";
print "    This distribution may include materials developed by third parties.\n\n";
print "    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered\n";
print "    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.\n";
print " ******************************************************************************/\n\n";
print "/*\n";
print " * Definition of Fortress Unicode characters.\n";
print " *\n * Automatically generated file: Please don't manually edit.\n";
print " */\n";
print "module Unicode;\n\n";
print "transient String UnicodeIdStart = [" . (join '', ranges1(@codesstart1));
ranges2(@codesstart2);
print "\n";
print "transient String UnicodeIdRest  = [" . (join '', ranges1(@codesrest1));
ranges2(@codesrest2);
