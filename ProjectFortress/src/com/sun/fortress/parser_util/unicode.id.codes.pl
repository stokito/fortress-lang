#!/usr/bin/perl -w
use strict;
################################################################################
#    Copyright 2008,2010, Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
################################################################################

# Generates parser/Unicode.rats and parser_util/IdentifierUtil.java.
# http://www.unicode.org/Public/UNIDATA/UnicodeData.txt

my $file = '../../../../../third_party/unicode/UnicodeData.500.txt';

open IN, "<$file";
open RATS, '>>../parser/Unicode.rats';
open UTIL, '>>IdentifierUtil.java';

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
    if ( $name =~ /, First/ ) { # begin (range of characters)
      $line = <IN>;
      $line =~ /^([0-9A-F]+);([^;]+);([^;]+);/;
      my $last = hex $1;
      for (my($i) = $hex; $i <= $last; $i++) {
        if ( $i > 0xFFFF ) {
          push @codesstart2, $i;
        } else {
          push @codesstart1, $i;
        }
      }
    } # end (range of characters)
    elsif ( $hex > 0xFFFF ) {
      push @codesstart2, $hex;
    } else {
      push @codesstart1, $hex;
    }
  } elsif ( $category =~ /Pc/ ) {
    my $hex = hex $code;
    if ( $hex == 0x005F ) {
      push @codesrest1, $hex;
    }
  } elsif ( $category =~ /Mn|Mc|Nd|Cf/ ) {
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
        printf RATS ("\\u%04x", $firstback);
      } else {
        printf RATS ("]\n  / '\\u%04x'[\\u%04x", $firstfront, $firstback);
	$header = $firstfront;
      }
    } else {
      if ( $firstfront == $header ) {
        printf RATS ("\\u%04x-\\u%04x", $firstback, $lastback);
      } else { # $firstfront != $header
        printf RATS ("]\n  / '\\u%04x'[\\u%04x-\\u%04x",
	       $firstfront, $firstback, $lastback);
	$header = $firstfront;
      }
    }
  }
  print RATS "];\n";
}

sub ranges2ValidId {

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
        printf UTIL ("\\u%04x", $firstback);
      } else {
        printf UTIL ("])|('\\u%04x'[\\u%04x", $firstfront, $firstback);
	$header = $firstfront;
      }
    } else {
      if ( $firstfront == $header ) {
        printf UTIL ("\\u%04x-\\u%04x", $firstback, $lastback);
      } else { # $firstfront != $header
        printf UTIL ("])|('\\u%04x'[\\u%04x-\\u%04x",
	       $firstfront, $firstback, $lastback);
	$header = $firstfront;
      }
    }
  }
  print UTIL "]";
}

print RATS "/*******************************************************************************\n";
print RATS "    Copyright 2008,2010, Oracle and/or its affiliates.\n";
print RATS "    All rights reserved.\n\n";
print RATS "    Use is subject to license terms.\n\n";
print RATS "    This distribution may include materials developed by third parties.\n\n";
print RATS " ******************************************************************************/\n\n";
print RATS "/*\n";
print RATS " * Definition of Fortress Unicode characters.\n";
print RATS " *\n * Automatically generated file: Please don't manually edit.\n";
print RATS " */\n";
print RATS "module com.sun.fortress.parser.Unicode;\n";
print RATS "\n";
print RATS "transient String UnicodeIdStart = [" . (join '', ranges1(@codesstart1));
ranges2(@codesstart2);
print RATS "\n";
print RATS "transient String UnicodeIdRest  = [" . (join '', ranges1(@codesrest1));
ranges2(@codesrest2);

close RATS;

print UTIL "/*******************************************************************************\n";
print UTIL "    Copyright 2008,2010, Oracle and/or its affiliates.\n";
print UTIL "    All rights reserved.\n\n";
print UTIL "    Use is subject to license terms.\n\n";
print UTIL "    This distribution may include materials developed by third parties.\n\n";
print UTIL " ******************************************************************************/\n\n";
print UTIL "/*\n";
print UTIL " * Utility function for Fortress Identifiers.\n";
print UTIL " *\n * Automatically generated file: Please don't manually edit.\n";
print UTIL " */\n";
print UTIL "package com.sun.fortress.parser_util;\n\n";
print UTIL "import com.sun.fortress.nodes_util.NodeUtil;\n\n";
print UTIL "public final class IdentifierUtil {\n";
print UTIL "  private static java.util.regex.Pattern idStart = java.util.regex.Pattern.compile(\"([_".(join '', ranges1(@codesstart1));
ranges2ValidId(@codesstart2);
print UTIL ")\");\n";
print UTIL "\n";
print UTIL "  private static java.util.regex.Pattern idRest = java.util.regex.Pattern.compile(\"(([_'".(join '', ranges1(@codesstart1));
ranges2ValidId(@codesstart2);
print UTIL ")|([".(join '', ranges1(@codesrest1));
ranges2ValidId(@codesrest2);
print UTIL "))*\");\n";
print UTIL "\n";
print UTIL "  public static boolean validId(String id) {\n";
print UTIL "    if (id.length() > 0) {\n";
print UTIL "      java.util.regex.Matcher idStartMatcher = idStart.matcher(\"\"+id.charAt(0));\n";
print UTIL "      java.util.regex.Matcher idRestMatcher = idRest.matcher(id.substring(1));\n";
print UTIL "      return (idStartMatcher.matches() && idRestMatcher.matches() &&\n";
print UTIL "              NodeUtil.validId(id));\n";
print UTIL "    }\n";
print UTIL "    return false;\n";
print UTIL "  }\n";
print UTIL "\n}\n";

close UTIL;
