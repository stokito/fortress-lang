#!/bin/sh
cd `dirname $0`
rm printing/test_output.txt
../../bin/fortress run printing/ConcurrentPrinting.fss > printing/test_output.txt
cmp printing/test_output.txt printing/test_oracle.txt
