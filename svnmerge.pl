#!/usr/bin/perl
$command = "svn merge -r1813:HEAD https://projectfortress.sun.com/svn/Community/trunk";
print $command . "\n";
system $command;

