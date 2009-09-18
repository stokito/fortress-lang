#!/usr/bin/perl
# ################################################################################
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

use XML::Simple;
use IO::File;
use strict;

# Some special stuff happens if $debug > 1
my $debug=0;

my $path = $ENV{'FORTRESS_HOME'};
if (!$path) {
   print "FORTRESS_HOME must be set";
   exit;
 }

#print "$path\n";

chdir $path;

my $checkRev=3300;	 # Only examine files from versions greater than this.
my $checkDate = 2009;    # Only examine files in this year.
my $rootDir = $path;
my $copyright = "Copyright $checkDate"; # The message to look for.
my $maxlines = 10; 	 # The message must appear within this many lines of the top of the file.
my $ignoreThese = 'ant|fortress-keywords|UserDictionary|README.txt|testData|README$|\.fsg$|\.xml$|\_iml$|\.NW$|fortress.vim|\.ods|\.jar$|\.tic$|\.timing$|\.war$|\.zip$|\.fa$|\.tgz$|\/\.|^\.|^Sandbox';
my $tempFile = '/tmp/svnInfo.xml';

#
my $xmlfh;  		 # a filehandle for the XML output of the svn command.

if ($debug>1) {
   $xmlfh = new IO::File $tempFile;   # just read a local file on debug.
} else {
   $xmlfh = new IO::File 'svn info -R --xml $path |'; # Ask svn for info
   die("Can't get svn info") if (!$xmlfh);
}


my $xs1 = XML::Simple->new();
my $doc = $xs1->XMLin($xmlfh);   #  Read the XML filehandle

$xmlfh->close();		 # Done, close it.

foreach my $entry ((@{$doc->{entry}})){
     # Skip over...
     next if ( $entry->{kind} ne 'file' );  	# ...non-files
     next if ( $entry->{revision} < $checkRev );# ...old revisions
     next if ( $entry->{commit}->{date} !~ m/$checkDate/); # ...things before this year.
     next if ( $entry->{path} =~ m/$ignoreThese/ );    # ... file names to ignore

     # Scan the beginging of the file for the copyright message.
     my ($line,$cnt,$missingCopyRight);
     $missingCopyRight=1;
     $cnt=0;
     print "opening-> $rootDir/$entry->{path}\n" if ($debug);
     my $fh = new IO::File "$entry->{path}";
     warn ("Can't open $entry->{path}") if (!$fh);
     while (($line = <$fh> )|| ($cnt++<$maxlines)) {
       print $line if ($debug);
       if ($line =~ m/$copyright/) {
          $missingCopyRight=0;
          last;
	}
     }
     $fh->close();
     # If the copyRight wasn't found, print out the file name.
     if ($missingCopyRight) {
       print "$entry->{path}\n";
     }
  }
