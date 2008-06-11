#!/usr/bin/perl
if($#ARGV == -1) {
    die "Error: must supply a version number for merge.";
}

$version = shift @ARGV;
$command = "svn merge -r$version:HEAD https://projectfortress.sun.com/svn/Community/trunk";
print $command . "\n";
system $command;

