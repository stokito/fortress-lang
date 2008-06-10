#!/usr/bin/perl
$command = "svn merge -r1812:HEAD https://projectfortress.sun.com/svn/Community/trunk";
print $command . "\n";
system $command;

