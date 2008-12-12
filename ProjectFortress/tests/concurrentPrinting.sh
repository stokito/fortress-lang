#! /bin/sh
./bin/fortress run ProjectFortress/tests/printing/ConcurrentPrinting.fss > ProjectFortress/tests/printing/test_output.txt
cmp ProjectFortress/tests/printing/test_output.txt ProjectFortress/tests/printing/test_oracle.txt
